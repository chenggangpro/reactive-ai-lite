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

import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import reactor.core.publisher.Flux;

/**
 * Represents the data exchange specifically for streaming responses from the LLM provider.
 * <p>
 * This interface extends {@link LlmProviderResponseExchange} to handle Server-Sent Events (SSE).
 * It exposes the inbound stream as a {@link Flux} of {@link RawStreamResponse} chunks.
 * Interceptors operating after the request execution can use this to inspect, filter,
 * or aggregate the streamed data as it arrives.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmProviderStreamResponseExchange extends LlmProviderResponseExchange {

    /**
     * Retrieves the continuous stream of raw responses from the provider.
     * <p>
     * Each emitted {@link RawStreamResponse} represents a single parsed JSON chunk
     * from the underlying event stream.
     * </p>
     *
     * @return a {@link Flux} emitting raw stream chunks
     */
    Flux<RawStreamResponse> rawStreamResponse();

}
