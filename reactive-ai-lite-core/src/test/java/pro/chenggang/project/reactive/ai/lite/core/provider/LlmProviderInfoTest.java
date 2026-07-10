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
package pro.chenggang.project.reactive.ai.lite.core.provider;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmProviderInfoTest {

    @Test
    void testDefaultMethods() {
        LlmProviderInfo info = new LlmProviderInfo() {
            @Override
            public String name() { return "test"; }
            @Override
            public String baseUrl() { return "url"; }
            @Override
            public String endpoint() { return "endpoint"; }
            @Override
            public Set<String> profiles() { return Set.of(); }
        };
        
        assertThat(info.isDefault()).isFalse();
        assertThat(info.supportModel("any")).isTrue();
        
        assertThatThrownBy(() -> info.supportModel(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
