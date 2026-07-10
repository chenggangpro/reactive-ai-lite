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
package pro.chenggang.project.reactive.ai.lite.core.entity.usage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsagePackageTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testUsageBuilderWithNullRawUsage() {
        assertThatThrownBy(() -> Usage.newUsageBuilder(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testUsageBuilderWithValidRawUsageAndExtractors() {
        ObjectNode rawUsage = objectMapper.createObjectNode();
        rawUsage.put("prompt", 10);
        rawUsage.put("completion", 20);
        rawUsage.put("other", 5);

        Usage usage = Usage.newUsageBuilder(rawUsage)
                .promptTokensExtractor(node -> node.get("prompt").asInt())
                .completionTokensExtractor(node -> node.get("completion").asInt())
                .otherTokensExtractor(node -> node.get("other").asInt())
                .build();

        assertThat(usage.isValidUsage()).isTrue();
        assertThat(usage.getRawUsage()).isEqualTo(rawUsage);
        assertThat(usage.getPromptTokens()).isEqualTo(10);
        assertThat(usage.getCompletionTokens()).isEqualTo(20);
        assertThat(usage.getOtherTokens()).isEqualTo(5);
        assertThat(usage.getTotalTokens()).isEqualTo(35);
    }

    @Test
    void testUsageBuilderWithNullExtractorValues() {
        ObjectNode rawUsage = objectMapper.createObjectNode();

        Usage usage = Usage.newUsageBuilder(rawUsage)
                .promptTokensExtractor(node -> null)
                .completionTokensExtractor(node -> null)
                .otherTokensExtractor(node -> null)
                .build();

        assertThat(usage.isValidUsage()).isTrue();
        assertThat(usage.getPromptTokens()).isEqualTo(0);
        assertThat(usage.getCompletionTokens()).isEqualTo(0);
        assertThat(usage.getOtherTokens()).isEqualTo(0);
        assertThat(usage.getTotalTokens()).isEqualTo(0);
    }

    @Test
    void testUsageBuilderWithoutExtractors() {
        ObjectNode rawUsage = objectMapper.createObjectNode();

        Usage usage = Usage.newUsageBuilder(rawUsage).build();

        assertThat(usage.isValidUsage()).isTrue();
        assertThat(usage.getPromptTokens()).isEqualTo(0);
        assertThat(usage.getCompletionTokens()).isEqualTo(0);
        assertThat(usage.getOtherTokens()).isEqualTo(0);
        assertThat(usage.getTotalTokens()).isEqualTo(0);
    }

    @Test
    void testUsageBuilderWithNullExtractorsPassed() {
        ObjectNode rawUsage = objectMapper.createObjectNode();

        Usage.UsageBuilder builder = Usage.newUsageBuilder(rawUsage);
        assertThatThrownBy(() -> builder.promptTokensExtractor(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.completionTokensExtractor(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.otherTokensExtractor(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testDefaultUsageWithoutRawUsage() {
        Usage usage = DefaultUsage.builder()
                .promptTokens(1)
                .completionTokens(2)
                .otherTokens(3)
                .totalTokens(6)
                .build();

        assertThat(usage.isValidUsage()).isFalse();
        assertThat(usage.getRawUsage()).isNull();
        assertThat(usage.getPromptTokens()).isEqualTo(1);
        assertThat(usage.getCompletionTokens()).isEqualTo(2);
        assertThat(usage.getOtherTokens()).isEqualTo(3);
        assertThat(usage.getTotalTokens()).isEqualTo(6);
    }
}
