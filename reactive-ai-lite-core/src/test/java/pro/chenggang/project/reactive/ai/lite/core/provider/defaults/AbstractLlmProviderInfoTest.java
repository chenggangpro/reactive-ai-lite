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
package pro.chenggang.project.reactive.ai.lite.core.provider.defaults;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractLlmProviderInfoTest {

    static class TestLlmProviderInfo extends AbstractLlmProviderInfo {
        protected TestLlmProviderInfo(String baseUrl, String endpoint, boolean isDefault, Set<String> profiles, Set<String> supportedModels) {
            super(baseUrl, endpoint, isDefault, profiles, supportedModels);
        }

        @Override
        public String name() {
            return "test";
        }
    }

    @Test
    void testAbstractLlmProviderInfo() {
        TestLlmProviderInfo infoWithModels = new TestLlmProviderInfo("http://base", "/end", true, Set.of("p1"), Set.of("m1", "m2"));
        
        assertThat(infoWithModels.baseUrl()).isEqualTo("http://base");
        assertThat(infoWithModels.endpoint()).isEqualTo("/end");
        assertThat(infoWithModels.isDefault()).isTrue();
        assertThat(infoWithModels.profiles()).containsExactly("p1");
        
        assertThat(infoWithModels.supportModel("m1")).isTrue();
        assertThat(infoWithModels.supportModel("m3")).isFalse();
        
        assertThat(infoWithModels.toString()).contains("test", "http://base", "/end", "p1", "m1");

        TestLlmProviderInfo infoWithoutModels = new TestLlmProviderInfo("http://base", "/end", false, Set.of("p1"), null);
        
        assertThat(infoWithoutModels.supportModel("m1")).isTrue();
        assertThat(infoWithoutModels.supportModel("any-model")).isTrue();
    }
}
