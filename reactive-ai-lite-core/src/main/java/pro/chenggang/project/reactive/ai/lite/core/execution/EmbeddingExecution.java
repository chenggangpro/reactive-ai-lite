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
package pro.chenggang.project.reactive.ai.lite.core.execution;

import pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawResponseConverter;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.EmbeddingResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import reactor.core.publisher.Mono;

/**
 * Defines the contract for executing embedding requests against an AI provider.
 * <p>
 * This interface provides methods to retrieve both the processed {@link EmbeddingResponse}
 * (e.g., embedding vector) and the raw provider-specific response. Implementations are
 * expected to handle communication with the underlying AI service reactively using
 * Project Reactor's {@link Mono}.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface EmbeddingExecution {

    /**
     * Executes the embedding request and returns the processed result.
     * <p>
     * This method performs the full request lifecycle, including conversion of the raw
     * provider response into a standardized {@link EmbeddingResponse}. It is the primary
     * entry point for most use cases where only the embedding data is needed.
     * </p>
     *
     * @return a {@link Mono} emitting the processed {@link EmbeddingResponse}
     */
    Mono<EmbeddingResponse> execute();

    /**
     * Executes the embedding request and returns the raw, unprocessed provider response.
     * <p>
     * This method is useful when the calling code needs access to the exact JSON structure
     * returned by the specific AI API, for example to extract non-standard fields or
     * perform custom deserialization. The returned {@link RawResponse} is a wrapper around
     * the raw response body.
     * </p>
     *
     * @return a {@link Mono} emitting the raw JSON response as a {@link RawResponse}
     */
    Mono<RawResponse> executeRaw();

    /**
     * Executes the embedding request and converts the raw response to a custom type
     * using the provided converter.
     * <p>
     * This is a convenience method that combines {@link #executeRaw()} with the given
     * {@link RawResponseConverter}. It is ideal when the raw response should be
     * transformed directly into a domain-specific object without intermediate
     * {@link EmbeddingResponse} processing.
     * </p>
     *
     * @param <R>       the target type of the conversion
     * @param converter the converter to transform the {@link RawResponse} into the desired type; must not be {@code null}
     * @return a {@link Mono} emitting the converted response
     */
    default <R> Mono<R> execute(RawResponseConverter<R> converter) {
        return executeRaw().map(converter::convert);
    }

}