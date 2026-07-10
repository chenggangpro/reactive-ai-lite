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
package pro.chenggang.project.reactive.ai.lite.core.provider;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.EmbeddingResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.EmbeddingExecutionInfo;
import reactor.core.publisher.Mono;

/**
 * Contract for executing embedding operations within the reactive AI lite framework.
 * <p>
 * Embedding providers transform textual or multimodal input into numerical vector representations,
 * enabling similarity search, clustering, and other downstream ML tasks. This interface extends
 * {@link LlmProvider} to integrate with the broader provider ecosystem, and specializes in 
 * handling embedding-specific execution details.
 * <p>
 * Implementations are expected to support both structured responses (via {@link #executeEmbedding(EmbeddingExecutionInfo)})
 * that return a fully parsed {@link EmbeddingResponse}, and raw responses (via {@link #executeEmbeddingRaw(EmbeddingExecutionInfo)})
 * that give callers direct access to the provider's native payload for debuggability or custom processing.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmEmbeddingProvider extends LlmProvider {

    /**
     * Submits an embedding execution request and returns a {@link Mono} emitting the structured result.
     * <p>
     * This method is the primary entry point for obtaining embeddings in a clean, provider-agnostic format.
     * The {@link EmbeddingExecutionInfo} encapsulates all necessary parameters (e.g., model name, input texts,
     * dimensions configuration) and might carry contextual session or authentication data. The returned 
     * {@link EmbeddingResponse} provides a uniform representation of the embedding vectors and associated 
     * metadata, shielding callers from provider-specific intricacies.
     * <p>
     * The reactive {@link Mono} allows non-blocking integration, enabling the embedding operation to be
     * composed with other reactive steps or retried with backpressure-aware operators.
     *
     * @param executionInfo the fully prepared execution context and parameters; must not be {@code null}
     * @return a {@link Mono} that, when subscribed to, initiates the embedding process and emits the
     *         structured response upon successful completion
     */
    Mono<EmbeddingResponse> executeEmbedding(@NonNull EmbeddingExecutionInfo executionInfo);

    /**
     * Submits an embedding execution request and returns a {@link Mono} emitting the raw provider response.
     * <p>
     * Unlike {@link #executeEmbedding(EmbeddingExecutionInfo)}, this method bypasses the internal response
     * parsing, giving direct access to the original HTTP payload, status codes, and headers. This is
     * particularly useful for debugging unexpected responses, implementing provider-specific features not
     * covered by the common {@link EmbeddingResponse} model, or performing custom logging and auditing.
     * <p>
     * The {@link RawResponse} typically contains the raw body as a byte array or string, along with metadata
     * about the transport. As with other reactive methods, the {@link Mono} ensures non-blocking execution.
     *
     * @param executionInfo the fully prepared execution context and parameters; must not be {@code null}
     * @return a {@link Mono} that, when subscribed to, initiates the embedding request and emits the 
     *         raw provider response upon completion
     */
    Mono<RawResponse> executeEmbeddingRaw(@NonNull EmbeddingExecutionInfo executionInfo);
}