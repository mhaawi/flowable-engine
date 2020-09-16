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
package org.flowable.dmn.editor.converter;

import java.util.HashMap;
import java.util.Map;

public class StandaloneTestDmnConverterContext implements DmnJsonConverterContext {

    protected Map<String, String> decisionTableKeyToJsonStringMap = new HashMap<>();
    protected Map<String, String> decisionServiceKeyToJsonStringMap = new HashMap<>();

    @Override
    public Map<String, String> getDecisionTableKeyToJsonStringMap() {
        return decisionTableKeyToJsonStringMap;
    }
    @Override
    public Map<String, String> getDecisionServiceKeyToJsonStringMap() {
        return decisionServiceKeyToJsonStringMap;
    }
    @Override
    public String getDecisionTableModelKeyForDecisionTableModelId(String decisionTableModelId) {
        return null;
    }
    @Override
    public Map<String, String> getDecisionTableModelInfoForDecisionTableModelKey(String decisionTableModelKey) {
        return null;
    }
}
