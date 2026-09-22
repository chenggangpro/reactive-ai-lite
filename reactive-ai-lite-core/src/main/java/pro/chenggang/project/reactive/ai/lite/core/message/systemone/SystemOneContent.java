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

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent.ArrayContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent.NullContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent.ObjectContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent.TextContent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Sealed interface representing the content or unstructured data payload evaluated by a SystemOne model.
 * <p>
 * In SystemOne AI models (such as TypeSafe AI's Jev), the input state can be provided in various formats:
 * <ul>
 *   <li>{@link NullContent}: Represents an absent or empty input payload (e.g. evaluating context-free propositions).</li>
 *   <li>{@link TextContent}: Unstructured plain text, such as emails, document snippets, or log excerpts.</li>
 *   <li>{@link ObjectContent}: Key-value structured data representing state objects, JSON maps, or domain entities.</li>
 *   <li>{@link ArrayContent}: Ordered lists of elements or sequences evaluated concurrently.</li>
 * </ul>
 * </p>
 *
 * @param <T> the type of the underlying content value
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public sealed interface SystemOneContent<T> permits NullContent, TextContent, ObjectContent, ArrayContent {

    /**
     * Returns the underlying data payload value.
     *
     * @return the raw content value, which may be {@code null} for {@link NullContent}
     */
    T getValue();

    /**
     * Shared singleton instance representing an empty or null content payload.
     */
    NullContent NULL = new NullContent();

    /**
     * Creates a {@link NullContent} instance representing absent content.
     *
     * @return the singleton {@link #NULL} content
     */
    static NullContent nullContent() {
        return NULL;
    }

    /**
     * Creates a {@link TextContent} instance wrapping the given plain text string.
     *
     * @param text the text content to wrap
     * @return a new {@link TextContent} instance
     */
    static TextContent text(String text) {
        return new TextContent(text);
    }

    /**
     * Creates an empty {@link ObjectContent} instance ready to receive key-value pairs.
     *
     * @return a new, mutable {@link ObjectContent} instance
     */
    static ObjectContent object() {
        return new ObjectContent(null);
    }

    /**
     * Creates an {@link ObjectContent} instance initialized with the given key-value map.
     *
     * @param initialValue the initial map of entries, may be {@code null}
     * @return a new {@link ObjectContent} instance
     */
    static ObjectContent object(Map<String, Object> initialValue) {
        return new ObjectContent(initialValue);
    }

    /**
     * Creates an empty {@link ArrayContent} instance ready to receive elements.
     *
     * @return a new, mutable {@link ArrayContent} instance
     */
    static ArrayContent array() {
        return new ArrayContent(null);
    }

    /**
     * Creates an {@link ArrayContent} instance initialized with the given varargs elements.
     *
     * @param initialValues elements to initialize the array content with
     * @return a new {@link ArrayContent} instance
     */
    static ArrayContent array(Object... initialValues) {
        if (Objects.isNull(initialValues) || initialValues.length == 0) {
            return new ArrayContent(null);
        }
        return new ArrayContent(Arrays.asList(initialValues));
    }

    /**
     * Represents an absent or null content payload.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    final class NullContent implements SystemOneContent<Object> {

        /**
         * The constant null value representing absence of content.
         */
        private final Object value = null;

    }

    /**
     * Represents a text-based unstructured data payload.
     */
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class TextContent implements SystemOneContent<String> {

        /**
         * The raw text payload string.
         */
        private final String value;

    }

    /**
     * Represents a key-value structured data payload.
     */
    final class ObjectContent implements SystemOneContent<Map<String, Object>> {

        /**
         * List of key-value entries comprising this object content.
         */
        private final List<Entry<String, ?>> value = new ArrayList<>();

        /**
         * Constructs an {@link ObjectContent} with the given map.
         *
         * @param contentValue the initial key-value map; may be {@code null}
         */
        @Jacksonized
        @Builder
        public ObjectContent(Map<String, Object> contentValue) {
            if (Objects.nonNull(contentValue) && !contentValue.isEmpty()) {
                contentValue.entrySet()
                        .forEach(value::add);
            }
        }

        /**
         * Appends a key-value entry to this object content.
         *
         * @param <V>              the entry value type
         * @param systemOneContent the entry to append; must not be null
         * @return this {@link ObjectContent} instance for method chaining
         */
        public <V> ObjectContent addContent(Map.Entry<String, V> systemOneContent) {
            this.value.add(systemOneContent);
            return this;
        }

        /**
         * Appends a key and value pair to this object content.
         *
         * @param key   the attribute key; must not be null
         * @param value the attribute value
         * @return this {@link ObjectContent} instance for method chaining
         */
        public ObjectContent addContent(String key, Object value) {
            this.addContent(Map.entry(key, value));
            return this;
        }

        /**
         * Returns the aggregated key-value map represented by this object content.
         *
         * @return an immutable map of the collected entries
         */
        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> getValue() {
            if (value.isEmpty()) {
                return Map.of();
            }
            return Map.ofEntries(value.toArray(new Map.Entry[0]));
        }

    }

    /**
     * Represents an ordered list or array-based data payload.
     */
    @Getter
    final class ArrayContent implements SystemOneContent<List<Object>> {

        /**
         * The list of items comprising this array content.
         */
        private final List<Object> value = new ArrayList<>();

        /**
         * Constructs an {@link ArrayContent} with the given list.
         *
         * @param contentList the initial list of items; may be {@code null}
         */
        @Jacksonized
        @Builder
        public ArrayContent(List<Object> contentList) {
            if (Objects.nonNull(contentList) && !contentList.isEmpty()) {
                this.value.addAll(contentList);
            }
        }

        /**
         * Appends an element to this array content.
         *
         * @param systemOneContent the item to append
         * @return this {@link ArrayContent} instance for method chaining
         */
        public ArrayContent addContent(Object systemOneContent) {
            this.value.add(systemOneContent);
            return this;
        }

    }

}
