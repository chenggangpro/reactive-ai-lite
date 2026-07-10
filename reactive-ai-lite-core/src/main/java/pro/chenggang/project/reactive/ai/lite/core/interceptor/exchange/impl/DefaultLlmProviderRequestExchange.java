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
 * This class extends {@link AbstractLlmProviderExchange} to provide a concrete
 * representation of a request exchange intended for an LLM (Large Language Model) provider.
 * It captures the raw JSON payload that will be sent as the HTTP request body
 * and exposes it to the interceptor chain. Interceptors can access and modify
 * the {@code ObjectNode} before the request is actually transmitted, enabling
 * use cases such as request logging, payload enrichment, content filtering,
 * or dynamic vendor-specific transformation.
 * </p>
 * <p>
 * The class is designed to be instantiated via a builder pattern using
 * Lombok's {@link SuperBuilder}, which allows both the superclass fields and
 * the {@code rawRequestBody} field to be set in a fluent manner.
 * </p>
 * <p>
 * Because the {@code ObjectNode} is mutable, interceptors can alter the JSON
 * structure in-place; however, the reference to the {@code ObjectNode} itself
 * is final, ensuring that the exchange object always refers to the same
 * underlying JSON tree. The {@link NonNull} constraint guarantees that the
 * request body is never {@code null} when the exchange is constructed.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProviderRequestExchange
 * @see AbstractLlmProviderExchange
 */
@SuperBuilder
public class DefaultLlmProviderRequestExchange extends AbstractLlmProviderExchange implements LlmProviderRequestExchange {

    /**
     * The raw JSON payload intended for the LLM provider.
     * <p>
     * This field holds the entire request body as a Jackson {@link ObjectNode},
     * which is a tree representation of a JSON object. It is used by the
     * exchange to allow interceptors to inspect and mutate the request content
     * without needing to re-serialize the body. The mutability of the
     * {@code ObjectNode} enables on-the-fly modifications, while the final
     * modifier ensures the reference is never reassigned after creation.
     * The {@link NonNull} annotation enforces that this field must be provided
     * at builder time, preventing a {@code null} request body from being sent
     * to the provider.
     * </p>
     */
    @NonNull
    private final ObjectNode rawRequestBody;

    /**
     * Retrieves the raw JSON request body that will be sent to the LLM provider.
     * <p>
     * This method implements {@link LlmProviderRequestExchange#rawRequestBody()}
     * and returns the same {@link ObjectNode} instance that was provided during
     * construction. Interceptors call this method to access the payload for
     * inspection, transformation, or logging purposes. Because the returned
     * object is mutable, any changes made to it will directly affect the
     * actual HTTP request body.
     * </p>
     *
     * @return the JSON request body as a mutable {@link ObjectNode}; never {@code null}
     */
    @Override
    public ObjectNode rawRequestBody() {
        return this.rawRequestBody;
    }

}