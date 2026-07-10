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
package pro.chenggang.project.reactive.ai.lite.core.option;

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.message.chunk.TextStreamDataChunk;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmEmbeddingProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionPackageTest {

    @Test
    void testRole() {
        assertThat(Role.SYSTEM.getValue()).isEqualTo("system");
        assertThat(Role.USER.getValue()).isEqualTo("user");
        assertThat(Role.ASSISTANT.getValue()).isEqualTo("assistant");
        assertThat(Role.TOOL.getValue()).isEqualTo("tool");

        assertThat(Role.fromValue("system")).isEqualTo(Role.SYSTEM);
        assertThat(Role.fromValue("USER")).isEqualTo(Role.USER);
        assertThat(Role.fromValue("Assistant")).isEqualTo(Role.ASSISTANT);
        assertThat(Role.fromValue("tool")).isEqualTo(Role.TOOL);

        assertThatThrownBy(() -> Role.fromValue("invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testStreamDataType() {
        assertThat(StreamDataType.ANSWER_CONTENT.getDataChunkType()).isEqualTo(TextStreamDataChunk.class);
        assertThat(StreamDataType.values()).contains(StreamDataType.UNKNOWN, StreamDataType.ANSWER_CONTENT);
        assertThat(StreamDataType.valueOf("ROLE")).isEqualTo(StreamDataType.ROLE);
    }

    @Test
    void testCapability() {
        assertThat(Capability.CHAT.getProviderClass()).isEqualTo(LlmChatProvider.class);
        assertThat(Capability.EMBEDDING.getProviderClass()).isEqualTo(LlmEmbeddingProvider.class);
        assertThat(Capability.values()).containsExactly(Capability.CHAT, Capability.EMBEDDING);
        assertThat(Capability.valueOf("CHAT")).isEqualTo(Capability.CHAT);
    }

    @Test
    void testLlmClientType() {
        assertThat(LlmClientType.CHAT.getCapability()).isEqualTo(Capability.CHAT);
        assertThat(LlmClientType.EMBEDDING.getCapability()).isEqualTo(Capability.EMBEDDING);
        assertThat(LlmClientType.values()).containsExactly(LlmClientType.CHAT, LlmClientType.EMBEDDING);
        assertThat(LlmClientType.valueOf("CHAT")).isEqualTo(LlmClientType.CHAT);
    }

    @Test
    void testBuildInPrompt() {
        assertThat(BuildInPrompt.SYSTEM_PROMPT).isNotEmpty();
    }
}
