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
import pro.chenggang.project.reactive.ai.lite.core.entity.values.TraceId;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A specification for configuring the execution context of an AI operation.
 * This interface provides a fluent API to set up tracing, attributes, and other
 * context-related properties before executing a request.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface ExecutionContextSpec {

    /**
     * Sets the trace ID from a parent execution context. This is useful for linking
     * a series of related AI calls together for observability and debugging.
     *
     * @param parentTraceId The {@link TraceId} of the parent execution.
     * @return This {@link ExecutionContextSpec} instance for method chaining.
     */
    ExecutionContextSpec parentTraceId(@NonNull TraceId parentTraceId);

    /**
     * Sets attributes inherited from a parent execution context. These attributes
     * can carry metadata across related AI operations.
     *
     * @param parentAttributes A map of attributes from the parent context.
     * @return This {@link ExecutionContextSpec} instance for method chaining.
     */
    ExecutionContextSpec parentAttributes(@NonNull Map<String, Object> parentAttributes);

    /**
     * Registers a custom supplier for generating unique trace IDs. If not provided,
     * a default generator will be used.
     *
     * @param traceIdGenerator A {@link Supplier} that returns a new trace ID as a String.
     * @return This {@link ExecutionContextSpec} instance for method chaining.
     */
    ExecutionContextSpec traceIdGenerator(@NonNull Supplier<String> traceIdGenerator);

    /**
     * Provides a consumer to perform advanced or custom configuration on the
     * {@link ExecutionContext} instance after it has been initialized with the
     * other settings from this spec.
     *
     * @param contextConfigure A {@link Consumer} that accepts the {@link ExecutionContext} for further setup.
     * @return This {@link ExecutionContextSpec} instance for method chaining.
     */
    ExecutionContextSpec contextConfigure(@NonNull Consumer<ExecutionContext> contextConfigure);

    /**
     * Returns the next specification in the fluent API chain for configuring
     * provider-specific settings.
     *
     * @return A {@link ProviderSpec} instance.
     */
    ProviderSpec providerSpec();
}
