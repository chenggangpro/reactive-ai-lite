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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmChatRequestData;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StructuredResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LLmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;
import pro.chenggang.project.reactive.ai.lite.core.util.LlmProviderUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser.StreamChunk;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType.CHAT;

/**
 * The common implementation of the LlmChatProvider interface.
 * This class provides default implementations for methods that are required by the interface.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Slf4j
public abstract class AbstractLlmChatProvider implements LlmChatProvider {

    private final LLmProviderInterceptorRegistry lLmProviderInterceptorRegistry;
    protected final Map<String, TokenCertification> certificationMap = new HashMap<>();
    protected final TokenCertification defaultCertification;
    protected final LlmProviderInfo llmProviderInfo;

    protected AbstractLlmChatProvider(@NonNull List<TokenCertification> certifications,
                                      @NonNull Function<Map<String, TokenCertification>, LlmProviderInfo> llmProviderInfoInitializer,
                                      @NonNull LLmProviderInterceptorRegistry lLmProviderInterceptorRegistry) {
        this.lLmProviderInterceptorRegistry = lLmProviderInterceptorRegistry;
        certifications.forEach(cert -> certificationMap.put(cert.profile(), cert));
        this.llmProviderInfo = llmProviderInfoInitializer.apply(this.certificationMap);
        if (!certifications.isEmpty()) {
            this.defaultCertification = certifications.stream()
                    .filter(TokenCertification::isDefault)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("At least one default TokenCertification is required  for " + this.llmProviderInfo));
        } else {
            this.defaultCertification = null;
        }
    }

    /**
     * Initialize Request Body Spec for LLM request.
     *
     * @param llmChatRequestData The LLM request data.
     * @return A RequestBodySpec.
     */
    protected abstract RequestBodySpec loadRequestBodySpec(@NonNull LlmChatRequestData llmChatRequestData);

    /**
     * Initialize Request Body for LLM request.
     *
     * @param llmChatRequestData The LLM request data.
     * @return the initialized Request Body.
     */
    protected abstract ObjectNode initializeRequestBody(@NonNull LlmChatRequestData llmChatRequestData);

    /**
     * Extract Request Messages from the raw response data.
     *
     * @param requestBody the raw response data.
     * @return the extracted Request Messages.
     */
    protected abstract ArrayNode extractRequestMessages(@NonNull ObjectNode requestBody);

    /**
     * Extract Stream Chunks from the raw response data.
     * This method is used for stream responses to determine stream data type and chunks.
     *
     * @param rawResponseData the raw response data.
     * @return the extracted Stream Chunks.
     */
    protected abstract StreamChunk[] extractStreamChunks(@NonNull ObjectNode rawResponseData);

    /**
     * Merge Raw Tool Call Messages into a single ObjectNode.
     *
     * @param rawToolCallMessages the raw Tool Call Messages.
     * @param distinctToolCalls   whether to distinct the tool calls of response
     * @return the merged ObjectNode.
     */
    protected abstract ObjectNode mergeRawToolCallMessages(@NonNull List<ObjectNode> rawToolCallMessages, boolean distinctToolCalls);

    /**
     * Extract raw response data to a GeneralResponse.
     *
     * @param toolDefinitions the tool definitions in the request.
     * @param rawResponse     the raw response data.
     * @return the GeneralResponse.
     */
    protected abstract Mono<GeneralResponse> extraGeneralResponse(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse);

    /**
     * Extract raw response data to a StructuredResponse.
     *
     * @param toolDefinitions the tool definitions in the request.
     * @param rawResponse     the raw response data.
     * @param resultType      the class type to deserialize the response into
     * @param <R>             result data
     * @return StructuredResponse.
     */
    protected abstract <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse, @NonNull Class<R> resultType);

    /**
     * Extract raw response data to a StructuredResponse.
     *
     * @param toolDefinitions the tool definitions in the request.
     * @param rawResponse     the raw response data.
     * @param resultType      the parameterized type reference to deserialize the response into
     * @param <R>             result data
     * @return StructuredResponse.
     */
    protected abstract <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull List<ToolDefinition> toolDefinitions,
                                                                                        @NonNull RawResponse rawResponse,
                                                                                        @NonNull ParameterizedTypeReference<R> resultType);

    /**
     * Extract raw stream response data to a StreamResponse.
     *
     * @param toolDefinitions   the tool definitions in the request.
     * @param rawStreamResponse the raw stream response data.
     * @return A StreamResponse.
     */
    protected abstract Mono<StreamResponse> extractStreamResponseContent(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawStreamResponse rawStreamResponse);

    @Override
    public LlmProviderInfo info() {
        return this.llmProviderInfo;
    }

    @Override
    public Mono<GeneralResponse> executeGeneral(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, false, null, null)
                .flatMap(llmRequestData -> {
                            return lLmProviderInterceptorRegistry.interceptMono(CHAT,
                                    this.llmProviderInfo,
                                    llmRequestData,
                                    Mono.defer(() -> this.executeInternalRaw(llmRequestData)
                                            .flatMap(rawResponse -> this.extraGeneralResponse(llmRequestData.getToolDefinitions(), rawResponse))
                                    )
                            );
                        }
                );
    }

    @Override
    public Mono<RawResponse> executeGeneralRaw(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, false, null, null)
                .flatMap(llmChatRequestData -> {
                    return lLmProviderInterceptorRegistry.interceptMono(CHAT,
                            this.llmProviderInfo,
                            llmChatRequestData,
                            Mono.defer(() -> executeInternalRaw(llmChatRequestData))
                    );
                });
    }

    @Override
    public Flux<StreamResponse> executeStream(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, true, null, null)
                .flatMapMany(llmRequestData -> {
                            return lLmProviderInterceptorRegistry.interceptFlux(CHAT,
                                    this.llmProviderInfo,
                                    llmRequestData,
                                    Flux.defer(() -> {
                                        return Mono.fromCallable(() -> {
                                                    ObjectNode requestBody = this.initializeRequestBody(llmRequestData);
                                                    BiConsumer<ExecutionContextView, ObjectNode> rawRequestCustomizer = llmRequestData.getRawRequestCustomizer();
                                                    if (Objects.nonNull(rawRequestCustomizer)) {
                                                        rawRequestCustomizer.accept(llmRequestData.getExecutionContextView(), requestBody);
                                                    }
                                                    if (log.isTraceEnabled()) {
                                                        log.trace("Executing request with body: {}", requestBody);
                                                    }
                                                    return requestBody;
                                                })
                                                .flatMapMany(requestBody -> {
                                                    return Flux.deferContextual(contextView -> {
                                                        return StreamResponseParser.parseStreamResponse(
                                                                        this.extractRequestMessages(requestBody),
                                                                        llmRequestData.getExecutionContextView(),
                                                                        this.getRawStreamResponseFlux(llmRequestData, requestBody),
                                                                        this::extractStreamChunks,
                                                                        rawToolCallMessages -> this.mergeRawToolCallMessages(rawToolCallMessages, llmRequestData.isDistinctToolCalls())
                                                                )
                                                                .concatMap(rawStreamResponse -> {
                                                                    if (log.isTraceEnabled()) {
                                                                        log.trace("Receiving raw stream response: {}", rawStreamResponse);
                                                                    }
                                                                    return Mono.justOrEmpty(llmRequestData.getRawStreamResponseCustomizer())
                                                                            .flatMap(consumer -> Mono
                                                                                    .<Void>fromRunnable(() -> consumer.accept(llmRequestData.getExecutionContextView(), rawStreamResponse))
                                                                            )
                                                                            .then(Mono.defer(() -> this.extractStreamResponseContent(llmRequestData.getToolDefinitions(), rawStreamResponse)));
                                                                })
                                                                .contextWrite(contextView);
                                                    });
                                                });
                                    })
                            );
                        }
                );
    }

    @Override
    public Flux<RawStreamResponse> executeStreamRaw(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, true, null, null)
                .flatMapMany(llmRequestData -> {
                            return lLmProviderInterceptorRegistry.interceptFlux(CHAT,
                                    this.llmProviderInfo,
                                    llmRequestData,
                                    Flux.defer(() -> {
                                        return Mono.fromCallable(() -> {
                                                    ObjectNode requestBody = this.initializeRequestBody(llmRequestData);
                                                    BiConsumer<ExecutionContextView, ObjectNode> rawRequestCustomizer = llmRequestData.getRawRequestCustomizer();
                                                    if (Objects.nonNull(rawRequestCustomizer)) {
                                                        rawRequestCustomizer.accept(llmRequestData.getExecutionContextView(), requestBody);
                                                    }
                                                    if (log.isTraceEnabled()) {
                                                        log.trace("Executing request with body: {}", requestBody);
                                                    }
                                                    return requestBody;
                                                })
                                                .flatMapMany(responseBody -> {

                                                            return Flux.deferContextual(contextView -> {
                                                                return StreamResponseParser.parseStreamResponse(
                                                                                this.extractRequestMessages(responseBody),
                                                                                llmRequestData.getExecutionContextView(),
                                                                                this.getRawStreamResponseFlux(llmRequestData, responseBody),
                                                                                this::extractStreamChunks,
                                                                                rawToolCallMessages -> this.mergeRawToolCallMessages(rawToolCallMessages, llmRequestData.isDistinctToolCalls())
                                                                        )
                                                                        .concatMap(rawStreamResponse -> {
                                                                            if (log.isTraceEnabled()) {
                                                                                log.trace("Receiving raw stream response: {}", rawStreamResponse);
                                                                            }
                                                                            return Mono.justOrEmpty(llmRequestData.getRawStreamResponseCustomizer())
                                                                                    .flatMap(consumer -> Mono
                                                                                            .<Void>fromRunnable(() -> consumer.accept(llmRequestData.getExecutionContextView(), rawStreamResponse))
                                                                                    )
                                                                                    .then(Mono.defer(() -> Mono.just(rawStreamResponse)));
                                                                        })
                                                                        .contextWrite(contextView);
                                                            });
                                                        }
                                                );
                                    })
                            );
                        }
                );
    }

    @Override
    public <R> Mono<StructuredResponse<R>> executeStructured(@NonNull ExecutionInfo executionInfo, @NonNull Class<R> resultType) {
        return this.initializeLlmRequestData(executionInfo, false, null, null)
                .flatMap(llmRequestData -> {
                            return lLmProviderInterceptorRegistry.interceptMono(CHAT,
                                    this.llmProviderInfo,
                                    llmRequestData,
                                    Mono.defer(() -> this.executeInternalRaw(llmRequestData)
                                            .flatMap(rawResponse -> this.extractStructuredResponseContent(llmRequestData.getToolDefinitions(), rawResponse, resultType)))
                            );
                        }
                );
    }

    @Override
    public <R> Mono<StructuredResponse<R>> executeStructured(@NonNull ExecutionInfo executionInfo, @NonNull ParameterizedTypeReference<R> resultType) {
        return this.initializeLlmRequestData(executionInfo, false, null, null)
                .flatMap(llmRequestData -> {
                            return lLmProviderInterceptorRegistry.interceptMono(CHAT,
                                    this.llmProviderInfo,
                                    llmRequestData,
                                    Mono.defer(() -> {
                                        return this.executeInternalRaw(llmRequestData)
                                                .flatMap(rawResponse -> this.extractStructuredResponseContent(llmRequestData.getToolDefinitions(), rawResponse, resultType));
                                    })
                            );
                        }
                );
    }

    @Override
    public Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull String responseJsonSchema) {
        return this.initializeLlmRequestData(executionInfo, false, null, responseJsonSchema)
                .flatMap(llmChatRequestData -> {
                    return lLmProviderInterceptorRegistry.interceptMono(CHAT,
                            this.llmProviderInfo,
                            llmChatRequestData,
                            Mono.defer(() -> executeInternalRaw(llmChatRequestData))
                    );
                });
    }

    @Override
    public <R> Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull Class<R> resultType) {
        return this.initializeLlmRequestData(executionInfo, false, resultType, null)
                .flatMap(llmChatRequestData -> {
                    return lLmProviderInterceptorRegistry.interceptMono(CHAT,
                            this.llmProviderInfo,
                            llmChatRequestData,
                            Mono.defer(() -> executeInternalRaw(llmChatRequestData))
                    );
                });
    }

    @Override
    public <R> Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull ParameterizedTypeReference<R> resultType) {
        return this.initializeLlmRequestData(executionInfo, false, resultType.getType(), null)
                .flatMap(llmChatRequestData -> {
                    return lLmProviderInterceptorRegistry.interceptMono(CHAT,
                            this.llmProviderInfo,
                            llmChatRequestData,
                            Mono.defer(() -> executeInternalRaw(llmChatRequestData))
                    );
                });
    }

    protected Mono<RawResponse> executeInternalRaw(@NonNull LlmChatRequestData llmChatRequestData) {
        return Mono.fromCallable(() -> {
                    ObjectNode requestBody = this.initializeRequestBody(llmChatRequestData);
                    BiConsumer<ExecutionContextView, ObjectNode> rawRequestCustomizer = llmChatRequestData.getRawRequestCustomizer();
                    if (Objects.nonNull(rawRequestCustomizer)) {
                        rawRequestCustomizer.accept(llmChatRequestData.getExecutionContextView(), requestBody);
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Executing request with body: {}", requestBody);
                    }
                    return requestBody;
                })
                .flatMap(body -> {
                    return this.toResponseSpec(llmChatRequestData, body)
                            .flatMap(responseSpec -> responseSpec.bodyToMono(new ParameterizedTypeReference<ObjectNode>() {}))
                            .map(rawJsonResponse -> {
                                if (log.isTraceEnabled()) {
                                    log.trace("Received response with body: {}", rawJsonResponse);
                                }
                                return RawResponse.builder()
                                        .contextView(llmChatRequestData.getExecutionContextView())
                                        .rawRequestMessages(this.extractRequestMessages(body))
                                        .rawResponse(rawJsonResponse)
                                        .build();
                            })
                            .flatMap(rawResponse -> {
                                return Mono.justOrEmpty(llmChatRequestData.getRawResponseCustomizer())
                                        .flatMap(consumer -> Mono.
                                                <Void>fromRunnable(() -> consumer.accept(llmChatRequestData.getExecutionContextView(), rawResponse)
                                        ))
                                        .then(Mono.defer(() -> Mono.just(rawResponse)));
                            });
                });
    }

    private Mono<LlmChatRequestData> initializeLlmRequestData(@NonNull ExecutionInfo executionInfo, boolean isStream, Type structuredOutputType, String responseJsonSchema) {
        return Mono.fromCallable(() -> LlmChatRequestData.LlmChatRequestDataInitializer
                .of(certificationMap, defaultCertification, llmProviderInfo, executionInfo, isStream, structuredOutputType, responseJsonSchema)
                .initialize()
        );
    }

    private Flux<String> getRawStreamResponseFlux(LlmChatRequestData llmChatRequestData, ObjectNode body) {
        return this.toResponseSpec(llmChatRequestData, body)
                .flatMapMany(responseSpec -> responseSpec.bodyToFlux(String.class));
    }

    protected RequestBodySpec initializeRequestBodySpec(@NonNull LlmChatRequestData llmChatRequestData) {
        AtomicBoolean certificationSet = new AtomicBoolean(false);
        Optional<TokenCertification> optionalTokenCertification = llmChatRequestData.getTokenCertification();
        RequestBodySpec requestBodySpec = this.loadRequestBodySpec(llmChatRequestData);
        requestBodySpec.contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, "reactive-ai-lite")
                .acceptCharset(StandardCharsets.UTF_8);
        if (llmChatRequestData.isStream()) {
            requestBodySpec.accept(MediaType.TEXT_EVENT_STREAM);
        } else {
            requestBodySpec.accept(MediaType.APPLICATION_JSON);
        }
        if (optionalTokenCertification.isPresent()) {
            TokenCertification tokenCertification = optionalTokenCertification.get();
            if (tokenCertification instanceof BearerTokenCertification bearerTokenCertification) {
                if (!certificationSet.get()) {
                    requestBodySpec.headers(bearerTokenCertification::applyTo);
                    certificationSet.set(true);
                }
            }
        }
        if (!certificationSet.get()) {
            log.debug("No token certification be applied, cause of the unknown TokenCertification : {}", optionalTokenCertification);
        }
        return requestBodySpec;
    }

    protected Mono<ResponseSpec> toResponseSpec(LlmChatRequestData llmChatRequestData, ObjectNode body) {
        return Mono.create(sink -> {
            RequestBodySpec requestBodySpec;
            try {
                requestBodySpec = this.initializeRequestBodySpec(llmChatRequestData);
                if (Objects.nonNull(body)) {
                    requestBodySpec.bodyValue(body);
                }
            } catch (Exception e) {
                sink.error(e);
                return;
            }
            ResponseSpec responseSpec = requestBodySpec.retrieve()
                    .onStatus(HttpStatusCode::isError, LlmProviderUtil::handleClientResponseError);
            sink.success(responseSpec);
        });
    }
}
