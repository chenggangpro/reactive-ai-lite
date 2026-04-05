/*
 *    Copyright 2025-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package pro.chenggang.project.reactive.ai.lite.core.spec;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigurableChatSpecTest {

    @Test
    void testDefaultMethods() {
        ConfigurableChatSpec spec = mock(ConfigurableChatSpec.class);
        
        when(spec.model(any(String.class))).thenCallRealMethod();
        when(spec.temperature(any(Double.class))).thenCallRealMethod();
        when(spec.topP(any(Double.class))).thenCallRealMethod();
        when(spec.includeUsage()).thenCallRealMethod();
        when(spec.reasoning(any(String.class))).thenCallRealMethod();
        when(spec.systemMessage(any(String.class))).thenCallRealMethod();
        when(spec.historicalMessage(any(List.class))).thenCallRealMethod();
        when(spec.textMessage(any(String.class))).thenCallRealMethod();
        when(spec.mediaMessage(any(String.class), any(List.class))).thenCallRealMethod();
        when(spec.maxCompletionTokens(any(Integer.class))).thenCallRealMethod();
        when(spec.tools(any(List.class))).thenCallRealMethod();
        when(spec.toolChoice(any(String.class))).thenCallRealMethod();
        when(spec.toolsResponse(any(List.class))).thenCallRealMethod();

        spec.model("test-model");
        verify(spec).model(any(java.util.function.Function.class));

        spec.temperature(0.5);
        verify(spec).temperature(any(java.util.function.Function.class));

        spec.topP(0.9);
        verify(spec).topP(any(java.util.function.Function.class));

        spec.includeUsage();
        verify(spec).includeUsage(any(java.util.function.Function.class));

        spec.reasoning("thinking");
        verify(spec).reasoning(any(java.util.function.Function.class));

        spec.systemMessage("sys");
        verify(spec).systemMessage(any(java.util.function.Function.class));

        spec.historicalMessage(Collections.emptyList());
        // verify NOT called since it's empty
        
        spec.textMessage("user");
        verify(spec).textMessage(any(java.util.function.Function.class));

        spec.mediaMessage("user", Collections.emptyList());
        verify(spec).mediaMessage(any(java.util.function.Function.class));

        spec.maxCompletionTokens(100);
        verify(spec).maxCompletionTokens(any(java.util.function.Function.class));

        spec.tools(Collections.emptyList());
        verify(spec).tools(any(java.util.function.Function.class));

        spec.toolChoice("auto");
        verify(spec).toolChoice(any(java.util.function.Function.class));

        spec.toolsResponse(Collections.emptyList());
        verify(spec).toolsResponse(any(java.util.function.Function.class));
    }
}
