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
package pro.chenggang.project.reactive.ai.lite.core.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultToolDefinitionTest {

    @Test
    void testToolDefinitionBuilderWithSchema() {
        ToolDefinition tool = ToolDefinition.newToolDefinition()
                .name("test-tool")
                .description("A test tool")
                .inputSchema("{\"type\": \"object\"}")
                .strict(true)
                .build();
        
        assertThat(tool.name()).isEqualTo("test-tool");
        assertThat(tool.description()).isEqualTo("A test tool");
        assertThat(tool.inputSchema()).isEqualTo("{\"type\": \"object\"}");
        assertThat(tool.strict()).isTrue();
    }

    @Test
    void testToolDefinitionBuilderWithType() {
        ToolDefinition tool = ToolDefinition.newToolDefinition()
                .name("test-tool")
                .description("A test tool")
                .inputSchemaType(String.class)
                .build();
        
        assertThat(tool.name()).isEqualTo("test-tool");
        assertThat(tool.inputSchema()).contains("string");
    }

    @Test
    void testToolDefinitionBuilderMissingName() {
        assertThatThrownBy(() -> ToolDefinition.newToolDefinition()
                .description("desc")
                .inputSchema("{}")
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testToolDefinitionBuilderMissingDescription() {
        assertThatThrownBy(() -> ToolDefinition.newToolDefinition()
                .name("name")
                .inputSchema("{}")
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testToolDefinitionBuilderMissingSchema() {
        assertThatThrownBy(() -> ToolDefinition.newToolDefinition()
                .name("name")
                .description("desc")
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
