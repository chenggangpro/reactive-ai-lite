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
package pro.chenggang.project.reactive.ai.lite.client.ollama.provider.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaEmbeddingRequest;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaEmbeddingRequest.OllamaEmbeddingRequestBuilder;
import pro.chenggang.project.reactive.ai.lite.client.ollama.provider.OllamaLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.usage.Usage;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmEmbeddingRequestData;
import pro.chenggang.project.reactive.ai.lite.core.exception.ResponseMessageExtractFailedException;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.EmbeddingResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.delegate.LlmEmbeddingProviderDelegate;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * Default implementation of {@link LlmEmbeddingProviderDelegate} for communicating with
 * the Ollama embedding API. This delegate builds HTTP requests according to the Ollama
 * embedding specification, converts generic {@link LlmEmbeddingRequestData} into
 * Ollama‑specific request objects, and extracts {@link EmbeddingResponse} from the raw
 * JSON response.
 * <p>
 * Instances are typically created via the Lombok {@code @Builder} pattern and are
 * registered as a Spring bean. Options such as vector dimensions are handled when
 * building the request, and optional token usage information is parsed if present in
 * the response.
 * <p>
 * This provider does not require any token certification; the
 * {@link #checkTokenCertification(LlmEmbeddingRequestData)} method is intentionally
 * left empty.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class OllamaEmbeddingProviderDelegate implements LlmEmbeddingProviderDelegate {

    /**
     * Immutable metadata describing this Ollama provider (name, supported models,
     * base URL, etc.). Constructed once during initialization and returned by
     * {@link #providerInfo()}.
     */
    private final LlmProviderInfo llmProviderInfo;

    /**
     * The base URL of the Ollama server (e.g., {@code http://localhost:11434}).
     */
    private final String baseUrL;

    /**
     * The API path segment for embedding operations appended to {@link #baseUrL}
     * (e.g., {@code /api/embed}).
     */
    private final String embeddingEndpoint;

    /**
     * Pre‑configured {@link WebClient} that sends all requests to {@link #baseUrL}.
     */
    private final WebClient webClient;

    /**
     * Private constructor invoked by the Lombok {@code @Builder} generator.
     * <p>
     * The constructor builds a {@link WebClient} from the supplied
     * {@link WebClient.Builder} using the given {@code baseUrL}, and assembles
     * the {@link OllamaLlmProviderInfo} from the remaining parameters.
     *
     * @param webClientBuilder  the {@link WebClient.Builder} used to create the
     *                          central {@link WebClient} instance; must not be null.
     * @param baseUrL            the Ollama server base URL, e.g., {@code http://localhost:11434}.
     * @param embeddingEndpoint  the API endpoint for embeddings, e.g., {@code /api/embed}.
     * @param isDefault          whether this provider should be considered the default
     *                          embedding provider.
     * @param name               the logical name of this provider (e.g., {@code ollama}).
     * @param supportedModels    optional set of model names that this provider claims
     *                          to support; may be empty.
     * @param certifications     list of {@link TokenCertification} objects used to
     *                           extract profile names for the provider info; must not
     *                           be null.
     */
    @Builder
    private OllamaEmbeddingProviderDelegate(@NonNull WebClient.Builder webClientBuilder,
                                            @NonNull String baseUrL,
                                            @NonNull String embeddingEndpoint,
                                            boolean isDefault,
                                            @NonNull String name,
                                            Set<String> supportedModels,
                                            @NonNull List<TokenCertification> certifications) {
        this.baseUrL = baseUrL;
        this.embeddingEndpoint = embeddingEndpoint;
        this.webClient = webClientBuilder.baseUrl(baseUrL).build();
        this.llmProviderInfo = OllamaLlmProviderInfo.builder()
                .isDefault(isDefault)
                .name(name)
                .supportedModels(supportedModels)
                .profiles(certifications.stream().map(TokenCertification::profile).collect(Collectors.toSet()))
                .baseUrl(baseUrL)
                .endpoint(embeddingEndpoint)
                .build();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the pre‑computed {@link LlmProviderInfo} that was built during
     * construction.
     */
    @Override
    public LlmProviderInfo providerInfo() {
        return this.llmProviderInfo;
    }

    /**
     * Extracts the prompt token usage count from the JSON object that represents
     * the usage information in the Ollama response.
     * <p>
     * The method navigates to the JSON path {@code /prompt_eval_count}. If the
     * node exists and contains an integral value, that value is returned;
     * otherwise {@code 0} is returned. This extraction function is designed to
     * be passed to {@link Usage#newUsageBuilder(ObjectNode)
     * Usage.newUsageBuilder(ObjectNode).promptTokensExtractor(…)}.
     *
     * @param rawUsage the root JSON node that may contain token usage fields.
     * @return the number of prompt tokens evaluated, or {@code 0} if the field is absent or not a number.
     */
    protected Integer extractPromptTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/prompt_eval_count");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Converts the generic {@link LlmEmbeddingRequestData} into an Ollama‑specific
     * request object by delegating to {@link #buildRequest(LlmEmbeddingRequestData)},
     * then serializes that object to a JSON tree using the pre‑configured
     * {@link com.fasterxml.jackson.databind.ObjectMapper}.
     */
    @Override
    public ObjectNode initializeRequestBody(@NonNull LlmEmbeddingRequestData llmEmbeddingRequestData) {
        return OBJECT_MAPPER.valueToTree(this.buildRequest(llmEmbeddingRequestData));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Processes the raw JSON response from Ollama to build an {@link EmbeddingResponse}.
     * <p>
     * The embedding vectors are expected under the JSON path {@code /embeddings} as
     * an array of arrays of numbers. Each inner array is converted to a {@code float[]}.
     * If the root response object contains a {@code prompt_eval_count} field, a
     * {@link Usage} object is also populated with the extracted prompt token count.
     * <p>
     * If the {@code /embeddings} node is missing or not an array, the method signals
     * an error via a {@link ResponseMessageExtractFailedException}.
     *
     * @param rawResponse the raw HTTP response containing the unmarshalled JSON body.
     * @return a {@link Mono} that emits the fully populated {@link EmbeddingResponse}.
     */
    @Override
    public Mono<EmbeddingResponse> extractGeneralResponse(@NonNull RawResponse rawResponse) {
        return Mono.fromCallable(rawResponse::getResponseBody)
                .handle((rawResponseBody, syncSink) -> {
                    var embeddingResponseBuilder = EmbeddingResponse.builder()
                            .executionContext(rawResponse.getExecutionContext())
                            .rawResponseBody(rawResponseBody);

                    JsonNode dataNode = rawResponseBody.at("/embeddings");
                    if (dataNode.isMissingNode() || !dataNode.isArray()) {
                        log.error("Failed to extract embedding data from response body. Response body: {}", rawResponseBody.toPrettyString());
                        syncSink.error(new ResponseMessageExtractFailedException(rawResponseBody));
                        return;
                    }

                    // Ollama's usage stats might not be provided for embeddings, or they might be flat in the response root.
                    // Depending on version, they might have `prompt_eval_count` at root level.
                    // We'll treat the root response as the usage node if it has prompt_eval_count
                    if (rawResponseBody.has("prompt_eval_count")) {
                        Usage usage = Usage.newUsageBuilder((ObjectNode) rawResponseBody)
                                .promptTokensExtractor(this::extractPromptTokenUsage)
                                .build();
                        embeddingResponseBuilder.usage(usage);
                    }

                    List<float[]> embeddings = new ArrayList<>();
                    for (JsonNode embeddingArray : dataNode) {
                        if (embeddingArray.isArray()) {
                            float[] floatArray = new float[embeddingArray.size()];
                            for (int i = 0; i < embeddingArray.size(); i++) {
                                floatArray[i] = (float) embeddingArray.get(i).asDouble();
                            }
                            embeddings.add(floatArray);
                        }
                    }
                    embeddingResponseBuilder.embeddings(embeddings);
                    EmbeddingResponse embeddingResponse = embeddingResponseBuilder.build();
                    syncSink.next(embeddingResponse);
                });
    }

    /**
     * {@inheritDoc}
     * <p>
     * Prepares a {@link RequestBodySpec} for a POST request to the Ollama embedding
     * endpoint. The URI is built by appending {@link #embeddingEndpoint} to the
     * pre‑configured base URL. The following headers are set:
     * <ul>
     *   <li>{@code Content-Type: application/json}</li>
     *   <li>{@code User-Agent: reactive-ai-lite}</li>
     *   <li>{@code Accept-Charset: UTF-8}</li>
     *   <li>{@code Accept: application/json}</li>
     * </ul>
     *
     * @param llmEmbeddingRequestData the embedding request data (unused here but
     *                                available for future customization of headers).
     * @return a {@link RequestBodySpec} onto which the JSON body can be set.
     */
    @Override
    public RequestBodySpec loadRequestBodySpec(@NonNull LlmEmbeddingRequestData llmEmbeddingRequestData) {
        RequestBodyUriSpec requestBodyUriSpec = this.webClient.post();
        RequestBodySpec requestBodySpec = requestBodyUriSpec.uri(uriBuilder -> {
            uriBuilder.path(this.embeddingEndpoint);
            return uriBuilder.build();
        });
        requestBodySpec.contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, "reactive-ai-lite")
                .acceptCharset(StandardCharsets.UTF_8)
                .accept(MediaType.APPLICATION_JSON);
        return requestBodySpec;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation is intentionally empty because the Ollama embedding endpoint
     * does not require any token‐based authentication at the HTTP level. Any required
     * credentials are assumed to be handled by the reverse proxy or network configuration.
     *
     * @param llmEmbeddingRequestData the embedding request data (unused).
     */
    @Override
    public void checkTokenCertification(LlmEmbeddingRequestData llmEmbeddingRequestData) {
        log.debug("Ollama embedding provider can work without api certification");
    }

    /**
     * Returns a string representation including the provider info, base URL,
     * and embedding endpoint.
     *
     * @return a string describing this delegate.
     */
    @Override
    public String toString() {
        return "OllamaEmbeddingProviderDelegate{" +
                "llmProviderInfo=" + llmProviderInfo +
                ", baseUrL='" + baseUrL + '\'' +
                ", embeddingEndpoint='" + embeddingEndpoint + '\'' +
                '}';
    }

    /**
     * Converts the generic {@link LlmEmbeddingRequestData} into an
     * {@link OllamaEmbeddingRequest} suitable for the Ollama embedding API.
     * <p>
     * The model name and input text are taken directly from the request data.
     * If a {@link LlmEmbeddingRequestData#getDimensions()} dimension is set,
     * it is passed as an option (the key {@code dimensions}) inside the request's
     * {@code options} map, following Ollama's embedding request format.
     *
     * @param llmEmbeddingRequestData the generic request data.
     * @return a fully built {@link OllamaEmbeddingRequest} object.
     */
    protected OllamaEmbeddingRequest buildRequest(LlmEmbeddingRequestData llmEmbeddingRequestData) {
        OllamaEmbeddingRequestBuilder ollamaEmbeddingRequestBuilder = OllamaEmbeddingRequest.builder()
                .model(llmEmbeddingRequestData.getModelName())
                .input(llmEmbeddingRequestData.getInput());
        if (Objects.nonNull(llmEmbeddingRequestData.getDimensions())) {
            Map<String, Object> options = Map.of("dimensions", llmEmbeddingRequestData.getDimensions());
            ollamaEmbeddingRequestBuilder.options(options);
        }
        return ollamaEmbeddingRequestBuilder.build();
    }
}