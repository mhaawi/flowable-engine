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
package org.flowable.engine.impl.bpmn.parser.handler;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.ConditionalEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class IntermediateCatchEventParseHandler extends AbstractFlowNodeBpmnParseHandler<IntermediateCatchEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntermediateCatchEventParseHandler.class);

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return IntermediateCatchEvent.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, IntermediateCatchEvent intermediateCatchEvent) {
        EventDefinition eventDefinition = null;
        if (!intermediateCatchEvent.getEventDefinitions().isEmpty()) {
            eventDefinition = intermediateCatchEvent.getEventDefinitions().get(0);
        }

        if (eventDefinition == null) {

            Map<String, List<ExtensionElement>> extensionElements = intermediateCatchEvent.getExtensionElements();
            if (!extensionElements.isEmpty()) {
                List<ExtensionElement> eventTypeExtensionElements = intermediateCatchEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
                if (eventTypeExtensionElements != null && !eventTypeExtensionElements.isEmpty()) {
                    String eventTypeValue = eventTypeExtensionElements.get(0).getElementText();
                    if (StringUtils.isNotEmpty(eventTypeValue)) {
                        intermediateCatchEvent.setBehavior(bpmnParse.getActivityBehaviorFactory().createIntermediateCatchEventRegistryEventActivityBehavior(intermediateCatchEvent, eventTypeValue));
                        return;
                    }
                }
            }

            intermediateCatchEvent.setBehavior(bpmnParse.getActivityBehaviorFactory().createIntermediateCatchEventActivityBehavior(intermediateCatchEvent));

        } else {
            if (eventDefinition instanceof TimerEventDefinition || eventDefinition instanceof SignalEventDefinition || 
                            eventDefinition instanceof MessageEventDefinition || eventDefinition instanceof ConditionalEventDefinition) {

                bpmnParse.getBpmnParserHandlers().parseElement(bpmnParse, eventDefinition);

            } else {
                LOGGER.warn("Unsupported intermediate catch event type for event {}", intermediateCatchEvent.getId());
            }
        }
    }

}
