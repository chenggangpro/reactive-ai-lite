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
package pro.chenggang.project.reactive.ai.lite.core.provider.delegate;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.HttpHeaderTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmSpeechRequestData;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Delegate interface that encapsulates the specific capabilities and API integration logic
 * of an LLM Speech Provider.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmSpeechProviderDelegate {

    /**
     * Returns the metadata and configuration information for this provider.
     *
     * @return the {@link LlmProviderInfo} object containing provider information
     */
    LlmProviderInfo providerInfo();

    /**
     * Initializes the WebClient request specification for this specific provider's speech endpoint.
     *
     * @param llmSpeechRequestData the structured request data containing model selection,
     *                             input texts, and other parameters
     * @return a configured {@link RequestBodySpec} ready for body insertion and execution
     */
    RequestBodySpec loadRequestBodySpec(LlmSpeechRequestData llmSpeechRequestData);

    /**
     * Transforms the generic speech request data into the provider-specific JSON payload.
     *
     * @param llmSpeechRequestData the structured request data containing input and parameters
     * @return a JSON {@link ObjectNode} representing the request body
     */
    ObjectNode initializeRequestBody(LlmSpeechRequestData llmSpeechRequestData);

    /**
     * Extracts the generic speech response from the underlying WebClient response.
     *
     * @param responseSpec     the WebClient response specification
     * @param executionContext the execution context of the current request
     * @return a {@link Mono} of {@link SpeechResponse}
     */
    Mono<DataBuffer> extractGeneralResponse(ResponseSpec responseSpec, ExecutionContext executionContext);

    /**
     * Extracts the streaming speech response from the underlying WebClient response.
     *
     * @param responseSpec     the WebClient response specification
     * @param executionContext the execution context of the current request
     * @return a {@link Flux} of {@link SpeechStreamResponse}
     */
    Flux<DataBuffer> extractStreamResponse(ResponseSpec responseSpec, ExecutionContext executionContext);

    /**
     * Verifies that at least one token certification is present in the request data.
     *
     * @param llmSpeechRequestData the structured request data to validate
     * @throws IllegalStateException if no token certification is found
     */
    default void checkTokenCertification(LlmSpeechRequestData llmSpeechRequestData) {
        if (llmSpeechRequestData.getTokenCertification().isEmpty()) {
            throw new IllegalStateException("At least one token certification is required for the speech completion request.");
        }
    }

    /**
     * Applies standard token authentication to a WebClient request specification.
     *
     * @param requestBodySpec    the request specification to which headers will be added
     * @param tokenCertification the authentication token to apply
     */
    default void applyStandardTokenCertification(RequestBodySpec requestBodySpec, TokenCertification tokenCertification) {
        if (tokenCertification instanceof BearerTokenCertification bearerTokenCertification) {
            requestBodySpec.headers(bearerTokenCertification::applyTo);
            return;
        }
        if (tokenCertification instanceof HttpHeaderTokenCertification httpHeaderTokenCertification) {
            requestBodySpec.headers(httpHeaderTokenCertification::applyTo);
            return;
        }
    }
}
