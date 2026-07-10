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
package pro.chenggang.project.reactive.ai.lite.core.entity;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributesAbilityTest {

    static class TestAttributesAbility implements AttributesAbility {
        private final Map<String, Object> attributes = new HashMap<>();

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }

    @Test
    void testAttributesAbility() {
        TestAttributesAbility ability = new TestAttributesAbility();
        ability.getAttributes().put("key1", "value1");

        assertThat((String) ability.getAttribute("key1")).isEqualTo("value1");
        assertThat((String) ability.getAttribute("key2")).isNull();

        assertThat(ability.getAttributeOrDefault("key1", "default")).isEqualTo("value1");
        assertThat(ability.getAttributeOrDefault("key2", "default")).isEqualTo("default");

        assertThat(ability.attributesStream()).hasSize(1);
        
        Map<String, Object> collected = new HashMap<>();
        ability.forEachAttribute(collected::put);
        assertThat(collected).containsEntry("key1", "value1");

        assertThatThrownBy(() -> ability.getAttribute(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ability.getAttributeOrDefault(null, "default"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ability.getAttributeOrDefault("key", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ability.forEachAttribute(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
