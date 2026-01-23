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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmRequestData;
import pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StructuredResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.TextMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.ExecutionType;
import pro.chenggang.project.reactive.ai.lite.core.option.ResponseDataType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.tool.LlmToolCallResponse;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;
import pro.chenggang.project.reactive.ai.lite.core.util.LlmProviderUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static pro.chenggang.project.reactive.ai.lite.core.message.Message.EMPTY_MESSAGE;

/**
 * The common implementation of the LlmChatProvider interface.
 * This class provides default implementations for methods that are required by the interface.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public abstract class AbstractLlmChatProvider implements LlmChatProvider {

    protected static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;
    protected final Map<String, TokenCertification> certificationMap = new HashMap<>();
    protected final TokenCertification defaultCertification;
    protected final LlmProviderInfo llmProviderInfo;

    protected AbstractLlmChatProvider(@NonNull List<TokenCertification> certifications,
                                      @NonNull Function<Map<String, TokenCertification>, LlmProviderInfo> llmProviderInfoInitializer) {
        certifications.forEach(cert -> certificationMap.put(cert.profile(), cert));
        this.llmProviderInfo = llmProviderInfoInitializer.apply(this.certificationMap);
        this.defaultCertification = certifications.stream()
                .filter(TokenCertification::isDefault)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("At least one default TokenCertification is required  for " + this.llmProviderInfo));
    }

    protected abstract RequestBodySpec initializeRequestBodySpec(@NonNull LlmRequestData llmRequestData);

    protected abstract ObjectNode initializeRequestBody(@NonNull LlmRequestData llmRequestData);

    protected abstract ArrayNode extractRequestMessages(@NonNull ObjectNode requestBody);

    protected abstract ResponseDataType extractStreamDataType(@NonNull ObjectNode rawResponseData);

    protected abstract ObjectNode mergeRawToolCallMessages(@NonNull List<ObjectNode> rawToolCallMessages);

    protected abstract Mono<GeneralResponse> extraGeneralResponse(@NonNull RawResponse rawResponse);

    protected abstract <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull RawResponse rawResponse, @NonNull Class<R> resultType);

    protected abstract <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull RawResponse rawResponse, @NonNull ParameterizedTypeReference<R> resultType);

    protected abstract Mono<StreamResponse> extractStreamResponseContent(@NonNull RawStreamResponse rawStreamResponse);

    @Override
    public LlmProviderInfo info() {
        return this.llmProviderInfo;
    }

    @Override
    public Mono<GeneralResponse> executeGeneral(@NonNull ExecutionInfo executionInfo) {
        return this.executeGeneralRaw(executionInfo)
                .flatMap(this::extraGeneralResponse);
    }

    @Override
    public Mono<RawResponse> executeGeneralRaw(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestExchange(executionInfo, false, null, null)
                .flatMap((LlmRequestData llmRequestData) -> executeInternalRaw(llmRequestData, ExecutionType.GENERAL));
    }

    @Override
    public Flux<StreamResponse> executeStream(@NonNull ExecutionInfo executionInfo) {
        return this.executeStreamRaw(executionInfo)
                .flatMap(this::extractStreamResponseContent);
    }

    @Override
    public Flux<RawStreamResponse> executeStreamRaw(@NonNull ExecutionInfo executionInfo) {
        return this.initializeLlmRequestExchange(executionInfo, true, null, null)
                .flatMapMany(llmRequestData -> Mono.fromCallable(() -> this.initializeRequestBody(llmRequestData))
                        .flatMapMany(body -> {
                                    return Flux.deferContextual(contextView -> {
                                        return StreamResponseParser.parseStreamResponse(
                                                        this.extractRequestMessages(body),
                                                        executionInfo.getExecutionContext().getContextView(),
                                                        this.getrRawStreamResponseFlux(llmRequestData, body),
                                                        this::extractStreamDataType,
                                                        this::mergeRawToolCallMessages
                                                )
                                                .concatMap(rawStreamResponse -> {
                                                    return Mono.justOrEmpty(executionInfo.getRawStreamResponseConsumer())
                                                            .flatMap(consumer -> Mono
                                                                    .fromRunnable(() -> consumer.accept(executionInfo.getExecutionContext().getContextView(), rawStreamResponse))
                                                            )
                                                            .thenReturn(rawStreamResponse);
                                                })
                                                .contextWrite(contextView);
                                    });
                                }
                        )
                );
    }

    private Flux<String> getrRawStreamResponseFlux(LlmRequestData llmRequestData, ObjectNode body) {
        return this.toResponseSpec(llmRequestData, body)
                .flatMapMany(responseSpec -> responseSpec.bodyToFlux(String.class))
                .takeUntil(SSE_DONE_PREDICATE)
                .filter(SSE_DONE_PREDICATE.negate());
    }


    @Override
    public <R> Mono<StructuredResponse<R>> executeStructured(@NonNull ExecutionInfo executionInfo, @NonNull Class<R> resultType) {
        return this.executeStructuredRaw(executionInfo, resultType)
                .flatMap(rawContent -> this.extractStructuredResponseContent(rawContent, resultType));
    }

    @Override
    public <R> Mono<StructuredResponse<R>> executeStructured(@NonNull ExecutionInfo executionInfo, @NonNull ParameterizedTypeReference<R> resultType) {
        return this.executeStructuredRaw(executionInfo, resultType)
                .flatMap(rawContent -> this.extractStructuredResponseContent(rawContent, resultType));
    }

    @Override
    public Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull String responseJsonSchema) {
        return this.initializeLlmRequestExchange(executionInfo, false, null, responseJsonSchema)
                .flatMap((LlmRequestData llmRequestData) -> executeInternalRaw(llmRequestData, ExecutionType.STRUCTURED));
    }

    @Override
    public <R> Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull Class<R> resultType) {
        return this.initializeLlmRequestExchange(executionInfo, false, resultType, null)
                .flatMap((LlmRequestData llmRequestData) -> executeInternalRaw(llmRequestData, ExecutionType.STRUCTURED));
    }

    @Override
    public <R> Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull ParameterizedTypeReference<R> resultType) {
        return this.initializeLlmRequestExchange(executionInfo, false, resultType.getType(), null)
                .flatMap((LlmRequestData llmRequestData) -> executeInternalRaw(llmRequestData, ExecutionType.STRUCTURED));
    }

    protected Mono<RawResponse> executeInternalRaw(@NonNull LlmRequestData llmRequestData, @NonNull ExecutionType executionType) {
        return Mono.fromCallable(() -> this.initializeRequestBody(llmRequestData))
                .flatMap(body -> {
                    return this.toResponseSpec(llmRequestData, body)
                            .flatMap(responseSpec -> responseSpec.bodyToMono(new ParameterizedTypeReference<ObjectNode>() {}))
                            .map(rawJsonResponse -> RawResponse.builder()
                                    .contextView(llmRequestData.getExecutionContextView())
                                    .rawRequestMessages(this.extractRequestMessages(body))
                                    .rawResponse(rawJsonResponse)
                                    .build()
                            )
                            .flatMap(rawResponse -> {
                                return Mono.justOrEmpty(llmRequestData.getRawResponseConsumer())
                                        .flatMap(consumer -> Mono.
                                                <Void>fromRunnable(() -> consumer.accept(llmRequestData.getExecutionContextView(), rawResponse)
                                        ))
                                        .thenReturn(rawResponse);
                            });
                });
    }

    private Mono<LlmRequestData> initializeLlmRequestExchange(@NonNull ExecutionInfo executionInfo, boolean isStream, Type structuredOutputType, String responseJsonSchema) {
        return Mono.fromCallable(() -> LlmRequestData.builder()
                .llmProviderInfo(this.llmProviderInfo)
                .modelName(this.loadModelName(executionInfo))
                .tokenCertification(this.loadTokenCertification(executionInfo))
                .executionContextView(executionInfo.getExecutionContext().getContextView())
                .systemMessage(this.loadSystemMessage(executionInfo))
                .userTextMessage(this.loadUserMessage(executionInfo))
                .userMediaMessage(this.loadMediaMessage(executionInfo))
                .historicalMessages(this.loadHistoricalMessage(executionInfo))
                .latestAssistantMessage(this.loadLatestAssistantMessage(executionInfo))
                .temperature(this.loadTemperature(executionInfo))
                .topP(this.loadTopP(executionInfo))
                .maxCompletionTokens(this.loadMaxCompletionTokens(executionInfo))
                .extraData(this.loadExtraData(executionInfo))
                .toolDefinitions(this.loadToolDefinitions(executionInfo))
                .llmToolCallResponse(this.loadToolResponse(executionInfo))
                .rawRequestCustomizer(executionInfo.getRawRequestCustomizer())
                .isStream(isStream)
                .toolChoice(this.loadToolChoice(executionInfo))
                .structuredOutputType(structuredOutputType)
                .responseJsonSchema(responseJsonSchema)
                .build()
        );
    }

    protected TokenCertification loadTokenCertification(@NonNull ExecutionInfo executionInfo) {
        if (executionInfo.isDefaultProfile()) {
            return this.defaultCertification;
        }
        ExecutionContextView executionContextView = executionInfo.getExecutionContext().getContextView();
        String pickedProfile = executionInfo.getProfilePicker().apply(executionContextView, this.llmProviderInfo.profiles());
        if (Objects.isNull(pickedProfile)) {
            throw new NoProfileFoundLlmClientException(this.llmProviderInfo);
        }
        if (!this.certificationMap.containsKey(pickedProfile)) {
            throw new NoProfileFoundLlmClientException(this.llmProviderInfo, pickedProfile);
        }
        return certificationMap.get(pickedProfile);
    }

    protected String loadModelName(@NonNull ExecutionInfo executionInfo) {
        String modelName = executionInfo.getModelNameConfigure().apply(executionInfo.getExecutionContext().getContextView());
        if (!StringUtils.hasText(modelName)) {
            throw new IllegalArgumentException("Model name cannot be null or empty");
        }
        return modelName;
    }

    protected TextMessage loadSystemMessage(@NonNull ExecutionInfo executionInfo) {
        Function<ExecutionContextView, TextMessage> defaultSystemMessageConfigure = executionInfo.getDefaultSystemMessageConfigure();
        Function<ExecutionContextView, TextMessage> systemMessageConfigure = executionInfo.getSystemMessageConfigure();
        TextMessage systemMessage = EMPTY_MESSAGE;
        if (Objects.nonNull(systemMessageConfigure)) {
            systemMessage = systemMessageConfigure.apply(executionInfo.getExecutionContext().getContextView());
        } else if (Objects.nonNull(defaultSystemMessageConfigure)) {
            systemMessage = defaultSystemMessageConfigure.apply(executionInfo.getExecutionContext().getContextView());
        }
        return systemMessage;
    }

    protected List<Message> loadHistoricalMessage(@NonNull ExecutionInfo executionInfo) {
        Function<ExecutionContextView, Collection<Message>> historicalMessageConfigure = executionInfo.getHistoricalMessageConfigure();
        if (Objects.isNull(historicalMessageConfigure)) {
            return List.of();
        }
        Collection<Message> textMessages = historicalMessageConfigure.apply(executionInfo.getExecutionContext().getContextView());
        if (CollectionUtils.isEmpty(textMessages)) {
            return List.of();
        }
        return textMessages.stream().toList();
    }

    protected ObjectNode loadLatestAssistantMessage(@NonNull ExecutionInfo executionInfo) {
        Function<ExecutionContextView, ObjectNode> latestAssistantMessageConfigure = executionInfo.getLatestAssistantMessageConfigure();
        if (Objects.isNull(latestAssistantMessageConfigure)) {
            return null;
        }
        return latestAssistantMessageConfigure.apply(executionInfo.getExecutionContext().getContextView());
    }

    protected TextMessage loadUserMessage(@NonNull ExecutionInfo executionInfo) {
        Function<ExecutionContextView, TextMessage> textMessageConfigure = executionInfo.getTextMessageConfigure();
        TextMessage userMessage = EMPTY_MESSAGE;
        if (Objects.nonNull(textMessageConfigure)) {
            userMessage = textMessageConfigure.apply(executionInfo.getExecutionContext().getContextView());
        }
        return userMessage;
    }

    protected MediaMessage loadMediaMessage(@NonNull ExecutionInfo executionInfo) {
        Function<ExecutionContextView, MediaMessage> mediaMessageConfigure = executionInfo.getMediaMessageConfigure();
        MediaMessage mediaMessage = null;
        if (Objects.nonNull(mediaMessageConfigure)) {
            mediaMessage = mediaMessageConfigure.apply(executionInfo.getExecutionContext().getContextView());
        }
        return mediaMessage;
    }

    protected Double loadTemperature(@NonNull ExecutionInfo executionInfo) {
        Function<ExecutionContextView, Double> temperatureConfigure = executionInfo.getTemperatureConfigure();
        Double temperature = null;
        if (Objects.nonNull(temperatureConfigure)) {
            temperature = temperatureConfigure.apply(executionInfo.getExecutionContext().getContextView());
        }
        return temperature;
    }

    protected Double loadTopP(@NonNull ExecutionInfo executionInfo) {
        Function<ExecutionContextView, Double> topPConfigure = executionInfo.getTopPConfigure();
        Double topP = null;
        if (Objects.nonNull(topPConfigure)) {
            topP = topPConfigure.apply(executionInfo.getExecutionContext().getContextView());
        }
        return topP;
    }

    protected Integer loadMaxCompletionTokens(@NonNull ExecutionInfo executionInfo) {
        Function<ExecutionContextView, Integer> maxCompletionTokensConfigure = executionInfo.getMaxCompletionTokensConfigure();
        Integer maxCompletionTokens = null;
        if (Objects.nonNull(maxCompletionTokensConfigure)) {
            maxCompletionTokens = maxCompletionTokensConfigure.apply(executionInfo.getExecutionContext().getContextView());
        }
        return maxCompletionTokens;
    }

    protected Map<String, Object> loadExtraData(@NonNull ExecutionInfo executionInfo) {
        Function<ExecutionContextView, Map<String, Object>> extraDataConfigure = executionInfo.getExtraDataConfigure();
        Map<String, Object> extraData = null;
        if (Objects.nonNull(extraDataConfigure)) {
            extraData = extraDataConfigure.apply(executionInfo.getExecutionContext().getContextView());
        }
        return extraData;
    }

    protected List<ToolDefinition> loadToolDefinitions(@NonNull ExecutionInfo executionInfo) {
        Function<ExecutionContextView, Collection<ToolDefinition>> toolsConfigure = executionInfo.getToolsConfigure();
        if (Objects.isNull(toolsConfigure)) {
            return List.of();
        }
        Collection<ToolDefinition> toolDefinitions = toolsConfigure.apply(executionInfo.getExecutionContext().getContextView());
        if (Objects.isNull(toolDefinitions) || toolDefinitions.isEmpty()) {
            return List.of();
        }
        return List.copyOf(toolDefinitions);
    }

    protected String loadToolChoice(@NonNull ExecutionInfo executionInfo) {
        Function<ExecutionContextView, String> toolChoiceConfigure = executionInfo.getToolChoiceConfigure();
        if (Objects.isNull(toolChoiceConfigure)) {
            return null;
        }
        return toolChoiceConfigure.apply(executionInfo.getExecutionContext().getContextView());
    }

    protected List<LlmToolCallResponse> loadToolResponse(@NonNull ExecutionInfo executionInfo) {
        Function<ExecutionContextView, Collection<LlmToolCallResponse>> toolsConfigure = executionInfo.getToolsResponseConfigure();
        if (Objects.isNull(toolsConfigure)) {
            return List.of();
        }
        Collection<LlmToolCallResponse> toolCallResponses = toolsConfigure.apply(executionInfo.getExecutionContext().getContextView());
        if (Objects.isNull(toolCallResponses) || toolCallResponses.isEmpty()) {
            return List.of();
        }
        return List.copyOf(toolCallResponses);
    }

    protected Mono<ResponseSpec> toResponseSpec(LlmRequestData llmRequestData, ObjectNode body) {
        return Mono.create(sink -> {
            RequestBodySpec requestBodySpec;
            try {
                requestBodySpec = this.initializeRequestBodySpec(llmRequestData);
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
