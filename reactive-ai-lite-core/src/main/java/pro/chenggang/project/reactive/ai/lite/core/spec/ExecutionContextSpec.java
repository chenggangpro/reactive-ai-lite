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
package pro.chenggang.project.reactive.ai.lite.core.spec;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;

import java.util.Map;
import java.util.Objects;

/**
 * A specification for configuring the execution context of an AI operation.
 * <p>
 * This interface provides a fluent API to set up tracing, parsingAttributes, and other
 * context-related properties before executing a request. It serves as the initial
 * stage in configuring an AI interaction.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface ExecutionContextSpec {

    /**
     * Sets parsingAttributes inherited from a parent execution context.
     * <p>
     * These parsingAttributes can carry metadata, correlation IDs, or shared state
     * across related AI operations, facilitating distributed tracing and context propagation.
     * </p>
     *
     * @param parentAttributes a map of parsingAttributes from the parent context
     * @return this {@link ExecutionContextSpec} instance for method chaining
     */
    ExecutionContextSpec parentAttributes(Map<String, Object> parentAttributes);

    /**
     * Provides a mechanism for custom or advanced configuration of the
     * execution context.
     * <p>
     * The provided merger is invoked with the {@link ExecutionContext} instance
     * and parent attributes to perform custom merging and setup.
     * </p>
     *
     * @param contextConfigure a {@link ContextMerger} to merge attributes and customize the context
     * @return this {@link ExecutionContextSpec} instance for method chaining
     */
    ExecutionContextSpec contextConfigure(@NonNull ContextMerger contextConfigure);

    /**
     * Transitions from context configuration to provider specification.
     * <p>
     * This method returns the next specification in the fluent API chain,
     * allowing for the selection and configuration of the specific AI provider
     * to handle the request.
     * </p>
     *
     * @return a {@link ProviderSpec} instance for further configuration
     */
    ProviderSpec providerSpec();

    /**
     * Functional interface used to merge parent attributes and apply custom configurations
     * to the active {@link ExecutionContext}.
     */
    @FunctionalInterface
    interface ContextMerger {

        /**
         * A default merger implementation that appends all non-empty parent attributes
         * directly into the execution context's attribute map.
         */
        ContextMerger APPEND_ALL = (executionContext, parentAttributes) -> {
            if (Objects.nonNull(parentAttributes) && !parentAttributes.isEmpty()) {
                executionContext.getAttributes().putAll(parentAttributes);
            }
        };

        /**
         * Merges parent attributes and custom configurations into the given execution context.
         *
         * @param executionContext the active execution context to populate
         * @param parentAttributes the attributes from the parent context, can be null
         */
        void merge(ExecutionContext executionContext, Map<String, Object> parentAttributes);
    }
}
