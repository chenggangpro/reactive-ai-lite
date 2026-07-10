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
package pro.chenggang.project.reactive.ai.lite.client.anthropic.provider.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
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
import pro.chenggang.project.reactive.ai.lite.client.anthropic.provider.AnthropicLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.UriTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.usage.Usage;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmChatRequestData;
import pro.chenggang.project.reactive.ai.lite.core.exception.ErrorServerSentEventException;
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
 * Anthropic chat provider delegate that implements {@link LlmChatProviderDelegate}
 * to handle communication with the Anthropic API. It translates generic
 * {@link LlmChatRequestData} into Anthropic-specific requests, parses streaming
 * server-sent events, merges tool call fragments, and extracts both standard
 * response messages and token usage information. It also adapts historical
 * messages, tool results, and media attachments to Anthropic's message format.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class AnthropicChatProviderDelegate implements LlmChatProviderDelegate {

    /**
     * Information about this LLM provider, including supported models, base URL,
     * and endpoint, built from the configuration passed to the constructor.
     */
    private final LlmProviderInfo llmProviderInfo;

    /**
     * The base URL of the Anthropic API, e.g., {@code https://api.anthropic.com}.
     * It is used to construct requests and to populate the provider info.
     */
    private final String baseUrL;

    /**
     * The specific endpoint path for chat completions, appended to the base URL.
     */
    private final String chatCompletionEndpoint;

    /**
     * Configured {@link WebClient} instance used to execute HTTP requests to
     * the Anthropic API. Its default headers include the {@code anthropic-version}.
     */
    private final WebClient webClient;

    /**
     * The Anthropic API version to use, set as the value of the
     * {@code anthropic-version} request header.
     */
    private final String apiVersion;

    /**
     * Constructs an {@code AnthropicChatProviderDelegate} with the necessary
     * dependencies and configuration.
     *
     * @param webClientBuilder       builder for the {@link WebClient}, pre-configured
     *                               with any custom settings (e.g., codecs, timeouts).
     * @param baseUrL               base URL of the Anthropic API
     * @param chatCompletionEndpoint path to the chat completion endpoint (e.g., {@code /v1/messages})
     * @param isDefault             whether this provider should be treated as the default
     * @param name                  a human-readable name for this provider
     * @param supportedModels       set of model identifiers that this provider can handle
     * @param certifications        list of {@link TokenCertification} instances for authorization
     * @param apiVersion            Anthropic API version string, e.g., {@code 2023-06-01}
     */
    @Builder
    protected AnthropicChatProviderDelegate(@NonNull WebClient.Builder webClientBuilder,
                                            @NonNull String baseUrL,
                                            @NonNull String chatCompletionEndpoint,
                                            boolean isDefault,
                                            @NonNull String name,
                                            Set<String> supportedModels,
                                            @NonNull List<TokenCertification> certifications,
                                            @NonNull String apiVersion) {
        this.baseUrL = baseUrL;
        this.chatCompletionEndpoint = chatCompletionEndpoint;
        this.apiVersion = apiVersion;
        this.webClient = webClientBuilder.baseUrl(baseUrL)
                .defaultHeader("anthropic-version", apiVersion)
                .build();
        this.llmProviderInfo = AnthropicLlmProviderInfo.builder()
                .isDefault(isDefault)
                .name(name)
                .supportedModels(supportedModels)
                .profiles(certifications.stream().map(TokenCertification::profile).collect(java.util.stream.Collectors.toSet()))
                .baseUrl(baseUrL)
                .endpoint(chatCompletionEndpoint)
                .build();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the {@link LlmProviderInfo} built during construction, providing
     * details such as the supported models and base URL.
     */
    @Override
    public LlmProviderInfo providerInfo() {
        return this.llmProviderInfo;
    }

    /**
     * Extracts the number of prompt tokens from a raw usage JSON node.
     * Anthropic reports prompt tokens under the {@code input_tokens} field.
     *
     * @param rawUsage the raw usage object (from the API response)
     * @return the number of prompt tokens, or 0 if not present or not a number
     */
    protected Integer extractPromptTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/input_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    /**
     * Extracts the number of completion tokens from a raw usage JSON node.
     * Anthropic reports completion tokens under the {@code output_tokens} field.
     *
     * @param rawUsage the raw usage object
     * @return the number of completion tokens, or 0 if not present or not a number
     */
    protected Integer extractCompletionTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/output_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    /**
     * Extracts the number of other (non-prompt, non-completion) tokens.
     * Currently Anthropic does not expose such tokens, so this always returns 0.
     *
     * @param rawUsage the raw usage object (ignored)
     * @return always 0
     */
    protected Integer extractOtherTokenUsage(ObjectNode rawUsage) {
        return 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Prepares an HTTP POST request spec for the Anthropic chat completions
     * endpoint. The URI is built by combining the base URL with the
     * {@code chatCompletionEndpoint} and applying any {@link UriTokenCertification}
     * from the request data. The {@code anthropic-version} header is also added.
     */
    @Override
    public RequestBodySpec loadRequestBodySpec(@NonNull LlmChatRequestData llmChatRequestData) {
        RequestBodySpec requestBodySpec = this.webClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.path(this.chatCompletionEndpoint);
                    llmChatRequestData.getTokenCertification()
                            .ifPresent(tokenCertification -> {
                                if (tokenCertification instanceof UriTokenCertification uriTokenCertification) {
                                    uriTokenCertification.applyTo(uriBuilder);
                                }
                            });
                    return uriBuilder.build();
                })
                .header("anthropic-version", apiVersion);
        llmChatRequestData.getTokenCertification().ifPresent(cert -> applyStandardTokenCertification(requestBodySpec, cert));
        return requestBodySpec;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Converts the given {@link LlmChatRequestData} into an Anthropic-specific
     * request body represented as an {@link ObjectNode}. The conversion delegates
     * to {@link #buildRequest(LlmChatRequestData)} and then serializes the result.
     */
    @Override
    public ObjectNode initializeRequestBody(@NonNull LlmChatRequestData llmChatRequestData) {
        return OBJECT_MAPPER.valueToTree(this.buildRequest(llmChatRequestData));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Analyzes a single JSON chunk from Anthropic's server‑sent event stream
     * and categorizes it into one or more {@link JsonStreamChunkSlide} objects
     * based on the {@code type} field. Different event types (e.g., {@code message_start},
     * {@code content_block_delta}) are mapped to appropriate {@link StreamDataType}
     * values with any necessary data extraction. Error events trigger an
     * {@link ErrorServerSentEventException}.
     */
    @Override
    public JsonStreamChunkSlide[] extractStreamChunks(@NonNull JsonChunkParsingData jsonChunkParsingData) {
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

    /**
     * {@inheritDoc}
     * <p>
     * Merges a list of raw tool call JSON chunks (typically from streamed
     * {@code content_block_start} and {@code content_block_delta} events) into a
     * single {@link ObjectNode} containing an array of merged tool calls. If
     * {@code distinctToolCalls} is {@code true}, duplicate tool calls (by name)
     * are removed, keeping only the first occurrence.
     */
    @Override
    public ObjectNode mergeRawToolCallMessages(@NonNull List<ObjectNode> rawToolCallMessages, boolean distinctToolCalls) {
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

    /**
     * {@inheritDoc}
     * <p>
     * Extracts a {@link GeneralResponse} from a complete Anthropic response body.
     * The method parses the {@code content} array for text, thinking, and tool use
     * blocks. Token usage information is extracted from the {@code usage} field.
     * If tool calls are present, a {@link ToolCallMessage} is built and set as
     * the assistant message; otherwise a plain {@link AssistantTextMessage} is
     * used. In both cases the thinking signature (if any) is stored as an
     * attribute.
     */
    @Override
    public Mono<GeneralResponse> extractGeneralResponse(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse) {
        return Mono.fromCallable(rawResponse::getResponseBody)
                .handle((rawResponseBody, syncSink) -> {
                    var generalResponseBuilder = GeneralResponse.builder()
                            .executionContext(rawResponse.getExecutionContext())
                            .rawResponseBody(rawResponseBody);
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

    /**
     * Parses an individual tool call JSON node into an {@link AssistantToolCall}.
     * Validates that the tool name matches a known {@link ToolDefinition}; throws
     * an {@link IllegalStateException} if it does not.
     *
     * @param toolDefinitionMapByName a map of tool names to their definitions
     * @param toolCallNode            the JSON node representing a tool use block
     * @param index                   the sequential index of this tool call in the response
     * @return a fully populated {@link AssistantToolCall} instance
     * @throws IllegalStateException if the tool name is not found in the definitions
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * Converts a raw streamed response chunk (identified by its
     * {@link StreamDataType}) into a publisher of {@link StreamResponse}
     * containing the appropriate data chunk type. For example,
     * {@code ANSWER_CONTENT} yields a {@link TextStreamDataChunk},
     * {@code TOOL_CALL} yields a {@link ToolCallStreamDataChunk}, etc.
     * Unrecognized data types are wrapped as {@link RawStreamDataChunk}
     * and a warning is logged.
     */
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
                JsonNode roleNode = dataContent.at("/message/role");
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
                JsonNode contentNode = dataContent.at("/content_block/text");
                JsonNode deltaNode = dataContent.at("/delta/text");
                if (!contentNode.isMissingNode() && !contentNode.isNull() && contentNode.isTextual()) {
                    chunkBuilder.value(contentNode.asText());
                } else if (!deltaNode.isMissingNode() && !deltaNode.isNull() && deltaNode.isTextual()) {
                    chunkBuilder.value(deltaNode.asText());
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
                JsonNode finishReasonNode = dataContent.at("/stop_reason");
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

    /**
     * Returns a string representation of this delegate, including the provider
     * information, base URL, and endpoint.
     *
     * @return a descriptive string
     */
    @Override
    public String toString() {
        return "AnthropicChatProviderDelegate{" +
                "llmProviderInfo=" + llmProviderInfo +
                ", baseUrL='" + baseUrL + '\'' +
                ", chatCompletionEndpoint='" + chatCompletionEndpoint + '\'' +
                '}';
    }

    /**
     * Builds a fully populated {@link AnthropicChatRequest} from the generic
     * {@link LlmChatRequestData}. This involves translating system messages,
     * user/media messages, historical messages, tool definitions, and various
     * configuration options (temperature, top‑P, reasoning, structured output,
     * etc.) into the equivalent Anthropic structures.
     *
     * @param llmChatRequestData the high‑level request data
     * @return a ready‑to‑use Anthropic chat request object
     */
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
                    if (!toolChoice.isBlank()) {
                        return ToolChoice.tool(toolChoice);
                    }
                    log.warn("Unrecognized tool choice: {}", toolChoice);
                    return null;
                })
                .ifPresent(anthropicChatRequestBuilder::toolChoice);
        var functionTools = buildFunctionTools(llmChatRequestData.getToolDefinitions());
        boolean isEmptyTools = Objects.isNull(functionTools) || functionTools.isEmpty();
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
                .tools(isEmptyTools ? null : functionTools)
                .build();
    }

    /**
     * Converts a list of {@link ToolDefinition} objects into a list of Anthropic
     * {@link Tool} objects, mapping each definition's name, description, input schema,
     * and strictness.
     *
     * @param toolDefinitions the list of available tool definitions
     * @return a list of Anthropic tools, or an empty list if the input is empty
     */
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

    /**
     * Builds a list of Anthropic chat messages representing tool result messages.
     * Each {@link ToolResultMessage} is wrapped into a {@link ToolResultBlock}
     * inside an Anthropic message with {@link Role#USER}.
     *
     * @param toolResultMessages the list of tool result messages
     * @return a list of Anthropic chat messages, or empty if the input is empty
     */
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

    /**
     * Converts a list of generic historical {@link Message} instances into the
     * corresponding Anthropic {@link AnthropicChatMessage} objects. Supports
     * {@link ToolCallMessage}, {@link ToolResultMessage}, {@link MediaMessage},
     * {@link TextMessage}, and {@link AssistantTextMessage}. If a message is of
     * an unrecognized type it is skipped (and a warning is logged).
     *
     * @param historicalMessages the list of previous conversation messages
     * @return a list of Anthropic chat messages, with unknown messages filtered out
     */
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
                        return this.buildMediaMessage(Role.fromValue(mediaMessage.getRole()), mediaMessage);
                    }
                    if (message instanceof TextMessage textMessage) {
                        return this.buildTextMessage(Role.fromValue(textMessage.getRole()), textMessage);
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

    /**
     * Builds an Anthropic chat message containing both the textual content and
     * any attached media (images) from a {@link MediaMessage}. The role is
     * determined by the provided {@link Role} argument.
     *
     * @param role         the role for the message (typically {@code USER})
     * @param mediaMessage the media message containing text and attachments
     * @return a fully constructed Anthropic chat message
     */
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

    /**
     * Builds a simple Anthropic chat message containing only a text block,
     * using the given role.
     *
     * @param role        the role for the message
     * @param textMessage the text message to convert
     * @return an Anthropic chat message with a single text content block
     */
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