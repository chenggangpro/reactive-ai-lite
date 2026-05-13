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
 * Represents the data exchange specifically for outbound LLM requests.
 * <p>
 * This interface extends {@link LlmProviderExchange} to provide access to the raw
 * JSON request body that is about to be dispatched to the AI provider. Interceptors
 * operating before the request execution can use this interface to inspect or modify
 * the payload.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmProviderRequestExchange extends LlmProviderExchange {

    /**
     * The key used to store the raw request body within the parsingAttributes map.
     */
    String RAW_REQUEST_BODY_ATTRIBUTE_KEY = LlmProviderRequestExchange.class.getName() + ".raw-request-body";

    /**
     * Retrieves the raw JSON request body that will be sent to the provider.
     * <p>
     * Interceptors can modify this {@link ObjectNode} to alter the request payload before it is sent.
     * </p>
     *
     * @return the raw request payload as a JSON {@link ObjectNode}
     */
    ObjectNode rawRequestBody();
}
