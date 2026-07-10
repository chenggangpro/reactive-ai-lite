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

/**
 * Interceptor exchange phase that represents the outbound LLM request and provides
 * mutable access to its JSON payload.
 * <p>
 * Implementations of this interface are made available to interceptors that execute
 * <em>before</em> the request is dispatched to the AI provider. By exposing a
 * mutable {@link ObjectNode}, it allows the interceptor chain to inspect, transform,
 * or augment the request body (e.g., injecting system prompts, adjusting temperature,
 * attaching tracing metadata) in a standardized fashion.
 * </p>
 * <p>
 * In addition, this interface declares a well‑known attribute key under which the
 * raw provider response can be stored once available, enabling post‑request
 * interceptors to access the original response payload through the exchange.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProviderExchange
 */
public interface LlmProviderRequestExchange extends LlmProviderExchange {

    /**
     * Attribute key used to store the raw JSON response body received from the LLM
     * provider.
     * <p>
     * After the request is executed, the unprocessed response payload (as a
     * {@link com.fasterxml.jackson.databind.JsonNode}) should be placed into the
     * exchange's {@link #parsingAttributes()} map using this key. This enables
     * downstream interceptors (e.g., logging, auditing, or response‑transformation
     * interceptors) to retrieve the original provider response without having to
     * re‑parse or buffer it again.
     * </p>
     */
    String RAW_RESPONSE_BODY_ATTRIBUTE_KEY = LlmProviderRequestExchange.class.getName() + ".raw-response-body";

    /**
     * Returns the mutable JSON object node that constitutes the payload of the
     * outbound LLM request.
     * <p>
     * Because the returned {@link ObjectNode} is the actual reference used during
     * serialization, any modifications made by an interceptor (e.g., adding,
     * removing, or changing fields) are directly reflected in the request that
     * reaches the provider. This allows interceptors to implement dynamic prompt
     * engineering, parameter overrides, and custom metadata injection without
     * altering the client code.
     * </p>
     *
     * @return the raw request payload as a mutable {@link ObjectNode}, never
     * {@code null}
     */
    ObjectNode rawRequestBody();
}