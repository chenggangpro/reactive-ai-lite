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
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.HttpHeaderTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmSystemOneRequestData;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneResponse;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestions;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.core.publisher.Mono;

/**
 * Delegate interface that encapsulates the specific capabilities and API integration logic
 * of an LLM SystemOne Provider.
 * <p>
 * Implementations of this interface define how the framework interacts with a particular
 * provider's SystemOne API (such as TypeSafe AI's Jev), including HTTP request construction,
 * authentication token application, and parsing of raw JSON responses into structured
 * {@link SystemOneResponse} objects.
 * </p>
 * <p>
 * The SystemOne request flow typically involves:
 * <ol>
 *   <li>Constructing a provider-specific HTTP request specification via {@link #loadRequestBodySpec(LlmSystemOneRequestData)}.</li>
 *   <li>Generating the provider-specific JSON payload from the questions and input state via {@link #initializeRequestBody(LlmSystemOneRequestData)}.</li>
 *   <li>Applying authentication headers via {@link #applyStandardTokenCertification(RequestBodySpec, TokenCertification)}.</li>
 *   <li>Parsing the raw HTTP response into a strongly typed {@link SystemOneResponse} via {@link #extractGeneralResponse(RawResponse)}.</li>
 * </ol>
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmSystemOneRequestData
 * @see SystemOneResponse
 * @see SystemOneQuestions
 * @since 0.1.0
 */
public interface LlmSystemOneProviderDelegate {

    /**
     * Returns the metadata and configuration information for this provider.
     * This includes details such as provider name, base URL, and supported profiles.
     *
     * @return the {@link LlmProviderInfo} object containing provider metadata
     */
    LlmProviderInfo providerInfo();

    /**
     * Initializes the WebClient request specification for this specific provider's SystemOne endpoint.
     * Sets the URI, content type, and any provider-specific headers based on the request data.
     *
     * @param llmSystemOneRequestData the structured request data containing model name, state, questions, and context
     * @return a configured {@link RequestBodySpec} ready for payload attachment and execution
     */
    RequestBodySpec loadRequestBodySpec(LlmSystemOneRequestData llmSystemOneRequestData);

    /**
     * Transforms the generic SystemOne request data into the provider-specific JSON payload.
     * Implementations should serialize the input state and questions schema into the format expected by the backend API.
     *
     * @param llmSystemOneRequestData the structured request data containing input state and questions schema
     * @return a JSON {@link ObjectNode} representing the request payload
     */
    ObjectNode initializeRequestBody(LlmSystemOneRequestData llmSystemOneRequestData);

    /**
     * Extracts a standardized {@link SystemOneResponse} from the raw provider response.
     * This method is responsible for parsing the HTTP response body, handling errors,
     * and converting the provider-specific SystemOne evaluation data into the common model.
     *
     * @param rawResponse the raw response wrapper containing status, headers, and body
     * @return a Mono emitting the parsed {@link SystemOneResponse}
     */
    Mono<SystemOneResponse> extractGeneralResponse(RawResponse rawResponse);

    /**
     * Verifies that at least one token certification is present in the request data.
     * SystemOne requests typically require API key authentication; throws an {@link IllegalStateException}
     * if no certification is present.
     *
     * @param llmSystemOneRequestData the structured request data to validate
     * @throws IllegalStateException if no token certification is found
     */
    default void checkTokenCertification(LlmSystemOneRequestData llmSystemOneRequestData) {
        if (llmSystemOneRequestData.getTokenCertification().isEmpty()) {
            throw new IllegalStateException("At least one token certification is required for the SystemOne request.");
        }
    }

    /**
     * Applies standard token authentication to a WebClient request specification.
     * Supports {@link BearerTokenCertification} (Authorization: Bearer ...) and
     * {@link HttpHeaderTokenCertification} (custom header name and value).
     *
     * @param requestBodySpec    the request specification to configure
     * @param tokenCertification the authentication token certification to apply
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
