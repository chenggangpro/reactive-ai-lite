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
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmEmbeddingRequestData;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.EmbeddingResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.core.publisher.Mono;

/**
 * Delegate interface that encapsulates the specific capabilities and API integration logic
 * of an LLM Embedding Provider. Implementations of this interface define how the framework
 * interacts with a particular provider's embedding API, including request construction,
 * authentication, and response parsing.
 *
 * <p>The embedding request flow typically involves:
 * <ol>
 *   <li>Constructing a provider-specific HTTP request (URI, headers, body).</li>
 *   <li>Applying appropriate authentication via {@link TokenCertification}.</li>
 *   <li>Parsing the raw HTTP response into a standardized {@link EmbeddingResponse}.</li>
 * </ol>
 *
 * <p>Default methods provide common utility checks and authentication handling that can be
 * reused across implementations, reducing duplication and enforcing consistent behavior.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmEmbeddingProviderDelegate {

    /**
     * Returns the metadata and configuration information for this provider.
     * This includes details such as the provider name, base URL, and supported models,
     * which are used by the framework to register and select the appropriate delegate.
     *
     * @return the {@link LlmProviderInfo} object containing provider information
     */
    LlmProviderInfo providerInfo();

    /**
     * Initializes the WebClient request specification for this specific provider's embedding endpoint.
     * This typically sets the URI, content type, and any required headers based on the
     * provided request data and the provider's API definition. Post-processing (e.g., adding
     * authentication headers) is handled separately via {@link #applyStandardTokenCertification}.
     *
     * @param llmEmbeddingRequestData the structured request data containing model selection,
     *                                input texts, and other parameters
     * @return a configured {@link RequestBodySpec} ready for body insertion and execution
     */
    RequestBodySpec loadRequestBodySpec(LlmEmbeddingRequestData llmEmbeddingRequestData);

    /**
     * Transforms the generic embedding request data into the provider-specific JSON payload.
     * Implementations should map common fields (e.g., input list, model name) into the format
     * expected by the LLM provider's API.
     *
     * @param llmEmbeddingRequestData the structured request data containing input and parameters
     * @return a JSON {@link ObjectNode} representing the request body
     */
    ObjectNode initializeRequestBody(LlmEmbeddingRequestData llmEmbeddingRequestData);

    /**
     * Extracts a standardized {@link EmbeddingResponse} from the raw provider response.
     * This method is responsible for parsing the HTTP response body, handling errors,
     * and converting the provider-specific embedding data into the common model.
     *
     * @param rawResponse the raw response wrapper containing status, headers, and body
     * @return a Mono emitting the parsed {@link EmbeddingResponse}
     */
    Mono<EmbeddingResponse> extractGeneralResponse(RawResponse rawResponse);

    /**
     * Verifies that at least one token certification is present in the request data.
     * Embedding requests typically require authentication; this default implementation
     * throws an {@link IllegalStateException} if no certification is provided.
     *
     * @param llmEmbeddingRequestData the structured request data to validate
     * @throws IllegalStateException if no token certification is found
     */
    default void checkTokenCertification(LlmEmbeddingRequestData llmEmbeddingRequestData) {
        if (llmEmbeddingRequestData.getTokenCertification().isEmpty()) {
            throw new IllegalStateException("At least one token certification is required for the embedding completion request.");
        }
    }

    /**
     * Applies standard token authentication to a WebClient request specification.
     * Supports both {@link BearerTokenCertification} (Authorization: Bearer ...) and
     * {@link HttpHeaderTokenCertification} (custom header name and value) patterns.
     * This default method can be called inside {@link #loadRequestBodySpec} to add
     * authentication headers without duplicating logic.
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