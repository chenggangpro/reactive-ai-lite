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
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmSystemOneRequestData;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SystemOneExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmSystemOneProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.delegate.LlmSystemOneProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.core.util.LlmProviderUtil;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType.SYSTEM_ONE;

/**
 * Default implementation of {@link LlmSystemOneProvider} that acts as a generic composition host.
 * <p>
 * This class coordinates the {@link LlmSystemOneProviderDelegate} (provider-specific logic) with common
 * framework infrastructure such as {@link TokenCertification} management, interceptor integration,
 * dynamic execution context propagation, and request/response transformation.
 * </p>
 * <p>
 * It executes SystemOne operations following these high-level steps:
 * <ol>
 *   <li>Initialize the request data (resolving profile, model name, input state, and questions schema)
 *       from the given {@link SystemOneExecutionInfo}.</li>
 *   <li>Generate the raw JSON request payload using the delegate, applying customizer functions if provided.</li>
 *   <li>Pass the request through the interceptor pipeline for pre- and post-processing.</li>
 *   <li>Send the HTTP request using the WebClient {@link ResponseSpec} constructed by the delegate.</li>
 *   <li>Extract the response into either a raw payload ({@link RawResponse}) or a parsed, strongly typed
 *       domain model ({@link SystemOneResponse}).</li>
 * </ol>
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmSystemOneProvider
 * @see LlmSystemOneProviderDelegate
 * @see SystemOneExecutionInfo
 * @see SystemOneResponse
 * @since 0.1.0
 */
@Slf4j
public class DefaultLlmSystemOneProvider implements LlmSystemOneProvider {

    /**
     * The provider-specific strategy delegate encapsulating backend HTTP details,
     * payload serialization, and response parsing.
     */
    private final LlmSystemOneProviderDelegate delegate;

    /**
     * The registry of interceptors capable of observing and mutating raw request and response payloads.
     */
    private final LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry;

    /**
     * Map of profile names to their configured {@link TokenCertification} instances for multi-profile routing.
     */
    protected final Map<String, TokenCertification> certificationMap = new ConcurrentHashMap<>();

    /**
     * The default token certification used when no explicit profile is selected.
     */
    protected final TokenCertification defaultCertification;

    /**
     * Constructs a new {@code DefaultLlmSystemOneProvider} with the given delegate, certifications,
     * and interceptor registry.
     * <p>
     * All provided certifications are indexed by profile name. Exactly one certification must be
     * marked as default if the list is non-empty; otherwise, an {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param delegate                       the provider-specific delegate; must not be null
     * @param certifications                 the list of token certifications; must not be null
     * @param lLmProviderInterceptorRegistry the interceptor registry; must not be null
     * @throws IllegalArgumentException if certifications list is non-empty but contains no default certification
     */
    public DefaultLlmSystemOneProvider(@NonNull LlmSystemOneProviderDelegate delegate,
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
     * Returns the capability of this provider, which is always {@link Capability#SYSTEM_ONE}.
     *
     * @return capability for SystemOne
     */
    @Override
    public Capability capability() {
        return Capability.SYSTEM_ONE;
    }

    /**
     * Returns the provider metadata by delegating to {@link LlmSystemOneProviderDelegate#providerInfo()}.
     *
     * @return the provider metadata (name, base URL, profiles)
     */
    @Override
    public LlmProviderInfo info() {
        return this.delegate.providerInfo();
    }

    /**
     * Executes a SystemOne operation and returns a structured {@link SystemOneResponse}.
     *
     * @param executionInfo the SystemOne execution configuration; must not be null
     * @return a {@link Mono} emitting the parsed {@link SystemOneResponse}
     */
    @Override
    public Mono<SystemOneResponse> executeSystemOne(@NonNull SystemOneExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo)
                .flatMap(llmRequestData -> this.executeInternalRaw(llmRequestData)
                        .flatMap(this.delegate::extractGeneralResponse)
                );
    }

    /**
     * Executes a SystemOne operation and returns the unprocessed raw response as a {@link RawResponse}.
     *
     * @param executionInfo the SystemOne execution configuration; must not be null
     * @return a {@link Mono} emitting the {@link RawResponse}
     */
    @Override
    public Mono<RawResponse> executeSystemOneRaw(@NonNull SystemOneExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo)
                .flatMap(this::executeInternalRaw);
    }

    /**
     * Internal pipeline method that executes a fully initialized {@link LlmSystemOneRequestData}
     * through the interceptor pipeline and WebClient transport.
     *
     * @param llmSystemOneRequestData the initialized request data; must not be null
     * @return a {@link Mono} emitting the {@link RawResponse}
     */
    protected Mono<RawResponse> executeInternalRaw(@NonNull LlmSystemOneRequestData llmSystemOneRequestData) {
        return this.generateRawRequestBody(llmSystemOneRequestData)
                .flatMap(requestBody -> LlmProviderInterceptorRegistry.InterceptedDataInfo.builder()
                        .clientType(SYSTEM_ONE)
                        .llmProviderInfo(this.info())
                        .executionContext(llmSystemOneRequestData.getExecutionContext())
                        .rawRequestBody(requestBody)
                        .build()
                        .interceptGeneral(this.lLmProviderInterceptorRegistry,
                                this.toResponseSpec(llmSystemOneRequestData, requestBody)
                                        .flatMap(responseSpec -> responseSpec.bodyToMono(new ParameterizedTypeReference<ObjectNode>() {}))
                        )
                )
                .map(rawResponseBody -> RawResponse.builder()
                        .executionContext(llmSystemOneRequestData.getExecutionContext())
                        .responseBody(rawResponseBody)
                        .build()
                );
    }

    /**
     * Initializes the {@link LlmSystemOneRequestData} from the given {@link SystemOneExecutionInfo}.
     *
     * @param executionInfo the execution configuration; must not be null
     * @return a {@link Mono} emitting the initialized request data
     */
    private Mono<LlmSystemOneRequestData> initializeLlmRequestData(@NonNull SystemOneExecutionInfo executionInfo) {
        return LlmSystemOneRequestData.LlmSystemOneRequestDataInitializer
                .of(certificationMap, defaultCertification, this.info(), executionInfo)
                .initialize();
    }

    /**
     * Constructs a WebClient {@link ResponseSpec} from the prepared request data and body.
     *
     * @param llmSystemOneRequestData the request data containing authentication and context
     * @param body                    the JSON request payload
     * @return a {@link Mono} emitting the configured {@link ResponseSpec}
     */
    protected Mono<ResponseSpec> toResponseSpec(LlmSystemOneRequestData llmSystemOneRequestData, ObjectNode body) {
        return Mono.fromCallable(() -> {
            this.delegate.checkTokenCertification(llmSystemOneRequestData);
            RequestBodySpec requestBodySpec = this.delegate.loadRequestBodySpec(llmSystemOneRequestData);
            if (Objects.nonNull(body)) {
                requestBodySpec.bodyValue(body);
            }
            return requestBodySpec.retrieve()
                    .onStatus(HttpStatusCode::isError, LlmProviderUtil::handleClientResponseError);
        });
    }

    /**
     * Generates the raw JSON request body by delegating to {@link LlmSystemOneProviderDelegate#initializeRequestBody}
     * and applying any configured {@link LlmSystemOneRequestData#getRawRequestCustomizerConfigure()}.
     *
     * @param llmSystemOneRequestData the request data containing the customizer
     * @return a {@link Mono} emitting the finalized JSON payload
     */
    protected Mono<ObjectNode> generateRawRequestBody(@NonNull LlmSystemOneRequestData llmSystemOneRequestData) {
        return Mono.fromCallable(() -> {
            ObjectNode rawRequestBody = this.delegate.initializeRequestBody(llmSystemOneRequestData);
            BiConsumer<ExecutionContext, ObjectNode> customizer = llmSystemOneRequestData.getRawRequestCustomizerConfigure();
            if (Objects.nonNull(customizer)) {
                customizer.accept(llmSystemOneRequestData.getExecutionContext(), rawRequestBody);
            }
            return rawRequestBody;
        });
    }
}
