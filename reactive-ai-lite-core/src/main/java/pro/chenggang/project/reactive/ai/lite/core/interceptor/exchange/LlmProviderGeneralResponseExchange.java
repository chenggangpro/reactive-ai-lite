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
 * a response, this exchange carries the unprocessed JSON response body. It extends
 * {@link LlmProviderResponseExchange} to allow interceptors to access common response
 * metadata (status, headers, etc.) in addition to manipulating the raw response content
 * before it is transformed into domain-specific objects.
 * </p>
 * <p>
 * This exchange is typically used by interceptors that need to inspect, transform, or
 * enrich the raw LLM output, for instance for auditing, content filtering, or adding
 * provider-specific metadata. The {@link #rawResponseBody()} method provides direct
 * access to the Jackson {@link ObjectNode}, enabling structural modifications.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProviderResponseExchange
 * @see ObjectNode
 */
public interface LlmProviderGeneralResponseExchange extends LlmProviderResponseExchange {

    /**
     * Returns the raw JSON response body as a Jackson {@link ObjectNode} tree, if available.
     * <p>
     * This method provides mutable access to the entire JSON structure returned by the
     * provider. Interceptors can navigate and modify the tree directly using the Jackson
     * node API. Changes made here will propagate to the subsequent parsing and object mapping
     * stages.
     * </p>
     * <p>
     * An empty {@link Optional} indicates that the response could not be parsed as JSON
     * (e.g., due to a network error or a non-JSON response). In such cases, the exchange
     * may still contain error details accessible via superclass methods.
     * </p>
     *
     * @return an {@link Optional} containing the JSON tree, or {@link Optional#empty()} if no valid JSON body
     */
    Optional<ObjectNode> rawResponseBody();
}