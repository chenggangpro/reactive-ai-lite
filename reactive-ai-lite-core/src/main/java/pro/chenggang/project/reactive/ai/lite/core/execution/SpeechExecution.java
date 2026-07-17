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

import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechRawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Defines the contract for executing text-to-speech requests against an AI provider.
 * <p>
 * This interface provides methods to retrieve both the processed {@link SpeechResponse}
 * and the raw provider-specific response. It supports both general (non-streaming) and
 * streaming execution modes.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface SpeechExecution {

    /**
     * Executes the speech request and returns the processed non-streaming result.
     *
     * @return a {@link Mono} emitting the processed {@link SpeechResponse}
     */
    Mono<SpeechResponse> execute();

    /**
     * Executes the speech request and returns the non-processed raw response.
     *
     * @return a {@link Flux} emitting a {@link SpeechRawResponse} result
     */
    Mono<SpeechRawResponse> executeRaw();

}
