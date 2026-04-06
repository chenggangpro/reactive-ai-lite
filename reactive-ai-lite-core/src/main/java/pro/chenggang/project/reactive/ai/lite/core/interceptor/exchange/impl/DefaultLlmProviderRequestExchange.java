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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderRequestExchange;

/**
 * The default implementation of {@link LlmProviderRequestExchange}.
 * <p>
 * This class extends {@link AbstractLlmProviderExchange} to include the raw JSON
 * request body that will be sent to the LLM provider. Interceptors can access
 * and modify this {@code ObjectNode} before the HTTP request is executed.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@SuperBuilder
public class DefaultLlmProviderRequestExchange extends AbstractLlmProviderExchange implements LlmProviderRequestExchange {

    /**
     * The raw JSON payload intended for the provider.
     */
    @NonNull
    private final ObjectNode rawRequestBody;

    /**
     * Retrieves the raw JSON request body.
     *
     * @return the JSON request body
     */
    @Override
    public ObjectNode rawRequestBody() {
        return this.rawRequestBody;
    }

}
