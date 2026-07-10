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

import org.springframework.lang.Nullable;
import pro.chenggang.project.reactive.ai.lite.core.entity.AttributesAbility;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe base implementation of {@link AttributesAbility} that provides a mutable,
 * concurrent attribute store.
 * <p>
 * This class is designed to be extended by any entity that needs to carry additional metadata
 * or context-specific key-value pairs through the application pipeline. By using a
 * {@link ConcurrentHashMap} as the underlying storage, it guarantees safe concurrent access
 * from multiple threads without the need for external synchronization. This is especially
 * important in reactive and asynchronous environments where messages and contexts can be
 * processed in parallel.
 * <p>
 * Subclasses can choose to interact with the attribute map directly through the protected
 * {@link #attributes} field, or rely on the methods exposed by {@link AttributesAbility}.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see AttributesAbility
 */
public abstract class AbstractAttribute implements AttributesAbility {

    /**
     * The concurrent map that holds all attributes.
     * <p>
     * A {@link ConcurrentHashMap} is chosen over a plain {@link java.util.HashMap} to ensure
     * thread safety without locking the entire map. It allows multiple readers and a limited
     * number of writers to operate concurrently, which is essential for high-throughput,
     * non-blocking applications. The map is declared {@code protected} so that subclasses can
     * provide custom serialization, extension methods, or direct access if required, while
     * still maintaining encapsulation of the core storage.
     * </p>
     */
    protected final Map<String, Object> attributes;

    /**
     * Constructs a new instance with an empty attribute map.
     * <p>
     * Delegates to {@link #AbstractAttribute(Map)} with a {@code null} argument, which
     * results in a fresh, empty {@link ConcurrentHashMap}. This constructor is ideal for
     * situations where no initial metadata is available and attributes will be built up
     * progressively.
     * </p>
     */
    public AbstractAttribute() {
        this(null);
    }

    /**
     * Constructs a new instance and initializes the attribute map from an existing map.
     * <p>
     * If the provided map is non-null, its entries are copied into a new
     * {@link ConcurrentHashMap}. This copying ensures that the internal store remains
     * thread-safe and isolated from any external modifications to the original map. If the
     * supplied map is {@code null}, a new empty {@link ConcurrentHashMap} is used,
     * guaranteeing that the instance always has a valid, mutable attribute container.
     * </p>
     *
     * @param attributes the initial attributes to seed the store, or {@code null} if none
     */
    protected AbstractAttribute(@Nullable Map<String, Object> attributes) {
        this.attributes = Objects.isNull(attributes) ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(attributes);
    }

    /**
     * Returns the underlying mutable map that holds all attributes.
     * <p>
     * The returned map is the same instance used internally, so any modifications made to
     * it are immediately reflected in the attribute store. This aligns with the contract of
     * {@link AttributesAbility#getAttributes()}, allowing callers to read, add, or remove
     * metadata without additional synchronization. Because the backing map is a
     * {@link ConcurrentHashMap}, concurrent operations are safe even in highly parallel
     * scenarios.
     * </p>
     *
     * @return the modifiable, thread-safe map of attributes
     */
    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }
}