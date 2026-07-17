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

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

/**
 * An exchange representing a general, non-streaming response from an LLM provider.
 * <p>
 * In the interceptor pipeline, after the request is executed and the provider returns
 * a response, this exchange carries the unprocessed response body (usually JSON). It extends
 * {@link LlmProviderResponseExchange} to allow interceptors to access common response
 * metadata (status, headers, etc.) in addition to manipulating the raw response content
 * before it is transformed into domain-specific objects.
 * </p>
 * <p>
 * This exchange is typically used by interceptors that need to inspect, transform, or
 * enrich the raw LLM output, for instance for auditing, content filtering, or adding
 * provider-specific metadata. The {@link #rawResponseBody()} method provides direct
 * access to the raw response object (e.g., Jackson {@link ObjectNode}), enabling structural modifications.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProviderResponseExchange
 * @see ObjectNode
 */
public interface LlmProviderGeneralResponseExchange extends LlmProviderResponseExchange {

    /**
     * Returns the raw response body as a Java {@link Object}, if available.
     * <p>
     * Usually, it is a Jackson {@link ObjectNode} tree for JSON responses,
     * providing mutable access to the entire JSON structure returned by the provider.
     * However, for binary responses (e.g., speech audio), this could be empty or of a different type.
     * Interceptors can navigate and modify the structure directly. Changes made here will propagate
     * to the subsequent parsing and object mapping stages.
     * </p>
     * <p>
     * An empty {@link Optional} indicates that the response body is not available
     * (e.g., due to a network error or an empty response). In such cases, the exchange
     * may still contain error details accessible via superclass methods.
     * </p>
     *
     * @return an {@link Optional} containing the response body, or {@link Optional#empty()} if not available
     */
    Optional<Object> rawResponseBody();
}