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

import lombok.experimental.SuperBuilder;
import org.springframework.lang.Nullable;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderStreamResponseExchange;
import reactor.core.publisher.Flux;

import java.util.Optional;

/**
 * The default implementation of {@link LlmProviderStreamResponseExchange}.
 * <p>
 * This class extends {@link AbstractLlmProviderExchange} to provide the
 * specific response data for a streaming request. It encapsulates the inbound
 * {@link Flux} of raw JSON stream chunks and any error that might have
 * occurred during the HTTP exchange.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@SuperBuilder
public class DefaultLlmProviderStreamResponseExchange extends AbstractLlmProviderExchange implements LlmProviderStreamResponseExchange {

    /**
     * The stream of raw JSON response chunks. May be null if the stream failed to initiate.
     */
    @Nullable
    private final Flux<RawStreamResponse> rawStreamResponse;

    /**
     * An error that occurred during the execution, if any.
     */
    @Nullable
    private final Throwable error;

    /**
     * Retrieves the stream of raw JSON responses.
     *
     * @return the {@link Flux} of stream chunks
     */
    @Override
    public Flux<RawStreamResponse> rawStreamResponse() {
        return this.rawStreamResponse;
    }

    /**
     * Retrieves the execution error, if one occurred.
     *
     * @return an {@link Optional} containing the error, or empty
     */
    @Override
    public Optional<Throwable> error() {
        return Optional.ofNullable(error);
    }
}
