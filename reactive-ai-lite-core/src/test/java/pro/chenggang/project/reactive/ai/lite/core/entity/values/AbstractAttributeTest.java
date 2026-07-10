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
package pro.chenggang.project.reactive.ai.lite.core.entity.values;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractAttributeTest {

    private static class TestAttribute extends AbstractAttribute {
        public TestAttribute() {
            super();
        }

        public TestAttribute(Map<String, Object> attributes) {
            super(attributes);
        }
    }

    @Test
    void testDefaultConstructor() {
        TestAttribute attribute = new TestAttribute();
        assertThat(attribute.getAttributes()).isEmpty();
    }

    @Test
    void testConstructorWithMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        TestAttribute attribute = new TestAttribute(map);
        
        assertThat(attribute.getAttributes()).hasSize(1).containsEntry("key1", "value1");
        
        // Ensure it's a copy/new map
        map.put("key2", "value2");
        assertThat(attribute.getAttributes()).hasSize(1).doesNotContainKey("key2");
    }

    @Test
    void testGetAttribute() {
        TestAttribute attribute = new TestAttribute();
        attribute.getAttributes().put("key1", "value1");
        
        String val = attribute.getAttribute("key1");
        assertThat(val).isEqualTo("value1");
        
        String missing = attribute.getAttribute("missing");
        assertThat(missing).isNull();
    }

    @Test
    void testGetAttributeOrDefault() {
        TestAttribute attribute = new TestAttribute();
        attribute.getAttributes().put("key1", "value1");
        
        String val = attribute.getAttributeOrDefault("key1", "default1");
        assertThat(val).isEqualTo("value1");
        
        String missing = attribute.getAttributeOrDefault("missing", "default2");
        assertThat(missing).isEqualTo("default2");
    }

    @Test
    void testAttributesStream() {
        TestAttribute attribute = new TestAttribute();
        attribute.getAttributes().put("key1", "value1");
        attribute.getAttributes().put("key2", "value2");
        
        long count = attribute.attributesStream().count();
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testForEachAttribute() {
        TestAttribute attribute = new TestAttribute();
        attribute.getAttributes().put("key1", "value1");
        attribute.getAttributes().put("key2", "value2");
        
        AtomicInteger count = new AtomicInteger(0);
        attribute.forEachAttribute((k, v) -> {
            count.incrementAndGet();
            assertThat(k).startsWith("key");
            assertThat(v).asString().startsWith("value");
        });
        
        assertThat(count.get()).isEqualTo(2);
    }
}
