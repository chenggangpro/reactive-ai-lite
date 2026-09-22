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
package pro.chenggang.project.reactive.ai.lite.client.typesafeai.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TypeSafeAiLlmProviderInfo}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
class TypeSafeAiLlmProviderInfoTest {

    @Test
    @DisplayName("Provider info initialized with constants and baseUrl via builder")
    void testProviderInfo() {
        TypeSafeAiLlmProviderInfo info = TypeSafeAiLlmProviderInfo.builder()
                .name(TypeSafeAiLlmProviderInfo.DEFAULT_NAME)
                .baseUrl(TypeSafeAiLlmProviderInfo.DEFAULT_BASE_URL)
                .endpoint(TypeSafeAiLlmProviderInfo.DEFAULT_SYSTEM_ONE_ENDPOINT)
                .isDefault(true)
                .profiles(Set.of("default"))
                .build();

        assertThat(info.name()).isEqualTo("TypeSafeAI");
        assertThat(info.baseUrl()).isEqualTo("https://api.typesafe.ai");
        assertThat(info.endpoint()).isEqualTo("/v1/systemone");
        assertThat(info.isDefault()).isTrue();
        assertThat(info.profiles()).containsExactly("default");

        assertThat(TypeSafeAiLlmProviderInfo.DEFAULT_NAME).isEqualTo("TypeSafeAI");
    }

    @Test
    @DisplayName("Provider info with custom configuration")
    void testProviderInfoWithCustomConfiguration() {
        TypeSafeAiLlmProviderInfo info = TypeSafeAiLlmProviderInfo.builder()
                .name("CustomTypeSafeAI")
                .baseUrl("https://custom.typesafe.ai")
                .endpoint("/custom/systemone")
                .supportedModels(Set.of("jev-1"))
                .build();

        assertThat(info.name()).isEqualTo("CustomTypeSafeAI");
        assertThat(info.baseUrl()).isEqualTo("https://custom.typesafe.ai");
        assertThat(info.endpoint()).isEqualTo("/custom/systemone");
        assertThat(info.supportModel("jev-1")).isTrue();
        assertThat(info.supportModel("other-model")).isFalse();
    }
}
