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
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneResponse;
import reactor.core.publisher.Mono;

/**
 * Defines the contract for executing SystemOne requests against an AI provider.
 * <p>
 * Exposes methods to retrieve the processed {@link SystemOneResponse}, the raw provider JSON
 * response via {@link RawResponse}, or a custom converted type via {@link RawResponseConverter}.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 * @see SystemOneResponse
 * @see RawResponse
 * @see RawResponseConverter
 */
public interface SystemOneExecution {

    /**
     * Executes the SystemOne request and returns the processed result.
     *
     * @return a {@link Mono} emitting the processed {@link SystemOneResponse}
     */
    Mono<SystemOneResponse> execute();

    /**
     * Executes the SystemOne request and returns the raw, unprocessed provider response.
     *
     * @return a {@link Mono} emitting the raw response as {@link RawResponse}
     */
    Mono<RawResponse> executeRaw();

    /**
     * Executes the SystemOne request and transforms the raw response using the provided converter.
     *
     * @param <R>       the target type of the conversion
     * @param converter the converter to transform {@link RawResponse} into the desired type; must not be null
     * @return a {@link Mono} emitting the converted result
     */
    default <R> Mono<R> execute(RawResponseConverter<R> converter) {
        return executeRaw().map(converter::convert);
    }
}
