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

class ToolPackageTest {

    @Test
    void testDefaultToolDefinition() {
        DefaultToolDefinition tool1 = DefaultToolDefinition.builder()
                .name("test-tool")
                .description("test description")
                .inputSchema("{\"type\":\"object\"}")
                .strict(true)
                .build();

        assertThat(tool1.name()).isEqualTo("test-tool");
        assertThat(tool1.description()).isEqualTo("test description");
        assertThat(tool1.inputSchema()).isEqualTo("{\"type\":\"object\"}");
        assertThat(tool1.strict()).isTrue();

        DefaultToolDefinition tool2 = DefaultToolDefinition.builder()
                .name("tool2")
                .description("desc2")
                .build();

        assertThat(tool2.name()).isEqualTo("tool2");
        assertThat(tool2.description()).isEqualTo("desc2");
        assertThat(tool2.inputSchema()).isEqualTo("{}");
        assertThat(tool2.strict()).isNull();
    }
}
