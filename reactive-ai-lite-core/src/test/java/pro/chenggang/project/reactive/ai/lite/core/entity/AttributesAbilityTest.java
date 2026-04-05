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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AttributesAbilityTest {

    static class TestAttributesAbility implements AttributesAbility {
        private final Map<String, Object> attributes = new HashMap<>();
        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }

    @Test
    void testGetAttribute() {
        TestAttributesAbility ability = new TestAttributesAbility();
        ability.getAttributes().put("key", "value");
        
        String value = ability.getAttribute("key");
        assertThat(value).isEqualTo("value");
        
        Object missing = ability.getAttribute("missing");
        assertThat(missing).isNull();
    }

    @Test
    void testGetAttributeOrDefault() {
        TestAttributesAbility ability = new TestAttributesAbility();
        ability.getAttributes().put("key", "value");
        
        String value = ability.getAttributeOrDefault("key", "default");
        assertThat(value).isEqualTo("value");
        
        String missingValue = ability.getAttributeOrDefault("missing", "default");
        assertThat(missingValue).isEqualTo("default");
    }

    @Test
    void testAttributesStream() {
        TestAttributesAbility ability = new TestAttributesAbility();
        ability.getAttributes().put("key1", "value1");
        ability.getAttributes().put("key2", "value2");
        
        List<Map.Entry<String, Object>> attributeList = ability.attributesStream().toList();
        assertThat(attributeList.size()).isEqualTo(2);
    }

    @Test
    void testForEachAttribute() {
        TestAttributesAbility ability = new TestAttributesAbility();
        ability.getAttributes().put("key1", "value1");
        ability.getAttributes().put("key2", "value2");
        
        AtomicInteger count = new AtomicInteger();
        ability.forEachAttribute((k, v) -> count.incrementAndGet());
        
        assertThat(count.get()).isEqualTo(2);
    }
}
