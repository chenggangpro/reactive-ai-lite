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
import pro.chenggang.project.reactive.ai.lite.core.message.ToolCallMessage.AssistantToolCall;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolCallMessage.AssistantToolCallFunction;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ToolCallMessageTest {

    @Test
    void testAssistantToolCallFunction() {
        AssistantToolCallFunction func = AssistantToolCallFunction.builder()
                .name("test")
                .arguments("{\"key\":\"value\"}")
                .build();
                
        assertThat(func.getName()).isEqualTo("test");
        assertThat(func.getArguments()).isEqualTo("{\"key\":\"value\"}");
        assertThat(func.jsonArguments().get("key").asText()).isEqualTo("value");
        
        AssistantToolCallFunction emptyArgs = AssistantToolCallFunction.builder()
                .name("test2")
                .arguments(null)
                .build();
        assertThat(emptyArgs.getArguments()).isEqualTo("{}");
        assertThat(emptyArgs.jsonArguments().isEmpty()).isTrue();
        
        AssistantToolCallFunction emptyArgs2 = AssistantToolCallFunction.builder()
                .name("test2")
                .arguments("")
                .build();
        assertThat(emptyArgs2.getArguments()).isEqualTo("{}");
        
        AssistantToolCallFunction invalidArgs = AssistantToolCallFunction.builder()
                .name("test3")
                .arguments("invalid")
                .build();
        assertThatThrownBy(invalidArgs::jsonArguments)
                .isInstanceOf(RuntimeException.class);
    }
    
    @Test
    void testAssistantToolCall() {
        AssistantToolCallFunction func = AssistantToolCallFunction.builder()
                .name("test")
                .arguments("{}")
                .build();
                
        ToolDefinition toolDef = mock(ToolDefinition.class);
        
        AssistantToolCall call = AssistantToolCall.builder()
                .index(1)
                .id("call_1")
                .type("function")
                .function(func)
                .toolDefinition(toolDef)
                .build();
                
        assertThat(call.getIndex()).isEqualTo(1);
        assertThat(call.getId()).isEqualTo("call_1");
        assertThat(call.getType()).isEqualTo("function");
        assertThat(call.getFunction()).isEqualTo(func);
        assertThat(call.getToolDefinition()).isEqualTo(toolDef);
    }
}
