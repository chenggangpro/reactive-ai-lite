package pro.chenggang.project.reactive.ai.lite.client.anthropic.chat;

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
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.AnthropicChatMessage;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.AnthropicChatMessage.ContentBlock;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.AnthropicChatMessage.ImageBlock;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.AnthropicChatMessage.ImageBlock.Base64ImageSource;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.AnthropicChatMessage.ImageBlock.UrlImageSource;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.AnthropicChatMessage.TextBlock;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.AnthropicChatMessage.ThinkingBlock;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.AnthropicChatMessage.ToolResultBlock;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.AnthropicChatMessage.ToolUseBlock;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.AnthropicChatRequest;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.AnthropicChatRequest.ThinkingConfig;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.AnthropicChatRequest.Tool;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.AnthropicChatRequest.ToolChoice;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.OutputConfig;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.dto.OutputConfig.OutputFormat;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.UriTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.entity.usage.Usage;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmChatRequestData;
import pro.chenggang.project.reactive.ai.lite.core.exception.ErrorServerSentEventException;
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
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.UrlAttachment;
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
import pro.chenggang.project.reactive.ai.lite.core.util.JsonChunkMerger;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonSchemaUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser.JsonChunkParsingData;
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
 * @author Cheng Gang
 * @version 0.1.0
 */
@Slf4j
public class AnthropicChatProvider extends AbstractLlmChatProvider {

    private final String baseUrL;
    private final String chatCompletionEndpoint;
    private final WebClient webClient;
    private final String apiVersion;

    @Builder
    private AnthropicChatProvider(@NonNull WebClient.Builder webClientBuilder,
                                  @NonNull String baseUrL,
                                  @NonNull String chatCompletionEndpoint,
                                  boolean isDefault,
                                  @NonNull String name,
                                  Set<String> supportedModels,
                                  @NonNull List<TokenCertification> certifications,
                                  @NonNull LLmProviderInterceptorRegistry lLmProviderInterceptorRegistry,
                                  @NonNull String apiVersion) {
        super(certifications,
                (certificationMap) -> AnthropicLlmProviderInfo.builder()
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
        this.apiVersion = apiVersion;
        this.webClient = webClientBuilder.baseUrl(baseUrL).build();
    }

    protected Integer extractPromptTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/input_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    protected Integer extractCompletionTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/output_tokens");
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
                })
                .header("anthropic-version", apiVersion);
    }

    @Override
    protected ObjectNode initializeRequestBody(@NonNull LlmChatRequestData llmChatRequestData) {
        return OBJECT_MAPPER.valueToTree(this.buildRequest(llmChatRequestData));
    }

    @Override
    protected JsonStreamChunkSlide[] extractStreamChunks(@NonNull JsonChunkParsingData jsonChunkParsingData) {
        ObjectNode parsingDataContent = jsonChunkParsingData.getData();
        JsonNode typeNode = parsingDataContent.at("/type");
        if (typeNode.isMissingNode() || !typeNode.isTextual() || typeNode.isNull()) {
            log.warn("Type node is missing in parsing data chunk: {}", jsonChunkParsingData);
            return new JsonStreamChunkSlide[0];
        }
        String contentType = typeNode.textValue();
        if ("error".equalsIgnoreCase(contentType)) {
            JsonNode errorTypeNode = parsingDataContent.at("/error/type");
            JsonNode errorMessageNode = parsingDataContent.at("/error/message");
            String errorType = null;
            String errorMessage = null;
            if (!errorTypeNode.isMissingNode() && !errorTypeNode.isNull() && errorTypeNode.isTextual()) {
                errorType = errorTypeNode.textValue();
            }
            if (!errorMessageNode.isMissingNode() && !errorMessageNode.isNull() && errorMessageNode.isTextual()) {
                errorMessage = errorMessageNode.textValue();
            }
            throw new ErrorServerSentEventException(errorType, errorMessage, parsingDataContent);
        }
        if ("ping".equalsIgnoreCase(contentType)) {
            return new JsonStreamChunkSlide[0];
        }
        if ("content_block_stop".equalsIgnoreCase(contentType)) {
            return new JsonStreamChunkSlide[0];
        }
        if ("message_stop".equalsIgnoreCase(contentType)) {
            return new JsonStreamChunkSlide[0];
        }
        if ("message_start".equalsIgnoreCase(contentType)) {
            JsonNode jsonNode = parsingDataContent.at("/message/usage");
            // cache the usage
            if (!jsonNode.isMissingNode() && !jsonNode.isNull() && jsonNode.isObject()) {
                jsonChunkParsingData.getParsingAttributes()
                        .put("usage", jsonNode.deepCopy());
            }
            JsonNode roleNode = parsingDataContent.at("/message/role");
            if (!roleNode.isMissingNode() && !roleNode.isNull() && roleNode.isTextual()) {
                return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.ROLE)
                        .dataContent(parsingDataContent)
                        .build()
                };
            }
            log.warn("Unrecognized content: {} --> data: {}", contentType, parsingDataContent);
            return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.UNKNOWN)
                    .dataContent(parsingDataContent)
                    .build()
            };
        }
        if ("content_block_start".equalsIgnoreCase(contentType)) {
            JsonNode jsonNode = parsingDataContent.at("/content_block/type");
            if (!jsonNode.isMissingNode() && !jsonNode.isNull() && jsonNode.isTextual()) {
                String contentBlockType = jsonNode.textValue();
                if ("thinking".equalsIgnoreCase(contentBlockType)) {
                    return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                            .streamDataType(StreamDataType.REASONING_CONTENT)
                            .dataContent(parsingDataContent)
                            .build()
                    };
                }
                if ("text".equalsIgnoreCase(contentBlockType)) {
                    return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                            .streamDataType(StreamDataType.ANSWER_CONTENT)
                            .dataContent(parsingDataContent)
                            .build()
                    };
                }
                if ("tool_use".equalsIgnoreCase(contentBlockType)) {
                    return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                            .streamDataType(StreamDataType.TOOL_CALL)
                            .dataContent(parsingDataContent)
                            .build()
                    };
                }
            }
            log.warn("Unrecognized content: {} --> data: {}", contentType, parsingDataContent);
            return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.UNKNOWN)
                    .dataContent(parsingDataContent)
                    .build()
            };
        }
        if ("content_block_delta".equalsIgnoreCase(contentType)) {
            JsonNode jsonNode = parsingDataContent.at("/delta/type");
            if (!jsonNode.isMissingNode() && !jsonNode.isNull() && jsonNode.isTextual()) {
                String deltaType = jsonNode.textValue();
                if ("thinking_delta".equalsIgnoreCase(deltaType) || "signature_delta".equalsIgnoreCase(deltaType)) {
                    return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                            .streamDataType(StreamDataType.REASONING_CONTENT)
                            .dataContent(parsingDataContent)
                            .build()
                    };
                }
                if ("text_delta".equalsIgnoreCase(deltaType)) {
                    return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                            .streamDataType(StreamDataType.ANSWER_CONTENT)
                            .dataContent(parsingDataContent)
                            .build()
                    };
                }
                if ("input_json_delta".equalsIgnoreCase(deltaType)) {
                    return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                            .streamDataType(StreamDataType.TOOL_CALL)
                            .dataContent(parsingDataContent)
                            .build()
                    };
                }
            }
            log.warn("Unrecognized content: {} --> data: {}", contentType, parsingDataContent);
            return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                    .streamDataType(StreamDataType.UNKNOWN)
                    .dataContent(parsingDataContent)
                    .build()
            };
        }
        if ("message_delta".equalsIgnoreCase(contentType)) {
            JsonNode usageNode = parsingDataContent.at("/usage");
            ObjectNode cachedUsage = jsonChunkParsingData.getParsingAttribute("usage");
            ObjectNode allUsageNode = null;
            if (!usageNode.isMissingNode() && !usageNode.isNull() && usageNode.isObject()) {
                if (Objects.nonNull(cachedUsage)) {
                    allUsageNode = JsonChunkMerger.merge(cachedUsage, usageNode.deepCopy());
                } else {
                    allUsageNode = usageNode.deepCopy();
                }
            } else if (Objects.nonNull(cachedUsage)) {
                allUsageNode = cachedUsage;
            }
            JsonNode stopReasonNode = parsingDataContent.at("/delta/stop_reason");
            ObjectNode finishNode = null;
            if (!stopReasonNode.isMissingNode() && !stopReasonNode.isNull() && stopReasonNode.isObject()) {
                finishNode = parsingDataContent.at("/delta").deepCopy();
            }
            if (Objects.nonNull(allUsageNode) && Objects.nonNull(finishNode)) {
                JsonStreamChunkSlide usageChunkSlide = JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.USAGE)
                        .dataContent(allUsageNode)
                        .build();
                JsonStreamChunkSlide finishChunkSlide = JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.FINISHED)
                        .dataContent(finishNode)
                        .build();
                return new JsonStreamChunkSlide[]{usageChunkSlide, finishChunkSlide};
            }
            if (Objects.nonNull(allUsageNode)) {
                return new JsonStreamChunkSlide[]{
                        JsonStreamChunkSlide.builder()
                                .streamDataType(StreamDataType.USAGE)
                                .dataContent(allUsageNode)
                                .build()
                };
            }
            if (Objects.nonNull(finishNode)) {
                return new JsonStreamChunkSlide[]{
                        JsonStreamChunkSlide.builder()
                                .streamDataType(StreamDataType.FINISHED)
                                .dataContent(finishNode)
                                .build()
                };
            }
        }
        log.warn("Unrecognized content: {} --> data: {}", contentType, parsingDataContent);
        return new JsonStreamChunkSlide[]{JsonStreamChunkSlide.builder()
                .streamDataType(StreamDataType.UNKNOWN)
                .dataContent(parsingDataContent)
                .build()
        };
    }

    @Override
    protected ObjectNode mergeRawToolCallMessages(@NonNull List<ObjectNode> rawToolCallMessages, boolean distinctToolCalls) {
        List<ObjectNode> toolCalls = new ArrayList<>();
        for (ObjectNode rawToolCallMessage : rawToolCallMessages) {
            JsonNode messageTypeNode = rawToolCallMessage.at("/type");
            if (messageTypeNode.isMissingNode() || !messageTypeNode.isTextual() || messageTypeNode.isNull()) {
                continue;
            }
            JsonNode indexNode = rawToolCallMessage.at("/index");
            if (indexNode.isMissingNode() || indexNode.isNull() || !indexNode.isInt()) {
                continue;
            }
            String messageType = messageTypeNode.textValue();
            if ("content_block_start".equalsIgnoreCase(messageType)) {
                ObjectNode contentBlockNode = (ObjectNode) rawToolCallMessage.at("/content_block");
                toolCalls.add(contentBlockNode);
                continue;
            }
            if ("content_block_delta".equalsIgnoreCase(messageType)) {
                JsonNode partialJsonNode = rawToolCallMessage.at("/delta/partial_json");
                if (!partialJsonNode.isMissingNode() && partialJsonNode.isTextual() && !partialJsonNode.isNull()) {
                    String partialJsonValue = partialJsonNode.textValue();
                    ObjectNode objectNode = toolCalls.getLast();
                    JsonNode inputNode = objectNode.at("/input");
                    if (!inputNode.isMissingNode() && inputNode.isTextual()) {
                        String inputValue = inputNode.textValue();
                        objectNode.put("input", inputValue + partialJsonValue);
                    } else {
                        objectNode.put("input", partialJsonValue);
                    }
                }
            }
        }
        if (distinctToolCalls) {
            Set<String> toolCallNames = new HashSet<>();
            if (toolCalls.size() > 1) {
                Iterator<ObjectNode> iterator = toolCalls.iterator();
                while (iterator.hasNext()) {
                    ObjectNode toolCall = iterator.next();
                    String toolName = toolCall.at("/name").textValue();
                    if (toolCallNames.contains(toolName)) {
                        iterator.remove();
                        continue;
                    }
                    toolCallNames.add(toolName);
                }
            }
            toolCallNames.clear();
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
                    JsonNode contentArrayNode = rawResponseBody.at("/content");
                    if (contentArrayNode.isMissingNode() || !contentArrayNode.isArray()) {
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
                    String reasoningContent = null;
                    String thinkingSignature = null;
                    List<AssistantToolCall> assistantToolCallList = new ArrayList<>();
                    Map<String, ToolDefinition> toolDefinitionMapByIdentifier = toolDefinitions.stream()
                            .collect(Collectors.toMap(
                                    ToolDefinition::name,
                                    java.util.function.Function.identity(),
                                    (o1, o2) -> o1,
                                    HashMap::new
                            ));
                    int toolCallIndex = 0;
                    for (JsonNode contentNode : contentArrayNode) {
                        JsonNode contentTypeNode = contentNode.at("/type");
                        if (!contentTypeNode.isMissingNode() && contentTypeNode.isTextual() && !contentTypeNode.isNull()) {
                            String contentType = contentTypeNode.textValue();
                            if (Objects.isNull(answerContent) && "text".equalsIgnoreCase(contentType)) {
                                answerContent = contentNode.get("text").textValue();
                            } else if (Objects.isNull(reasoningContent) && "thinking".equalsIgnoreCase(contentType)) {
                                reasoningContent = contentNode.get("thinking").textValue();
                                thinkingSignature = contentNode.get("signature").textValue();
                            } else if ("tool_use".equalsIgnoreCase(contentType)) {
                                AssistantToolCall assistantToolCall = parseToolCall(toolDefinitionMapByIdentifier, (ObjectNode) contentNode, toolCallIndex);
                                assistantToolCallList.add(assistantToolCall);
                                toolCallIndex++;
                            }
                        }
                    }
                    if (!assistantToolCallList.isEmpty()) {
                        ToolCallMessage toolCallMessage = DefaultToolCallMessage.builder()
                                .toolCalls(assistantToolCallList)
                                .content(answerContent)
                                .reasoningContent(reasoningContent)
                                .build();
                        if (StringUtils.hasText(thinkingSignature)) {
                            toolCallMessage.getAttributes()
                                    .put("signature", thinkingSignature);
                        }
                        GeneralResponse generalResponse = generalResponseBuilder.assistantTextMessage(toolCallMessage)
                                .build();
                        syncSink.next(generalResponse);
                        return;
                    }
                    AssistantTextMessage assistantTextMessage = DefaultAssistantTextMessage.builder()
                            .content(answerContent)
                            .reasoningContent(reasoningContent)
                            .build();
                    if (StringUtils.hasText(thinkingSignature)) {
                        assistantTextMessage.getAttributes()
                                .put("signature", thinkingSignature);
                    }
                    GeneralResponse generalResponse = generalResponseBuilder.assistantTextMessage(assistantTextMessage)
                            .build();
                    syncSink.next(generalResponse);
                });
    }

    private AssistantToolCall parseToolCall(@NonNull Map<String, ToolDefinition> toolDefinitionMapByName, @NonNull ObjectNode toolCallNode, int index) {
        String toolName = toolCallNode.get("name").asText();
        if (!toolDefinitionMapByName.containsKey(toolName)) {
            throw new IllegalStateException("The tool identifier of tool-calling response '" + toolName + "' is not found in the tool definitions.");
        }
        AssistantToolCallFunction toolCallFunction = AssistantToolCallFunction.builder()
                .name(toolName)
                .arguments(toolCallNode.at("/input").toString())
                .build();
        ToolDefinition toolDefinition = toolDefinitionMapByName.get(toolName);
        return AssistantToolCall.builder()
                .index(index)
                .id(toolCallNode.get("id").asText())
                .type("function")
                .toolDefinition(toolDefinition)
                .function(toolCallFunction)
                .build();
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
                }
        );
    }

    protected <R> Mono<StructuredResponse<R>> extractStructuredResponseContentInternal(@NonNull List<ToolDefinition> toolDefinitions,
                                                                                       @NonNull RawResponse rawResponse,
                                                                                       @NonNull java.util.function.Function<String, R> textValueConverter) {
        return Mono.fromCallable(rawResponse::getResponseBody)
                .handle((rawResponseBody, syncSink) -> {
                    var structuredResponseBuilder = StructuredResponse.<R>builder()
                            .contextView(rawResponse.getContextView());
                    JsonNode contentArrayNode = rawResponseBody.at("/content");
                    if (contentArrayNode.isMissingNode() || !contentArrayNode.isArray()) {
                        log.error("Failed to extract response message from response body. Response body: {}", rawResponseBody.toPrettyString());
                        syncSink.error(new ResponseMessageExtractFailedException(rawResponseBody));
                        return;
                    }
                    String answerContent = null;
                    String reasoningContent = null;
                    String thinkingSignature = null;
                    List<AssistantToolCall> assistantToolCallList = new ArrayList<>();
                    Map<String, ToolDefinition> toolDefinitionMapByIdentifier = toolDefinitions.stream()
                            .collect(Collectors.toMap(
                                    ToolDefinition::name,
                                    java.util.function.Function.identity(),
                                    (o1, o2) -> o1,
                                    HashMap::new
                            ));
                    int toolCallIndex = 0;
                    for (JsonNode contentNode : contentArrayNode) {
                        JsonNode contentTypeNode = contentNode.at("/type");
                        if (!contentTypeNode.isMissingNode() && contentTypeNode.isTextual() && !contentTypeNode.isNull()) {
                            String contentType = contentTypeNode.textValue();
                            if (Objects.isNull(answerContent) && "text".equalsIgnoreCase(contentType)) {
                                answerContent = contentNode.get("text").textValue();
                            } else if (Objects.isNull(reasoningContent) && "thinking".equalsIgnoreCase(contentType)) {
                                reasoningContent = contentNode.get("thinking").textValue();
                                thinkingSignature = contentNode.get("signature").textValue();
                            } else if ("tool_use".equalsIgnoreCase(contentType)) {
                                AssistantToolCall assistantToolCall = parseToolCall(toolDefinitionMapByIdentifier, (ObjectNode) contentNode, toolCallIndex);
                                assistantToolCallList.add(assistantToolCall);
                                toolCallIndex++;
                            }
                        }
                    }
                    R structuredValue = null;
                    try {
                        structuredValue = textValueConverter.apply(answerContent);
                    } catch (Exception e) {
                        log.error("Failed to parse content : {}", answerContent, e);
                        syncSink.error(e);
                        return;
                    }
                    if (!assistantToolCallList.isEmpty()) {
                        ToolCallMessage toolCallMessage = DefaultToolCallMessage.builder()
                                .toolCalls(assistantToolCallList)
                                .content(answerContent)
                                .reasoningContent(reasoningContent)
                                .build();
                        if (StringUtils.hasText(thinkingSignature)) {
                            toolCallMessage.getAttributes()
                                    .put("signature", thinkingSignature);
                        }
                        StructuredResponse<R> structuredResponse = structuredResponseBuilder.assistantTextMessage(toolCallMessage)
                                .build();
                        syncSink.next(structuredResponse);
                        return;
                    }
                    AssistantTextMessage assistantTextMessage = DefaultAssistantTextMessage.builder()
                            .content(answerContent)
                            .reasoningContent(reasoningContent)
                            .build();
                    if (StringUtils.hasText(thinkingSignature)) {
                        assistantTextMessage.getAttributes()
                                .put("signature", thinkingSignature);
                    }
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
                JsonNode contentNode = dataContent.at("/content_block/text");
                JsonNode deltaNode = dataContent.at("/delta/text");
                if (!contentNode.isMissingNode() && !contentNode.isNull() && contentNode.isTextual()) {
                    chunkBuilder.value(contentNode.asText());
                } else if (!deltaNode.isMissingNode() && !deltaNode.isNull() && deltaNode.isTextual()) {
                    chunkBuilder.value(deltaNode.asText());
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
                JsonNode contentNode = dataContent.at("/content_block/thinking");
                JsonNode deltaNode = dataContent.at("/delta/thinking");
                if (!contentNode.isMissingNode() && !contentNode.isNull() && contentNode.isTextual()) {
                    chunkBuilder.value(contentNode.asText());
                } else if (!deltaNode.isMissingNode() && !deltaNode.isNull() && deltaNode.isTextual()) {
                    chunkBuilder.value(deltaNode.asText());
                }
                TextStreamDataChunk textStreamDataChunk = chunkBuilder
                        .build();
                JsonNode signatureNode = dataContent.at("/content_block/signature");
                if (!signatureNode.isMissingNode() && !signatureNode.isNull() && signatureNode.isTextual()) {
                    String signature = signatureNode.asText();
                    textStreamDataChunk.getAttributes()
                            .put("signature", signature);
                }
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
                    List<AssistantToolCall> assistantToolCallList = new ArrayList<>();
                    Map<String, ToolDefinition> toolDefinitionMapByIdentifier = toolDefinitions.stream()
                            .collect(Collectors.toMap(
                                    ToolDefinition::name,
                                    java.util.function.Function.identity(),
                                    (o1, o2) -> o1,
                                    HashMap::new
                            ));
                    int toolCallIndex = 0;
                    for (JsonNode contentNode : toolCallsNode) {
                        JsonNode contentTypeNode = contentNode.at("/type");
                        if (!contentTypeNode.isMissingNode() && contentTypeNode.isTextual() && !contentTypeNode.isNull()) {
                            AssistantToolCall assistantToolCall = parseToolCall(toolDefinitionMapByIdentifier, (ObjectNode) contentNode, toolCallIndex);
                            assistantToolCallList.add(assistantToolCall);
                            toolCallIndex++;
                        }
                    }
                    chunkBuilder.toolCalls(assistantToolCallList);
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
                        .contextView(contextView)
                        .dataChunk(usageStreamDataChunk)
                        .build();
            });
        }
        if (StreamDataType.FINISHED.equals(streamDataType)) {
            return Mono.fromCallable(() -> {
                var chunkBuilder = TextStreamDataChunk.builder()
                        .dataType(StreamDataType.FINISHED);
                JsonNode finishReasonNode = dataContent.at("/stop_reason");
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
    public String toString() {
        return "AnthropicChatProvider{" +
                "llmProviderInfo=" + llmProviderInfo +
                ", baseUrL='" + baseUrL + '\'' +
                ", chatCompletionEndpoint='" + chatCompletionEndpoint + '\'' +
                ", certification=" + certificationMap.size() +
                '}';
    }

    protected AnthropicChatRequest buildRequest(LlmChatRequestData llmChatRequestData) {
        var anthropicChatRequestBuilder = AnthropicChatRequest.builder()
                .model(llmChatRequestData.getModelName());
        if (llmChatRequestData.getResponseJsonSchema().isPresent() || llmChatRequestData.getStructuredOutputType().isPresent()) {
            Map<String, Object> jsonSchemaMap = Map.of();
            if (llmChatRequestData.getResponseJsonSchema().isPresent()) {
                jsonSchemaMap = JsonRelatedUtil.jsonToMap(llmChatRequestData.getResponseJsonSchema().get());
            } else if (llmChatRequestData.getStructuredOutputType().isPresent()) {
                var structuredOutputType = llmChatRequestData.getStructuredOutputType().get();
                jsonSchemaMap = JsonRelatedUtil.jsonToMap(JsonSchemaUtil.generateForType(structuredOutputType));
            }
            anthropicChatRequestBuilder.outputConfig(OutputConfig.builder()
                    .format(OutputFormat.builder()
                            .jsonSchema(jsonSchemaMap)
                            .build())
                    .build()
            );
        }
        llmChatRequestData.getTemperature().ifPresent(anthropicChatRequestBuilder::temperature);
        llmChatRequestData.getTopP().ifPresent(anthropicChatRequestBuilder::topP);
        llmChatRequestData.getReasoning().ifPresent(reasoning -> {
            String[] anthropicReasoning = reasoning.split(":");
            String enabledOrDisabled = anthropicReasoning[0];
            if ("true".equalsIgnoreCase(enabledOrDisabled) || "enabled".equalsIgnoreCase(enabledOrDisabled) || "1".equalsIgnoreCase(enabledOrDisabled)) {
                int budgetTokens = 1024;
                if (anthropicReasoning.length > 1) {
                    try {
                        budgetTokens = Integer.parseInt(anthropicReasoning[1]);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid budge tokens in thinking , using 1024 instead.");
                    }
                } else {
                    log.warn("No budge tokens configured in thinking , using 1024 instead.");
                }
                anthropicChatRequestBuilder.thinking(ThinkingConfig.builder()
                        .budgetTokens(budgetTokens)
                        .build()
                );
            } else if ("false".equalsIgnoreCase(enabledOrDisabled) || "disabled".equalsIgnoreCase(enabledOrDisabled) || "0".equalsIgnoreCase(enabledOrDisabled)) {
                // not to do when thinking is disabled
            } else {
                log.warn("Invalid reasoning and budget tokens value: {}", reasoning);
            }
        });
        llmChatRequestData.getMaxCompletionTokens()
                .ifPresentOrElse(anthropicChatRequestBuilder::maxTokens, () -> anthropicChatRequestBuilder.maxTokens(1024));
        llmChatRequestData.getToolChoice()
                .map(toolChoice -> {
                    if ("auto".equalsIgnoreCase(toolChoice)) {
                        return ToolChoice.AUTO;
                    }
                    if ("any".equalsIgnoreCase(toolChoice)) {
                        return ToolChoice.ANY;
                    }
                    if ("none".equalsIgnoreCase(toolChoice)) {
                        return ToolChoice.NONE;
                    }
                    if (toolChoice.startsWith("tool")) {
                        String[] toolChoiceConfig = toolChoice.split(":");
                        if (toolChoiceConfig.length > 1) {
                            return ToolChoice.tool(toolChoiceConfig[1]);
                        }
                    }
                    log.warn("Unrecognized tool choice: {}", toolChoice);
                    return null;
                })
                .ifPresent(anthropicChatRequestBuilder::toolChoice);
        var functionTools = buildFunctionTools(llmChatRequestData.getToolDefinitions());
        var userMessage = llmChatRequestData.getUserMediaMessage()
                .map(mediaMessage -> buildMediaMessage(Role.USER, mediaMessage))
                .orElseGet(() -> this.buildTextMessage(Role.USER, llmChatRequestData.getUserTextMessage()));
        var historicalMessages = buildHistoricalMessages(llmChatRequestData.getHistoricalMessages());
        var toolMessages = buildToolMessages(llmChatRequestData.getToolResultMessages());
        var allMessages = Stream.of(historicalMessages.stream(),
                        toolMessages.stream(),
                        toolMessages.isEmpty() ? Stream.of(userMessage) : Stream.<AnthropicChatMessage>empty()
                )
                .flatMap(java.util.function.Function.identity())
                .toList();
        return anthropicChatRequestBuilder
                .system(llmChatRequestData.getSystemMessage().getContent())
                .stream(llmChatRequestData.isStream())
                .messages(allMessages)
                .tools(functionTools)
                .build();
    }

    protected List<Tool> buildFunctionTools(List<ToolDefinition> toolDefinitions) {
        if (toolDefinitions.isEmpty()) {
            return List.of();
        }
        return toolDefinitions.stream()
                .map(toolDefinition -> Tool.builder()
                        .name(toolDefinition.name())
                        .description(toolDefinition.description())
                        .inputSchema(JsonRelatedUtil.jsonToMap(toolDefinition.inputSchema()))
                        .strict(toolDefinition.strict())
                        .build()
                )
                .toList();
    }

    protected List<AnthropicChatMessage> buildToolMessages(List<ToolResultMessage> toolResultMessages) {
        if (toolResultMessages.isEmpty()) {
            return List.of();
        }
        return toolResultMessages.stream()
                .map(toolResultMessage -> {
                    ToolResultBlock toolResultBlock = ToolResultBlock.builder()
                            .toolUseId(toolResultMessage.toolCallId())
                            .content(toolResultMessage.content())
                            .build();
                    return AnthropicChatMessage.builder()
                            .role(Role.USER)
                            .content(List.of(toolResultBlock))
                            .build();
                })
                .toList();
    }

    protected List<AnthropicChatMessage> buildHistoricalMessages(List<Message> historicalMessages) {
        if (historicalMessages.isEmpty()) {
            return List.of();
        }
        return historicalMessages.stream()
                .map(message -> {
                    if (message instanceof ToolCallMessage toolCallMessage) {
                        List<ToolUseBlock> toolUseBlockList = toolCallMessage.getToolCalls()
                                .stream()
                                .map(assistantToolCall -> {
                                    return ToolUseBlock.builder()
                                            .id(assistantToolCall.getId())
                                            .name(assistantToolCall.getFunction().getName())
                                            .input(JsonRelatedUtil.jsonToMap(assistantToolCall.getFunction().getArguments()))
                                            .build();

                                })
                                .toList();
                        return AnthropicChatMessage.builder()
                                .role(Role.ASSISTANT)
                                .content(toolUseBlockList)
                                .build();
                    }
                    if (message instanceof ToolResultMessage toolResultMessage) {
                        ToolResultBlock toolResultBlock = ToolResultBlock.builder()
                                .toolUseId(toolResultMessage.toolCallId())
                                .content(toolResultMessage.content())
                                .build();
                        return AnthropicChatMessage.builder()
                                .role(Role.USER)
                                .content(List.of(toolResultBlock))
                                .build();
                    }
                    if (message instanceof MediaMessage mediaMessage) {
                        return this.buildMediaMessage(Role.valueOf(mediaMessage.getRole()), mediaMessage);
                    }
                    if (message instanceof TextMessage textMessage) {
                        return this.buildTextMessage(Role.valueOf(textMessage.getRole()), textMessage);
                    }
                    if (message instanceof AssistantTextMessage assistantTextMessage) {
                        String reasoningContent = assistantTextMessage.getReasoningContent();
                        if (StringUtils.hasText(reasoningContent)) {
                            String signature = assistantTextMessage.getAttribute("signature");
                            ThinkingBlock thinkingBlock = ThinkingBlock.builder()
                                    .signature(signature)
                                    .thinking(reasoningContent)
                                    .build();
                            TextBlock textBlock = TextBlock.builder()
                                    .text(assistantTextMessage.getContent())
                                    .build();
                            return AnthropicChatMessage.builder()
                                    .role(Role.ASSISTANT)
                                    .content(List.of(thinkingBlock, textBlock))
                                    .build();
                        }
                        TextBlock textBlock = TextBlock.builder()
                                .text(assistantTextMessage.getContent())
                                .build();
                        return AnthropicChatMessage.builder()
                                .role(Role.ASSISTANT)
                                .content(List.of(textBlock))
                                .build();
                    }
                    log.warn("Unhandled historical message: {}", message);
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    protected AnthropicChatMessage buildMediaMessage(Role role, MediaMessage mediaMessage) {
        Attachment[] attachments = mediaMessage.getAttachments();
        List<ContentBlock> mediaContents = new ArrayList<>(attachments.length + 1);
        TextBlock textBlock = TextBlock.builder()
                .text(mediaMessage.getContent())
                .build();
        mediaContents.add(textBlock);
        for (Attachment attachment : attachments) {
            MimeType mimeType = attachment.mimeType();
            if ("image".equalsIgnoreCase(mimeType.getType())) {
                if (attachment instanceof UrlAttachment urlAttachment) {
                    UrlImageSource urlImageSource = UrlImageSource.builder()
                            .url(urlAttachment.content())
                            .build();
                    ImageBlock imageBlock = ImageBlock.builder()
                            .source(urlImageSource)
                            .build();
                    mediaContents.add(imageBlock);
                    continue;
                }

            }
            if (attachment instanceof Base64Attachment base64Attachment) {
                Base64ImageSource base64ImageSource = Base64ImageSource.builder()
                        .mediaType(mimeType.toString())
                        .data(base64Attachment.content())
                        .build();
                ImageBlock imageBlock = ImageBlock.builder()
                        .source(base64ImageSource)
                        .build();
                mediaContents.add(imageBlock);
                continue;
            }
            log.warn("Unhandled media attachment: {}", attachment);
        }
        return AnthropicChatMessage.builder()
                .content(mediaContents)
                .role(role)
                .build();
    }

    protected AnthropicChatMessage buildTextMessage(Role role, TextMessage textMessage) {
        TextBlock textBlock = TextBlock.builder()
                .text(textMessage.getContent())
                .build();
        return AnthropicChatMessage.builder()
                .role(role)
                .content(List.of(textBlock))
                .build();
    }
}
