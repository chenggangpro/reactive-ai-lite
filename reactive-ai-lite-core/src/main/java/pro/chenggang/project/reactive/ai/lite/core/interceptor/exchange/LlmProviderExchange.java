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
package pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange;

import pro.chenggang.project.reactive.ai.lite.core.entity.AttributesAbility;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

/**
 * The central data structure that flows through the interceptor chain during an LLM request lifecycle.
 * <p>
 * This interface serves as a carrier for contextual information that interceptors need to make decisions,
 * such as logging, authentication, monitoring, or transformation. It combines the ability to carry custom
 * attributes (via {@link AttributesAbility}) with immutable facts about the current request: the shared
 * execution context, the type of LLM client being used, and metadata about the provider that will ultimately
 * handle the request.
 * </p>
 * <p>
 * The exchange object is typically created before the request is sent to the LLM provider and is passed
 * through each interceptor in the configured order. Because it aggregates provider- and client-specific
 * details, interceptors can be written in a generic way, relying on the exchange interface rather than
 * concrete implementation types. This promotes loose coupling and makes it easier to build reusable
 * interceptor components (e.g., for rate limiting, caching, or observability) that work across different
 * LLM clients and providers.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmProviderExchange extends AttributesAbility {

    /**
     * Returns the execution context associated with this exchange.
     * <p>
     * The execution context is established before the request begins and carries attributes such as
     * a correlation ID, user ID, or tenant information that should be preserved throughout the
     * request lifecycle. Interceptors can use this context to correlate logs, enforce tenant-based
     * policies, or propagate tracing headers. It differs from the per-exchange {@link AttributesAbility}
     * in that it is often set once and shared across multiple exchanges if needed.
     * </p>
     *
     * @return the {@link ExecutionContext} containing the pre-configured context of the current execution
     */
    ExecutionContext executionContext();

    /**
     * Indicates which kind of LLM client (chat, embedding, image generation, etc.) is being used.
     * <p>
     * This allows interceptors to apply type-specific logic without casting or guessing.
     * For example, a monitoring interceptor might tag metrics differently for chat completions
     * versus embeddings, or a security filter might apply stricter content checks on image
     * generation requests. The type is determined when the client is constructed and does not
     * change during the exchange.
     * </p>
     *
     * @return the {@link LlmClientType} that describes the LLM client in this exchange
     */
    LlmClientType clientType();

    /**
     * Provides metadata about the LLM provider that will handle the request.
     * <p>
     * The provider info includes identification details (like the provider name and model)
     * and can be used by interceptors to apply provider-specific transformations, validate
     * request compatibility, or enrich audit logs. Because the actual provider may be resolved
     * dynamically based on configuration, having this information in the exchange enables
     * interceptors to remain provider-agnostic while still being able to access necessary
     * metadata when required.
     * </p>
     *
     * @return the {@link LlmProviderInfo} containing details of the target LLM provider
     */
    LlmProviderInfo llmProviderInfo();

}