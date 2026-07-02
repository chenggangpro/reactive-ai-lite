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
package pro.chenggang.project.reactive.ai.lite.client.openai.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import pro.chenggang.project.reactive.ai.lite.client.openai.certification.OrganizationTokenCertification;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.ChatCompletionMessage;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.ChatCompletionMessage.ChatCompletionFunction;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.ChatCompletionMessage.ImageUrl;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.ChatCompletionMessage.InputAudio;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.ChatCompletionMessage.InputAudio.Format;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.ChatCompletionMessage.InputFile;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.ChatCompletionMessage.MediaContent;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.ChatCompletionMessage.ToolCall;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.FunctionTool;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.FunctionTool.Function;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.OpenaiChatRequest;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.ResponseFormat;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.ResponseFormat.JsonSchema;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.ResponseFormat.Type;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.UriTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.usage.Usage;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmChatRequestData;
import pro.chenggang.project.reactive.ai.lite.core.exception.ResponseMessageExtractFailedException;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
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
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.UrlAttachment;
import pro.chenggang.project.reactive.ai.lite.core.message.chunk.RawStreamDataChunk;
import pro.chenggang.project.reactive.ai.lite.core.message.chunk.TextStreamDataChunk;
import pro.chenggang.project.reactive.ai.lite.core.message.chunk.ToolCallStreamDataChunk;
import pro.chenggang.project.reactive.ai.lite.core.message.chunk.UsageStreamDataChunk;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultAssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultToolCallMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.delegate.LlmChatProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonSchemaUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser.JsonStreamChunkSlide;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pro.chenggang.project.reactive.ai.lite.client.openai.dto.OpenaiChatRequest.StreamOptions.INCLUDE_USAGE;
import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * The default OpenAI chat provider implementation.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class OpenaiChatProviderDelegate implements LlmChatProviderDelegate {

    private final LlmProviderInfo llmProviderInfo;

    private final String baseUrL;
    private final String chatCompletionEndpoint;
    private final WebClient webClient;

    @Builder
    private OpenaiChatProviderDelegate(@NonNull WebClient.Builder webClientBuilder,
                               @NonNull String baseUrL,
                               @NonNull String chatCompletionEndpoint,
                               boolean isDefault,
                               @NonNull String name,
                               Set<String> supportedModels,
                               @NonNull List<TokenCertification> certifications) {
        this.baseUrL = baseUrL;
        this.chatCompletionEndpoint = chatCompletionEndpoint;
        this.webClient = webClientBuilder.baseUrl(baseUrL).build();
        this.llmProviderInfo = OpenaiLlmProviderInfo.builder()
                        .isDefault(isDefault)
                        .name(name)
                        .supportedModels(supportedModels)
                        .profiles(certifications.stream().map(TokenCertification::profile).collect(Collectors.toSet()))
                        .baseUrl(baseUrL)
                        .endpoint(chatCompletionEndpoint)
                        .build();
    }

    @Override
    public LlmProviderInfo providerInfo() {
        return this.llmProviderInfo;
    }

    protected Integer extractPromptTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/prompt_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    protected Integer extractCompletionTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/completion_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    protected Integer extractOtherTokenUsage(ObjectNode rawUsage) {
        Integer promptTokenUsage = this.extractPromptTokenUsage(rawUsage);
        Integer completionTokenUsage = this.extractCompletionTokenUsage(rawUsage);
        int totalTokenUsage = 0;
        JsonNode jsonNode = rawUsage.at("/total_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            totalTokenUsage = jsonNode.intValue();
        }
        return totalTokenUsage - promptTokenUsage - completionTokenUsage;
    }



    @Override
    public ObjectNode initializeRequestBody(@NonNull LlmChatRequestData llmChatRequestData) {
        return OBJECT_MAPPER.valueToTree(this.buildRequest(llmChatRequestData));
    }

    @Override
    public JsonStreamChunkSlide[] extractStreamChunks(@NonNull StreamResponseParser.JsonChunkParsingData jsonChunkParsingData) {
        ObjectNode rawResponseData = jsonChunkParsingData.getData();
        JsonNode usageNode = rawResponseData.at("/usage");
        if (!usageNode.isMissingNode() && usageNode.isObject()) {
            return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.USAGE)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode finishNode = rawResponseData.at("/choices/0/finish_reason");
        if (!finishNode.isMissingNode() && !finishNode.isNull()) {
            return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.FINISHED)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode deltaNode = rawResponseData.at("/choices/0/delta");
        if (deltaNode.isMissingNode() || !deltaNode.isObject()) {
            log.debug("Missing or invalid delta node in raw response data : {}", rawResponseData);
            return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.UNKNOWN)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        if (deltaNode.has("role")) {
            return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.ROLE)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode reasoningContentNode = deltaNode.at("/reasoning_content");
        if (!reasoningContentNode.isMissingNode() && reasoningContentNode.isTextual() && !reasoningContentNode.isNull()) {
            return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.REASONING_CONTENT)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode contentNode = deltaNode.at("/content");
        if (!contentNode.isMissingNode() && contentNode.isTextual() && !contentNode.isNull()) {
            return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.ANSWER_CONTENT)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        JsonNode toolCallsNode = deltaNode.at("/tool_calls");
        if (!toolCallsNode.isMissingNode() && toolCallsNode.isArray()) {
            return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.TOOL_CALL)
                    .dataContent(rawResponseData)
                    .build()
            };
        }
        log.debug("Unrecognized delta node in raw response data : {}", rawResponseData);
        return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                .streamDataType(StreamDataType.UNKNOWN)
                .dataContent(rawResponseData)
                .build()
        };
    }

    @Override
    public ObjectNode mergeRawToolCallMessages(@NonNull List<ObjectNode> rawToolCallMessages, boolean distinctToolCalls) {
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
    public Mono<GeneralResponse> extractGeneralResponse(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse) {
        return Mono.fromCallable(rawResponse::getResponseBody)
                .handle((rawResponseBody, syncSink) -> {
                    var generalResponseBuilder = GeneralResponse.builder()
                            .executionContext(rawResponse.getExecutionContext())
                            .rawResponseBody(rawResponseBody);
                    JsonNode messageNode = rawResponseBody.at("/choices/0/message");
                    if (messageNode.isMissingNode() || !messageNode.isObject()) {
                        log.error("Failed to extract response message from response body. Response body: {}", rawResponseBody.toPrettyString());
                        syncSink.error(new ResponseMessageExtractFailedException(rawResponseBody));
                        return;
                    }
                    JsonNode usageNode = rawResponseBody.at("/usage");
                    if (!usageNode.isMissingNode() && usageNode.isObject() && !usageNode.isNull()) {
                        Usage usage = Usage.newUsageBuilder((ObjectNode) usageNode)
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
                    JsonNode reasoningContentNode = messageNode.at("/reasoning_content");
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
                    .type(toolCallObjectNode.get("type").asText())
                    .toolDefinition(toolDefinition)
                    .function(toolCallFunction)
                    .build();
            toolCallList.add(toolCall);
        }
        return List.copyOf(toolCallList);
    }

    @Override
    public Publisher<StreamResponse> extractStreamResponseContent(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawStreamResponse rawStreamResponse) {
        StreamDataType streamDataType = rawStreamResponse.getDataType();
        ObjectNode dataContent = rawStreamResponse.getDataContent();
        if (StreamDataType.UNKNOWN.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                RawStreamDataChunk rawStreamDataChunk = RawStreamDataChunk.builder()
                        .value(dataContent)
                        .build();
                return StreamResponse.builder()
                        .executionContext(rawStreamResponse.getExecutionContext())
                        .dataChunk(rawStreamDataChunk)
                        .build();
            });
        }
        if (StreamDataType.ROLE.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                var chunkBuilder = TextStreamDataChunk.builder()
                        .dataType(StreamDataType.ROLE);
                JsonNode roleNode = dataContent.at("/choices/0/delta/role");
                if (!roleNode.isMissingNode() && roleNode.isTextual()) {
                    chunkBuilder.value(roleNode.asText());
                }
                TextStreamDataChunk textStreamDataChunk = chunkBuilder.build();
                return StreamResponse.builder()
                        .executionContext(rawStreamResponse.getExecutionContext())
                        .dataChunk(textStreamDataChunk)
                        .build();
            });
        }
        if (StreamDataType.ANSWER_CONTENT.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                var chunkBuilder = TextStreamDataChunk.builder()
                        .dataType(StreamDataType.ANSWER_CONTENT);
                JsonNode contentNode = dataContent.at("/choices/0/delta/content");
                if (!contentNode.isMissingNode() && !contentNode.isNull() && contentNode.isTextual()) {
                    chunkBuilder.value(contentNode.asText());
                }
                TextStreamDataChunk textStreamDataChunk = chunkBuilder.build();
                return StreamResponse.builder()
                        .executionContext(rawStreamResponse.getExecutionContext())
                        .dataChunk(textStreamDataChunk)
                        .build();
            });
        }
        if (StreamDataType.REASONING_CONTENT.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                var chunkBuilder = TextStreamDataChunk.builder()
                        .dataType(StreamDataType.REASONING_CONTENT);
                JsonNode contentNode = dataContent.at("/choices/0/delta/reasoning_content");
                if (!contentNode.isMissingNode() && !contentNode.isNull() && contentNode.isTextual()) {
                    chunkBuilder.value(contentNode.asText());
                }
                TextStreamDataChunk textStreamDataChunk = chunkBuilder.build();
                return StreamResponse.builder()
                        .executionContext(rawStreamResponse.getExecutionContext())
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
                        .executionContext(rawStreamResponse.getExecutionContext())
                        .dataChunk(callStreamDataChunk)
                        .build();
            });
        }
        if (StreamDataType.USAGE.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                var chunkBuilder = UsageStreamDataChunk.builder();
                JsonNode usageNode = dataContent.at("/usage");
                if (!usageNode.isMissingNode() && usageNode.isObject() && !usageNode.isNull()) {
                    Usage usage = Usage.newUsageBuilder((ObjectNode) usageNode)
                            .promptTokensExtractor(this::extractPromptTokenUsage)
                            .completionTokensExtractor(this::extractCompletionTokenUsage)
                            .otherTokensExtractor(this::extractOtherTokenUsage)
                            .build();
                    chunkBuilder.usage(usage);
                }
                UsageStreamDataChunk usageStreamDataChunk = chunkBuilder.build();
                return StreamResponse.builder()
                        .executionContext(rawStreamResponse.getExecutionContext())
                        .dataChunk(usageStreamDataChunk)
                        .build();
            });
        }
        if (StreamDataType.FINISHED.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                var chunkBuilder = TextStreamDataChunk.builder()
                        .dataType(StreamDataType.FINISHED);
                JsonNode finishReasonNode = dataContent.at("/choices/0/finish_reason");
                if (!finishReasonNode.isMissingNode() && finishReasonNode.isTextual()) {
                    chunkBuilder.value(finishReasonNode.asText());
                }
                TextStreamDataChunk textStreamDataChunk = chunkBuilder.build();
                return StreamResponse.builder()
                        .executionContext(rawStreamResponse.getExecutionContext())
                        .dataChunk(textStreamDataChunk)
                        .build();
            });
        }
        log.warn("Unsupported response data type: {}, this raw response slice will be discard", streamDataType);
        return Mono.empty();
    }

    @Override
    public RequestBodySpec loadRequestBodySpec(@NonNull LlmChatRequestData llmChatRequestData) {
        AtomicBoolean certificationSet = new AtomicBoolean(false);
        RequestBodyUriSpec requestBodyUriSpec = this.webClient.post();
        Optional<TokenCertification> optionalTokenCertification = llmChatRequestData.getTokenCertification();
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
        if (llmChatRequestData.isStream()) {
            requestBodySpec.accept(MediaType.TEXT_EVENT_STREAM);
        } else {
            requestBodySpec.accept(MediaType.APPLICATION_JSON);
        }
        if (tokenCertification instanceof BearerTokenCertification bearerTokenCertification) {
            if (!certificationSet.get()) {
                requestBodySpec.headers(bearerTokenCertification::applyTo);
                certificationSet.set(true);
            }
        } else if (tokenCertification instanceof OrganizationTokenCertification organizationTokenCertification) {
            if (!certificationSet.get()) {
                requestBodySpec.headers(organizationTokenCertification::applyTo);
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
        return "OpenaiChatProviderDelegate{" +
                "llmProviderInfo=" + llmProviderInfo +
                ", baseUrL='" + baseUrL + '\'' +
                ", chatCompletionEndpoint='" + chatCompletionEndpoint + '\'' +
                '}';
    }

    protected OpenaiChatRequest buildRequest(LlmChatRequestData llmChatRequestData) {
        var openaiChatRequestBuilder = OpenaiChatRequest.builder()
                .model(llmChatRequestData.getModelName());
        if (llmChatRequestData.getResponseJsonSchema().isEmpty() && llmChatRequestData.getStructuredOutputType().isEmpty()) {
            openaiChatRequestBuilder.responseFormat(ResponseFormat.builder()
                    .type(Type.TEXT)
                    .build()
            );
        } else {
            String jsonSchemaName = "custom_json_schema";
            Map<String, Object> jsonSchemaMap = Map.of();
            if (llmChatRequestData.getResponseJsonSchema().isPresent()) {
                jsonSchemaMap = JsonRelatedUtil.jsonToMap(llmChatRequestData.getResponseJsonSchema().get());
            } else if (llmChatRequestData.getStructuredOutputType().isPresent()) {
                var structuredOutputType = llmChatRequestData.getStructuredOutputType().get();
                jsonSchemaName = this.extractTypeName(structuredOutputType);
                jsonSchemaMap = JsonRelatedUtil.jsonToMap(JsonSchemaUtil.generateForType(structuredOutputType));
            }
            if (jsonSchemaName.length() > 64) {
                jsonSchemaName = jsonSchemaName.substring(0, 64);
            }
            openaiChatRequestBuilder.responseFormat(ResponseFormat.builder()
                    .type(Type.JSON_SCHEMA)
                    .jsonSchema(JsonSchema.builder()
                            .name(jsonSchemaName)
                            .schema(jsonSchemaMap)
                            .strict(true)
                            .build()
                    )
                    .build()
            );

        }
        llmChatRequestData.getTemperature().ifPresent(openaiChatRequestBuilder::temperature);
        llmChatRequestData.getTopP().ifPresent(openaiChatRequestBuilder::topP);
        if (llmChatRequestData.isStream() && llmChatRequestData.isIncludeUsage()) {
            openaiChatRequestBuilder.streamOptions(INCLUDE_USAGE);
        }
        llmChatRequestData.getReasoning().ifPresent(openaiChatRequestBuilder::reasoningEffort);
        llmChatRequestData.getMaxCompletionTokens().ifPresent(openaiChatRequestBuilder::maxCompletionTokens);
        llmChatRequestData.getToolChoice().ifPresent(openaiChatRequestBuilder::toolChoice);
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
                        toolMessages.isEmpty() ? Stream.of(userMessage) : Stream.<ChatCompletionMessage>empty()
                )
                .flatMap(java.util.function.Function.identity())
                .toList();
        return openaiChatRequestBuilder
                .stream(llmChatRequestData.isStream())
                .messages(allMessages)
                .tools(functionTools)
                .parallelToolCalls(true)
                .build();
    }

    private String extractTypeName(java.lang.reflect.Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz.getSimpleName();
        }
        String typeName = type.getTypeName();
        if (typeName.contains("<")) {
            StringBuilder stringBuilder = new StringBuilder();
            char[] charArray = typeName.toCharArray();
            List<Character> tempCharacters = new ArrayList<>();
            for (char c : charArray) {
                if ('[' == c || ']' == c) {
                    continue;
                }
                if ('.' == c) {
                    tempCharacters.clear();
                    continue;
                }
                if (',' == c) {
                    tempCharacters.stream().filter(Predicate.not(Character::isSpaceChar)).forEach(stringBuilder::append);
                    if (!tempCharacters.isEmpty()) {
                        stringBuilder.append('_');
                    }
                    tempCharacters.clear();
                    continue;
                }
                if ('<' == c) {
                    tempCharacters.stream().filter(Predicate.not(Character::isSpaceChar)).forEach(stringBuilder::append);
                    if (!tempCharacters.isEmpty()) {
                        stringBuilder.append('_');
                    }
                    tempCharacters.clear();
                    continue;
                }
                if ('>' == c) {
                    tempCharacters.stream().filter(Predicate.not(Character::isSpaceChar)).forEach(stringBuilder::append);
                    tempCharacters.clear();
                    continue;
                }
                tempCharacters.add(c);
            }
            return stringBuilder.toString();
        }
        return typeName.substring(typeName.lastIndexOf(".") + 1);
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

    protected List<ChatCompletionMessage> buildToolMessages(List<ToolResultMessage> toolResultMessages) {
        if (toolResultMessages.isEmpty()) {
            return List.of();
        }
        return toolResultMessages.stream()
                .map(toolResultMessage -> {
                    return ChatCompletionMessage.builder()
                            .role(Role.TOOL)
                            .toolCallId(toolResultMessage.toolCallId())
                            .rawContent(toolResultMessage.content())
                            .build();
                })
                .toList();
    }

    protected ChatCompletionMessage buildSystemMessage(TextMessage textMessage) {
        return ChatCompletionMessage.builder()
                .role(Role.SYSTEM)
                .rawContent(textMessage.getContent())
                .build();
    }

    protected List<ChatCompletionMessage> buildHistoricalMessages(List<Message> historicalMessages) {
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
                                            .id(assistantToolCall.getId())
                                            .index(assistantToolCall.getIndex())
                                            .type(assistantToolCall.getType())
                                            .function(ChatCompletionFunction
                                                    .builder()
                                                    .name(assistantToolCall.getFunction().getName())
                                                    .arguments(assistantToolCall.getFunction().getArguments())
                                                    .build()
                                            )
                                            .build();
                                })
                                .toList();
                        return ChatCompletionMessage.builder()
                                .role(Role.ASSISTANT)
                                .rawContent(toolCallMessage.getContent())
                                .reasoningContent(toolCallMessage.getReasoningContent())
                                .toolCalls(toolCalls)
                                .build();
                    }
                    if (message instanceof ToolResultMessage toolResultMessage) {
                        return ChatCompletionMessage.builder()
                                .role(Role.TOOL)
                                .toolCallId(toolResultMessage.toolCallId())
                                .rawContent(toolResultMessage.content())
                                .build();
                    }
                    if (message instanceof MediaMessage mediaMessage) {
                        return this.buildMediaMessage(Role.fromValue(mediaMessage.getRole()), mediaMessage);
                    }
                    if (message instanceof TextMessage textMessage) {
                        return this.buildTextMessage(Role.fromValue(textMessage.getRole()), textMessage);
                    }
                    if (message instanceof AssistantTextMessage assistantTextMessage) {
                        return ChatCompletionMessage.builder()
                                .role(Role.ASSISTANT)
                                .rawContent(assistantTextMessage.getContent())
                                .reasoningContent(assistantTextMessage.getReasoningContent())
                                .build();
                    }
                    log.warn("Unhandled historical message: {}", message);
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    protected ChatCompletionMessage buildMediaMessage(Role role, MediaMessage mediaMessage) {
        Attachment[] attachments = mediaMessage.getAttachments();
        List<MediaContent> mediaContents = new ArrayList<>(attachments.length + 1);
        MediaContent textMediaContent = MediaContent.of(mediaMessage.getContent());
        mediaContents.add(textMediaContent);
        for (Attachment attachment : attachments) {
            MimeType mimeType = attachment.mimeType();
            if (attachment instanceof UrlAttachment urlAttachment && "image".equalsIgnoreCase(mimeType.getType())) {
                MediaContent imageContent = MediaContent.of(ImageUrl.of(urlAttachment.content(), null));
                mediaContents.add(imageContent);
                continue;
            }
            if (attachment instanceof Base64Attachment base64Attachment) {
                if ("audio".equalsIgnoreCase(mimeType.getType())) {
                    if (!Format.MP3.name().equalsIgnoreCase(mimeType.getSubtype()) && !Format.WAV.name().equalsIgnoreCase(mimeType.getSubtype())) {
                        log.warn("Unsupported audio format: {}", mimeType.getSubtype());
                        continue;
                    }
                    MediaContent audioContent = MediaContent.of(InputAudio.of(base64Attachment.content(), Format.valueOf(mimeType.getSubtype().toUpperCase())));
                    mediaContents.add(audioContent);
                    continue;
                }
                MediaContent fileContent = MediaContent.of(InputFile.of(base64Attachment.name(), base64Attachment.content()));
                mediaContents.add(fileContent);
                continue;
            }
            log.warn("Unhandled media attachment: {}", attachments);
        }
        return ChatCompletionMessage.builder()
                .rawContent(mediaContents)
                .role(role)
                .build();
    }

    protected ChatCompletionMessage buildTextMessage(Role role, TextMessage textMessage) {
        return ChatCompletionMessage.builder()
                .role(role)
                .rawContent(textMessage.getContent())
                .build();
    }

}

