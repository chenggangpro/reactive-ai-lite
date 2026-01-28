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
package pro.chenggang.project.reactive.ai.lite.client.deepseek.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.ChatCompletionMessage;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.DeepseekChatRequest;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.DeepseekChatRequest.DeepseekChatRequestBuilder;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.DeepseekChatRequest.Thinking;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.FunctionTool;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.FunctionTool.Function;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.ResponseFormat;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.ResponseFormat.Type;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.UriTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmRequestData;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StructuredResponse;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.AssistantTextMessage;
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
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser.StreamChunk;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.DeepseekChatRequest.StreamOptions.INCLUDE_USAGE;
import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * The default OpenAI chat provider implementation.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Slf4j
public class DeepseekChatProvider extends AbstractLlmChatProvider {

    private final String baseUrL;
    private final String chatCompletionEndpoint;
    private final WebClient webClient;


    @Builder
    private DeepseekChatProvider(@NonNull WebClient.Builder webClientBuilder,
                                 @NonNull String baseUrL,
                                 @NonNull String chatCompletionEndpoint,
                                 boolean isDefault,
                                 @NonNull String name,
                                 Set<String> supportedModels,
                                 @NonNull List<TokenCertification> certifications) {
        super(certifications, (certificationMap) -> DeepseekLlmProviderInfo.builder()
                .isDefault(isDefault)
                .name(name)
                .supportedModels(supportedModels)
                .profiles(certificationMap.keySet())
                .baseUrl(baseUrL)
                .endpoint(chatCompletionEndpoint)
                .build()
        );
        this.baseUrL = baseUrL;
        this.chatCompletionEndpoint = chatCompletionEndpoint;
        this.webClient = webClientBuilder.baseUrl(baseUrL).build();
    }

    @Override
    protected RequestBodySpec loadRequestBodySpec(@NonNull LlmRequestData llmRequestData) {
        throw new UnsupportedOperationException("This method is not supported for OpenAI chat provider.");
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
        JsonNode usageNode = rawResponseData.at("/usage");
        if (!usageNode.isMissingNode() && usageNode.isObject()) {
            return new StreamChunk[]{StreamChunk.builder()
                    .responseDataType(ResponseDataType.USAGE)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode finishNode = rawResponseData.at("/choices/0/finish_reason");
        if (!finishNode.isMissingNode() && !finishNode.isNull()) {
            return new StreamChunk[]{StreamChunk.builder()
                    .responseDataType(ResponseDataType.FINISHED)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode deltaNode = rawResponseData.at("/choices/0/delta");
        if (deltaNode.isMissingNode() || !deltaNode.isObject()) {
            log.debug("Missing or invalid delta node in raw response data : {}", rawResponseData);
            return new StreamChunk[]{StreamChunk.builder()
                    .responseDataType(ResponseDataType.UNKNOWN)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        if (deltaNode.has("role")) {
            return new StreamChunk[]{StreamChunk.builder()
                    .responseDataType(ResponseDataType.ROLE)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode reasoningContentNode = deltaNode.at("/reasoning_content");
        if (!reasoningContentNode.isMissingNode() && reasoningContentNode.isTextual() && !reasoningContentNode.isNull()) {
            return new StreamChunk[]{StreamChunk.builder()
                    .responseDataType(ResponseDataType.REASONING_CONTENT)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode contentNode = deltaNode.at("/content");
        if (!contentNode.isMissingNode() && contentNode.isTextual() && !contentNode.isNull()) {
            return new StreamChunk[]{StreamChunk.builder()
                    .responseDataType(ResponseDataType.ANSWER_CONTENT)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode toolCallsNode = deltaNode.at("/tool_calls");
        if (!toolCallsNode.isMissingNode() && toolCallsNode.isArray()) {
            return new StreamChunk[]{StreamChunk.builder()
                    .responseDataType(ResponseDataType.TOOL_CALL)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        log.debug("Unrecognized delta node in raw response data : {}", rawResponseData);
        return new StreamChunk[]{StreamChunk.builder()
                .responseDataType(ResponseDataType.UNKNOWN)
                .dataContent(rawResponseData)
                .build()
        };
    }

    @Override
    protected ObjectNode mergeRawToolCallMessages(@NonNull List<ObjectNode> rawToolCallMessages, boolean distinctToolCalls) {
        List<ObjectNode> toolCalls = new ArrayList<>();
        for (ObjectNode rawToolCallMessage : rawToolCallMessages) {
            JsonNode toolCallNode = rawToolCallMessage.at("/choices/0/delta/tool_calls/0");
            if (toolCallNode.isMissingNode() || !toolCallNode.isObject() || toolCallNode.isNull()) {
                continue;
            }
            JsonNode indexNode = toolCallNode.at("/index");
            if (indexNode.isMissingNode() || indexNode.isNull() || !indexNode.isInt()) {
                continue;
            }
            int index = indexNode.asInt();
            if (index < 0) {
                continue;
            }
            ObjectNode toolCall = (ObjectNode) toolCallNode;
            if (toolCalls.size() == index) {
                toolCalls.add(toolCall);
                continue;
            }
            ObjectNode existToolCall = toolCalls.get(index);
            if (toolCall.has("id")) {
                this.mergeJsonNode(existToolCall, toolCall, "id");
            } else if (toolCall.has("type")) {
                this.mergeJsonNode(existToolCall, toolCall, "type");
            } else if (toolCall.has("function")) {
                JsonNode functionNode = toolCall.at("/function");
                JsonNode existFunctionNode = existToolCall.at("/function");
                if (existFunctionNode.isMissingNode() || existFunctionNode.isNull()) {
                    existToolCall.set("function", functionNode);
                } else if (functionNode.isObject() && !functionNode.isNull()) {
                    ObjectNode newFunctionNode = (ObjectNode) functionNode;
                    if (newFunctionNode.has("name")) {
                        this.mergeJsonNode((ObjectNode) existFunctionNode, newFunctionNode, "name");
                    } else if (newFunctionNode.has("arguments")) {
                        this.mergeJsonNode((ObjectNode) existFunctionNode, newFunctionNode, "arguments");
                    }
                }
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

    private void mergeJsonNode(ObjectNode existNode, ObjectNode newNode, String fieldName) {
        JsonNode newValueNode = newNode.at("/" + fieldName);
        if (newValueNode.isMissingNode()) {
            return;
        }
        if (newValueNode.isNull()) {
            return;
        }
        if (!newValueNode.isValueNode()) {
            return;
        }
        String newValue = newValueNode.asText();
        if (existNode.has(fieldName)) {
            String existValue = existNode.get(fieldName).asText();
            existNode.put(fieldName, existValue + newValue);
            return;
        }
        existNode.put(fieldName, newValue);
    }

    @Override
    protected Mono<GeneralResponse> extraGeneralResponse(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse) {
        ObjectNode rawResponseBody = rawResponse.getRawResponse();
        return Mono.just(GeneralResponse.builder()
                        .rawResponse(rawResponseBody)
                        .contextView(rawResponse.getContextView())
                        .rawRequestMessages(rawResponse.getRawRequestMessages())
                )
                .map(builder -> {
                    JsonNode messageNode = rawResponseBody.at("/choices/0/message");
                    if (!messageNode.isMissingNode() && messageNode.isObject()) {
                        builder.responseMessage((ObjectNode) messageNode);
                    }
                    JsonNode contentNode = messageNode.at("/content");
                    if (!contentNode.isMissingNode() && contentNode.isTextual()) {
                        builder.answerContent(contentNode.asText());
                    }
                    JsonNode reasoningContentNode = messageNode.at("/reasoning_content");
                    if (!reasoningContentNode.isMissingNode() && reasoningContentNode.isTextual()) {
                        builder.reasoningContent(reasoningContentNode.asText());
                    }
                    JsonNode toolCallsNode = messageNode.at("/tool_calls");
                    if (!toolCallsNode.isMissingNode() && toolCallsNode.isArray()) {
                        ArrayNode toolCallsArrayNode = (ArrayNode) toolCallsNode;
                        List<LlmToolCallRequest> llmToolCallRequests = this.parseToolCallRequestList(toolDefinitions, toolCallsArrayNode);
                        builder.toolCallList(llmToolCallRequests);
                    }
                    JsonNode usageNode = rawResponseBody.at("/usage");
                    if (!usageNode.isMissingNode() && usageNode.isObject() && !usageNode.isNull()) {
                        builder.usages((ObjectNode) usageNode);
                    }
                    return builder.build();
                });
    }

    private List<LlmToolCallRequest> parseToolCallRequestList(@NonNull List<ToolDefinition> toolDefinitions, @NonNull ArrayNode toolCallsArrayNode) {
        Map<String, ToolDefinition> toolDefinitionMapByIdentifier = toolDefinitions.stream()
                .collect(Collectors.toMap(
                        ToolDefinition::identifier,
                        java.util.function.Function.identity(),
                        (o1, o2) -> o1,
                        HashMap::new
                ));
        return toolCallsArrayNode.valueStream()
                .filter(jsonNode -> jsonNode.isObject() && !jsonNode.isNull() && jsonNode.has("function") && jsonNode.get("function").isObject())
                .map(jsonNode -> {
                    ObjectNode toolCallObjectNode = (ObjectNode) jsonNode;
                    ObjectNode functionNode = (ObjectNode) toolCallObjectNode.get("function");
                    String arguments = functionNode.get("arguments").toString();
                    JsonNode args = null;
                    if (StringUtils.hasText(arguments)) {
                        try {
                            args = OBJECT_MAPPER.readTree(arguments);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    String toolIdentifier = functionNode.get("name").asText();
                    if (Objects.isNull(toolIdentifier)) {
                        throw new IllegalStateException("The tool name which in tool-calling response is missing.");
                    }
                    if (!toolDefinitionMapByIdentifier.containsKey(toolIdentifier)) {
                        throw new IllegalStateException("The tool identifier of tool-calling response '" + toolIdentifier + "' is not found in the tool definitions.");
                    }
                    ToolDefinition toolDefinition = toolDefinitionMapByIdentifier.get(toolIdentifier);
                    return LlmToolCallRequest.builder()
                            .identifier(toolIdentifier)
                            .id(toolCallObjectNode.get("id").asText())
                            .type(toolCallObjectNode.get("type").asText())
                            .name(toolDefinition.name())
                            .rawArgs(arguments)
                            .args(args)
                            .build();
                })
                .toList();
    }

    @Override
    protected <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse, @NonNull Class<R> resultType) {
        ObjectNode rawResponseBody = rawResponse.getRawResponse();
        return Mono.just(StructuredResponse.<R>builder()
                        .rawResponse(rawResponseBody)
                        .contextView(rawResponse.getContextView())
                        .rawRequestMessages(rawResponse.getRawRequestMessages())
                )
                .map(builder -> {
                    JsonNode messageNode = rawResponseBody.at("/choices/0/message");
                    if (!messageNode.isMissingNode() && messageNode.isObject()) {
                        builder.responseMessage((ObjectNode) messageNode);
                    }
                    JsonNode reasoningContentNode = messageNode.at("/reasoning_content");
                    if (!reasoningContentNode.isMissingNode() && reasoningContentNode.isTextual()) {
                        builder.reasoningContent(reasoningContentNode.asText());
                    }
                    JsonNode toolCallsNode = messageNode.at("/tool_calls");
                    if (!toolCallsNode.isMissingNode() && toolCallsNode.isArray()) {
                        ArrayNode toolCallsArrayNode = (ArrayNode) toolCallsNode;
                        List<LlmToolCallRequest> llmToolCallRequests = this.parseToolCallRequestList(toolDefinitions, toolCallsArrayNode);
                        builder.toolCallList(llmToolCallRequests);
                    }
                    JsonNode usageNode = rawResponseBody.at("/usage");
                    if (!usageNode.isMissingNode() && usageNode.isObject() && !usageNode.isNull()) {
                        builder.usages((ObjectNode) usageNode);
                    }
                    return builder;
                })
                .handle((builder, sink) -> {
                    JsonNode contentNode = rawResponseBody.at("/choices/0/message/content");
                    if (contentNode.isMissingNode() || contentNode.isNull()) {
                        sink.next(builder.build());
                        sink.complete();
                        return;
                    }
                    if (contentNode.isTextual() && StringUtils.hasText(contentNode.asText())) {
                        R value;
                        try {
                            value = OBJECT_MAPPER.readValue(contentNode.asText(), resultType);
                        } catch (Exception e) {
                            log.error("Failed to parse content as {}: {}", resultType, contentNode.asText(), e);
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
                            value = OBJECT_MAPPER.treeToValue(contentNode, resultType);
                        } catch (Exception e) {
                            log.error("Failed to parse content as {}: {}", resultType, contentNode, e);
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
    protected <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull List<ToolDefinition> toolDefinitions,
                                                                               @NonNull RawResponse rawResponse,
                                                                               @NonNull ParameterizedTypeReference<R> resultType) {
        ObjectNode rawResponseBody = rawResponse.getRawResponse();
        return Mono.just(StructuredResponse.<R>builder()
                        .rawResponse(rawResponseBody)
                        .contextView(rawResponse.getContextView())
                        .rawRequestMessages(rawResponse.getRawRequestMessages())
                )
                .map(builder -> {
                    JsonNode messageNode = rawResponseBody.at("/choices/0/message");
                    if (!messageNode.isMissingNode() && messageNode.isObject()) {
                        builder.responseMessage((ObjectNode) messageNode);
                    }
                    JsonNode reasoningContentNode = messageNode.at("/reasoning_content");
                    if (!reasoningContentNode.isMissingNode() && reasoningContentNode.isTextual()) {
                        builder.reasoningContent(reasoningContentNode.asText());
                    }
                    JsonNode toolCallsNode = messageNode.at("/tool_calls");
                    if (!toolCallsNode.isMissingNode() && toolCallsNode.isArray()) {
                        ArrayNode toolCallsArrayNode = (ArrayNode) toolCallsNode;
                        List<LlmToolCallRequest> llmToolCallRequests = this.parseToolCallRequestList(toolDefinitions, toolCallsArrayNode);
                        builder.toolCallList(llmToolCallRequests);
                    }
                    JsonNode usageNode = rawResponseBody.at("/usage");
                    if (!usageNode.isMissingNode() && usageNode.isObject() && !usageNode.isNull()) {
                        builder.usages((ObjectNode) usageNode);
                    }
                    return builder;
                })
                .handle((builder, sink) -> {
                    JsonNode contentNode = rawResponseBody.at("/choices/0/message/content");
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
    protected Mono<StreamResponse> extractStreamResponseContent(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawStreamResponse rawStreamResponse) {
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
            JsonNode deltaNode = dataContent.at("/choices/0/delta");
            if (deltaNode.isMissingNode() || deltaNode.isNull()) {
                return Mono.empty();
            }
            JsonNode contentNode = dataContent.at("/choices/0/delta/content");
            if (contentNode.isMissingNode() || contentNode.isNull() || !contentNode.isTextual()) {
                return Mono.empty();
            }
            return Mono.just(StreamResponse.builder()
                    .dataType(StreamDataType.ANSWER_CONTENT)
                    .dataContent(deltaNode)
                    .messageContent(contentNode.asText())
                    .contextView(rawStreamResponse.getContextView())
                    .build()
            );
        }
        if (ResponseDataType.REASONING_CONTENT.equals(responseDataType)) {
            ObjectNode dataContent = (ObjectNode) rawStreamResponse.getDataContent();
            JsonNode deltaNode = dataContent.at("/choices/0/delta");
            if (deltaNode.isMissingNode() || deltaNode.isNull()) {
                return Mono.empty();
            }
            JsonNode reasoningContentNode = dataContent.at("/choices/0/delta/reasoning_content");
            if (reasoningContentNode.isMissingNode() || reasoningContentNode.isNull() || !reasoningContentNode.isTextual()) {
                return Mono.empty();
            }
            return Mono.just(StreamResponse.builder()
                    .dataType(StreamDataType.ANSWER_CONTENT)
                    .dataContent(deltaNode)
                    .messageContent(reasoningContentNode.asText())
                    .contextView(rawStreamResponse.getContextView())
                    .build()
            );
        }
        if (ResponseDataType.USAGE.equals(responseDataType)) {
            ObjectNode dataContent = (ObjectNode) rawStreamResponse.getDataContent();
            JsonNode usageNode = dataContent.at("/usage");
            if (usageNode.isMissingNode() || usageNode.isNull()) {
                return Mono.empty();
            }
            return Mono.just(StreamResponse.builder()
                    .dataType(StreamDataType.USAGE)
                    .dataContent(usageNode)
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
            List<LlmToolCallRequest> llmToolCallRequests = this.parseToolCallRequestList(toolDefinitions, (ArrayNode) toolCallsNode);
            return Mono.just(StreamResponse.builder()
                    .dataType(StreamDataType.TOOL_CALL)
                    .dataContent(toolCallsNode)
                    .toolCallList(llmToolCallRequests)
                    .contextView(rawStreamResponse.getContextView())
                    .build()
            );
        }
        log.warn("Unsupported response data type: {}", responseDataType);
        return Mono.empty();
    }

    @Override
    protected RequestBodySpec initializeRequestBodySpec(@NonNull LlmRequestData llmRequestData) {
        AtomicBoolean certificationSet = new AtomicBoolean(false);
        RequestBodyUriSpec requestBodyUriSpec = this.webClient.post();
        Optional<TokenCertification> optionalTokenCertification = llmRequestData.getTokenCertification();
        if (optionalTokenCertification.isEmpty()) {
            throw new IllegalStateException("At least one token certification is required for the chat completion request.");
        }
        TokenCertification tokenCertification = optionalTokenCertification.get();
        RequestBodySpec requestBodySpec = requestBodyUriSpec.uri(uriBuilder -> {
            uriBuilder.path(this.chatCompletionEndpoint);
            if (tokenCertification instanceof UriTokenCertification uriTokenCertification) {
                uriTokenCertification.applyTo(uriBuilder);
                certificationSet.set(true);
            }
            return uriBuilder.build();
        });
        requestBodySpec.contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, "reactive-ai-lite")
                .acceptCharset(StandardCharsets.UTF_8);
        if (llmRequestData.isStream()) {
            requestBodySpec.accept(MediaType.TEXT_EVENT_STREAM);
        } else {
            requestBodySpec.accept(MediaType.APPLICATION_JSON);
        }
        if (tokenCertification instanceof BearerTokenCertification bearerTokenCertification) {
            if (!certificationSet.get()) {
                requestBodySpec.headers(bearerTokenCertification::applyTo);
                certificationSet.set(true);
            }
        }
        if (!certificationSet.get()) {
            log.warn("No token certification be applied, cause of the unknown TokenCertification : {}", tokenCertification);
        }
        return requestBodySpec;
    }

    @Override
    public String toString() {
        return "DeepseekChatProvider{" +
                "llmProviderInfo=" + llmProviderInfo +
                ", baseUrL='" + baseUrL + '\'' +
                ", chatCompletionEndpoint='" + chatCompletionEndpoint + '\'' +
                ", certification=" + certificationMap.size() +
                '}';
    }

    protected DeepseekChatRequest buildRequest(LlmRequestData llmRequestData) {
        DeepseekChatRequestBuilder deepseekChatRequestBuilder = DeepseekChatRequest.builder()
                .model(llmRequestData.getModelName());
        if (llmRequestData.getResponseJsonSchema().isEmpty() && llmRequestData.getStructuredOutputType().isEmpty()) {
            deepseekChatRequestBuilder.responseFormat(ResponseFormat.builder()
                    .type(Type.TEXT)
                    .build()
            );
        } else {
            deepseekChatRequestBuilder.responseFormat(ResponseFormat.builder()
                    .type(Type.JSON_OBJECT)
                    .build()
            );
        }
        llmRequestData.getTemperature().ifPresent(deepseekChatRequestBuilder::temperature);
        llmRequestData.getTopP().ifPresent(deepseekChatRequestBuilder::topP);
        if (llmRequestData.isStream() && llmRequestData.isIncludeUsage()) {
            deepseekChatRequestBuilder.streamOptions(INCLUDE_USAGE);
        }
        llmRequestData.getReasoning().ifPresent(reasoning -> {
            if ("true".equalsIgnoreCase(reasoning) || "enabled".equalsIgnoreCase(reasoning) || "1".equalsIgnoreCase(reasoning)) {
                deepseekChatRequestBuilder.thinking(Thinking.ENABLED);
            } else if ("false".equalsIgnoreCase(reasoning) || "disabled".equalsIgnoreCase(reasoning) || "0".equalsIgnoreCase(reasoning)) {
                deepseekChatRequestBuilder.thinking(Thinking.DISABLED);
            } else {
                log.warn("Invalid reasoning value: {}", reasoning);
            }
        });
        llmRequestData.getMaxCompletionTokens().ifPresent(deepseekChatRequestBuilder::maxTokens);
        llmRequestData.getToolChoice().ifPresent(deepseekChatRequestBuilder::toolChoice);
        var functionTools = buildFunctionTools(llmRequestData);
        var systemMessage = buildSystemMessage(llmRequestData);
        var userMessage = buildUserMessage(llmRequestData);
        var historicalMessages = buildHistoricalMessages(llmRequestData);
        var latestAssistantMessages = this.buildLatestAssistantMessages(llmRequestData);
        var toolMessages = buildToolMessages(llmRequestData);
        var allMessages = Stream.of(Stream.of(systemMessage),
                        historicalMessages.stream(),
                        latestAssistantMessages.stream(),
                        toolMessages.stream(),
                        toolMessages.isEmpty() ? Stream.of(userMessage) : Stream.<ChatCompletionMessage>empty()
                )
                .flatMap(java.util.function.Function.identity())
                .toList();
        return deepseekChatRequestBuilder
                .stream(llmRequestData.isStream())
                .messages(allMessages)
                .tools(functionTools)
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
                                .name(toolDefinition.identifier())
                                .description(toolDefinition.description())
                                .parameters(JsonRelatedUtil.jsonToMap(toolDefinition.inputSchema()))
                                .strict(toolDefinition.strict())
                                .build())
                        .build()
                )
                .toList();
    }

    protected Optional<ChatCompletionMessage> buildLatestAssistantMessages(LlmRequestData llmRequestData) {
        return llmRequestData.getLatestAssistantMessage()
                .map(latestAssistantMessage -> {
                    try {
                        return OBJECT_MAPPER.treeToValue(latestAssistantMessage, ChatCompletionMessage.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    protected List<ChatCompletionMessage> buildToolMessages(LlmRequestData llmRequestData) {
        List<LlmToolCallResponse> llmToolCallResponses = llmRequestData.getLlmToolCallResponse();
        if (llmToolCallResponses.isEmpty()) {
            return List.of();
        }
        return llmToolCallResponses.stream()
                .map(llmToolCallResponse -> {
                    return ChatCompletionMessage.builder()
                            .role(Role.TOOL)
                            .toolCallId(llmToolCallResponse.getId())
                            .rawContent(llmToolCallResponse.getContent())
                            .build();
                })
                .toList();
    }

    protected ChatCompletionMessage buildSystemMessage(LlmRequestData llmRequestData) {
        return ChatCompletionMessage.builder()
                .role(Role.SYSTEM)
                .rawContent(llmRequestData.getSystemMessage().text())
                .build();
    }

    protected List<ChatCompletionMessage> buildHistoricalMessages(LlmRequestData llmRequestData) {
        return llmRequestData.getHistoricalMessages()
                .stream()
                .flatMap(message -> {
                    if (message instanceof AssistantTextMessage assistantTextMessage) {
                        return Stream.of(
                                ChatCompletionMessage.builder()
                                        .role(Role.ASSISTANT)
                                        .rawContent(assistantTextMessage.text())
                                        .reasoningContent(assistantTextMessage.getReasoningContent())
                                        .build()
                        );
                    }
                    if (message instanceof ToolResponseMessage toolResponseMessage) {
                        return toolResponseMessage.getLlmToolCallResponses()
                                .stream()
                                .map(llmToolCallResponse -> {
                                    return ChatCompletionMessage.builder()
                                            .role(Role.TOOL)
                                            .toolCallId(llmToolCallResponse.getId())
                                            .rawContent(llmToolCallResponse.getContent())
                                            .build();
                                });
                    }
                    if (message instanceof MediaMessage mediaMessage) {
                        log.warn("Media message is not supported, only text messages are supported. Skipping media message: {}", mediaMessage);
                    }
                    return Stream.of(ChatCompletionMessage.builder()
                            .role(Role.USER)
                            .rawContent(message.text())
                            .build()
                    );
                })
                .toList();
    }

    protected ChatCompletionMessage buildUserMessage(LlmRequestData llmRequestData) {
        Optional<MediaMessage> optionalMediaMessage = llmRequestData.getUserMediaMessage();
        if (optionalMediaMessage.isPresent()) {
            MediaMessage mediaMessage = optionalMediaMessage.get();
            log.warn("Media message is not supported, only text messages are supported. Skipping media message: {}", mediaMessage);
        }
        return ChatCompletionMessage.builder()
                .role(Role.USER)
                .rawContent(llmRequestData.getUserTextMessage().text())
                .build();
    }

}

