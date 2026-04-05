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
package pro.chenggang.project.reactive.ai.lite.core.message;

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ToolCallMessageTest {

    @Test
    void testAssistantToolCallFunctionBuilder() {
        ToolCallMessage.AssistantToolCallFunction function = ToolCallMessage.AssistantToolCallFunction.builder()
                .name("test-func")
                .arguments("{\"a\": 1}")
                .build();
        
        assertThat(function.getName()).isEqualTo("test-func");
        assertThat(function.getArguments()).isEqualTo("{\"a\": 1}");
        assertThat(function.jsonArguments().get("a").asInt()).isEqualTo(1);
    }

    @Test
    void testAssistantToolCallFunctionWithEmptyArgs() {
        ToolCallMessage.AssistantToolCallFunction function = ToolCallMessage.AssistantToolCallFunction.builder()
                .name("test-func")
                .arguments("")
                .build();
        
        assertThat(function.getArguments()).isEqualTo("{}");
    }

    @Test
    void testAssistantToolCallFunctionWithInvalidJson() {
        ToolCallMessage.AssistantToolCallFunction function = ToolCallMessage.AssistantToolCallFunction.builder()
                .name("test-func")
                .arguments("{invalid}")
                .build();
        
        assertThatThrownBy(function::jsonArguments)
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testAssistantToolCallBuilder() {
        ToolCallMessage.AssistantToolCallFunction function = mock(ToolCallMessage.AssistantToolCallFunction.class);
        ToolDefinition toolDefinition = mock(ToolDefinition.class);
        ToolCallMessage.AssistantToolCall toolCall = ToolCallMessage.AssistantToolCall.builder()
                .index(0)
                .id("id")
                .type("function")
                .function(function)
                .toolDefinition(toolDefinition)
                .build();
        
        assertThat(toolCall.getIndex()).isEqualTo(0);
        assertThat(toolCall.getId()).isEqualTo("id");
        assertThat(toolCall.getType()).isEqualTo("function");
        assertThat(toolCall.getFunction()).isEqualTo(function);
        assertThat(toolCall.getToolDefinition()).isEqualTo(toolDefinition);
    }
}
