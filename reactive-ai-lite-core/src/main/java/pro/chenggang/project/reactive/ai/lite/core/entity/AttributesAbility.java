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

import lombok.NonNull;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Interface providing the ability to manage and access parsingAttributes.
 * <p>
 * This interface defines methods for storing and retrieving parsingAttributes in a key-value format,
 * allowing implementations to maintain contextual information throughout their lifecycle.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface AttributesAbility {

    /**
     * Return a mutable map of parsingAttributes for the current exchange.
     *
     * @return the parsingAttributes map
     */
    Map<String, Object> getAttributes();

    /**
     * Return the attribute value if present or {@code null} if not present.
     *
     * @param <T>  the attribute type
     * @param name the attribute name
     * @return the attribute value, or {@code null} if it does not exist
     */
    @SuppressWarnings("unchecked")
    default <T> T getAttribute(@NonNull String name) {
        return (T) getAttributes().get(name);
    }

    /**
     * Return the attribute value or a default value if the attribute is not present.
     *
     * @param <T>          the attribute type
     * @param name         the attribute name
     * @param defaultValue a default value to return instead if the attribute is missing
     * @return the attribute value, or the default value
     */
    @SuppressWarnings("unchecked")
    default <T> T getAttributeOrDefault(@NonNull String name, @NonNull T defaultValue) {
        return (T) getAttributes().getOrDefault(name, defaultValue);
    }

    /**
     * Returns a sequential {@link Stream} with the parsingAttributes as its source.
     *
     * @return a stream of attribute entries
     */
    default Stream<Entry<String, Object>> attributesStream() {
        return getAttributes().entrySet().stream();
    }

    /**
     * Performs the given action for each attribute entry in the map until all entries
     * have been processed or the action throws an exception.
     *
     * @param action the action to be performed for each attribute entry
     */
    default void forEachAttribute(@NonNull BiConsumer<String, Object> action) {
        attributesStream().forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
    }
}
