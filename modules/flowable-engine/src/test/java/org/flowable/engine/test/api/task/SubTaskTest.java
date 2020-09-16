/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.test.api.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 * @author Tijs Rademakers
 */
public class SubTaskTest extends PluggableFlowableTestCase {

    @Test
    public void testSubTask() {
        Task gonzoTask = taskService.newTask();
        gonzoTask.setName("gonzoTask");
        taskService.saveTask(gonzoTask);

        Task subTaskOne = taskService.newTask();
        subTaskOne.setName("subtask one");
        String gonzoTaskId = gonzoTask.getId();
        subTaskOne.setParentTaskId(gonzoTaskId);
        taskService.saveTask(subTaskOne);

        Task subTaskTwo = taskService.newTask();
        subTaskTwo.setName("subtask two");
        subTaskTwo.setParentTaskId(gonzoTaskId);
        taskService.saveTask(subTaskTwo);
        
        String subTaskId = subTaskOne.getId();
        assertThat(taskService.getSubTasks(subTaskId)).isEmpty();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskParentTaskId(subTaskId).list()).isEmpty();

        List<Task> subTasks = taskService.getSubTasks(gonzoTaskId);
        Set<String> subTaskNames = new HashSet<>();
        for (Task subTask : subTasks) {
            subTaskNames.add(subTask.getName());
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            Set<String> expectedSubTaskNames = new HashSet<>();
            expectedSubTaskNames.add("subtask one");
            expectedSubTaskNames.add("subtask two");

            assertThat(subTaskNames).isEqualTo(expectedSubTaskNames);

            List<HistoricTaskInstance> historicSubTasks = historyService.createHistoricTaskInstanceQuery().taskParentTaskId(gonzoTaskId).list();

            subTaskNames = new HashSet<>();
            for (HistoricTaskInstance historicSubTask : historicSubTasks) {
                subTaskNames.add(historicSubTask.getName());
            }

            assertThat(subTaskNames).isEqualTo(expectedSubTaskNames);
        }

        taskService.deleteTask(gonzoTaskId, true);
    }
    
    @Test
    public void testMakeSubTaskStandaloneTask() {
        Task parentTask = taskService.newTask();
        parentTask.setName("parent");
        taskService.saveTask(parentTask);

        Task subTaskOne = taskService.newTask();
        subTaskOne.setName("subtask one");
        subTaskOne.setParentTaskId(parentTask.getId());
        taskService.saveTask(subTaskOne);

        Task subTaskTwo = taskService.newTask();
        subTaskTwo.setName("subtask two");
        subTaskTwo.setParentTaskId(parentTask.getId());
        taskService.saveTask(subTaskTwo);

        assertThat(taskService.getSubTasks(parentTask.getId())).hasSize(2);
        
        if (processEngineConfiguration.getPerformanceSettings().isEnableTaskRelationshipCounts()) {
            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskService.createTaskQuery().taskId(parentTask.getId()).singleResult();
            assertThat(countingTaskEntity.getSubTaskCount()).isEqualTo(2);
        }
        
        subTaskTwo = taskService.createTaskQuery().taskId(subTaskTwo.getId()).singleResult();
        subTaskTwo.setParentTaskId(null);
        taskService.saveTask(subTaskTwo);
        
        if (processEngineConfiguration.getPerformanceSettings().isEnableTaskRelationshipCounts()) {
            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskService.createTaskQuery().taskId(parentTask.getId()).singleResult();
            assertThat(countingTaskEntity.getSubTaskCount()).isEqualTo(1);
        }
        
        assertThat(taskService.getSubTasks(parentTask.getId())).hasSize(1);
        taskService.deleteTask(parentTask.getId(), true);
        taskService.deleteTask(subTaskTwo.getId(), true);
    }

    @Test
    public void testSubTaskDeleteOnProcessInstanceDelete() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
                .deploy();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.setAssignee(task.getId(), "test");

        Task subTask1 = taskService.newTask();
        subTask1.setName("Sub task 1");
        subTask1.setParentTaskId(task.getId());
        subTask1.setAssignee("test");
        taskService.saveTask(subTask1);

        Task subTask2 = taskService.newTask();
        subTask2.setName("Sub task 2");
        subTask2.setParentTaskId(task.getId());
        subTask2.setAssignee("test");
        taskService.saveTask(subTask2);

        List<Task> tasks = taskService.createTaskQuery().taskAssignee("test").list();
        assertThat(tasks).hasSize(3);

        runtimeService.deleteProcessInstance(processInstance.getId(), "none");

        tasks = taskService.createTaskQuery().taskAssignee("test").list();
        assertThat(tasks).isEmpty();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().taskAssignee("test").list();
            assertThat(historicTasks).hasSize(3);

            historyService.deleteHistoricProcessInstance(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            historicTasks = historyService.createHistoricTaskInstanceQuery().taskAssignee("test").list();
            assertThat(historicTasks).isEmpty();
        }

        repositoryService.deleteDeployment(deployment.getId(), true);
        managementService.executeCommand(commandContext -> {
            processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(subTask1.getId());
            processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(subTask2.getId());
            return null;
        });
    }

}
