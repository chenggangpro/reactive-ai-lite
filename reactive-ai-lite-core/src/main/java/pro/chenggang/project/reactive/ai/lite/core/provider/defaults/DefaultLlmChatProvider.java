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
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmChatRequestData;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.delegate.LlmChatProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.core.util.LlmProviderUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType.CHAT;

/**
 * A concrete generic composition host for implementing the {@link LlmChatProvider} interface.
 * <p>
 * This class orchestrates the execution of reactive chat requests, stream parsing, and 
 * interceptor execution by delegating provider-specific logic to an injected {@link LlmChatProviderDelegate}.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class DefaultLlmChatProvider implements LlmChatProvider {

    private final LlmChatProviderDelegate delegate;
    private final LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry;
    protected final Map<String, TokenCertification> certificationMap = new ConcurrentHashMap<>();
    protected final TokenCertification defaultCertification;

    /**
     * Constructs a new {@link DefaultLlmChatProvider}.
     *
     * @param delegate                       the provider specific strategy delegate
     * @param certifications                 a list of token certifications to register
     * @param lLmProviderInterceptorRegistry the registry for interceptors
     */
    public DefaultLlmChatProvider(@NonNull LlmChatProviderDelegate delegate,
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

    @Override
    public LlmProviderInfo info() {
        return this.delegate.providerInfo();
    }

    @Override
    public Mono<GeneralResponse> executeGeneral(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, false)
                .flatMap(llmRequestData -> this.executeInternalRaw(llmRequestData)
                        .flatMap(rawResponse -> this.delegate.extractGeneralResponse(llmRequestData.getToolDefinitions(), rawResponse))
                );
    }

    @Override
    public Mono<RawResponse> executeGeneralRaw(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, false)
                .flatMap(this::executeInternalRaw);
    }

    @Override
    public Flux<StreamResponse> executeStream(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, true)
                .flatMapMany(llmRequestData -> {
                            return this.generateRawRequestBody(llmRequestData)
                                    .flatMapMany(requestBody -> {
                                        return LlmProviderInterceptorRegistry.newInterceptedDataInfoBuilder()
                                                .clientType(CHAT)
                                                .llmProviderInfo(this.info())
                                                .executionContext(llmRequestData.getExecutionContext())
                                                .rawRequestBody(requestBody)
                                                .build()
                                                .interceptStream(this.lLmProviderInterceptorRegistry,
                                                        StreamResponseParser.parseStreamResponse(
                                                                llmRequestData.getExecutionContext(),
                                                                this.getRawStreamResponseFlux(llmRequestData, requestBody),
                                                                this.delegate::extractStreamChunks,
                                                                rawToolCallMessages -> this.delegate.mergeRawToolCallMessages(rawToolCallMessages, llmRequestData.isDistinctToolCalls())
                                                        )
                                                );
                                    })
                                    .concatMap(rawStreamResponse -> this.delegate.extractStreamResponseContent(llmRequestData.getToolDefinitions(), rawStreamResponse));
                        }
                );
    }

    @Override
    public Flux<RawStreamResponse> executeStreamRaw(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, true)
                .flatMapMany(llmRequestData -> {
                            return this.generateRawRequestBody(llmRequestData)
                                    .flatMapMany(requestBody -> {
                                        return LlmProviderInterceptorRegistry.newInterceptedDataInfoBuilder()
                                                .clientType(CHAT)
                                                .llmProviderInfo(this.info())
                                                .executionContext(llmRequestData.getExecutionContext())
                                                .rawRequestBody(requestBody)
                                                .build()
                                                .interceptStream(this.lLmProviderInterceptorRegistry,
                                                        StreamResponseParser.parseStreamResponse(
                                                                llmRequestData.getExecutionContext(),
                                                                this.getRawStreamResponseFlux(llmRequestData, requestBody),
                                                                this.delegate::extractStreamChunks,
                                                                rawToolCallMessages -> this.delegate.mergeRawToolCallMessages(rawToolCallMessages, llmRequestData.isDistinctToolCalls())
                                                        )
                                                );
                                    });
                        }
                );
    }

    protected Mono<RawResponse> executeInternalRaw(@NonNull LlmChatRequestData llmChatRequestData) {
        return this.generateRawRequestBody(llmChatRequestData)
                .flatMap(requestBody -> {
                    return LlmProviderInterceptorRegistry.newInterceptedDataInfoBuilder()
                            .clientType(CHAT)
                            .llmProviderInfo(this.info())
                            .executionContext(llmChatRequestData.getExecutionContext())
                            .rawRequestBody(requestBody)
                            .build()
                            .interceptGeneral(this.lLmProviderInterceptorRegistry,
                                    this.toResponseSpec(llmChatRequestData, requestBody)
                                            .flatMap(responseSpec -> responseSpec.bodyToMono(new ParameterizedTypeReference<ObjectNode>() {}))
                            );
                })
                .map(rawResponseBody -> {
                    return RawResponse.builder()
                            .executionContext(llmChatRequestData.getExecutionContext())
                            .responseBody(rawResponseBody)
                            .build();
                });
    }

    private Mono<LlmChatRequestData> initializeLlmRequestData(@NonNull ExecutionInfo executionInfo, boolean isStream) {
        return LlmChatRequestData.LlmChatRequestDataInitializer
                .of(certificationMap, defaultCertification, this.info(), executionInfo, isStream)
                .initialize();
    }

    private Flux<String> getRawStreamResponseFlux(LlmChatRequestData llmChatRequestData, ObjectNode body) {
        return this.toResponseSpec(llmChatRequestData, body)
                .flatMapMany(responseSpec -> responseSpec.bodyToFlux(String.class));
    }

    protected Mono<ResponseSpec> toResponseSpec(LlmChatRequestData llmChatRequestData, ObjectNode body) {
        return Mono.fromCallable(() -> {
            this.delegate.checkTokenCertification(llmChatRequestData);
            RequestBodySpec requestBodySpec = this.delegate.loadRequestBodySpec(llmChatRequestData);
            if (Objects.nonNull(body)) {
                requestBodySpec.bodyValue(body);
            }
            return requestBodySpec.retrieve()
                    .onStatus(HttpStatusCode::isError, LlmProviderUtil::handleClientResponseError);
        });
    }


    protected Mono<ObjectNode> generateRawRequestBody(@NonNull LlmChatRequestData llmChatRequestData) {
        return Mono.fromCallable(() -> {
            ObjectNode rawRequestBody = this.delegate.initializeRequestBody(llmChatRequestData);
            BiConsumer<ExecutionContext, ObjectNode> customizer = llmChatRequestData.getRawRequestCustomizerConfigure();
            if (Objects.nonNull(customizer)) {
                customizer.accept(llmChatRequestData.getExecutionContext(), rawRequestBody);
            }
            return rawRequestBody;
        });
    }
}
