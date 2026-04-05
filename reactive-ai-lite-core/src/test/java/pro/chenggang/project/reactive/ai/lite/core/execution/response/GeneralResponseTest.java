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
package pro.chenggang.project.reactive.ai.lite.core.execution.response;

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.message.AssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolCallMessage;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneralResponseTest {

    @Test
    void testGetToolCallsWithToolCallMessage() {
        ToolCallMessage toolCallMessage = mock(ToolCallMessage.class);
        when(toolCallMessage.getToolCalls()).thenReturn(Collections.emptyList());
        
        GeneralResponse response = GeneralResponse.builder()
                .assistantTextMessage(toolCallMessage)
                .build();
        
        assertThat(response.getToolCalls()).isPresent();
        assertThat(response.getToolCalls().get()).isEmpty();
    }

    @Test
    void testGetToolCallsWithRegularMessage() {
        AssistantTextMessage assistantTextMessage = mock(AssistantTextMessage.class);
        
        GeneralResponse response = GeneralResponse.builder()
                .assistantTextMessage(assistantTextMessage)
                .build();
        
        assertThat(response.getToolCalls()).isEmpty();
    }
}
