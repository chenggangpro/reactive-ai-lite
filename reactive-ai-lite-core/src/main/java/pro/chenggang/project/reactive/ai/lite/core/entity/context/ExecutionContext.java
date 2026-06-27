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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.AbstractAttribute;
import pro.chenggang.project.reactive.ai.lite.core.spec.ExecutionContextSpec.ContextMerger;
import reactor.util.context.Context;

import java.util.Map;
import java.util.Objects;

/**
 * Represents the mutable execution context for reactive AI operations.
 * <p>
 * This class manages the execution state and attributes during the lifecycle of an AI request.
 * It acts as a thread-safe container for storing contextual metadata and shared data across
 * interceptors, providers, and execution engines.
 * </p>
 * <p>
 * The execution context is typically bound to and propagated through the Reactor {@link Context}.
 * It extends {@link AbstractAttribute} to provide a concurrent key-value store for arbitrary data,
 * allowing modular components in the pipeline to exchange information dynamically.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see AbstractAttribute
 * @see Context
 */
@Slf4j
@Getter
public class ExecutionContext extends AbstractAttribute {

    /**
     * Constructs a new {@link ExecutionContext}.
     * <p>
     * Initializes the thread-safe backing attribute map inherited from {@link AbstractAttribute}.
     * </p>
     */
    private ExecutionContext() {
    }

    /**
     * Creates a new instance of an {@link ExecutionContext}.
     * <p>
     * This factory method is the primary way to instantiate a fresh execution context
     * at the beginning of an AI request pipeline.
     * </p>
     *
     * @return a new, empty {@link ExecutionContext} instance
     */
    public static ExecutionContext newContext() {
        return new ExecutionContext();
    }

    /**
     * Initializes the {@link ExecutionContext} in the Reactor {@link Context} if it is not already present.
     * <p>
     * If an existing context is found, it will merge any parent attributes using the provided {@link ContextMerger}.
     * Otherwise, a new execution context is created, parent attributes are merged, and it is stored in the Reactor context.
     * </p>
     *
     * @param context          the current Reactor context
     * @param parentAttributes the attributes from the parent context, can be null
     * @param contextConfigure the merger logic to combine parent attributes and customize the context, can be null
     * @return the Reactor context containing the initialized or existing {@link ExecutionContext}
     */
    public static Context initializeExecutionContext(Context context, Map<String, Object> parentAttributes, ContextMerger contextConfigure) {
        ExecutionContext executionContext = ExecutionContext.newContext();
        context.<ExecutionContext>getOrEmpty(ExecutionContext.class)
                .ifPresent(existing -> executionContext.getAttributes().putAll(existing.getAttributes()));
        if (Objects.nonNull(contextConfigure)) {
            contextConfigure.merge(executionContext, parentAttributes);
        }
        log.debug("Initial execution context: {}", executionContext);
        return context.put(ExecutionContext.class, executionContext);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(this.hashCode()) + " with " + this.getAttributes().size() + " attributes";
    }
}
