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
 * An abstract base class that implements the {@link AttributesAbility} interface.
 * <p>
 * This class provides a thread-safe, concurrent backing map for storing and managing
 * attributes. It is intended to be extended by message or context classes that require
 * the ability to carry metadata or context-specific data across the application.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public abstract class AbstractAttributeMessage implements AttributesAbility {

    /**
     * The underlying map storing the attributes.
     * <p>
     * Protected access allows subclasses to potentially interact with the map
     * directly if necessary, though standard access should be via the methods
     * defined in {@link AttributesAbility}.
     * </p>
     */
    protected final Map<String, Object> attributes;

    /**
     * Constructs a new {@link AbstractAttributeMessage} with an empty, concurrent attribute map.
     */
    public AbstractAttributeMessage() {
        this(null);
    }

    /**
     * Constructs a new {@link AbstractAttributeMessage} using the provided attribute map.
     * <p>
     * If the provided map is {@code null}, a new {@link ConcurrentHashMap} is created
     * to ensure thread-safe operations on the attributes.
     * </p>
     *
     * @param attributes the initial map of attributes, or {@code null} to create a new empty map
     */
    protected AbstractAttributeMessage(@Nullable Map<String, Object> attributes) {
        this.attributes = Objects.isNull(attributes) ? new ConcurrentHashMap<>() : attributes;
    }

    /**
     * Retrieves the map of attributes.
     *
     * @return the mutable map of attributes
     */
    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }
}
