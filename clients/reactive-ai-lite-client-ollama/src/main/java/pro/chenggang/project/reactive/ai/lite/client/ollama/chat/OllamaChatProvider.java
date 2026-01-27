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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.FunctionTool;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.FunctionTool.Function;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaChatMessage;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaChatRequest;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmRequestData;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StructuredResponse;
import pro.chenggang.project.reactive.ai.lite.core.message.Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.AssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.Base64Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.ToolResponseMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.ResponseDataType;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.AbstractLlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.tool.LlmToolCallRequest;
import pro.chenggang.project.reactive.ai.lite.core.tool.LlmToolCallResponse;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonSchemaUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser.StreamChunk;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
                                 @NonNull List<TokenCertification> certifications) {
        super(certifications, (certificationMap) -> OllamaLlmProviderInfo.builder()
                .isDefault(isDefault)
                .name(name)
                .supportedModels(supportedModels)
                .profiles(certificationMap.keySet())
                .build()
        );
        this.baseUrL = baseUrL;
        this.chatCompletionEndpoint = chatCompletionEndpoint;
        this.webClient = webClientBuilder.baseUrl(baseUrL).build();
    }

    @Override
    protected RequestBodySpec loadRequestBodySpec(@NonNull LlmRequestData llmRequestData) {
        return this.webClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.path(this.chatCompletionEndpoint);
                    return uriBuilder.build();
                });
    }

    @Override
    protected ObjectNode initializeRequestBody(@NonNull LlmRequestData llmRequestData) {
        return OBJECT_MAPPER.valueToTree(this.buildRequest(llmRequestData));
    }

    @Override
    protected ArrayNode extractRequestMessages(@NonNull ObjectNode requestBody) {
        return (ArrayNode) requestBody.get("messages");
    }

    @Override
    protected StreamChunk[] extractStreamChunks(@NonNull ObjectNode rawResponseData) {
        JsonNode messageNode = rawResponseData.at("/message");
        if (messageNode.isMissingNode() || !messageNode.isObject()) {
            log.debug("Missing or invalid message node in raw response data : {}", rawResponseData);
            return new StreamChunk[]{StreamChunk.builder()
                    .responseDataType(ResponseDataType.UNKNOWN)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode reasoningContentNode = messageNode.at("/thinking");
        if (!reasoningContentNode.isMissingNode() && reasoningContentNode.isTextual() && !reasoningContentNode.isNull()) {
            return new StreamChunk[]{StreamChunk.builder()
                    .responseDataType(ResponseDataType.REASONING_CONTENT)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode contentNode = messageNode.at("/content");
        if (!contentNode.isMissingNode() && contentNode.isTextual() && !contentNode.isNull()) {
            return new StreamChunk[]{StreamChunk.builder()
                    .responseDataType(ResponseDataType.ANSWER_CONTENT)
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
                return new StreamChunk[]{StreamChunk.builder()
                        .responseDataType(ResponseDataType.TOOL_CALL)
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
            StreamChunk toolCallChunk = StreamChunk.builder()
                    .responseDataType(ResponseDataType.TOOL_CALL)
                    .dataContent(toolCallRawData)
                    .build();
            rawResponseData.remove("message");
            if (!anyMatchUsageField) {
                StreamChunk finishedChunk = StreamChunk.builder()
                        .responseDataType(ResponseDataType.FINISHED)
                        .dataContent(rawResponseData)
                        .build();
                return new StreamChunk[]{toolCallChunk, finishedChunk};
            }
            StreamChunk usageChunk = StreamChunk.builder()
                    .responseDataType(ResponseDataType.USAGE)
                    .dataContent(rawResponseData)
                    .build();
            return new StreamChunk[]{toolCallChunk, usageChunk};

        } else if (isDone) {
            boolean anyMatchUsageField = usageJsonField.stream()
                    .anyMatch(fieldName -> {
                        JsonNode fieldNode = rawResponseData.at("/" + fieldName);
                        return !fieldNode.isMissingNode() && fieldNode.isInt();
                    });
            if (anyMatchUsageField) {
                return new StreamChunk[]{StreamChunk.builder()
                        .responseDataType(ResponseDataType.USAGE)
                        .dataContent(rawResponseData)
                        .build()
                };
            }
        }
        log.warn("Unrecognized message node in raw response data : {}", rawResponseData);
        return new StreamChunk[]{StreamChunk.builder()
                .responseDataType(ResponseDataType.UNKNOWN)
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
            Set<ObjectNode> toolCallSet = new HashSet<>();
            if (toolCalls.size() > 1) {
                Iterator<ObjectNode> iterator = toolCalls.iterator();
                while (iterator.hasNext()) {
                    ObjectNode toolCall = iterator.next();
                    ObjectNode copied = toolCall.deepCopy();
                    copied.remove("index");
                    copied.remove("id");
                    if (toolCallSet.contains(copied)) {
                        iterator.remove();
                    } else {
                        toolCallSet.add(copied);
                    }
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
    protected Mono<GeneralResponse> extraGeneralResponse(@NonNull RawResponse rawResponse) {
        ObjectNode rawResponseBody = rawResponse.getRawResponse();
        return Mono.just(GeneralResponse.builder()
                        .rawResponse(rawResponseBody)
                        .contextView(rawResponse.getContextView())
                        .rawRequestMessages(rawResponse.getRawRequestMessages())
                )
                .map(builder -> {
                    JsonNode messageNode = rawResponseBody.at("/message");
                    if (!messageNode.isMissingNode() && messageNode.isObject()) {
                        builder.responseMessage((ObjectNode) messageNode);
                    }
                    JsonNode contentNode = messageNode.at("/content");
                    if (!contentNode.isMissingNode() && contentNode.isTextual()) {
                        builder.answerContent(contentNode.asText());
                    }
                    JsonNode reasoningContentNode = messageNode.at("/thinking");
                    if (!reasoningContentNode.isMissingNode() && reasoningContentNode.isTextual()) {
                        builder.reasoningContent(reasoningContentNode.asText());
                    }
                    JsonNode toolCallsNode = messageNode.at("/tool_calls");
                    if (!toolCallsNode.isMissingNode() && toolCallsNode.isArray()) {
                        ArrayNode toolCallsArrayNode = (ArrayNode) toolCallsNode;
                        List<LlmToolCallRequest> llmToolCallRequests = this.parseToolCallRequestList(toolCallsArrayNode);
                        builder.toolCallList(llmToolCallRequests);
                    }
                    ObjectNode usageNode = OBJECT_MAPPER.createObjectNode();
                    for (String fieldName : usageJsonField) {
                        JsonNode fieldNode = rawResponseBody.at("/" + fieldName);
                        if (!fieldNode.isMissingNode() && fieldNode.isInt()) {
                            usageNode.put(fieldName, fieldNode.asInt());
                        }
                    }
                    if (!usageNode.isEmpty()) {
                        builder.usages(usageNode);
                    }
                    return builder.build();
                });
    }

    private List<LlmToolCallRequest> parseToolCallRequestList(@NonNull ArrayNode toolCallsArrayNode) {
        return toolCallsArrayNode.valueStream()
                .filter(jsonNode -> jsonNode.isObject() && !jsonNode.isNull() && jsonNode.has("function") && jsonNode.get("function").isObject())
                .map(jsonNode -> {
                    ObjectNode functionNode = (ObjectNode) jsonNode.get("function");
                    String arguments = functionNode.get("arguments").toString();
                    JsonNode args = null;
                    if (StringUtils.hasText(arguments)) {
                        try {
                            args = OBJECT_MAPPER.readTree(arguments);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return LlmToolCallRequest.builder()
                            .id(functionNode.get("name").asText())
                            .name(functionNode.get("name").asText())
                            .type("function")
                            .rawArgs(arguments)
                            .args(args)
                            .build();
                })
                .toList();
    }

    @Override
    protected <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull RawResponse rawResponse, @NonNull Class<R> resultType) {
        ObjectNode rawResponseBody = rawResponse.getRawResponse();
        return Mono.just(StructuredResponse.<R>builder()
                        .rawResponse(rawResponseBody)
                        .contextView(rawResponse.getContextView())
                        .rawRequestMessages(rawResponse.getRawRequestMessages())
                )
                .map(builder -> {
                    JsonNode messageNode = rawResponseBody.at("/message");
                    if (!messageNode.isMissingNode() && messageNode.isObject()) {
                        builder.responseMessage((ObjectNode) messageNode);
                    }
                    JsonNode reasoningContentNode = messageNode.at("/thinking");
                    if (!reasoningContentNode.isMissingNode() && reasoningContentNode.isTextual()) {
                        builder.reasoningContent(reasoningContentNode.asText());
                    }
                    JsonNode toolCallsNode = messageNode.at("/tool_calls");
                    if (!toolCallsNode.isMissingNode() && toolCallsNode.isArray()) {
                        ArrayNode toolCallsArrayNode = (ArrayNode) toolCallsNode;
                        List<LlmToolCallRequest> llmToolCallRequests = this.parseToolCallRequestList(toolCallsArrayNode);
                        builder.toolCallList(llmToolCallRequests);
                    }
                    ObjectNode usageNode = OBJECT_MAPPER.createObjectNode();
                    for (String fieldName : usageJsonField) {
                        JsonNode fieldNode = rawResponseBody.at("/" + fieldName);
                        if (!fieldNode.isMissingNode() && fieldNode.isInt()) {
                            usageNode.put(fieldName, fieldNode.asInt());
                        }
                    }
                    if (!usageNode.isEmpty()) {
                        builder.usages(usageNode);
                    }
                    return builder;
                })
                .handle((builder, sink) -> {
                    JsonNode contentNode = rawResponseBody.at("/message/content");
                    if (contentNode.isMissingNode() || contentNode.isNull()) {
                        sink.next(builder.build());
                        sink.complete();
                        return;
                    }
                    if (contentNode.isTextual()) {
                        R value;
                        try {
                            value = OBJECT_MAPPER.readValue(contentNode.asText(), resultType);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to parse content as {}: {}", resultType, contentNode.asText(), e);
                            sink.error(e);
                            return;
                        }
                        sink.next(builder.structuredContent(value).build());
                        sink.complete();
                        return;
                    }
                    if (contentNode.isObject()) {
                        R value;
                        try {
                            value = OBJECT_MAPPER.treeToValue(contentNode, resultType);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to parse content as {}: {}", resultType, contentNode, e);
                            sink.error(e);
                            return;
                        }
                        sink.next(builder.structuredContent(value).build());
                        sink.complete();
                        return;
                    }
                    log.warn("Unsupported json node type: {}", contentNode.getNodeType());
                    sink.complete();
                });
    }

    @Override
    protected <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull RawResponse rawResponse, @NonNull ParameterizedTypeReference<R> resultType) {
        ObjectNode rawResponseBody = rawResponse.getRawResponse();
        return Mono.just(StructuredResponse.<R>builder()
                        .rawResponse(rawResponseBody)
                        .contextView(rawResponse.getContextView())
                        .rawRequestMessages(rawResponse.getRawRequestMessages())
                )
                .map(builder -> {
                    JsonNode messageNode = rawResponseBody.at("/message");
                    if (!messageNode.isMissingNode() && messageNode.isObject()) {
                        builder.responseMessage((ObjectNode) messageNode);
                    }
                    JsonNode reasoningContentNode = messageNode.at("/thinking");
                    if (!reasoningContentNode.isMissingNode() && reasoningContentNode.isTextual()) {
                        builder.reasoningContent(reasoningContentNode.asText());
                    }
                    JsonNode toolCallsNode = messageNode.at("/tool_calls");
                    if (!toolCallsNode.isMissingNode() && toolCallsNode.isArray()) {
                        ArrayNode toolCallsArrayNode = (ArrayNode) toolCallsNode;
                        List<LlmToolCallRequest> llmToolCallRequests = this.parseToolCallRequestList(toolCallsArrayNode);
                        builder.toolCallList(llmToolCallRequests);
                    }
                    ObjectNode usageNode = OBJECT_MAPPER.createObjectNode();
                    for (String fieldName : usageJsonField) {
                        JsonNode fieldNode = rawResponseBody.at("/" + fieldName);
                        if (!fieldNode.isMissingNode() && fieldNode.isInt()) {
                            usageNode.put(fieldName, fieldNode.asInt());
                        }
                    }
                    if (!usageNode.isEmpty()) {
                        builder.usages(usageNode);
                    }
                    return builder;
                })
                .handle((builder, sink) -> {
                    JsonNode contentNode = rawResponseBody.at("/message/content");
                    if (contentNode.isMissingNode() || contentNode.isNull()) {
                        sink.next(builder.build());
                        sink.complete();
                        return;
                    }
                    if (contentNode.isTextual() && StringUtils.hasText(contentNode.asText())) {
                        R value;
                        String content = contentNode.asText();
                        try {
                            value = OBJECT_MAPPER.readValue(content, new TypeReference<R>() {
                                        @Override
                                        public java.lang.reflect.Type getType() {
                                            return resultType.getType();
                                        }
                                    }
                            );
                        } catch (Exception e) {
                            log.error("Failed to parse content as {}: {}", resultType.getType(), content, e);
                            sink.error(e);
                            return;
                        }
                        sink.next(builder.structuredContent(value).build());
                        sink.complete();
                        return;
                    }
                    if (contentNode.isObject() || contentNode.isArray()) {
                        R value;
                        try {
                            value = OBJECT_MAPPER.treeToValue(contentNode, new TypeReference<R>() {
                                        @Override
                                        public java.lang.reflect.Type getType() {
                                            return resultType.getType();
                                        }
                                    }
                            );
                        } catch (Exception e) {
                            log.error("Failed to parse content as {}: {}", resultType.getType(), contentNode.asText(), e);
                            sink.error(e);
                            return;
                        }
                        sink.next(builder.structuredContent(value).build());
                        sink.complete();
                        return;
                    }
                    log.warn("Unable to parse json ( {} ) for type {}", contentNode, resultType);
                    sink.next(builder.build());
                    sink.complete();
                });
    }

    @Override
    protected Mono<StreamResponse> extractStreamResponseContent(@NonNull RawStreamResponse rawStreamResponse) {
        ResponseDataType responseDataType = rawStreamResponse.getDataType();
        if (ResponseDataType.UNKNOWN.equals(responseDataType)) {
            return Mono.empty();
        }
        if (ResponseDataType.FINISHED.equals(responseDataType)) {
            return Mono.empty();
        }
        if (ResponseDataType.ROLE.equals(responseDataType)) {
            return Mono.empty();
        }
        if (ResponseDataType.REQUEST_MESSAGE.equals(responseDataType)) {
            return Mono.just(StreamResponse.builder()
                    .dataType(StreamDataType.REQUEST_MESSAGE)
                    .dataContent(rawStreamResponse.getDataContent())
                    .contextView(rawStreamResponse.getContextView())
                    .build()
            );
        }
        if (ResponseDataType.ANSWER_CONTENT.equals(responseDataType)) {
            ObjectNode dataContent = (ObjectNode) rawStreamResponse.getDataContent();
            JsonNode contentNode = dataContent.at("/message/content");
            if (contentNode.isMissingNode() || contentNode.isNull()) {
                return Mono.empty();
            }
            return Mono.just(StreamResponse.builder()
                    .dataType(StreamDataType.ANSWER_CONTENT)
                    .dataContent(contentNode)
                    .messageContent(contentNode.asText())
                    .contextView(rawStreamResponse.getContextView())
                    .build()
            );
        }
        if (ResponseDataType.REASONING_CONTENT.equals(responseDataType)) {
            ObjectNode dataContent = (ObjectNode) rawStreamResponse.getDataContent();
            JsonNode thinkingNode = dataContent.at("/message/thinking");
            if (thinkingNode.isMissingNode() || thinkingNode.isNull()) {
                return Mono.empty();
            }
            return Mono.just(StreamResponse.builder()
                    .dataType(StreamDataType.ANSWER_CONTENT)
                    .dataContent(thinkingNode)
                    .messageContent(thinkingNode.asText())
                    .contextView(rawStreamResponse.getContextView())
                    .build()
            );
        }
        if (ResponseDataType.TOOL_CALL.equals(responseDataType)) {
            JsonNode dataContent = rawStreamResponse.getDataContent();
            JsonNode toolCallsNode = dataContent.at("/tool_calls");
            if (toolCallsNode.isMissingNode() || toolCallsNode.isNull() || !toolCallsNode.isArray()) {
                return Mono.empty();
            }
            List<LlmToolCallRequest> llmToolCallRequests = this.parseToolCallRequestList((ArrayNode) toolCallsNode);
            return Mono.just(StreamResponse.builder()
                    .dataType(StreamDataType.TOOL_CALL)
                    .dataContent(toolCallsNode)
                    .toolCallList(llmToolCallRequests)
                    .contextView(rawStreamResponse.getContextView())
                    .build()
            );
        }
        if (ResponseDataType.USAGE.equals(responseDataType)) {
            ObjectNode dataContent = (ObjectNode) rawStreamResponse.getDataContent();
            return Mono.just(StreamResponse.builder()
                    .dataType(StreamDataType.USAGE)
                    .dataContent(dataContent)
                    .contextView(rawStreamResponse.getContextView())
                    .build()
            );
        }
        log.warn("Unsupported response data type: {}", responseDataType);
        return Mono.empty();
    }

    protected OllamaChatRequest buildRequest(LlmRequestData llmRequestData) {
        var ollamaChatRequestBuilder = OllamaChatRequest.builder()
                .model(llmRequestData.getModelName());
        if (llmRequestData.getResponseJsonSchema().isEmpty() && llmRequestData.getStructuredOutputType().isEmpty()) {
            ollamaChatRequestBuilder.format("json");
        } else {
            Map<String, Object> jsonSchemaMap = Map.of();
            if (llmRequestData.getResponseJsonSchema().isPresent()) {
                jsonSchemaMap = JsonRelatedUtil.jsonToMap(llmRequestData.getResponseJsonSchema().get());
            } else if (llmRequestData.getStructuredOutputType().isPresent()) {
                var structuredOutputType = llmRequestData.getStructuredOutputType().get();
                jsonSchemaMap = JsonRelatedUtil.jsonToMap(JsonSchemaUtil.generateForType(structuredOutputType));
            }
            ollamaChatRequestBuilder.format(jsonSchemaMap);
        }
        Map<String, Object> options = new HashMap<>();
        llmRequestData.getTemperature().ifPresent(temperature -> options.put("temperature", temperature));
        llmRequestData.getTopP().ifPresent(topP -> options.put("top_p", topP));
        llmRequestData.getReasoning().ifPresent(think -> {
            if ("true".equalsIgnoreCase(think) || "false".equalsIgnoreCase(think)) {
                ollamaChatRequestBuilder.think(Boolean.parseBoolean(think));
                return;
            }
            ollamaChatRequestBuilder.think(think);
        });
        var systemMessage = buildSystemMessage(llmRequestData);
        var userMessage = buildUserMessage(llmRequestData);
        var historicalMessages = buildHistoricalMessages(llmRequestData);
        var latestAssistantMessages = this.buildLatestAssistantMessages(llmRequestData);
        var toolMessages = buildToolMessages(llmRequestData);
        var allMessages = Stream.of(Stream.of(systemMessage),
                        historicalMessages.stream(),
                        latestAssistantMessages.stream(),
                        toolMessages.stream(),
                        toolMessages.isEmpty() ? Stream.of(userMessage) : Stream.<OllamaChatMessage>empty()
                )
                .flatMap(java.util.function.Function.identity())
                .toList();
        return ollamaChatRequestBuilder.options(options)
                .stream(llmRequestData.isStream())
                .tools(this.buildFunctionTools(llmRequestData))
                .messages(allMessages)
                .build();
    }

    protected List<FunctionTool> buildFunctionTools(LlmRequestData llmRequestData) {
        List<ToolDefinition> toolDefinitions = llmRequestData.getToolDefinitions();
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

    protected Optional<OllamaChatMessage> buildLatestAssistantMessages(LlmRequestData llmRequestData) {
        return llmRequestData.getLatestAssistantMessage()
                .map(latestAssistantMessage -> {
                    try {
                        return OBJECT_MAPPER.treeToValue(latestAssistantMessage, OllamaChatMessage.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    protected List<OllamaChatMessage> buildToolMessages(LlmRequestData llmRequestData) {
        List<LlmToolCallResponse> llmToolCallResponses = llmRequestData.getLlmToolCallResponse();
        if (llmToolCallResponses.isEmpty()) {
            return List.of();
        }
        return llmToolCallResponses.stream()
                .map(llmToolCallResponse -> {
                    return OllamaChatMessage.builder()
                            .role(Role.TOOL)
                            .toolName(llmToolCallResponse.getId())
                            .content(llmToolCallResponse.getContent())
                            .build();
                })
                .toList();
    }

    protected OllamaChatMessage buildSystemMessage(LlmRequestData llmRequestData) {
        return OllamaChatMessage.builder()
                .role(Role.SYSTEM)
                .content(llmRequestData.getSystemMessage().text())
                .build();
    }

    protected List<OllamaChatMessage> buildHistoricalMessages(LlmRequestData llmRequestData) {
        return llmRequestData.getHistoricalMessages()
                .stream()
                .flatMap(message -> {
                    if (message instanceof AssistantTextMessage assistantTextMessage) {
                        return Stream.of(OllamaChatMessage.builder()
                                .role(Role.ASSISTANT)
                                .content(assistantTextMessage.text())
                                .thinking(assistantTextMessage.getReasoningContent())
                                .build());
                    }
                    if (message instanceof ToolResponseMessage toolResponseMessage) {
                        return toolResponseMessage.getLlmToolCallResponses()
                                .stream()
                                .map(llmToolCallResponse -> {
                                    return OllamaChatMessage.builder()
                                            .role(Role.TOOL)
                                            .toolName(llmToolCallResponse.getId())
                                            .content(llmToolCallResponse.getContent())
                                            .build();
                                });
                    }
                    if (message instanceof MediaMessage mediaMessage) {
                        List<Attachment> attachments = mediaMessage.getAttachments();
                        List<String> images = new ArrayList<>();
                        for (Attachment attachment : attachments) {
                            if (attachment instanceof Base64Attachment base64Attachment) {
                                images.add(base64Attachment.content());
                            } else {
                                log.warn("Unsupported attachment type: {}, MimeType: {}", attachment.name(), attachment.mimeType());
                            }
                        }
                        return Stream.of(OllamaChatMessage.builder()
                                .role(Role.USER)
                                .content(mediaMessage.text())
                                .images(images.isEmpty() ? null : images)
                                .build());
                    }
                    return Stream.of(OllamaChatMessage.builder()
                            .role(Role.USER)
                            .content(message.text())
                            .build()
                    );
                })
                .toList();
    }

    protected OllamaChatMessage buildUserMessage(LlmRequestData llmRequestData) {
        Optional<MediaMessage> optionalMediaMessage = llmRequestData.getUserMediaMessage();
        if (optionalMediaMessage.isPresent()) {
            MediaMessage mediaMessage = optionalMediaMessage.get();
            List<Attachment> attachments = mediaMessage.getAttachments();
            List<String> images = new ArrayList<>();
            for (Attachment attachment : attachments) {
                if (attachment instanceof Base64Attachment base64Attachment) {
                    images.add(base64Attachment.content());
                } else {
                    log.warn("Unsupported attachment type: {}, MimeType: {}", attachment.name(), attachment.mimeType());
                }
            }
            return OllamaChatMessage.builder()
                    .role(Role.USER)
                    .content(mediaMessage.text())
                    .images(images.isEmpty() ? null : images)
                    .build();
        }
        return OllamaChatMessage.builder()
                .role(Role.USER)
                .content(llmRequestData.getUserTextMessage().text())
                .build();
    }
}
