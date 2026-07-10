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
import pro.chenggang.project.reactive.ai.lite.core.util.JsonSchemaUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolDefinitionTest {

    @Test
    void testBuildWithInputSchemaString() {
        ToolDefinition definition = ToolDefinition.newToolDefinition()
                .name("testTool")
                .description("Test Description")
                .inputSchema("{\"type\":\"object\"}")
                .strict(true)
                .build();

        assertThat(definition.name()).isEqualTo("testTool");
        assertThat(definition.description()).isEqualTo("Test Description");
        assertThat(definition.inputSchema()).isEqualTo("{\"type\":\"object\"}");
        assertThat(definition.strict()).isTrue();
    }

    @Test
    void testBuildWithInputSchemaType() {
        ToolDefinition definition = ToolDefinition.newToolDefinition()
                .name("typeTool")
                .description("Type Description")
                .inputSchemaType(String.class)
                .build();

        assertThat(definition.name()).isEqualTo("typeTool");
        assertThat(definition.description()).isEqualTo("Type Description");
        assertThat(definition.inputSchema()).isNotBlank();
        assertThat(definition.strict()).isNull();
    }

    @Test
    void testBuildWithInputSchemaTypeNullOptions() {
        ToolDefinition definition = ToolDefinition.newToolDefinition()
                .name("typeTool2")
                .description("Type Description 2")
                .inputSchemaType(Integer.class, (JsonSchemaUtil.SchemaOption[]) null)
                .build();

        assertThat(definition.name()).isEqualTo("typeTool2");
        assertThat(definition.description()).isEqualTo("Type Description 2");
        assertThat(definition.inputSchema()).isNotBlank();
    }

    @Test
    void testBuildMissingName() {
        assertThatThrownBy(() -> ToolDefinition.newToolDefinition()
                .description("desc")
                .inputSchema("{}")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name of tool definition cannot be null or empty.");

        assertThatThrownBy(() -> ToolDefinition.newToolDefinition()
                .name("")
                .description("desc")
                .inputSchema("{}")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name of tool definition cannot be null or empty.");
    }

    @Test
    void testBuildMissingDescription() {
        assertThatThrownBy(() -> ToolDefinition.newToolDefinition()
                .name("name")
                .inputSchema("{}")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description of tool definition cannot be null or empty.");

        assertThatThrownBy(() -> ToolDefinition.newToolDefinition()
                .name("name")
                .description("")
                .inputSchema("{}")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description of tool definition cannot be null or empty.");
    }

    @Test
    void testBuildMissingSchema() {
        assertThatThrownBy(() -> ToolDefinition.newToolDefinition()
                .name("name")
                .description("desc")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Either inputSchema or inputSchemaType must be provided.");
    }

    @Test
    void testToolDefinitionBuilderNullChecks() throws Exception {
        ToolDefinition.ToolDefinitionBuilder builder = ToolDefinition.newToolDefinition();
        java.lang.reflect.Method[] methods = builder.getClass().getDeclaredMethods();
        for (java.lang.reflect.Method method : methods) {
            if (method.getName().equals("name") || method.getName().equals("description") 
                || method.getName().equals("inputSchema") || method.getName().equals("inputSchemaType") 
                || method.getName().equals("strict")) {
                method.setAccessible(true);
                org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
                        if (method.getParameterCount() == 1) {
                            method.invoke(builder, new Object[]{null});
                        } else if (method.getParameterCount() == 2) {
                            method.invoke(builder, new Object[]{null, null});
                        }
                    })
                    .hasCauseInstanceOf(IllegalArgumentException.class);
            }
        }
    }
}
