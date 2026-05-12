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
package pro.chenggang.project.reactive.ai.lite.client.ollama.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.FunctionTool;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.FunctionTool.Function;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaChatMessage;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaChatMessage.ToolCall;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaChatMessage.ToolCall.ToolCallFunction;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaChatRequest;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.UriTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.entity.usage.Usage;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmChatRequestData;
import pro.chenggang.project.reactive.ai.lite.core.exception.ResponseMessageExtractFailedException;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StructuredResponse;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LLmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.message.AssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.TextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolCallMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolCallMessage.AssistantToolCall;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolCallMessage.AssistantToolCallFunction;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolResultMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Base64Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.chunk.RawStreamDataChunk;
import pro.chenggang.project.reactive.ai.lite.core.message.chunk.TextStreamDataChunk;
import pro.chenggang.project.reactive.ai.lite.core.message.chunk.ToolCallStreamDataChunk;
import pro.chenggang.project.reactive.ai.lite.core.message.chunk.UsageStreamDataChunk;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultAssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultToolCallMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.AbstractLlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonSchemaUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser.JsonStreamChunkSlide;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * The default implementation for Ollama chat provider.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Slf4j
public class OllamaChatProvider extends AbstractLlmChatProvider {

    private final List<String> usageJsonField = List.of(
            "total_duration",
            "load_duration",
            "prompt_eval_count",
            "prompt_eval_duration",
            "eval_count",
            "eval_duration"
    );

    private final String baseUrL;
    private final String chatCompletionEndpoint;
    private final WebClient webClient;

    @Builder
    protected OllamaChatProvider(@NonNull WebClient.Builder webClientBuilder,
                                 @NonNull String baseUrL,
                                 @NonNull String chatCompletionEndpoint,
                                 boolean isDefault,
                                 @NonNull String name,
                                 Set<String> supportedModels,
                                 @NonNull List<TokenCertification> certifications,
                                 @NonNull LLmProviderInterceptorRegistry lLmProviderInterceptorRegistry) {
        super(certifications,
                (certificationMap) -> OllamaLlmProviderInfo.builder()
                        .isDefault(isDefault)
                        .name(name)
                        .supportedModels(supportedModels)
                        .profiles(certificationMap.keySet())
                        .baseUrl(baseUrL)
                        .endpoint(chatCompletionEndpoint)
                        .build(),
                lLmProviderInterceptorRegistry
        );
        this.baseUrL = baseUrL;
        this.chatCompletionEndpoint = chatCompletionEndpoint;
        this.webClient = webClientBuilder.baseUrl(baseUrL).build();
    }

    protected Integer extractPromptTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/prompt_eval_count");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    protected Integer extractCompletionTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/eval_count");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    protected Integer extractOtherTokenUsage(ObjectNode rawUsage) {
        return 0;
    }

    @Override
    protected RequestBodySpec loadRequestBodySpec(@NonNull LlmChatRequestData llmChatRequestData) {
        return this.webClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.path(this.chatCompletionEndpoint);
                    llmChatRequestData.getTokenCertification()
                            .ifPresent(tokenCertification -> {
                                if (tokenCertification instanceof UriTokenCertification uriTokenCertification) {
                                    super.applyCertificationWithUriBuilder(uriBuilder, uriTokenCertification);
                                }
                            });
                    return uriBuilder.build();
                });
    }

    @Override
    protected ObjectNode initializeRequestBody(@NonNull LlmChatRequestData llmChatRequestData) {
        return OBJECT_MAPPER.valueToTree(this.buildRequest(llmChatRequestData));
    }

    @Override
    protected JsonStreamChunkSlide[] extractStreamChunks(@NonNull StreamResponseParser.JsonChunkParsingData jsonChunkParsingData) {
        ObjectNode rawResponseData = jsonChunkParsingData.getData();
        JsonNode messageNode = rawResponseData.at("/message");
        if (messageNode.isMissingNode() || !messageNode.isObject()) {
            log.debug("Missing or invalid message node in raw response data : {}", rawResponseData);
            return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.UNKNOWN)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode reasoningContentNode = messageNode.at("/thinking");
        if (!reasoningContentNode.isMissingNode() && reasoningContentNode.isTextual() && !reasoningContentNode.isNull()) {
            return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.REASONING_CONTENT)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode contentNode = messageNode.at("/content");
        if (!contentNode.isMissingNode() && contentNode.isTextual() && !contentNode.isNull()) {
            return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.ANSWER_CONTENT)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode done = rawResponseData.at("/done");
        JsonNode toolCallsNode = messageNode.at("/tool_calls");
        boolean isToolCall = !toolCallsNode.isMissingNode() && toolCallsNode.isArray();
        boolean isDone = !done.isMissingNode() && done.isBoolean() && done.asBoolean();
        if (!isDone && isToolCall) {
            if (!toolCallsNode.isMissingNode() && toolCallsNode.isArray()) {
                return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.TOOL_CALL)
                        .dataContent(rawResponseData)
                        .build()
                };
            }
        } else if (isDone && isToolCall) {
            boolean anyMatchUsageField = usageJsonField.stream()
                    .anyMatch(fieldName -> {
                        JsonNode fieldNode = rawResponseData.at("/" + fieldName);
                        return !fieldNode.isMissingNode() && fieldNode.isInt();
                    });
            ObjectNode toolCallRawData = rawResponseData.deepCopy();
            for (String fieldName : usageJsonField) {
                JsonNode fieldNode = rawResponseData.at("/" + fieldName);
                if (!fieldNode.isMissingNode()) {
                    toolCallRawData.remove(fieldName);
                }
            }
            JsonStreamChunkSlide toolCallChunk = JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.TOOL_CALL)
                    .dataContent(toolCallRawData)
                    .build();
            rawResponseData.remove("message");
            if (!anyMatchUsageField) {
                JsonStreamChunkSlide finishedChunk = JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.FINISHED)
                        .dataContent(rawResponseData)
                        .build();
                return new JsonStreamChunkSlide[]{toolCallChunk, finishedChunk};
            }
            JsonStreamChunkSlide usageChunk = JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.USAGE)
                    .dataContent(rawResponseData)
                    .build();
            return new JsonStreamChunkSlide[]{toolCallChunk, usageChunk};

        } else if (isDone) {
            boolean anyMatchUsageField = usageJsonField.stream()
                    .anyMatch(fieldName -> {
                        JsonNode fieldNode = rawResponseData.at("/" + fieldName);
                        return !fieldNode.isMissingNode() && fieldNode.isInt();
                    });
            if (anyMatchUsageField) {
                return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.USAGE)
                        .dataContent(rawResponseData)
                        .build()
                };
            }
        }
        log.warn("Unrecognized message node in raw response data : {}", rawResponseData);
        return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                .streamDataType(StreamDataType.UNKNOWN)
                .dataContent(rawResponseData)
                .build()
        };
    }

    @Override
    protected ObjectNode mergeRawToolCallMessages(@NonNull List<ObjectNode> rawToolCallMessages, boolean distinctToolCalls) {
        if (rawToolCallMessages.size() == 1) {
            return rawToolCallMessages.get(0);
        }
        List<ObjectNode> toolCalls = new ArrayList<>();
        for (ObjectNode rawToolCallMessage : rawToolCallMessages) {
            JsonNode toolCallsNode = rawToolCallMessage.at("/message/tool_calls");
            if (toolCallsNode.isMissingNode() || !toolCallsNode.isArray() || toolCallsNode.isNull()) {
                continue;
            }
            for (JsonNode eachToolCalls : toolCallsNode) {
                if (Objects.isNull(eachToolCalls) || eachToolCalls.isMissingNode() || !eachToolCalls.isObject() || eachToolCalls.isNull()) {
                    continue;
                }
                toolCalls.add((ObjectNode) eachToolCalls);
            }
        }
        if (distinctToolCalls) {
            Set<String> toolCallSet = new HashSet<>();
            if (toolCalls.size() > 1) {
                Iterator<ObjectNode> iterator = toolCalls.iterator();
                while (iterator.hasNext()) {
                    ObjectNode toolCall = iterator.next();
                    String toolCallName = toolCall.at("/function/name").textValue();
                    if (toolCallSet.contains(toolCallName)) {
                        iterator.remove();
                        continue;
                    }
                    toolCallSet.add(toolCallName);
                }
            }
            toolCallSet.clear();
        }
        ArrayNode toolCallsArrayNode = OBJECT_MAPPER.createArrayNode();
        for (ObjectNode toolCall : toolCalls) {
            toolCallsArrayNode.add(toolCall);
        }
        ObjectNode toolCallsObject = OBJECT_MAPPER.createObjectNode();
        toolCallsObject.set("tool_calls", toolCallsArrayNode);
        return toolCallsObject;
    }

    @Override
    protected Mono<GeneralResponse> extraGeneralResponse(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse) {
        return Mono.fromCallable(rawResponse::getResponseBody)
                .handle((rawResponseBody, syncSink) -> {
                    var generalResponseBuilder = GeneralResponse.builder()
                            .contextView(rawResponse.getContextView());
                    JsonNode messageNode = rawResponseBody.at("/message");
                    if (messageNode.isMissingNode() || !messageNode.isObject()) {
                        log.error("Failed to extract response message from response body. Response body: {}", rawResponseBody.toPrettyString());
                        syncSink.error(new ResponseMessageExtractFailedException(rawResponseBody));
                        return;
                    }
                    ObjectNode usageNode = OBJECT_MAPPER.createObjectNode();
                    for (String fieldName : usageJsonField) {
                        JsonNode fieldNode = rawResponseBody.at("/" + fieldName);
                        if (!fieldNode.isMissingNode() && fieldNode.isInt()) {
                            usageNode.put(fieldName, fieldNode.asInt());
                        }
                    }
                    if (!usageNode.isEmpty()) {
                        Usage usage = Usage.newUsageBuilder(usageNode)
                                .promptTokensExtractor(this::extractPromptTokenUsage)
                                .completionTokensExtractor(this::extractCompletionTokenUsage)
                                .otherTokensExtractor(this::extractOtherTokenUsage)
                                .build();
                        generalResponseBuilder.usage(usage);
                    }
                    String answerContent = null;
                    JsonNode contentNode = messageNode.at("/content");
                    if (!contentNode.isMissingNode() && contentNode.isTextual()) {
                        answerContent = contentNode.asText();
                    }
                    String reasoningContent = null;
                    JsonNode reasoningContentNode = messageNode.at("/thinking");
                    if (!reasoningContentNode.isMissingNode() && reasoningContentNode.isTextual()) {
                        reasoningContent = reasoningContentNode.asText();
                    }
                    JsonNode toolCallsNode = messageNode.at("/tool_calls");
                    if (!toolCallsNode.isMissingNode() && toolCallsNode.isArray()) {
                        ArrayNode toolCallsArrayNode = (ArrayNode) toolCallsNode;
                        List<AssistantToolCall> toolCalls = null;
                        try {
                            toolCalls = this.parseToolCallList(toolDefinitions, toolCallsArrayNode);
                        } catch (Exception e) {
                            syncSink.error(e);
                            return;
                        }
                        ToolCallMessage toolCallMessage = DefaultToolCallMessage.builder()
                                .toolCalls(toolCalls)
                                .content(answerContent)
                                .reasoningContent(reasoningContent)
                                .build();
                        GeneralResponse generalResponse = generalResponseBuilder.assistantTextMessage(toolCallMessage)
                                .build();
                        syncSink.next(generalResponse);
                        return;
                    }
                    AssistantTextMessage assistantTextMessage = DefaultAssistantTextMessage.builder()
                            .content(answerContent)
                            .reasoningContent(reasoningContent)
                            .build();
                    GeneralResponse generalResponse = generalResponseBuilder.assistantTextMessage(assistantTextMessage)
                            .build();
                    syncSink.next(generalResponse);
                });
    }

    private List<AssistantToolCall> parseToolCallList(@NonNull List<ToolDefinition> toolDefinitions, @NonNull ArrayNode toolCallsArrayNode) {
        Map<String, ToolDefinition> toolDefinitionMapByIdentifier = toolDefinitions.stream()
                .collect(Collectors.toMap(
                        ToolDefinition::name,
                        java.util.function.Function.identity(),
                        (o1, o2) -> o1,
                        HashMap::new
                ));
        int index = 0;
        List<AssistantToolCall> toolCallList = new ArrayList<>();
        for (JsonNode jsonNode : toolCallsArrayNode) {
            if (!jsonNode.isObject() || jsonNode.isNull() || !jsonNode.has("function") || !jsonNode.get("function").isObject()) {
                continue;
            }
            ObjectNode toolCallObjectNode = (ObjectNode) jsonNode;
            ObjectNode functionNode = (ObjectNode) toolCallObjectNode.get("function");
            String arguments = functionNode.get("arguments").toString();
            String toolName = functionNode.get("name").asText();
            if (!toolDefinitionMapByIdentifier.containsKey(toolName)) {
                throw new IllegalStateException("The tool identifier of tool-calling response '" + toolName + "' is not found in the tool definitions.");
            }
            AssistantToolCallFunction toolCallFunction = AssistantToolCallFunction.builder()
                    .name(toolName)
                    .arguments(arguments)
                    .build();
            ToolDefinition toolDefinition = toolDefinitionMapByIdentifier.get(toolName);
            AssistantToolCall toolCall = AssistantToolCall.builder()
                    .index(index++)
                    .id(toolCallObjectNode.get("id").asText())
                    .type("function")
                    .toolDefinition(toolDefinition)
                    .function(toolCallFunction)
                    .build();
            toolCallList.add(toolCall);
        }
        return List.copyOf(toolCallList);
    }

    @Override
    protected <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse, @NonNull Class<R> resultType) {
        return this.extractStructuredResponseContentInternal(
                toolDefinitions,
                rawResponse,
                content -> {
                    try {
                        return OBJECT_MAPPER.readValue(content, resultType);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                },
                contentNode -> {
                    try {
                        return OBJECT_MAPPER.treeToValue(contentNode, resultType);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @Override
    protected <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull List<ToolDefinition> toolDefinitions,
                                                                               @NonNull RawResponse rawResponse,
                                                                               @NonNull ParameterizedTypeReference<R> resultType) {
        return this.extractStructuredResponseContentInternal(toolDefinitions,
                rawResponse,
                content -> {
                    try {
                        return OBJECT_MAPPER.readValue(content, new TypeReference<R>() {
                                    @Override
                                    public java.lang.reflect.Type getType() {
                                        return resultType.getType();
                                    }
                                }
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                },
                contentNode -> {
                    try {
                        return OBJECT_MAPPER.treeToValue(contentNode, new TypeReference<R>() {
                                    @Override
                                    public java.lang.reflect.Type getType() {
                                        return resultType.getType();
                                    }
                                }
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    protected <R> Mono<StructuredResponse<R>> extractStructuredResponseContentInternal(@NonNull List<ToolDefinition> toolDefinitions,
                                                                                       @NonNull RawResponse rawResponse,
                                                                                       @NonNull java.util.function.Function<String, R> textValueConverter,
                                                                                       @NonNull java.util.function.Function<JsonNode, R> jsonValueConverter) {
        return Mono.fromCallable(rawResponse::getResponseBody)
                .handle((rawResponseBody, syncSink) -> {
                    var structuredResponseBuilder = StructuredResponse.<R>builder()
                            .contextView(rawResponse.getContextView());
                    JsonNode messageNode = rawResponseBody.at("/message");
                    if (messageNode.isMissingNode() || !messageNode.isObject()) {
                        log.error("Failed to extract response message from response body. Response body: {}", rawResponseBody.toPrettyString());
                        syncSink.error(new ResponseMessageExtractFailedException(rawResponseBody));
                        return;
                    }
                    ObjectNode usageNode = OBJECT_MAPPER.createObjectNode();
                    for (String fieldName : usageJsonField) {
                        JsonNode fieldNode = rawResponseBody.at("/" + fieldName);
                        if (!fieldNode.isMissingNode() && fieldNode.isInt()) {
                            usageNode.put(fieldName, fieldNode.asInt());
                        }
                    }
                    if (!usageNode.isEmpty()) {
                        Usage usage = Usage.newUsageBuilder((ObjectNode) usageNode)
                                .promptTokensExtractor(this::extractPromptTokenUsage)
                                .completionTokensExtractor(this::extractCompletionTokenUsage)
                                .otherTokensExtractor(this::extractOtherTokenUsage)
                                .build();
                        structuredResponseBuilder.usage(usage);
                    }
                    String answerContent = null;
                    R structuredValue = null;
                    JsonNode contentNode = messageNode.at("/content");
                    if (!contentNode.isMissingNode() && contentNode.isTextual()) {
                        answerContent = contentNode.asText();
                        if (StringUtils.hasText(answerContent)) {
                            try {
                                structuredValue = textValueConverter.apply(answerContent);
                            } catch (Exception e) {
                                log.error("Failed to parse content : {}", answerContent, e);
                                syncSink.error(e);
                                return;
                            }
                        }
                    } else if (contentNode.isObject() || contentNode.isArray()) {
                        try {
                            answerContent = contentNode.toString();
                            structuredValue = jsonValueConverter.apply(contentNode);
                        } catch (Exception e) {
                            log.error("Failed to parse content: {}", contentNode, e);
                            syncSink.error(e);
                            return;
                        }
                    }
                    structuredResponseBuilder.structuredContent(structuredValue);
                    String reasoningContent = null;
                    JsonNode reasoningContentNode = messageNode.at("/thinking");
                    if (!reasoningContentNode.isMissingNode() && reasoningContentNode.isTextual()) {
                        reasoningContent = reasoningContentNode.asText();
                    }
                    JsonNode toolCallsNode = messageNode.at("/tool_calls");
                    if (!toolCallsNode.isMissingNode() && toolCallsNode.isArray()) {
                        ArrayNode toolCallsArrayNode = (ArrayNode) toolCallsNode;
                        List<AssistantToolCall> toolCalls = null;
                        try {
                            toolCalls = this.parseToolCallList(toolDefinitions, toolCallsArrayNode);
                        } catch (Exception e) {
                            syncSink.error(e);
                            return;
                        }
                        ToolCallMessage toolCallMessage = DefaultToolCallMessage.builder()
                                .toolCalls(toolCalls)
                                .content(answerContent)
                                .reasoningContent(reasoningContent)
                                .build();
                        StructuredResponse<R> structuredResponse = structuredResponseBuilder.assistantTextMessage(toolCallMessage)
                                .build();
                        syncSink.next(structuredResponse);
                        return;
                    }
                    AssistantTextMessage assistantTextMessage = DefaultAssistantTextMessage.builder()
                            .content(answerContent)
                            .reasoningContent(reasoningContent)
                            .build();
                    StructuredResponse<R> structuredResponse = structuredResponseBuilder.assistantTextMessage(assistantTextMessage)
                            .build();
                    syncSink.next(structuredResponse);
                });
    }

    @Override
    protected Publisher<StreamResponse> extractStreamResponseContent(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawStreamResponse rawStreamResponse) {
        StreamDataType streamDataType = rawStreamResponse.getDataType();
        ObjectNode dataContent = rawStreamResponse.getDataContent();
        ExecutionContextView contextView = rawStreamResponse.getContextView();
        if (StreamDataType.UNKNOWN.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                RawStreamDataChunk rawStreamDataChunk = RawStreamDataChunk.builder()
                        .value(dataContent)
                        .build();
                return StreamResponse.builder()
                        .contextView(contextView)
                        .dataChunk(rawStreamDataChunk)
                        .build();
            });
        }
        if (StreamDataType.ROLE.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                var chunkBuilder = TextStreamDataChunk.builder()
                        .dataType(StreamDataType.ROLE);
                JsonNode roleNode = dataContent.at("/message/role");
                if (!roleNode.isMissingNode() && roleNode.isTextual()) {
                    chunkBuilder.value(roleNode.asText());
                }
                TextStreamDataChunk textStreamDataChunk = chunkBuilder.build();
                return StreamResponse.builder()
                        .contextView(contextView)
                        .dataChunk(textStreamDataChunk)
                        .build();
            });
        }
        if (StreamDataType.ANSWER_CONTENT.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                var chunkBuilder = TextStreamDataChunk.builder()
                        .dataType(StreamDataType.ANSWER_CONTENT);
                JsonNode contentNode = dataContent.at("/message/content");
                if (!contentNode.isMissingNode() && !contentNode.isNull() && contentNode.isTextual()) {
                    chunkBuilder.value(contentNode.asText());
                }
                TextStreamDataChunk textStreamDataChunk = chunkBuilder.build();
                return StreamResponse.builder()
                        .contextView(contextView)
                        .dataChunk(textStreamDataChunk)
                        .build();
            });
        }
        if (StreamDataType.REASONING_CONTENT.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                var chunkBuilder = TextStreamDataChunk.builder()
                        .dataType(StreamDataType.REASONING_CONTENT);
                JsonNode contentNode = dataContent.at("/message/thinking");
                if (!contentNode.isMissingNode() && !contentNode.isNull() && contentNode.isTextual()) {
                    chunkBuilder.value(contentNode.asText());
                }
                TextStreamDataChunk textStreamDataChunk = chunkBuilder.build();
                return StreamResponse.builder()
                        .contextView(contextView)
                        .dataChunk(textStreamDataChunk)
                        .build();
            });
        }
        if (StreamDataType.TOOL_CALL.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                var chunkBuilder = ToolCallStreamDataChunk.builder();
                JsonNode toolCallsNode = dataContent.at("/tool_calls");
                if (!toolCallsNode.isMissingNode() && !toolCallsNode.isNull() && toolCallsNode.isArray()) {
                    List<AssistantToolCall> toolCalls = this.parseToolCallList(toolDefinitions, (ArrayNode) toolCallsNode);
                    chunkBuilder.toolCalls(toolCalls);
                }
                ToolCallStreamDataChunk callStreamDataChunk = chunkBuilder.build();
                return StreamResponse.builder()
                        .contextView(contextView)
                        .dataChunk(callStreamDataChunk)
                        .build();
            });
        }
        if (StreamDataType.USAGE.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                var chunkBuilder = UsageStreamDataChunk.builder();
                if (!dataContent.isMissingNode() && dataContent.isObject() && !dataContent.isEmpty()) {
                    Usage usage = Usage.newUsageBuilder((ObjectNode) dataContent)
                            .promptTokensExtractor(this::extractPromptTokenUsage)
                            .completionTokensExtractor(this::extractCompletionTokenUsage)
                            .otherTokensExtractor(this::extractOtherTokenUsage)
                            .build();
                    chunkBuilder.usage(usage);
                }
                UsageStreamDataChunk usageStreamDataChunk = chunkBuilder.build();
                return StreamResponse.builder()
                        .contextView(contextView)
                        .dataChunk(usageStreamDataChunk)
                        .build();
            });
        }
        if (StreamDataType.FINISHED.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                var chunkBuilder = TextStreamDataChunk.builder()
                        .dataType(StreamDataType.FINISHED);
                JsonNode finishReasonNode = dataContent.at("/done_reason");
                if (!finishReasonNode.isMissingNode() && finishReasonNode.isTextual()) {
                    chunkBuilder.value(finishReasonNode.asText());
                }
                TextStreamDataChunk textStreamDataChunk = chunkBuilder.build();
                return StreamResponse.builder()
                        .contextView(contextView)
                        .dataChunk(textStreamDataChunk)
                        .build();
            });
        }
        log.warn("Unsupported response data type: {}, this raw response slice will be discard", streamDataType);
        return Mono.empty();
    }

    @Override
    protected void checkTokenCertification(@NonNull LlmChatRequestData llmChatRequestData) {
        log.debug("Ollama chat provider can work without api certification");
    }

    @Override
    public String toString() {
        return "OllamaChatProvider{" +
                "llmProviderInfo=" + llmProviderInfo +
                ", baseUrL='" + baseUrL + '\'' +
                ", chatCompletionEndpoint='" + chatCompletionEndpoint + '\'' +
                ", certification=" + certificationMap.size() +
                '}';
    }

    protected OllamaChatRequest buildRequest(LlmChatRequestData llmChatRequestData) {
        var ollamaChatRequestBuilder = OllamaChatRequest.builder()
                .model(llmChatRequestData.getModelName());
        if (llmChatRequestData.getResponseJsonSchema().isEmpty() && llmChatRequestData.getStructuredOutputType().isEmpty()) {
            ollamaChatRequestBuilder.format("json");
        } else {
            Map<String, Object> jsonSchemaMap = Map.of();
            if (llmChatRequestData.getResponseJsonSchema().isPresent()) {
                jsonSchemaMap = JsonRelatedUtil.jsonToMap(llmChatRequestData.getResponseJsonSchema().get());
            } else if (llmChatRequestData.getStructuredOutputType().isPresent()) {
                var structuredOutputType = llmChatRequestData.getStructuredOutputType().get();
                jsonSchemaMap = JsonRelatedUtil.jsonToMap(JsonSchemaUtil.generateForType(structuredOutputType));
            }
            ollamaChatRequestBuilder.format(jsonSchemaMap);
        }
        Map<String, Object> options = new HashMap<>();
        llmChatRequestData.getTemperature().ifPresent(temperature -> options.put("temperature", temperature));
        llmChatRequestData.getTopP().ifPresent(topP -> options.put("top_p", topP));
        llmChatRequestData.getReasoning().ifPresent(think -> {
            if ("true".equalsIgnoreCase(think) || "false".equalsIgnoreCase(think)) {
                ollamaChatRequestBuilder.think(Boolean.parseBoolean(think));
                return;
            }
            ollamaChatRequestBuilder.think(think);
        });
        var functionTools = buildFunctionTools(llmChatRequestData.getToolDefinitions());
        var systemMessage = buildSystemMessage(llmChatRequestData.getSystemMessage());
        var userMessage = llmChatRequestData.getUserMediaMessage()
                .map(mediaMessage -> buildMediaMessage(Role.USER, mediaMessage))
                .orElseGet(() -> this.buildTextMessage(Role.USER, llmChatRequestData.getUserTextMessage()));
        var historicalMessages = buildHistoricalMessages(llmChatRequestData.getHistoricalMessages());
        var toolMessages = buildToolMessages(llmChatRequestData.getToolResultMessages());
        var allMessages = Stream.of(Stream.of(systemMessage),
                        historicalMessages.stream(),
                        toolMessages.stream(),
                        toolMessages.isEmpty() ? Stream.of(userMessage) : Stream.<OllamaChatMessage>empty()
                )
                .flatMap(java.util.function.Function.identity())
                .toList();
        return ollamaChatRequestBuilder.options(options)
                .stream(llmChatRequestData.isStream())
                .tools(functionTools)
                .messages(allMessages)
                .build();
    }

    protected List<FunctionTool> buildFunctionTools(List<ToolDefinition> toolDefinitions) {
        if (toolDefinitions.isEmpty()) {
            return List.of();
        }
        return toolDefinitions.stream()
                .map(toolDefinition -> FunctionTool.builder()
                        .type(FunctionTool.Type.FUNCTION)
                        .function(Function.builder()
                                .name(toolDefinition.name())
                                .description(toolDefinition.description())
                                .parameters(JsonRelatedUtil.jsonToMap(toolDefinition.inputSchema()))
                                .strict(toolDefinition.strict())
                                .build())
                        .build()
                )
                .toList();
    }

    protected List<OllamaChatMessage> buildToolMessages(List<ToolResultMessage> toolResultMessages) {
        if (toolResultMessages.isEmpty()) {
            return List.of();
        }
        return toolResultMessages.stream()
                .map(llmToolCallResponse -> {
                    return OllamaChatMessage.builder()
                            .role(Role.TOOL)
                            .toolName(llmToolCallResponse.toolCallId())
                            .content(llmToolCallResponse.content())
                            .build();
                })
                .toList();
    }

    protected OllamaChatMessage buildSystemMessage(TextMessage textMessage) {
        return OllamaChatMessage.builder()
                .role(Role.SYSTEM)
                .content(textMessage.getContent())
                .build();
    }

    protected List<OllamaChatMessage> buildHistoricalMessages(List<Message> historicalMessages) {
        if (historicalMessages.isEmpty()) {
            return List.of();
        }
        return historicalMessages.stream()
                .map(message -> {
                    if (message instanceof ToolCallMessage toolCallMessage) {
                        List<ToolCall> toolCalls = toolCallMessage.getToolCalls()
                                .stream()
                                .map(assistantToolCall -> {
                                    return ToolCall
                                            .builder()
                                            .function(ToolCallFunction
                                                    .builder()
                                                    .name(assistantToolCall.getFunction().getName())
                                                    .arguments(JsonRelatedUtil.jsonToMap(assistantToolCall.getFunction().getArguments()))
                                                    .build()
                                            )
                                            .build();
                                })
                                .toList();
                        return OllamaChatMessage.builder()
                                .role(Role.ASSISTANT)
                                .content(toolCallMessage.getContent())
                                .thinking(toolCallMessage.getReasoningContent())
                                .toolCalls(toolCalls)
                                .build();
                    }
                    if (message instanceof ToolResultMessage toolResultMessage) {
                        return OllamaChatMessage.builder()
                                .role(Role.TOOL)
                                .toolName(toolResultMessage.toolCallId())
                                .content(toolResultMessage.content())
                                .build();
                    }
                    if (message instanceof MediaMessage mediaMessage) {
                        return this.buildMediaMessage(Role.valueOf(mediaMessage.getRole()), mediaMessage);
                    }
                    if (message instanceof TextMessage textMessage) {
                        return this.buildTextMessage(Role.valueOf(textMessage.getRole()), textMessage);
                    }
                    if (message instanceof AssistantTextMessage assistantTextMessage) {
                        return OllamaChatMessage.builder()
                                .role(Role.ASSISTANT)
                                .content(assistantTextMessage.getContent())
                                .thinking(assistantTextMessage.getReasoningContent())
                                .build();
                    }
                    log.warn("Unhandled historical message: {}", message);
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    protected OllamaChatMessage buildMediaMessage(Role role, MediaMessage mediaMessage) {
        Attachment[] attachments = mediaMessage.getAttachments();
        List<String> images = new ArrayList<>();
        for (Attachment attachment : attachments) {
            if (attachment instanceof Base64Attachment base64Attachment) {
                images.add(base64Attachment.content());
            } else {
                log.warn("Unsupported attachment type: {}, MimeType: {}", attachment.name(), attachment.mimeType());
            }
        }
        return OllamaChatMessage.builder()
                .role(role)
                .content(mediaMessage.getContent())
                .images(images.isEmpty() ? null : images)
                .build();
    }

    protected OllamaChatMessage buildTextMessage(Role role, TextMessage textMessage) {
        return OllamaChatMessage.builder()
                .role(role)
                .content(textMessage.getContent())
                .build();
    }

}
