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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmSpeechRequestData;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmSpeechRequestData.LlmSpeechRequestDataInitializer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechRawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SpeechExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmSpeechProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.delegate.LlmSpeechProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.core.util.LlmProviderUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType.SPEECH;

/**
 * Default implementation of {@link LlmSpeechProvider} that acts as a generic composition host.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class DefaultLlmSpeechProvider implements LlmSpeechProvider {

    private final LlmSpeechProviderDelegate delegate;
    private final LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry;
    protected final Map<String, TokenCertification> certificationMap = new ConcurrentHashMap<>();
    protected final TokenCertification defaultCertification;

    /**
     * Constructs a new DefaultLlmSpeechProvider.
     *
     * @param delegate                       the provider-specific delegate implementation
     * @param certifications                 the list of token certifications for authentication
     * @param lLmProviderInterceptorRegistry the registry for provider interceptors
     */
    public DefaultLlmSpeechProvider(@NonNull LlmSpeechProviderDelegate delegate,
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
     * Returns the capability type of this provider.
     *
     * @return the capability type (SPEECH)
     */
    @Override
    public Capability capability() {
        return Capability.SPEECH;
    }

    /**
     * Returns the provider information.
     *
     * @return the LLM provider info
     */
    @Override
    public LlmProviderInfo info() {
        return this.delegate.providerInfo();
    }

    /**
     * Executes the speech request and returns the processed response.
     *
     * @param executionInfo the speech execution information containing dynamic settings
     * @return a Mono emitting the SpeechResponse
     */
    @Override
    public Mono<SpeechResponse> executeSpeech(@NonNull SpeechExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo)
                .flatMap(llmSpeechRequestData -> {
                    return this.generateRawRequestBody(llmSpeechRequestData)
                            .flatMap(requestBody -> {
                                return LlmProviderInterceptorRegistry.InterceptedDataInfo.builder()
                                        .clientType(SPEECH)
                                        .llmProviderInfo(this.info())
                                        .executionContext(llmSpeechRequestData.getExecutionContext())
                                        .rawRequestBody(requestBody)
                                        .build()
                                        .interceptGeneral(this.lLmProviderInterceptorRegistry, this.toResponseSpec(llmSpeechRequestData, requestBody)
                                                .flatMap(responseSpec -> this.delegate.extractGeneralResponse(responseSpec, llmSpeechRequestData.getExecutionContext()))
                                                .map(dataBuffer -> {
                                                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                                    dataBuffer.read(bytes);
                                                    DataBufferUtils.release(dataBuffer);
                                                    return bytes;
                                                })
                                        );
                            })
                            .map(bytes -> {
                                return SpeechResponse.builder()
                                        .executionContext(llmSpeechRequestData.getExecutionContext())
                                        .audioData(bytes)
                                        .build();
                            });
                });
    }

    /**
     * Executes the speech request returning the raw response from the LLM.
     *
     * @param executionInfo the speech execution information containing dynamic settings
     * @return a Mono emitting the SpeechRawResponse
     */
    @Override
    public Mono<SpeechRawResponse> executeSpeechRaw(@NonNull SpeechExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo)
                .flatMap(llmSpeechRequestData -> {
                    return this.generateRawRequestBody(llmSpeechRequestData)
                            .flatMap(requestBody -> {
                                return LlmProviderInterceptorRegistry.InterceptedDataInfo.builder()
                                        .clientType(SPEECH)
                                        .llmProviderInfo(this.info())
                                        .executionContext(llmSpeechRequestData.getExecutionContext())
                                        .rawRequestBody(requestBody)
                                        .build()
                                        .interceptGeneral(this.lLmProviderInterceptorRegistry, this.toResponseSpec(llmSpeechRequestData, requestBody)
                                                .flatMap(responseSpec -> this.delegate.extractGeneralResponse(responseSpec, llmSpeechRequestData.getExecutionContext()))
                                        );
                            })
                            .map(dataBuffer -> {
                                return SpeechRawResponse.builder()
                                        .executionContext(llmSpeechRequestData.getExecutionContext())
                                        .dataChunk(dataBuffer)
                                        .build();
                            });
                });
    }

    /**
     * Executes a streaming speech request.
     *
     * @param executionInfo the speech execution information containing dynamic settings
     * @return a Flux emitting SpeechStreamResponse chunks
     */
    @Override
    public Flux<SpeechStreamResponse> executeSpeechStream(@NonNull SpeechExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo)
                .flatMapMany(llmSpeechRequestData -> {
                    return this.generateRawRequestBody(llmSpeechRequestData)
                            .flatMapMany(requestBody -> {
                                return LlmProviderInterceptorRegistry.InterceptedDataInfo.builder()
                                        .clientType(SPEECH)
                                        .llmProviderInfo(this.info())
                                        .executionContext(llmSpeechRequestData.getExecutionContext())
                                        .rawRequestBody(requestBody)
                                        .build()
                                        .interceptStream(this.lLmProviderInterceptorRegistry, this.toResponseSpec(llmSpeechRequestData, requestBody)
                                                .flatMapMany(responseSpec -> this.delegate.extractStreamResponse(responseSpec, llmSpeechRequestData.getExecutionContext()))
                                                .map(dataBuffer -> {
                                                    try {
                                                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                                        dataBuffer.read(bytes);
                                                        return bytes;
                                                    } finally {
                                                        DataBufferUtils.release(dataBuffer);
                                                    }
                                                })
                                                .doOnDiscard(DataBuffer.class, DataBufferUtils::release)
                                        );
                            })
                            .map(bytes -> {
                                return SpeechStreamResponse.builder()
                                        .executionContext(llmSpeechRequestData.getExecutionContext())
                                        .chunk(bytes)
                                        .build();
                            });
                });
    }

    /**
     * Executes a streaming speech request returning raw response chunks.
     *
     * @param executionInfo the speech execution information containing dynamic settings
     * @return a Flux emitting SpeechRawResponse chunks
     */
    @Override
    public Flux<SpeechRawResponse> executeSpeechStreamRaw(@NonNull SpeechExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo)
                .flatMapMany(llmSpeechRequestData -> {
                    return this.generateRawRequestBody(llmSpeechRequestData)
                            .flatMapMany(requestBody -> {
                                return LlmProviderInterceptorRegistry.InterceptedDataInfo.builder()
                                        .clientType(SPEECH)
                                        .llmProviderInfo(this.info())
                                        .executionContext(llmSpeechRequestData.getExecutionContext())
                                        .rawRequestBody(requestBody)
                                        .build()
                                        .interceptStream(this.lLmProviderInterceptorRegistry, this.toResponseSpec(llmSpeechRequestData, requestBody)
                                                .flatMapMany(responseSpec -> this.delegate.extractStreamResponse(responseSpec, llmSpeechRequestData.getExecutionContext()))
                                        );
                            })
                            .map(dataBuffer -> {
                                return SpeechRawResponse.builder()
                                        .executionContext(llmSpeechRequestData.getExecutionContext())
                                        .dataChunk(dataBuffer)
                                        .build();
                            });
                });
    }

    /**
     * Initializes the LLM speech request data based on the execution info.
     *
     * @param executionInfo the speech execution information
     * @return a Mono emitting the constructed LlmSpeechRequestData
     */
    private Mono<LlmSpeechRequestData> initializeLlmRequestData(@NonNull SpeechExecutionInfo executionInfo) {
        return LlmSpeechRequestDataInitializer
                .of(certificationMap, defaultCertification, this.info(), executionInfo)
                .initialize();
    }

    /**
     * Creates a WebClient ResponseSpec from the request data and body.
     *
     * @param llmSpeechRequestData the LLM speech request data
     * @param body                 the JSON body to send
     * @return a Mono emitting the ResponseSpec
     */
    protected Mono<ResponseSpec> toResponseSpec(LlmSpeechRequestData llmSpeechRequestData, ObjectNode body) {
        return Mono.fromCallable(() -> {
            this.delegate.checkTokenCertification(llmSpeechRequestData);
            RequestBodySpec requestBodySpec = this.delegate.loadRequestBodySpec(llmSpeechRequestData);
            if (Objects.nonNull(body)) {
                requestBodySpec.bodyValue(body);
            }
            return requestBodySpec.retrieve()
                    .onStatus(HttpStatusCode::isError, LlmProviderUtil::handleClientResponseError);
        });
    }

    /**
     * Generates the raw JSON request body to send to the provider.
     *
     * @param llmSpeechRequestData the LLM speech request data
     * @return a Mono emitting the constructed ObjectNode body
     */
    protected Mono<ObjectNode> generateRawRequestBody(@NonNull LlmSpeechRequestData llmSpeechRequestData) {
        return Mono.fromCallable(() -> {
            ObjectNode rawRequestBody = this.delegate.initializeRequestBody(llmSpeechRequestData);
            BiConsumer<ExecutionContext, ObjectNode> customizer = llmSpeechRequestData.getRawRequestCustomizerConfigure();
            if (Objects.nonNull(customizer)) {
                customizer.accept(llmSpeechRequestData.getExecutionContext(), rawRequestBody);
            }
            return rawRequestBody;
        });
    }
}
