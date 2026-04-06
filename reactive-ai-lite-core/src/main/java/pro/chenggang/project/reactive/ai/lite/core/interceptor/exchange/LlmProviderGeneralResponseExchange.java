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
 * Represents the data exchange specifically for general, non-streaming responses from the LLM provider.
 * <p>
 * This interface extends {@link LlmProviderResponseExchange} to provide access to the complete,
 * unparsed JSON response body received from the provider. Interceptors operating after the request
 * execution can use this to inspect or modify the payload before it is parsed into domain objects.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmProviderGeneralResponseExchange extends LlmProviderResponseExchange {

    /**
     * Retrieves the raw JSON response body returned by the provider, if available.
     * <p>
     * This will be empty if the request failed before a valid JSON body could be
     * received or parsed (e.g., a network error occurred).
     * </p>
     *
     * @return an {@link Optional} containing the JSON {@link ObjectNode}, or empty
     */
    Optional<ObjectNode> rawResponseBody();
}
