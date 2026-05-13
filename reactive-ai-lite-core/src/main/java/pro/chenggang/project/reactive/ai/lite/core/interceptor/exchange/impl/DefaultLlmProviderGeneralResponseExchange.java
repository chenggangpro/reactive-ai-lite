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
import lombok.experimental.SuperBuilder;
import org.springframework.lang.Nullable;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderGeneralResponseExchange;

import java.util.Optional;

/**
 * The default implementation of {@link LlmProviderGeneralResponseExchange}.
 * <p>
 * This class extends {@link AbstractLlmProviderExchange} to provide the specific
 * response data for a non-streaming (general) request. It encapsulates the raw JSON
 * response body as an {@link ObjectNode} and any error that might have occurred
 * during the HTTP exchange.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@SuperBuilder
public class DefaultLlmProviderGeneralResponseExchange extends AbstractLlmProviderExchange implements LlmProviderGeneralResponseExchange {

    /**
     * The raw JSON response body. May be null if the request failed.
     */
    @Nullable
    private final ObjectNode rawResponseBody;

    /**
     * An error that occurred during the execution, if any.
     */
    @Nullable
    private final Throwable error;

    /**
     * Retrieves the raw JSON response body, if present.
     *
     * @return an {@link Optional} containing the raw JSON body, or empty
     */
    @Override
    public Optional<ObjectNode> rawResponseBody() {
        return Optional.ofNullable(this.rawResponseBody);
    }

    /**
     * Retrieves the execution error, if one occurred.
     *
     * @return an {@link Optional} containing the error, or empty
     */
    @Override
    public Optional<Throwable> error() {
        return Optional.ofNullable(this.error);
    }
}
