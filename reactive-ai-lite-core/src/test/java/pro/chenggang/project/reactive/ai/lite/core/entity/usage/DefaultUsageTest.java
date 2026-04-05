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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

class DefaultUsageTest {

    @Test
    void testBuilder() {
        ObjectNode raw = OBJECT_MAPPER.createObjectNode().put("test", "data");
        DefaultUsage usage = DefaultUsage.builder()
                .promptTokens(10)
                .completionTokens(20)
                .totalTokens(30)
                .rawUsage(raw)
                .build();

        assertThat(usage.getPromptTokens()).isEqualTo(10);
        assertThat(usage.getCompletionTokens()).isEqualTo(20);
        assertThat(usage.getTotalTokens()).isEqualTo(30);
        assertThat(usage.getRawUsage()).isEqualTo(raw);
    }

    @Test
    void testDefaultValues() {
        DefaultUsage usage = DefaultUsage.builder().build();
        assertThat(usage.getPromptTokens()).isEqualTo(0);
        assertThat(usage.getCompletionTokens()).isEqualTo(0);
        assertThat(usage.getTotalTokens()).isEqualTo(0);
        assertThat(usage.getOtherTokens()).isEqualTo(0);
    }
}
