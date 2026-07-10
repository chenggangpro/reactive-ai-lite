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
package pro.chenggang.project.reactive.ai.lite.client.openai.provider.embedding;

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
import pro.chenggang.project.reactive.ai.lite.client.openai.certification.OrganizationTokenCertification;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.OpenaiEmbeddingRequest;
import pro.chenggang.project.reactive.ai.lite.client.openai.provider.OpenaiLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.UriTokenCertification;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * Default implementation of the {@link LlmEmbeddingProviderDelegate} for OpenAI embedding models.
 * <p>
 * This delegate encapsulates the HTTP communication details, request/response transformations,
 * and provider metadata specific to the OpenAI embeddings API. It is designed to work with
 * the reactive WebClient and various authentication schemes supported by the framework
 * (Bearer tokens, URI query parameter tokens, and OpenAI organization tokens).
 * </p>
 * <p>
 * Instances are typically created via the {@code Builder} pattern, which ensures all mandatory
 * configuration (base URL, endpoint, supported models, and at least one certification) is provided.
 * The delegate then exposes a unified {@link LlmProviderInfo} and handles the complete embedding
 * request lifecycle, from body preparation to response parsing.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class OpenaiEmbeddingProviderDelegate implements LlmEmbeddingProviderDelegate {

    /**
     * The unified LLM provider information representing this OpenAI embedding provider instance.
     * Contains metadata such as name, supported models, authentication profiles, and the base URL.
     */
    private final LlmProviderInfo llmProviderInfo;

    /**
     * The base URL used for all API requests to the OpenAI embedding service.
     * This is the root address (e.g., {@code https://api.openai.com}) to which the embedding endpoint will be appended.
     */
    private final String baseUrL;

    /**
     * The specific endpoint path appended to the base URL for executing embedding tasks.
     * For example, {@code /v1/embeddings}.
     */
    private final String embeddingEndpoint;

    /**
     * The configured WebClient instance responsible for executing HTTP requests to the OpenAI API.
     * It is built with the specified base URL and used throughout the request lifecycle.
     */
    private final WebClient webClient;

    /**
     * Constructs a new {@code OpenaiEmbeddingProviderDelegate} with the given configuration.
     * <p>
     * This constructor is intended for use by the Lombok {@link Builder}. It initializes the internal WebClient,
     * stores the base URL and embedding endpoint, and assembles an {@link OpenaiLlmProviderInfo} object that
     * aggregates the provider's identity, supported models, and authentication profiles. The provider info
     * is later exposed via {@link #providerInfo()} to the framework.
     *
     * @param webClientBuilder  the WebClient builder used to create the internal HTTP client; must not be {@code null}
     * @param baseUrL           the base URL for the OpenAI API; must not be {@code null}
     * @param embeddingEndpoint the endpoint path for embeddings; must not be {@code null}
     * @param isDefault         whether this provider is considered the default embedding provider
     * @param name              the internal name identifying this provider; must not be {@code null}
     * @param supportedModels   the set of OpenAI models supported by this provider instance; may be {@code null} or empty
     * @param certifications    the list of valid token certifications for authentication; must not be {@code null}
     */
    @Builder
    private OpenaiEmbeddingProviderDelegate(@NonNull WebClient.Builder webClientBuilder,
                                            @NonNull String baseUrL,
                                            @NonNull String embeddingEndpoint,
                                            boolean isDefault,
                                            @NonNull String name,
                                            Set<String> supportedModels,
                                            @NonNull List<TokenCertification> certifications) {
        this.baseUrL = baseUrL;
        this.embeddingEndpoint = embeddingEndpoint;
        this.webClient = webClientBuilder.baseUrl(baseUrL).build();
        this.llmProviderInfo = OpenaiLlmProviderInfo.builder()
                .isDefault(isDefault)
                .name(name)
                .supportedModels(supportedModels)
                .profiles(certifications.stream().map(TokenCertification::profile).collect(Collectors.toSet()))
                .baseUrl(baseUrL)
                .endpoint(embeddingEndpoint)
                .build();
    }

    /**
     * Returns the unified provider information for this OpenAI embedding delegate.
     * <p>
     * This method is used by the framework to discover the provider's capabilities,
     * supported models, and authentication requirements. The returned object is constructed
     * once during instantiation and remains immutable.
     *
     * @return the {@link LlmProviderInfo} representing this OpenAI embedding provider
     */
    @Override
    public LlmProviderInfo providerInfo() {
        return this.llmProviderInfo;
    }

    /**
     * Extracts the number of prompt tokens used from the raw usage JSON data.
     * <p>
     * This method looks for the {@code prompt_tokens} field in the usage object
     * provided by the OpenAI response to accurately report token consumption.
     * If the field is missing or not a valid integer, it defaults to 0.
     * </p>
     *
     * @param rawUsage the raw JSON object representing the usage statistics; must not be {@code null}
     * @return the number of prompt tokens consumed, or 0 if missing or invalid
     */
    protected Integer extractPromptTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/prompt_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    /**
     * Initializes the request body for an embedding call by converting the core request data
     * into the OpenAI-specific JSON structure.
     * <p>
     * This method uses the internal {@link #buildRequest(LlmEmbeddingRequestData)} helper to create
     * an {@link OpenaiEmbeddingRequest} and then serializes it into an {@link ObjectNode}. The resulting
     * JSON object is later sent as the HTTP request body.
     *
     * @param llmEmbeddingRequestData the generic request data holding input text, model, and dimensions; must not be {@code null}
     * @return a JSON object node ready to be used as the request body
     */
    @Override
    public ObjectNode initializeRequestBody(@NonNull LlmEmbeddingRequestData llmEmbeddingRequestData) {
        return OBJECT_MAPPER.valueToTree(this.buildRequest(llmEmbeddingRequestData));
    }

    /**
     * Extracts the embedding response from the raw HTTP response, including embeddings vectors
     * and usage statistics.
     * <p>
     * This method processes the JSON response body as follows:
     * <ul>
     *     <li>Verifies that the {@code data} array exists and contains embedding objects.</li>
     *     <li>If present, parses the {@code usage} object to extract token consumption using
     *         {@link #extractPromptTokenUsage(ObjectNode)}.</li>
     *     <li>Iterates over each element in the {@code data} array, extracting the {@code embedding}
     *         float array and collecting them into a list of {@code float[]}.</li>
     * </ul>
     * If the required {@code data} field is missing or invalid, the method emits an error.
     *
     * @param rawResponse the raw HTTP response wrapper containing status, headers, and the JSON body; must not be {@code null}
     * @return a {@link Mono} that emits the parsed {@link EmbeddingResponse} or an error if extraction fails
     */
    @Override
    public Mono<EmbeddingResponse> extractGeneralResponse(@NonNull RawResponse rawResponse) {
        return Mono.fromCallable(rawResponse::getResponseBody)
                .handle((rawResponseBody, syncSink) -> {
                    var embeddingResponseBuilder = EmbeddingResponse.builder()
                            .executionContext(rawResponse.getExecutionContext())
                            .rawResponseBody(rawResponseBody);
                    JsonNode dataNode = rawResponseBody.at("/data");
                    if (dataNode.isMissingNode() || !dataNode.isArray()) {
                        log.error("Failed to extract embedding data from response body. Response body: {}", rawResponseBody.toPrettyString());
                        syncSink.error(new ResponseMessageExtractFailedException(rawResponseBody));
                        return;
                    }
                    JsonNode usageNode = rawResponseBody.at("/usage");
                    if (!usageNode.isMissingNode() && usageNode.isObject() && !usageNode.isNull()) {
                        Usage usage = Usage.newUsageBuilder((ObjectNode) usageNode)
                                .promptTokensExtractor(this::extractPromptTokenUsage)
                                .build();
                        embeddingResponseBuilder.usage(usage);
                    }
                    List<float[]> embeddings = new ArrayList<>();
                    for (JsonNode embeddingObj : dataNode) {
                        JsonNode embeddingArray = embeddingObj.at("/embedding");
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
     * Prepares the WebClient request body specification, applying appropriate authentication and headers.
     * <p>
     * This method builds a POST request to the configured embedding endpoint. Authentication is handled
     * based on the type of {@link TokenCertification} provided in the request data:
     * <ul>
     *     <li>If the certification is a {@link UriTokenCertification}, it appends the token as a query parameter.</li>
     *     <li>If a {@link BearerTokenCertification} or {@link OrganizationTokenCertification} is present,
     *         the corresponding headers are added (unless already set via URI).</li>
     * </ul>
     * The method also sets standard headers such as content type, user agent, and accepted charset.
     * A warning is logged if no certification could be applied.
     *
     * @param llmEmbeddingRequestData the request data, which must contain at least one valid token certification; must not be {@code null}
     * @return the {@link RequestBodySpec} ready to have its body set and the HTTP call executed
     * @throws IllegalStateException if no token certification is provided
     */
    @Override
    public RequestBodySpec loadRequestBodySpec(@NonNull LlmEmbeddingRequestData llmEmbeddingRequestData) {
        AtomicBoolean certificationSet = new AtomicBoolean(false);
        RequestBodyUriSpec requestBodyUriSpec = this.webClient.post();
        Optional<TokenCertification> optionalTokenCertification = llmEmbeddingRequestData.getTokenCertification();
        if (optionalTokenCertification.isEmpty()) {
            throw new IllegalStateException("At least one token certification is required for the embedding request.");
        }
        TokenCertification tokenCertification = optionalTokenCertification.get();
        RequestBodySpec requestBodySpec = requestBodyUriSpec.uri(uriBuilder -> {
            uriBuilder.path(this.embeddingEndpoint);
            if (tokenCertification instanceof UriTokenCertification uriTokenCertification) {
                uriTokenCertification.applyTo(uriBuilder);
                certificationSet.set(true);
            }
            return uriBuilder.build();
        });
        requestBodySpec.contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, "reactive-ai-lite")
                .acceptCharset(StandardCharsets.UTF_8)
                .accept(MediaType.APPLICATION_JSON);

        if (tokenCertification instanceof BearerTokenCertification bearerTokenCertification) {
            if (!certificationSet.get()) {
                requestBodySpec.headers(bearerTokenCertification::applyTo);
                certificationSet.set(true);
            }
        } else if (tokenCertification instanceof OrganizationTokenCertification organizationTokenCertification) {
            if (!certificationSet.get()) {
                requestBodySpec.headers(organizationTokenCertification::applyTo);
                certificationSet.set(true);
            }
        }
        if (!certificationSet.get()) {
            log.warn("No token certification be applied, cause of the unknown TokenCertification : {}", tokenCertification);
        }
        return requestBodySpec;
    }

    /**
     * Returns a string representation of this delegate, including its provider info and endpoint details.
     *
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        return "OpenaiEmbeddingProviderDelegate{" +
                "llmProviderInfo=" + llmProviderInfo +
                ", baseUrL='" + baseUrL + '\'' +
                ", embeddingEndpoint='" + embeddingEndpoint + '\'' +
                '}';
    }

    /**
     * Builds the specific OpenAI embedding request object from the provided generic request data.
     * <p>
     * This internal helper method maps properties such as the target model,
     * input text, and dimensions from the {@link LlmEmbeddingRequestData} into the specialized
     * {@link OpenaiEmbeddingRequest} expected by the OpenAI API. The encoding format is always set to
     * {@code "float"} to match the parsing logic that interprets the response as float arrays.
     * </p>
     *
     * @param llmEmbeddingRequestData the generic core request structure; must not be {@code null}
     * @return the constructed {@link OpenaiEmbeddingRequest} tailored for OpenAI
     */
    protected OpenaiEmbeddingRequest buildRequest(LlmEmbeddingRequestData llmEmbeddingRequestData) {
        return OpenaiEmbeddingRequest.builder()
                .model(llmEmbeddingRequestData.getModelName())
                .input(llmEmbeddingRequestData.getInput())
                .dimensions(llmEmbeddingRequestData.getDimensions())
                .encodingFormat("float")
                .build();
    }
}
