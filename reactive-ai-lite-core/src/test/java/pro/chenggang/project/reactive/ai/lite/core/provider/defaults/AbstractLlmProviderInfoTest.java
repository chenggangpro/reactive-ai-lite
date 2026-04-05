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

import lombok.NonNull;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractLlmProviderInfoTest {

    static class TestProviderInfo extends AbstractLlmProviderInfo {
        protected TestProviderInfo(String baseUrl, String endpoint, boolean isDefault, @NonNull Set<String> profiles, Set<String> supportedModels) {
            super(baseUrl, endpoint, isDefault, profiles, supportedModels);
        }

        @Override
        public String name() {
            return "test";
        }
    }

    @Test
    void testBasicProperties() {
        Set<String> profiles = Collections.singleton("default");
        TestProviderInfo info = new TestProviderInfo("url", "end", true, profiles, null);
        assertThat(info.baseUrl()).isEqualTo("url");
        assertThat(info.endpoint()).isEqualTo("end");
        assertThat(info.isDefault()).isTrue();
        assertThat(info.profiles()).isEqualTo(profiles);
    }

    @Test
    void testSupportModel() {
        Set<String> profiles = Collections.singleton("default");
        Set<String> models = Set.of("gpt-4", "gpt-3.5");
        TestProviderInfo info = new TestProviderInfo("url", "end", true, profiles, models);
        
        assertThat(info.supportModel("gpt-4")).isTrue();
        assertThat(info.supportModel("gpt-3.5")).isTrue();
        assertThat(info.supportModel("claude")).isFalse();
    }

    @Test
    void testSupportModelAll() {
        Set<String> profiles = Collections.singleton("default");
        TestProviderInfo info = new TestProviderInfo("url", "end", true, profiles, null);
        
        assertThat(info.supportModel("any")).isTrue();
    }

    @Test
    void testToString() {
        Set<String> profiles = Collections.singleton("default");
        TestProviderInfo info = new TestProviderInfo("url", "end", true, profiles, null);
        assertThat(info.toString()).contains("url").contains("end").contains("default");
    }

    @Test
    void testEmptySupportedModels() {
        Set<String> profiles = Collections.singleton("default");
        TestProviderInfo info = new TestProviderInfo("url", "end", true, profiles, Collections.emptySet());
        assertThat(info.supportModel("any")).isTrue();
    }
}
