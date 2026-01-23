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
package pro.chenggang.project.reactive.ai.lite.core.entity.context;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.TraceId;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * The view of the execution context.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ExecutionContextView {

    @NonNull
    private final ExecutionContext executionContext;

    public TraceId getTraceId() {
        return executionContext.getTraceId();
    }

    /**
     * Return the attribute value if present or {@code null} if not present.
     *
     * @param <T>  the attribute type
     * @param name the attribute name
     * @return the attribute value
     */
    <T> T getAttribute(@NonNull String name) {
        return executionContext.getAttribute(name);
    }

    /**
     * Return the attribute value or a default value if the attribute is not present.
     *
     * @param <T>          the attribute type
     * @param name         the attribute name
     * @param defaultValue a default value to return instead
     * @return the attribute value
     */
    <T> T getAttributeOrDefault(@NonNull String name, @NonNull T defaultValue) {
        return executionContext.getAttributeOrDefault(name, defaultValue);
    }

    /**
     * Retrieves the attribute value as an {@code Optional} if present, or an empty {@code Optional} if not.
     *
     * @param <T> the attribute type
     * @param key the attribute key
     * @return an {@code Optional} containing the attribute value, or an empty {@code Optional} if not present
     */
    public <T> Optional<T> getOrEmpty(@NonNull String key) {
        return Optional.ofNullable(getAttribute(key));
    }

    /**
     * Checks whether an attribute with the specified key exists in this context.
     *
     * @param key the attribute key to check
     * @return {@code true} if the attribute exists, {@code false} otherwise
     */
    public boolean hasKey(@NonNull String key) {
        return executionContext.getAttributes().containsKey(key);
    }

    /**
     * Checks whether this context contains no attributes.
     *
     * @return {@code true} if the context is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return executionContext.getAttributes().isEmpty();
    }

    /**
     * Returns the number of attributes in this context.
     *
     * @return the number of attributes
     */
    public int size() {
        return executionContext.getAttributes().size();
    }

    /**
     * Returns a stream of all attribute entries in this context.
     *
     * @return a stream of map entries containing attribute keys and values
     */
    public Stream<Map.Entry<String, Object>> stream() {
        return executionContext.getAttributes().entrySet().stream();
    }

    /**
     * Performs the specified action for each attribute in this context.
     *
     * @param action a {@code BiConsumer} that accepts the attribute key and value
     */
    public void forEach(@NonNull BiConsumer<String, Object> action) {
        stream().forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
    }

    /**
     * Returns a map containing all attributes in this context.
     *
     * @return a map of all attributes with their keys and values
     */
    public Map<String, Object> getAllAttributes() {
        return executionContext.getAttributes();
    }
}
