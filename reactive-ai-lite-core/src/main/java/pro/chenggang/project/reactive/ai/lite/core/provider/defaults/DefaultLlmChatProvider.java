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
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionInfo;
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
 * Default implementation of the {@link LlmChatProvider} interface.
 * <p>
 * This class serves as a generic execution host that orchestrates the lifecycle of a chat request,
 * delegating provider‑specific behavior (request construction, response parsing, token handling)
 * to an injected {@link LlmChatProviderDelegate} and enriching the pipeline with
 * {@link LlmProviderInterceptorRegistry} hooks.
 * </p>
 * <p>
 * The request flow is:
 * <ol>
 *   <li>Initialize a {@link LlmChatRequestData} object from the execution info,
 *       resolving the correct {@link TokenCertification} and populating chat options.</li>
 *   <li>Generate the raw request body (JSON) via the delegate, optionally applying a
 *       customizer stored in the request data.</li>
 *   <li>Wrap the HTTP interaction with interceptor logic:
 *       <ul>
 *         <li>For general (non‑streaming) requests, a synchronous interceptor chain
 *             modulates the {@code Mono} of the response body.</li>
 *         <li>For streaming requests, a stream interceptor chain wraps the
 *             {@code Flux} of raw SSE lines, which are then parsed by
 *             {@link StreamResponseParser} using delegate’s chunk extraction and
 *             tool‑call merging strategies.</li>
 *       </ul>
 *   </li>
 *   <li>Hand the operation to the delegate for final extraction of the appropriate
 *       response model (general or streaming).</li>
 * </ol>
 * This separation of concerns allows the provider to remain agnostic to the concrete
 * LLM API while still supporting certification, customization, and
 * request/response interception in a uniform manner.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class DefaultLlmChatProvider implements LlmChatProvider {

    /**
     * The provider‑specific delegate that implements the concrete API contract
     * for a particular LLM service.
     */
    private final LlmChatProviderDelegate delegate;

    /**
     * Registry of interceptors that can enhance or modify request/response
     * cycles for general and streaming operations.
     */
    private final LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry;

    /**
     * Maps a logical profile name to its corresponding token certification,
     * enabling dynamic certification selection based on the execution context.
     */
    protected final Map<String, TokenCertification> certificationMap = new ConcurrentHashMap<>();

    /**
     * The fallback certification used when no explicit profile is specified
     * in the execution info. If certifications are provided, exactly one
     * must be marked as default.
     */
    protected final TokenCertification defaultCertification;

    /**
     * Constructs a new {@link DefaultLlmChatProvider} with the required delegate,
     * a list of token certifications, and the interceptor registry.
     * <p>
     * The list of certifications is converted into a concurrent map keyed by
     * {@link TokenCertification#profile()}, and the single certification flagged
     * as {@code isDefault()} is selected as the default. If certifications are
     * present but none is flagged, an {@link IllegalArgumentException} is thrown.
     *
     * @param delegate                       the provider‑specific strategy delegate; must not be {@code null}
     * @param certifications                 a non‑null list of token certifications; may be empty
     * @param lLmProviderInterceptorRegistry the interceptor registry; must not be {@code null}
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

    /**
     * Returns the provider metadata describing the underlying LLM service.
     * Delegates directly to {@link LlmChatProviderDelegate#providerInfo()}.
     *
     * @return the LLM provider info
     */
    @Override
    public LlmProviderInfo info() {
        return this.delegate.providerInfo();
    }

    /**
     * Executes a general (non‑streaming) chat request and returns a
     * fully parsed {@link GeneralResponse}.
     * <p>
     * The operation first initializes the request data with certifications
     * and chat options (stream flag set to {@code false}), obtains the raw
     * JSON response through the internal execution pipeline, and finally
     * extracts the general response using the delegate.
     *
     * @param executionInfo the chat execution information; must not be {@code null}
     * @return a Mono emitting the general chat response
     */
    @Override
    public Mono<GeneralResponse> executeGeneral(@NonNull ChatExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, false)
                .flatMap(llmRequestData -> this.executeInternalRaw(llmRequestData)
                        .flatMap(rawResponse -> this.delegate.extractGeneralResponse(llmRequestData.getToolDefinitions(), rawResponse))
                );
    }

    /**
     * Executes a general (non‑streaming) chat request and returns the raw
     * JSON response body wrapped in a {@link RawResponse}.
     * <p>
     * This is the unfiltered, provider‑specific payload before any
     * extraction into application‑level models. It is useful for
     * debugging or when custom parsing is required.
     *
     * @param executionInfo the chat execution information; must not be {@code null}
     * @return a Mono emitting the raw response wrapper
     */
    @Override
    public Mono<RawResponse> executeGeneralRaw(@NonNull ChatExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, false)
                .flatMap(this::executeInternalRaw);
    }

    /**
     * Executes a streaming chat request and returns a flux of
     * {@link StreamResponse} items representing the parsed
     * streaming content.
     * <p>
     * The implementation initializes the request data with stream mode enabled,
     * generates the raw request body, and then builds an intercepted stream
     * through the following steps:
     * <ol>
     *   <li>The raw request body is passed to the interceptor registry to
     *       allow pre‑stream hooks.</li>
     *   <li>A {@link StreamResponseParser} is used to convert the raw
     *       SSE lines into {@code RawStreamResponse} chunks, applying the
     *       delegate’s chunk extraction logic and tool‑call merging strategy.</li>
     *   <li>The parsed stream is then further intercepted through the registry’s
     *       stream interceptor chain.</li>
     *   <li>Finally, the delegate extracts the high‑level {@link StreamResponse}
     *       from each {@code RawStreamResponse}.</li>
     * </ol>
     *
     * @param executionInfo the chat execution information; must not be {@code null}
     * @return a Flux of streaming responses
     */
    @Override
    public Flux<StreamResponse> executeStream(@NonNull ChatExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, true)
                .flatMapMany(llmRequestData -> {
                            return this.generateRawRequestBody(llmRequestData)
                                    .flatMapMany(requestBody -> {
                                        return LlmProviderInterceptorRegistry.InterceptedDataInfo.builder()
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

    /**
     * Executes a streaming chat request and returns the raw SSE stream as a flux of
     * {@link RawStreamResponse} items.
     * <p>
     * This method operates identically to {@link #executeStream(ChatExecutionInfo)}
     * but omits the final extraction step, emitting the unchecked stream chunks
     * directly. It is useful for custom aggregation or for inspecting the raw
     * streaming protocol details.
     *
     * @param executionInfo the chat execution information; must not be {@code null}
     * @return a Flux of raw streaming responses
     */
    @Override
    public Flux<RawStreamResponse> executeStreamRaw(@NonNull ChatExecutionInfo executionInfo) {
        return this.initializeLlmRequestData(executionInfo, true)
                .flatMapMany(llmRequestData -> {
                            return this.generateRawRequestBody(llmRequestData)
                                    .flatMapMany(requestBody -> {
                                        return LlmProviderInterceptorRegistry.InterceptedDataInfo.builder()
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

    /**
     * Core internal execution method for a general (non‑streaming) request.
     * <p>
     * It generates the raw request body, runs it through the interceptor chain
     * configured for general requests, and then wraps the resulting JSON node
     * in a {@link RawResponse} along with the original execution context.
     *
     * @param llmChatRequestData the fully initialized request data; must not be {@code null}
     * @return a Mono emitting a raw response
     */
    protected Mono<RawResponse> executeInternalRaw(@NonNull LlmChatRequestData llmChatRequestData) {
        return this.generateRawRequestBody(llmChatRequestData)
                .flatMap(requestBody -> {
                    return LlmProviderInterceptorRegistry.InterceptedDataInfo.builder()
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

    /**
     * Initializes an {@link LlmChatRequestData} object from the given execution info
     * and stream flag.
     * <p>
     * An {@link LlmChatRequestData.LlmChatRequestDataInitializer} is created,
     * populated with the certification map, default certification, provider info,
     * execution info, and stream mode. The initializer resolves the appropriate
     * certification, applies default options, and injects the execution context.
     *
     * @param executionInfo the chat execution details; must not be {@code null}
     * @param isStream      {@code true} if the request should be treated as a streaming interaction
     * @return a Mono emitting the initialized request data
     */
    private Mono<LlmChatRequestData> initializeLlmRequestData(@NonNull ChatExecutionInfo executionInfo, boolean isStream) {
        return LlmChatRequestData.LlmChatRequestDataInitializer
                .of(certificationMap, defaultCertification, this.info(), executionInfo, isStream)
                .initialize();
    }

    /**
     * Converts the given request data and body into a Flux of raw string event
     * stream lines.
     * <p>
     * This method obtains a {@link ResponseSpec} via {@link #toResponseSpec(LlmChatRequestData, ObjectNode)}
     * and requests the body as a {@code Flux<String>}, which corresponds to the
     * server‑sent events (SSE) stream.
     *
     * @param llmChatRequestData the request data holding execution context and options
     * @param body               the raw JSON request body; may be {@code null} if the delegate handles it differently
     * @return a Flux emitting each SSE line as a string
     */
    private Flux<String> getRawStreamResponseFlux(LlmChatRequestData llmChatRequestData, ObjectNode body) {
        return this.toResponseSpec(llmChatRequestData, body)
                .flatMapMany(responseSpec -> responseSpec.bodyToFlux(String.class));
    }

    /**
     * Prepares a reactive {@link ResponseSpec} for executing the HTTP request.
     * <p>
     * The method performs token certification checks via the delegate, loads the
     * {@link RequestBodySpec} (including authentication headers), optionally
     * attaches the request body, and finalizes the spec with a status‑error
     * handler from {@link LlmProviderUtil#handleClientResponseError}.
     *
     * @param llmChatRequestData the request data; must not be {@code null}
     * @param body               the raw JSON body node; may be {@code null}
     * @return a Mono of the configured {@code ResponseSpec}
     */
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

    /**
     * Generates the raw request body (JSON) for the given request data.
     * <p>
     * The delegate initializes a base body structure (e.g. a JSON object with
     * model, messages, parameters). If the request data contains a
     * {@link BiConsumer} raw‑request customizer, it is applied after the base
     * initialization, allowing the client to inject additional provider‑specific
     * fields.
     *
     * @param llmChatRequestData the request data with execution context and options; must not be {@code null}
     * @return a Mono emitting the finalized JSON body node
     */
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