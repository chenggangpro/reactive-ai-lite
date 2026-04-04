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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
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
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser.JsonStreamChunkSlide;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType.CHAT;

/**
 * A common base class for implementing the {@link LlmChatProvider} interface.
 * <p>
 * This abstract class provides the scaffolding for executing reactive chat requests
 * against an LLM API using Spring's WebClient. It handles the orchestration of
 * interceptors, parsing stream responses, managing token certifications, and mapping
 * the generic execution flows defined in {@link LlmChatProvider} to specific
 * provider implementations via abstract template methods.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Slf4j
public abstract class AbstractLlmChatProvider implements LlmChatProvider {

    /**
     * The registry for executing interceptors before, during, and after LLM requests.
     */
    private final LLmProviderInterceptorRegistry lLmProviderInterceptorRegistry;

    /**
     * A map storing available token certifications, keyed by profile name.
     */
    protected final Map<String, TokenCertification> certificationMap = new ConcurrentHashMap<>();

    /**
     * The default token certification to use if no profile is explicitly selected.
     */
    protected final TokenCertification defaultCertification;

    /**
     * Metadata information about this LLM provider.
     */
    protected final LlmProviderInfo llmProviderInfo;

    /**
     * Constructs a new {@link AbstractLlmChatProvider}.
     *
     * @param certifications               a list of token certifications to register
     * @param llmProviderInfoInitializer   a function to initialize the provider info based on the registered certifications
     * @param lLmProviderInterceptorRegistry the registry for interceptors
     */
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
     * Initializes the WebClient request specification for this specific provider.
     * <p>
     * Implementations must provide the base URL and endpoint setup here.
     * </p>
     *
     * @param llmChatRequestData the structured request data
     * @return a configured {@link RequestBodySpec}
     */
    protected abstract RequestBodySpec loadRequestBodySpec(@NonNull LlmChatRequestData llmChatRequestData);

    /**
     * Transforms the generic chat request data into the provider-specific JSON payload.
     *
     * @param llmChatRequestData the structured request data
     * @return a JSON {@link ObjectNode} representing the request body
     */
    protected abstract ObjectNode initializeRequestBody(@NonNull LlmChatRequestData llmChatRequestData);

    /**
     * Extracts parsed stream chunks from a raw JSON stream response object.
     * <p>
     * Since different providers have different SSE payload structures, implementations
     * must parse the provider's specific JSON into standardized {@link JsonStreamChunkSlide}s.
     * </p>
     *
     * @param rawResponseData the raw JSON object from the stream
     * @return an array of extracted stream chunks
     */
    protected abstract JsonStreamChunkSlide[] extractStreamChunks(@NonNull ObjectNode rawResponseData);

    /**
     * Merges multiple raw tool call message chunks into a single, cohesive JSON object.
     *
     * @param rawToolCallMessages a list of raw JSON tool call fragments
     * @param distinctToolCalls   whether to filter for distinct tool calls
     * @return the merged {@link ObjectNode}
     */
    protected abstract ObjectNode mergeRawToolCallMessages(@NonNull List<ObjectNode> rawToolCallMessages, boolean distinctToolCalls);

    /**
     * Extracts a standardized {@link GeneralResponse} from the raw provider response.
     *
     * @param toolDefinitions the tools that were available during the request
     * @param rawResponse     the raw response data
     * @return a Mono emitting the parsed {@link GeneralResponse}
     */
    protected abstract Mono<GeneralResponse> extraGeneralResponse(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse);

    /**
     * Extracts a {@link StructuredResponse} from the raw provider response based on a class type.
     *
     * @param toolDefinitions the tools that were available during the request
     * @param rawResponse     the raw response data
     * @param resultType      the class type to deserialize into
     * @param <R>             the target type
     * @return a Mono emitting the {@link StructuredResponse}
     */
    protected abstract <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse, @NonNull Class<R> resultType);

    /**
     * Extracts a {@link StructuredResponse} from the raw provider response based on a parameterized type.
     *
     * @param toolDefinitions the tools that were available during the request
     * @param rawResponse     the raw response data
     * @param resultType      the parameterized type reference to deserialize into
     * @param <R>             the target type
     * @return a Mono emitting the {@link StructuredResponse}
     */
    protected abstract <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull List<ToolDefinition> toolDefinitions,
                                                                                        @NonNull RawResponse rawResponse,
                                                                                        @NonNull ParameterizedTypeReference<R> resultType);

    /**
     * Extracts a standardized {@link StreamResponse} from a raw stream chunk.
     *
     * @param toolDefinitions   the tools that were available during the request
     * @param rawStreamResponse the raw stream chunk
     * @return a Mono emitting the parsed {@link StreamResponse}
     */
    protected abstract Mono<StreamResponse> extractStreamResponseContent(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawStreamResponse rawStreamResponse);

    /**
     * Returns the metadata and configuration information for this provider.
     *
     * @return the {@link LlmProviderInfo}
     */
    @Override
    public LlmProviderInfo info() {
        return this.llmProviderInfo;
    }

    /**
     * Executes a general chat request.
     */
    @Override
    public Mono<GeneralResponse> executeGeneral(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, false, null, null)
                .flatMap(llmRequestData -> this.executeInternalRaw(llmRequestData)
                        .flatMap(rawResponse -> this.extraGeneralResponse(llmRequestData.getToolDefinitions(), rawResponse))
                );
    }

    /**
     * Executes a general chat request, returning raw output.
     */
    @Override
    public Mono<RawResponse> executeGeneralRaw(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, false, null, null)
                .flatMap(this::executeInternalRaw);
    }

    /**
     * Executes a streaming chat request.
     */
    @Override
    public Flux<StreamResponse> executeStream(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, true, null, null)
                .flatMapMany(llmRequestData -> {
                            return Mono.fromCallable(() -> this.initializeRequestBody(llmRequestData))
                                    .flatMapMany(requestBody -> {
                                        return LLmProviderInterceptorRegistry.newInterceptedDataInfoBuilder()
                                                .clientType(CHAT)
                                                .llmProviderInfo(this.llmProviderInfo)
                                                .executionContextView(llmRequestData.getExecutionContextView())
                                                .rawRequestBody(requestBody)
                                                .build()
                                                .interceptStream(this.lLmProviderInterceptorRegistry,
                                                        Flux.deferContextual(contextView -> {
                                                            return StreamResponseParser.parseStreamResponse(
                                                                            llmRequestData.getExecutionContextView(),
                                                                            this.getRawStreamResponseFlux(llmRequestData, requestBody),
                                                                            this::extractStreamChunks,
                                                                            rawToolCallMessages -> this.mergeRawToolCallMessages(rawToolCallMessages, llmRequestData.isDistinctToolCalls())
                                                                    )
                                                                    .contextWrite(contextView);
                                                        })
                                                );
                                    })
                                    .concatMap(rawStreamResponse -> this.extractStreamResponseContent(llmRequestData.getToolDefinitions(), rawStreamResponse));
                        }
                );
    }

    /**
     * Executes a streaming chat request, returning raw stream chunks.
     */
    @Override
    public Flux<RawStreamResponse> executeStreamRaw(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, true, null, null)
                .flatMapMany(llmRequestData -> {
                            return Mono.fromCallable(() -> this.initializeRequestBody(llmRequestData))
                                    .flatMapMany(requestBody -> {
                                        return LLmProviderInterceptorRegistry.newInterceptedDataInfoBuilder()
                                                .clientType(CHAT)
                                                .llmProviderInfo(this.llmProviderInfo)
                                                .executionContextView(llmRequestData.getExecutionContextView())
                                                .rawRequestBody(requestBody)
                                                .build()
                                                .interceptStream(this.lLmProviderInterceptorRegistry,
                                                        Flux.deferContextual(contextView -> {
                                                            return StreamResponseParser.parseStreamResponse(
                                                                            llmRequestData.getExecutionContextView(),
                                                                            this.getRawStreamResponseFlux(llmRequestData, requestBody),
                                                                            this::extractStreamChunks,
                                                                            rawToolCallMessages -> this.mergeRawToolCallMessages(rawToolCallMessages, llmRequestData.isDistinctToolCalls())
                                                                    )
                                                                    .contextWrite(contextView);
                                                        })
                                                );
                                    });
                        }
                );
    }

    /**
     * Executes a structured chat request using a class type.
     */
    @Override
    public <R> Mono<StructuredResponse<R>> executeStructured(@NonNull ExecutionInfo executionInfo, @NonNull Class<R> resultType) {
        return this.initializeLlmRequestData(executionInfo, false, resultType, null)
                .flatMap(llmRequestData -> {
                            return this.executeInternalRaw(llmRequestData)
                                    .flatMap(rawResponse -> this.extractStructuredResponseContent(llmRequestData.getToolDefinitions(), rawResponse, resultType));
                        }
                );
    }

    /**
     * Executes a structured chat request using a parameterized type.
     */
    @Override
    public <R> Mono<StructuredResponse<R>> executeStructured(@NonNull ExecutionInfo executionInfo, @NonNull ParameterizedTypeReference<R> resultType) {
        return this.initializeLlmRequestData(executionInfo, false, resultType.getType(), null)
                .flatMap(llmRequestData -> {
                            return this.executeInternalRaw(llmRequestData)
                                    .flatMap(rawResponse -> this.extractStructuredResponseContent(llmRequestData.getToolDefinitions(), rawResponse, resultType));
                        }
                );
    }

    /**
     * Executes a structured chat request using a JSON schema string, returning raw JSON output.
     */
    @Override
    public Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull String responseJsonSchema) {
        return this.initializeLlmRequestData(executionInfo, false, null, responseJsonSchema)
                .flatMap(this::executeInternalRaw);
    }

    /**
     * Executes a structured chat request using a class type, returning raw JSON output.
     */
    @Override
    public <R> Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull Class<R> resultType) {
        return this.initializeLlmRequestData(executionInfo, false, resultType, null)
                .flatMap(this::executeInternalRaw);
    }

    /**
     * Executes a structured chat request using a parameterized type, returning raw JSON output.
     */
    @Override
    public <R> Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull ParameterizedTypeReference<R> resultType) {
        return this.initializeLlmRequestData(executionInfo, false, resultType.getType(), null)
                .flatMap(this::executeInternalRaw);
    }

    /**
     * Executes the core logic for a non-streaming request, applying interceptors and fetching the raw response.
     *
     * @param llmChatRequestData the request data
     * @return a Mono emitting the raw response
     */
    protected Mono<RawResponse> executeInternalRaw(@NonNull LlmChatRequestData llmChatRequestData) {
        return Mono.fromCallable(() -> this.initializeRequestBody(llmChatRequestData))
                .flatMap(requestBody -> {
                    return LLmProviderInterceptorRegistry.newInterceptedDataInfoBuilder()
                            .clientType(CHAT)
                            .llmProviderInfo(this.llmProviderInfo)
                            .executionContextView(llmChatRequestData.getExecutionContextView())
                            .rawRequestBody(requestBody)
                            .build()
                            .interceptGeneral(this.lLmProviderInterceptorRegistry,
                                    this.toResponseSpec(llmChatRequestData, requestBody)
                                            .flatMap(responseSpec -> responseSpec.bodyToMono(new ParameterizedTypeReference<ObjectNode>() {}))
                            );
                })
                .map(rawResponseBody -> {
                    return RawResponse.builder()
                            .contextView(llmChatRequestData.getExecutionContextView())
                            .responseBody(rawResponseBody)
                            .build();
                });
    }

    /**
     * Initializes the {@link LlmChatRequestData} from the given execution info.
     */
    private Mono<LlmChatRequestData> initializeLlmRequestData(@NonNull ExecutionInfo executionInfo, boolean isStream, Type structuredOutputType, String responseJsonSchema) {
        return Mono.fromCallable(() -> LlmChatRequestData.LlmChatRequestDataInitializer
                .of(certificationMap, defaultCertification, llmProviderInfo, executionInfo, isStream, structuredOutputType, responseJsonSchema)
                .initialize()
        );
    }

    /**
     * Retrieves the raw string flux from the WebClient response spec.
     */
    private Flux<String> getRawStreamResponseFlux(LlmChatRequestData llmChatRequestData, ObjectNode body) {
        return this.toResponseSpec(llmChatRequestData, body)
                .flatMapMany(responseSpec -> responseSpec.bodyToFlux(String.class));
    }

    /**
     * Configures the common headers and accept types for the WebClient request.
     *
     * @param llmChatRequestData the request data containing token info
     * @return the configured RequestBodySpec
     */
    protected RequestBodySpec initializeRequestBodySpec(@NonNull LlmChatRequestData llmChatRequestData) {
        boolean certificationSet = false;
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
                requestBodySpec.headers(bearerTokenCertification::applyTo);
                certificationSet = true;
            }
        }
        if (!certificationSet) {
            log.debug("No token certification be applied, cause of the unknown TokenCertification : {}", optionalTokenCertification);
        }
        return requestBodySpec;
    }

    /**
     * Performs the actual WebClient exchange to obtain a ResponseSpec.
     *
     * @param llmChatRequestData the request data
     * @param body               the JSON request body
     * @return a Mono emitting the ResponseSpec
     */
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
