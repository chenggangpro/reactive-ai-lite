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
package pro.chenggang.project.reactive.ai.lite.core.message.systemone;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SystemOneContent} implementations.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
class SystemOneContentTest {

    @Test
    void testNullContent() {
        SystemOneContent.NullContent nullContent = SystemOneContent.nullContent();
        assertThat(nullContent).isNotNull();
        assertThat(nullContent.getValue()).isNull();
        assertThat(SystemOneContent.NULL).isNotNull();
        assertThat(SystemOneContent.NULL.getValue()).isNull();
    }

    @Test
    void testTextContent() {
        SystemOneContent.TextContent textContent = SystemOneContent.text("Hello SystemOne");
        assertThat(textContent).isNotNull();
        assertThat(textContent.getValue()).isEqualTo("Hello SystemOne");

        SystemOneContent.TextContent built = SystemOneContent.TextContent.builder()
                .value("custom text")
                .build();
        assertThat(built.getValue()).isEqualTo("custom text");
    }

    @Test
    void testObjectContent() {
        Map<String, Object> initial = Map.of("email", "test@example.com", "age", 30);
        SystemOneContent.ObjectContent objectContent = SystemOneContent.object(initial);
        assertThat(objectContent.getValue()).containsEntry("email", "test@example.com");
        assertThat(objectContent.getValue()).containsEntry("age", 30);

        objectContent.addContent("role", "admin");
        assertThat(objectContent.getValue()).containsEntry("role", "admin");

        objectContent.addContent(Map.entry("active", true));
        assertThat(objectContent.getValue()).containsEntry("active", true);

        SystemOneContent.ObjectContent empty = SystemOneContent.object(Map.of());
        assertThat(empty.getValue()).isEmpty();

        SystemOneContent.ObjectContent nullInitial = SystemOneContent.object(null);
        assertThat(nullInitial.getValue()).isEmpty();
    }

    @Test
    void testArrayContent() {
        SystemOneContent.ArrayContent arrayContent = SystemOneContent.array("item1", "item2", 42);
        assertThat(arrayContent.getValue()).containsExactly("item1", "item2", 42);

        arrayContent.addContent("item3");
        assertThat(arrayContent.getValue()).containsExactly("item1", "item2", 42, "item3");

        SystemOneContent.ArrayContent emptyArray = SystemOneContent.array();
        assertThat(emptyArray.getValue()).isEmpty();

        SystemOneContent.ArrayContent nullArray = SystemOneContent.array((Object[]) null);
        assertThat(nullArray.getValue()).isEmpty();

        SystemOneContent.ArrayContent fromList = new SystemOneContent.ArrayContent(List.of("a", "b"));
        assertThat(fromList.getValue()).containsExactly("a", "b");
    }
}
