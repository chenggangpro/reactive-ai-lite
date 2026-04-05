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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testUsageBuilder() {
        ObjectNode rawUsage = objectMapper.createObjectNode();
        rawUsage.put("prompt", 10);
        rawUsage.put("completion", 20);
        rawUsage.put("extra", 5);

        Usage usage = Usage.newUsageBuilder(rawUsage)
                .promptTokensExtractor(node -> node.get("prompt").asInt())
                .completionTokensExtractor(node -> node.get("completion").asInt())
                .otherTokensExtractor(node -> node.get("extra").asInt())
                .build();

        assertThat(usage.getPromptTokens()).isEqualTo(10);
        assertThat(usage.getCompletionTokens()).isEqualTo(20);
        assertThat(usage.getOtherTokens()).isEqualTo(5);
        assertThat(usage.getTotalTokens()).isEqualTo(35);
        assertThat(usage.getRawUsage()).isEqualTo(rawUsage);
        assertThat(usage.isValidUsage()).isTrue();
    }

    @Test
    void testUsageBuilderWithDefaults() {
        ObjectNode rawUsage = objectMapper.createObjectNode();

        Usage usage = Usage.newUsageBuilder(rawUsage).build();

        assertThat(usage.getPromptTokens()).isEqualTo(-1);
        assertThat(usage.getCompletionTokens()).isEqualTo(-1);
        assertThat(usage.getOtherTokens()).isEqualTo(-1);
        assertThat(usage.getTotalTokens()).isEqualTo(-3);
    }

    @Test
    void testUsageBuilderWithNullExtractorResult() {
        ObjectNode rawUsage = objectMapper.createObjectNode();

        Usage usage = Usage.newUsageBuilder(rawUsage)
                .promptTokensExtractor(node -> null)
                .build();

        assertThat(usage.getPromptTokens()).isEqualTo(0);
    }
}
