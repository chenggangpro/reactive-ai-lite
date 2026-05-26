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
package pro.chenggang.project.reactive.ai.lite.core.message.defaults;

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultToolResultMessageTest {

    @Test
    void testBuilderAndGetters() {
        String toolCallId = "call_123";
        String content = "Tool result data";

        DefaultToolResultMessage message = DefaultToolResultMessage.builder()
                .toolCallId(toolCallId)
                .content(content)
                .build();

        assertThat(message.toolCallId()).isEqualTo(toolCallId);
        assertThat(message.content()).isEqualTo(content);
        assertThat(message.getRole()).isEqualTo(Role.TOOL.name().toUpperCase());
        assertThat(message.getActualType()).isEqualTo(DefaultToolResultMessage.class);
    }
}
