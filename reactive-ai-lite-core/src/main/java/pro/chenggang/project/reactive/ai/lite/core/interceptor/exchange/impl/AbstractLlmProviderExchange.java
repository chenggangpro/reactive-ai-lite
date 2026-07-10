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
package pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.impl;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderExchange;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.util.Map;

/**
 * An abstract foundation for {@link LlmProviderExchange} implementations that carry phase-agnostic metadata
 * through the interceptor chain.
 * <p>
 * This class encapsulates the common elements required by all exchange types (request, generic response,
 * streaming response): the mutable {@link #attributes} map for interceptor communication, the
 * {@link ExecutionContext} holding request-specific state, the {@link LlmClientType} identifying the LLM adapter,
 * and the {@link LlmProviderInfo} containing provider-specific details. By centralizing these fields, subclasses
 * only need to extend this base and add phase-specific payloads, ensuring consistency and enabling the
 * {@link SuperBuilder} pattern to construct immutable exchange instances with a fluent API.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@SuperBuilder
public abstract class AbstractLlmProviderExchange implements LlmProviderExchange {

    /**
     * A mutable map that serves as a shared clipboard for interceptors along the processing pipeline.
     * Interceptors can store arbitrary key–value pairs to communicate state or pass metadata between
     * themselves (e.g., caching parsed results, timestamp markers, or custom flags). The map is
     * intentionally mutable to allow dynamic addition and removal during chain execution.
     * Populated via the builder and made visible to all interceptors through {@link #getAttributes()}.
     */
    @NonNull
    protected final Map<String, Object> attributes;

    /**
     * The type of the LLM client adapter used to handle the current request. This field distinguishes
     * between different LLM backends (e.g., OpenAI, Azure, Ollama) and is used by interceptors to
     * tailor behavior based on the client type, such as adjusting request formats or interpreting
     * response structures.
     */
    @NonNull
    protected final LlmClientType clientType;

    /**
     * Metadata describing the concrete LLM provider, including endpoint URLs, authentication tokens,
     * model naming conventions, and any provider-specific options. This immutable information is
     * injected at construction time and serves as a reference for interceptors that need to know
     * exactly which provider is being called, without requiring repeated lookups.
     */
    @NonNull
    protected final LlmProviderInfo llmProviderInfo;

    /**
     * The execution context that carries all request-scoped data, including original user messages,
     * conversation history, tool definitions, and other parameters. It is passed unchanged through
     * the entire interception chain and is available via {@link #executionContext()}.
     */
    @NonNull
    protected final ExecutionContext executionContext;

    /**
     * Returns the mutable parsingAttributes map, which is the primary vehicle for interceptor communication.
     * Interceptors can read from and write to this map during the exchange. Because the map is the same instance
     * across all phases, state set in a request interceptor can be retrieved in a response interceptor.
     *
     * @return the parsingAttributes map
     */
    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    /**
     * Returns the execution context that encapsulates the current request's conversation state,
     * tool history, and configuration. Interceptors can inspect this context to enrich logging
     * or make conditional decisions.
     *
     * @return the execution context
     */
    @Override
    public ExecutionContext executionContext() {
        return this.executionContext;
    }

    /**
     * Returns the {@link LlmClientType} that identifies the LLM adapter handling the request.
     * This value remains constant throughout the exchange and can be used by interceptors to
     * implement client-type-specific logic.
     *
     * @return the client type
     */
    @Override
    public LlmClientType clientType() {
        return this.clientType;
    }

    /**
     * Returns the {@link LlmProviderInfo} object containing the details of the LLM provider being invoked.
     * This includes information such as provider name, endpoint, and API version, which may be used by
     * interceptors to customize authentication or logging.
     *
     * @return the provider info
     */
    @Override
    public LlmProviderInfo llmProviderInfo() {
        return this.llmProviderInfo;
    }
}