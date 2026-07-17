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
 * The default implementation of {@link LlmProviderStreamResponseExchange} used by the reactive AI lite framework.
 * <p>
 * This class serves as a container for the streaming response received from an LLM (Large Language Model) provider
 * over a reactive HTTP connection. Unlike its non-streaming counterpart, this exchange holds a {@link Flux} of
 * {@link RawStreamResponse} chunks that are produced by the provider in real-time, allowing downstream components
 * (such as interceptors or response handlers) to process the stream reactively with backpressure support.
 * <p>
 * Additionally, this exchange captures any {@link Throwable} that occurred during the initial request or stream
 * establishment, enabling error-aware processing without disrupting the reactive flow. The {@code @SuperBuilder}
 * annotation provides a builder pattern that can be extended by subclasses, ensuring consistency in object creation
 * across the interceptor chain.
 * <p>
 * Instances of this class are typically created by the framework's HTTP client layer after successfully initiating
 * a streaming connection. They are then passed through a chain of interceptors (implementing
 * {@link pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptor}) before reaching the
 * final response handler. The raw stream can be transformed, filtered, or enriched by interceptors, and the error
 * field can be inspected to trigger fallback logic.
 * <p>
 * The immutability of the fields (marked as {@code private final}) guarantees thread-safety and prevents accidental
 * modifications during the exchange lifecycle.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see AbstractLlmProviderExchange
 * @see LlmProviderStreamResponseExchange
 * @see RawStreamResponse
 */
@SuperBuilder
public class DefaultLlmProviderStreamResponseExchange extends AbstractLlmProviderExchange implements LlmProviderStreamResponseExchange {

    /**
     * The reactive stream of raw JSON response chunks or binary payloads from the LLM provider.
     * <p>
     * This field typically holds a {@link reactor.core.publisher.Flux} that emits chunk objects (e.g. {@link RawStreamResponse}
     * or {@link org.springframework.core.io.buffer.DataBuffer}) as they become available from the underlying HTTP
     * connection.
     * <p>
     * The field is nullable to indicate that the stream could not be established at all (e.g., due to network issues,
     * HTTP 4xx/5xx responses, or timeout). In such cases, the {@link #error()} method provides the reason. By default,
     * the {@link #rawStreamResponse()} method returns an empty {@link reactor.core.publisher.Flux} when this field is {@code null} to avoid
     * null checks in downstream operators.
     * <p>
     * This field is set once via the builder and is not intended to be modified afterward, aligning with the immutable
     * design pattern of the exchange object.
     */
    @Nullable
    private final Flux<?> rawStreamResponse;

    /**
     * The error that prevented the normal execution of the streaming request, if any.
     * <p>
     * This field holds the exception or {@link Throwable} that occurred during the HTTP request initiation or stream
     * processing. Common scenarios include network timeouts, SSL handshake failures, provider-side errors returned
     * as HTTP status codes, or interceptors deliberately aborting the exchange. When this field is non-null, the
     * {@link #rawStreamResponse()} will typically be empty or null.
     * <p>
     * Interceptors can use this field to implement error handling strategies such as retries, fallback responses,
     * or metric recording. The {@link #error()} method provides convenient {@link Optional} access.
     */
    @Nullable
    private final Throwable error;

    /**
     * Returns the reactive stream of raw response chunks or binary buffers.
     * <p>
     * This accessor provides a safe, non-null view of the {@link #rawStreamResponse} field. If the stream could not
     * be initiated (field is {@code null}), an empty {@link reactor.core.publisher.Flux} is returned to simplify downstream operators and
     * avoid {@link NullPointerException} risks. The returned stream is the original instance if present,
     * preserving its lazy and cold nature.
     *
     * @return the raw stream response object (typically a {@link reactor.core.publisher.Flux}); never {@code null}
     */
    @Override
    public Flux<?> rawStreamResponse() {
        return this.rawStreamResponse != null ? this.rawStreamResponse : Flux.empty();
    }

    /**
     * Returns the execution error, if any, wrapped in an {@link Optional}.
     * <p>
     * This method allows interceptors and response handlers to check for errors without null checks. A non-empty
     * optional indicates that the streaming request failed, and the contained {@link Throwable} can be used for
     * detailed diagnosis, logging, or recovery. It is recommended to inspect this error before attempting to
     * consume the raw stream, as errors often correlate with a missing stream.
     *
     * @return an {@link Optional} containing the error if present, otherwise {@link Optional#empty()}
     */
    @Override
    public Optional<Throwable> error() {
        return Optional.ofNullable(error);
    }
}