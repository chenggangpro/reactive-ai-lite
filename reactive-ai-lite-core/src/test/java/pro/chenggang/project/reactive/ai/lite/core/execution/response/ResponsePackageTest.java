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
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultAssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultToolCallMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponsePackageTest {

    @Test
    void testGeneralResponseWithTextMessage() {
        DefaultAssistantTextMessage textMessage = DefaultAssistantTextMessage.builder()
                .content("test")
                .build();
                
        GeneralResponse response = GeneralResponse.builder()
                .assistantTextMessage(textMessage)
                .build();
                
        assertThat(response.getAssistantTextMessage()).isEqualTo(textMessage);
        assertThat(response.getToolCalls()).isEmpty();
    }

    @Test
    void testGeneralResponseWithToolCallMessage() {
        DefaultToolCallMessage toolCallMessage = DefaultToolCallMessage.builder()
                .toolCalls(List.of())
                .build();
                
        GeneralResponse response = GeneralResponse.builder()
                .assistantTextMessage(toolCallMessage)
                .build();
                
        assertThat(response.getAssistantTextMessage()).isEqualTo(toolCallMessage);
        assertThat(response.getToolCalls()).isPresent();
    }
}
