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
import java.util.function.Consumer;

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
     * The provided consumer is invoked with the {@link ExecutionContext} instance
     * after it has been initialized with the standard settings, allowing for arbitrary
     * attribute manipulation.
     * </p>
     *
     * @param contextConfigure a {@link Consumer} that accepts the {@link ExecutionContext} for further setup
     * @return this {@link ExecutionContextSpec} instance for method chaining
     */
    ExecutionContextSpec contextConfigure(@NonNull Consumer<ExecutionContext> contextConfigure);

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
}
