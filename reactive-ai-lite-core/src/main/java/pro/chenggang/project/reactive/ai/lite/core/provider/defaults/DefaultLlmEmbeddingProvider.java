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
package pro.chenggang.project.reactive.ai.lite.core.provider.defaults;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmEmbeddingRequestData;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.EmbeddingResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.EmbeddingExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmEmbeddingProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.delegate.LlmEmbeddingProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.core.util.LlmProviderUtil;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType.EMBEDDING;

/**
 * Default implementation of {@link LlmEmbeddingProvider} that acts as a generic composition host.
 * <p>
 * This class bridges the {@link LlmEmbeddingProviderDelegate} (provider-specific logic) with common
 * infrastructure such as {@link TokenCertification} management, interceptor integration, and
 * request/response handling. It performs the following high-level steps for each embedding request:
 * <ol>
 *   <li>Initialize the request data (including profile/certification selection) from the given
 *       {@link EmbeddingExecutionInfo}.</li>
 *   <li>Generate the raw JSON request body using the delegate, allowing optional customizations.</li>
 *   <li>Pass the request through the interceptor chain (if any) to enable pre/post-processing.</li>
 *   <li>Send the request via the WebClient {@link ResponseSpec} built by the delegate.</li>
 *   <li>Extract the raw response and optionally parse it into a structured {@link EmbeddingResponse}.</li>
 * </ol>
 * <p>
 * Certification selection is driven by the {@link ExecutionContext}'s profile name, defaulting to the
 * designated default certification if no profile is specified or a match is not found.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class DefaultLlmEmbeddingProvider implements LlmEmbeddingProvider {

    /**
     * The provider-specific strategy delegate that encapsulates the actual HTTP communication details,
     * including URL building, headers, and authentication token injection based on the selected
     * {@link TokenCertification}.
     */
    private final LlmEmbeddingProviderDelegate delegate;

    /**
     * The registry of interceptors that can observe and potentially modify the raw request body
     * and response body for embedding operations. Interceptors are executed after the request body
     * is built and before the HTTP call is made, and after the raw response is received.
     */
    private final LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry;

    /**
     * A map of profile names to their corresponding {@link TokenCertification}. This allows
     * multi-tenancy or environment-specific credentials to be selected at runtime based on the
     * {@code profile} field in the {@link ExecutionContext}.
     */
    protected final Map<String, TokenCertification> certificationMap = new ConcurrentHashMap<>();

    /**
     * The default {@link TokenCertification} to use when no profile is specified or when no
     * matching entry exists in {@link #certificationMap}. It is mandatory that at least one
     * certification is marked as default when multiple certifications are provided.
     */
    protected final TokenCertification defaultCertification;

    /**
     * Constructs a new {@code DefaultLlmEmbeddingProvider} with the given delegate, certifications,
     * and interceptor registry.
     * <p>
     * All provided certifications are registered in an internal map keyed by their profile name.
     * The default certification is determined as the first certification marked as {@code isDefault = true}.
     * If multiple certifications are supplied, exactly one must be the default; otherwise an
     * {@link IllegalArgumentException} is thrown. If the list is empty, the default certification
     * is set to {@code null}, and authentication expectations should be handled by the delegate directly.
     *
     * @param delegate                       the provider-specific strategy delegate; must not be null
     * @param certifications                 the list of token certifications to register; must not be null
     * @param lLmProviderInterceptorRegistry the interceptor registry; must not be null
     * @throws IllegalArgumentException if the certifications list is non-empty but no default is found
     */
    public DefaultLlmEmbeddingProvider(@NonNull LlmEmbeddingProviderDelegate delegate,
                                       @NonNull List<TokenCertification> certifications,
                                       @NonNull LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry) {
        this.delegate = delegate;
        this.lLmProviderInterceptorRegistry = lLmProviderInterceptorRegistry;
        certifications.forEach(cert -> certificationMap.put(cert.profile(), cert));
        if (!certifications.isEmpty()) {
            this.defaultCertification = certifications.stream()
                    .filter(TokenCertification::isDefault)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("At least one default TokenCertification is required for " + this.delegate.providerInfo()));
        } else {
            this.defaultCertification = null;
        }
    }

    /**
     * Returns the capability of this provider, which is always {@link Capability#EMBEDDING}.
     *
     * @return capability for embedding
     */
    @Override
    public Capability capability() {
        return Capability.EMBEDDING;
    }

    /**
     * Returns the provider information by delegating to the underlying {@link LlmEmbeddingProviderDelegate#providerInfo()}.
     *
     * @return the provider metadata (name, model, etc.)
     */
    @Override
    public LlmProviderInfo info() {
        return this.delegate.providerInfo();
    }

    /**
     * Executes an embedding request and returns a structured {@link EmbeddingResponse}.
     * <p>
     * This method orchestrates the full flow:
     * <ol>
     *   <li>Initialize {@link LlmEmbeddingRequestData} from the given execution info.</li>
     *   <li>Execute the raw request internally (which includes interceptor and HTTP call).</li>
     *   <li>Delegate extraction of the general embedding response from the raw response body.</li>
     * </ol>
     *
     * @param executionInfo the embedding execution configuration, including input texts, model, and optional profile; must not be null
     * @return a Mono of {@link EmbeddingResponse} containing the embedding vectors and metadata
     */
    @Override
    public Mono<EmbeddingResponse> executeEmbedding(@NonNull EmbeddingExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo)
                .flatMap(llmRequestData -> this.executeInternalRaw(llmRequestData)
                        .flatMap(this.delegate::extractGeneralResponse)
                );
    }

    /**
     * Executes an embedding request and returns the raw response as a {@link RawResponse} without
     * parsing into a higher-level embedding result.
     * <p>
     * Useful for debugging, logging, or when the structured extraction is not needed.
     *
     * @param executionInfo the embedding execution configuration; must not be null
     * @return a Mono of {@link RawResponse} containing the original JSON response body and context
     */
    @Override
    public Mono<RawResponse> executeEmbeddingRaw(@NonNull EmbeddingExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo)
                .flatMap(this::executeInternalRaw);
    }

    /**
     * Internal method that processes a fully initialized {@link LlmEmbeddingRequestData} to produce a raw response.
     * <p>
     * Steps:
     * <ol>
     *   <li>Generate the raw JSON request body via {@link #generateRawRequestBody(LlmEmbeddingRequestData)}.</li>
     *   <li>Wrap the HTTP call in an interceptor context if interceptors are registered, allowing
     *       pre-processing of the request body and post-processing of the response body.</li>
     *   <li>Execute the request using the {@link org.springframework.web.reactive.function.client.WebClient.ResponseSpec}
     *       obtained from {@link #toResponseSpec(LlmEmbeddingRequestData, ObjectNode)}.</li>
     *   <li>Map the resulting raw object (as {@link ObjectNode}) to a {@link RawResponse}.</li>
     * </ol>
     * <p>
     * The interceptor integration uses {@link LlmProviderInterceptorRegistry.InterceptedDataInfo} to pass
     * the client type, provider info, context, and raw request body.
     *
     * @param llmEmbeddingRequestData the initialized request data containing context, certification, and customizer; must not be null
     * @return a Mono of {@link RawResponse} with the execution context and raw JSON body
     */
    protected Mono<RawResponse> executeInternalRaw(@NonNull LlmEmbeddingRequestData llmEmbeddingRequestData) {
        return this.generateRawRequestBody(llmEmbeddingRequestData)
                .flatMap(requestBody -> {
                    return LlmProviderInterceptorRegistry.InterceptedDataInfo.builder()
                            .clientType(EMBEDDING)
                            .llmProviderInfo(this.info())
                            .executionContext(llmEmbeddingRequestData.getExecutionContext())
                            .rawRequestBody(requestBody)
                            .build()
                            .interceptGeneral(this.lLmProviderInterceptorRegistry,
                                    this.toResponseSpec(llmEmbeddingRequestData, requestBody)
                                            .flatMap(responseSpec -> responseSpec.bodyToMono(new ParameterizedTypeReference<ObjectNode>() {}))
                            );
                })
                .map(rawResponseBody -> {
                    return RawResponse.builder()
                            .executionContext(llmEmbeddingRequestData.getExecutionContext())
                            .responseBody(rawResponseBody)
                            .build();
                });
    }

    /**
     * Initializes the {@link LlmEmbeddingRequestData} from the given {@link EmbeddingExecutionInfo}.
     * <p>
     * This involves selecting the appropriate {@link TokenCertification} based on the profile
     * name in the execution context, and merging it with provider-specific defaults through
     * the {@link LlmEmbeddingRequestData.LlmEmbeddingRequestDataInitializer} pattern.
     *
     * @param executionInfo the embedding execution configuration; must not be null
     * @return a Mono emitting the initialized request data ready for request body generation and execution
     */
    private Mono<LlmEmbeddingRequestData> initializeLlmRequestData(@NonNull EmbeddingExecutionInfo executionInfo) {
        return LlmEmbeddingRequestData.LlmEmbeddingRequestDataInitializer
                .of(certificationMap, defaultCertification, this.info(), executionInfo)
                .initialize();
    }

    /**
     * Creates a {@link org.springframework.web.reactive.function.client.WebClient.ResponseSpec} from the
     * prepared request data and request body.
     * <p>
     * This method performs the following:
     * <ol>
     *   <li>Verifies that the token certification is valid via {@link LlmEmbeddingProviderDelegate#checkTokenCertification(LlmEmbeddingRequestData)}.</li>
     *   <li>Loads the request body specification (with headers, URL, and authentication) from the delegate
     *       via {@link LlmEmbeddingProviderDelegate#loadRequestBodySpec(LlmEmbeddingRequestData)}.</li>
     *   <li>Attaches the request body if it is non-null.</li>
     *   <li>Configures error handling for non-success status codes using
     *       {@link LlmProviderUtil#handleClientResponseError(org.springframework.web.reactive.function.client.ClientResponse)}.</li>
     *   <li>Returns the resulting {@code ResponseSpec} for the actual HTTP call.</li>
     * </ol>
     *
     * @param llmEmbeddingRequestData the request data containing the certification and context; must not be null
     * @param body                    the JSON request body, may be null if the delegate handles body separately
     * @return a Mono of the response specification ready to be executed
     */
    protected Mono<ResponseSpec> toResponseSpec(LlmEmbeddingRequestData llmEmbeddingRequestData, ObjectNode body) {
        return Mono.fromCallable(() -> {
            this.delegate.checkTokenCertification(llmEmbeddingRequestData);
            RequestBodySpec requestBodySpec = this.delegate.loadRequestBodySpec(llmEmbeddingRequestData);
            if (Objects.nonNull(body)) {
                requestBodySpec.bodyValue(body);
            }
            return requestBodySpec.retrieve()
                    .onStatus(HttpStatusCode::isError, LlmProviderUtil::handleClientResponseError);
        });
    }

    /**
     * Generates the raw JSON request body for an embedding request.
     * <p>
     * It uses the delegate's {@link LlmEmbeddingProviderDelegate#initializeRequestBody(LlmEmbeddingRequestData)}
     * to create the base request body object, then applies any customizer function provided via
     * {@link LlmEmbeddingRequestData#getRawRequestCustomizerConfigure()} if present. The customizer
     * receives the {@link ExecutionContext} and the request body for modification.
     *
     * @param llmEmbeddingRequestData the request data containing the customizer and context; must not be null
     * @return a Mono emitting the finalized JSON request body
     */
    protected Mono<ObjectNode> generateRawRequestBody(@NonNull LlmEmbeddingRequestData llmEmbeddingRequestData) {
        return Mono.fromCallable(() -> {
            ObjectNode rawRequestBody = this.delegate.initializeRequestBody(llmEmbeddingRequestData);
            BiConsumer<ExecutionContext, ObjectNode> customizer = llmEmbeddingRequestData.getRawRequestCustomizerConfigure();
            if (Objects.nonNull(customizer)) {
                customizer.accept(llmEmbeddingRequestData.getExecutionContext(), rawRequestBody);
            }
            return rawRequestBody;
        });
    }
}