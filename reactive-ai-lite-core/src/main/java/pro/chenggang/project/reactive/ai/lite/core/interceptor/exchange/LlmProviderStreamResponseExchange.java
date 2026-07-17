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
 * An exchange context that encapsulates the streaming response from an LLM provider.
 * <p>
 * This interface is a specialized subtype of {@link LlmProviderResponseExchange} tailored
 * for Server-Sent Events (SSE) and similar streaming protocols. It is created after a
 * streaming request has been dispatched to the provider and becomes accessible to
 * response-phase interceptors. These interceptors can inspect, transform, or aggregate
 * the inbound event stream without blocking the entire pipeline.
 * </p>
 * <p>
 * The core data source is a {@link Flux} of {@link RawStreamResponse} items, each
 * representing a single parsed JSON chunk emitted by the provider. Because the flux
 * is potentially infinite (until a completion event or error), this exchange is designed
 * to be used in a reactive, non-blocking manner.
 * </p>
 * <p>
 * Typical use cases include:
 * <ul>
 *   <li><strong>Logging:</strong> recording each chunk as it arrives without delaying the downstream consumer.</li>
 *   <li><strong>Metrics:</strong> calculating token usage or latency by buffering and analysing chunks.</li>
 *   <li><strong>Content filtering:</strong> modifying or discarding chunks on-the-fly before they reach the client.</li>
 *   <li><strong>Aggregation:</strong> assembling a complete response from multiple chunks for caching or auditing.</li>
 * </ul>
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmProviderStreamResponseExchange extends LlmProviderResponseExchange {

    /**
     * Provides the raw event stream as a cold {@link reactor.core.publisher.Flux} or other stream objects.
     * <p>
     * The returned object is the original source from the provider's HTTP connection. It is
     * <em>cold</em> by design, meaning that each subscription triggers a new replay of the
     * streamed data from the underlying buffer or network. Interceptors should subscribe
     * cautiously and avoid multiple subscriptions without proper sharing (e.g., via
     * {@code flux.cache()} or {@code flux.share()}) to prevent data duplication or loss.
     * </p>
     * <p>
     * Each chunk typically corresponds to one parsed JSON event from the
     * SSE stream or a raw byte buffer for binary streams. Typical events include:
     * <ul>
     *   <li><strong>Data events:</strong> contain partial or full response content.</li>
     *   <li><strong>Finish events:</strong> indicate the end of the stream with optional metadata (e.g., token usage).</li>
     *   <li><strong>Error events:</strong> propagate provider-side errors.</li>
     * </ul>
     * </p>
     *
     * @return a non-null {@link Object} representing raw stream chunks as they become available
     */
    Flux<?> rawStreamResponse();

}