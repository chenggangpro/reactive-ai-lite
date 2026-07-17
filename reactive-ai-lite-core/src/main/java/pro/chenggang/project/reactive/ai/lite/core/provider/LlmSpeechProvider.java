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
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechRawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SpeechExecutionInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service provider interface for text-to-speech operations.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmSpeechProvider extends LlmProvider {

    /**
     * Executes a generalized text-to-speech request.
     *
     * @param executionInfo the speech execution information containing all dynamic settings
     * @return a Mono emitting the processed {@link SpeechResponse}
     */
    Mono<SpeechResponse> executeSpeech(@NonNull SpeechExecutionInfo executionInfo);

    /**
     * Executes a text-to-speech request returning the raw response from the LLM.
     *
     * @param executionInfo the speech execution information containing all dynamic settings
     * @return a Mono emitting the raw {@link SpeechRawResponse}
     */
    Mono<SpeechRawResponse> executeSpeechRaw(@NonNull SpeechExecutionInfo executionInfo);

    /**
     * Executes a streaming text-to-speech request.
     *
     * @param executionInfo the speech execution information containing all dynamic settings
     * @return a Flux emitting the sequence of {@link SpeechStreamResponse} chunks
     */
    Flux<SpeechStreamResponse> executeSpeechStream(@NonNull SpeechExecutionInfo executionInfo);

    /**
     * Executes a streaming text-to-speech request returning raw response chunks.
     *
     * @param executionInfo the speech execution information containing all dynamic settings
     * @return a Flux emitting the sequence of raw {@link SpeechRawResponse} chunks
     */
    Flux<SpeechRawResponse> executeSpeechStreamRaw(@NonNull SpeechExecutionInfo executionInfo);
}
