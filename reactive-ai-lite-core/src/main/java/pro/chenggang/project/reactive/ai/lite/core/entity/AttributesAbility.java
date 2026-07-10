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
 * Defines the contract for managing and accessing a mutable, contextual attribute map.
 * <p>
 * Implementations of this interface provide a {@link Map} that can be used to store arbitrary
 * key-value pairs throughout the lifecycle of an AI exchange. This allows components to share
 * state, metadata, or temporary results without relying on static or global variables.
 * The map is intentionally mutable; elements can be added, replaced, or removed at any point
 * during processing, enabling dynamic enrichment of the context.
 * </p>
 * <p>
 * The type-safe convenience methods {@link #getAttribute(String)} and
 * {@link #getAttributeOrDefault(String, Object)} perform unchecked casts internally,
 * relying on the caller to know the expected type. Incorrect type assumptions will lead to
 * {@link ClassCastException} at runtime. Common use cases include storing user-defined
 * parameters, intermediate computation results, or framework-specific flags.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface AttributesAbility {

    /**
     * Returns a mutable map that serves as the backing attribute store.
     * <p>
     * The returned map is the same instance across multiple invocations. Any modifications
     * made to this map are immediately visible to other components sharing the same exchange
     * context. Implementors may choose a thread-safe map if concurrent access is expected,
     * though the interface itself does not enforce thread-safety.
     * </p>
     *
     * @return a mutable map containing the current attributes, never {@code null}
     */
    Map<String, Object> getAttributes();

    /**
     * Retrieves the value of an attribute, casting it to the expected type.
     * <p>
     * The cast is performed without explicit type checking; the caller must ensure
     * that the attribute was stored with a compatible type. If the attribute does not
     * exist, {@code null} is returned.
     * </p>
     *
     * @param <T>  the expected type of the attribute value
     * @param name the case-sensitive attribute key; must not be {@code null}
     * @return the attribute value cast to type {@code T}, or {@code null} if the
     *         attribute is absent
     */
    @SuppressWarnings("unchecked")
    default <T> T getAttribute(@NonNull String name) {
        return (T) getAttributes().get(name);
    }

    /**
     * Retrieves the value of an attribute, returning a default value if the attribute is
     * missing or is {@code null}.
     * <p>
     * This method simplifies null-safe access by guaranteeing a non-null fallback.
     * The default value is returned even if the attribute exists but its value is
     * {@code null}. The unchecked cast is applied to the result of
     * {@link Map#getOrDefault(Object, Object)}.
     * </p>
     *
     * @param <T>          the expected type of the attribute value and default
     * @param name         the case-sensitive attribute key; must not be {@code null}
     * @param defaultValue the value to return if the attribute is absent; must not be {@code null}
     * @return the existing attribute value cast to type {@code T}, or {@code defaultValue}
     *         if the attribute is missing
     */
    @SuppressWarnings("unchecked")
    default <T> T getAttributeOrDefault(@NonNull String name, @NonNull T defaultValue) {
        return (T) getAttributes().getOrDefault(name, defaultValue);
    }

    /**
     * Returns a sequential {@link Stream} over the attribute entries.
     * <p>
     * The stream is backed by the mutable attribute map; structural modifications to the map
     * during stream traversal may result in undefined behavior (e.g., a
     * {@link java.util.ConcurrentModificationException}). This method is useful for
     * functional-style processing, such as filtering, logging, or bulk operations.
     * </p>
     *
     * @return a sequential stream of key-value entry pairs representing the current attributes
     */
    default Stream<Entry<String, Object>> attributesStream() {
        return getAttributes().entrySet().stream();
    }

    /**
     * Iterates over all attribute entries and performs the given action for each.
     * <p>
     * This is a convenience wrapper around {@link #attributesStream()} that avoids
     * explicit stream handling. The action receives the key and value of each entry;
     * both are non-{@code null} for existing entries. As with direct map iteration,
     * concurrent modification of the backing map from within the action may lead to
     * unpredictable results.
     * </p>
     *
     * @param action a consumer that accepts attribute keys and values; must not be {@code null}
     */
    default void forEachAttribute(@NonNull BiConsumer<String, Object> action) {
        attributesStream().forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
    }
}