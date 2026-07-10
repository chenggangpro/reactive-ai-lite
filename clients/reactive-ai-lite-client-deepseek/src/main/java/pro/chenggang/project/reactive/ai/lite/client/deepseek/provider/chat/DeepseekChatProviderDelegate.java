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
package pro.chenggang.project.reactive.ai.lite.client.deepseek.provider.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.ChatCompletionMessage;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.ChatCompletionMessage.ChatCompletionFunction;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.ChatCompletionMessage.ToolCall;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.DeepseekChatRequest;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.DeepseekChatRequest.Thinking;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.FunctionTool;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.FunctionTool.Function;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.ResponseFormat;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.ResponseFormat.JsonSchema;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.ResponseFormat.Type;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.provider.DeepseekLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
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

import static pro.chenggang.project.reactive.ai.lite.client.deepseek.dto.DeepseekChatRequest.StreamOptions.INCLUDE_USAGE;
import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * The default DeepSeek chat provider implementation that translates internal
 * chat request data into DeepSeek-specific API calls and converts responses back.
 * <p>
 * This class encapsulates all the logic required to interact with the DeepSeek
 * chat completion API, including building request JSON bodies, handling streaming
 * and non-streaming responses, extracting usage information, and merging tool call
 * chunks across multiple streamed events.
 * <p>
 * It uses {@link WebClient} for HTTP communication and configures the provider's
 * metadata via {@link DeepseekLlmProviderInfo}.
 * <p>
 * The constructor is designed to be called via a Lombok {@link Builder}, which
 * allows injecting dependencies such as the {@link WebClient.Builder}, base URL,
 * endpoint path, authentication certifications, and provider identity.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class DeepseekChatProviderDelegate implements LlmChatProviderDelegate {

    /**
     * Metadata about this LLM provider (name, models, base URL, etc.) that may be
     * exposed externally (e.g., for diagnostics or provider selection).
     */
    private final LlmProviderInfo llmProviderInfo;

    /**
     * The base URL of the DeepSeek API, used to construct absolute request URIs.
     */
    private final String baseUrL;

    /**
     * The path segment of the chat completion endpoint (e.g., <code>/v1/chat/completions</code>)
     * appended to {@link #baseUrL}.
     */
    private final String chatCompletionEndpoint;

    /**
     * The reactive HTTP client configured with the base URL and used to execute all
     * outgoing requests.
     */
    private final WebClient webClient;

    /**
     * Constructs a {@code DeepseekChatProviderDelegate} using the provided hard‑coded
     * dependencies.
     * <p>
     * The builder pattern allows the `applicationContext` or configuration layer to
     * supply the {@code WebClient.Builder}, the base URL, the endpoint, and the list
     * of {@link TokenCertification} implementations that will be applied to every request.
     * It also records whether this provider is the default and what models it supports.
     *
     * @param webClientBuilder    a pre‑configured {@link WebClient.Builder} (usually from
     *                            the Spring WebFlux auto‑configuration)
     * @param baseUrL            the root URL of the DeepSeek API (e.g., {@code https://api.deepseek.com})
     * @param chatCompletionEndpoint the relative path for chat completions (e.g., {@code /v1/chat/completions})
     * @param isDefault           whether this provider should be treated as the default
     *                            in a multi‑provider setup
     * @param name                a unique symbolic name for this provider instance
     * @param supportedModels     the set of model identifiers that this provider can serve
     * @param certifications      a list of token‑based authentication mechanisms that will be
     *                            attached to each HTTP request
     */
    @Builder
    private DeepseekChatProviderDelegate(@NonNull WebClient.Builder webClientBuilder,
                                         @NonNull String baseUrL,
                                         @NonNull String chatCompletionEndpoint,
                                         boolean isDefault,
                                         @NonNull String name,
                                         Set<String> supportedModels,
                                         @NonNull List<TokenCertification> certifications) {
        this.baseUrL = baseUrL;
        this.chatCompletionEndpoint = chatCompletionEndpoint;
        this.webClient = webClientBuilder.baseUrl(baseUrL).build();
        this.llmProviderInfo = DeepseekLlmProviderInfo.builder()
                .isDefault(isDefault)
                .name(name)
                .supportedModels(supportedModels)
                .profiles(certifications.stream().map(TokenCertification::profile).collect(Collectors.toSet()))
                .baseUrl(baseUrL)
                .endpoint(chatCompletionEndpoint)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LlmProviderInfo providerInfo() {
        return this.llmProviderInfo;
    }

    /**
     * Extracts the number of prompt tokens from a DeepSeek usage object.
     * <p>
     * Looks for the JSON path {@code /prompt_tokens} and returns its integer value
     * if present and numeric; otherwise returns {@code 0}.
     *
     * @param rawUsage the {@code usage} object from a DeepSeek response
     * @return the prompt token count, or 0 if not available
     */
    protected Integer extractPromptTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/prompt_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    /**
     * Extracts the number of completion tokens from a DeepSeek usage object.
     * <p>
     * Looks for the JSON path {@code /completion_tokens} and returns its integer value
     * if present and numeric; otherwise returns {@code 0}.
     *
     * @param rawUsage the {@code usage} object from a DeepSeek response
     * @return the completion token count, or 0 if not available
     */
    protected Integer extractCompletionTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/completion_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    /**
     * Computes the "other" token usage as the total token count minus prompt and
     * completion tokens.
     * <p>
     * If the total token count is missing or not numeric, the result defaults to 0,
     * which may mean the other category is effectively 0 (or negative in edge cases,
     * though that is highly unlikely).
     *
     * @param rawUsage the {@code usage} object from a DeepSeek response
     * @return the computed other token count
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * Constructs a {@link RequestBodySpec} that points to the configured
     * {@code chatCompletionEndpoint}. If the request contains an
     * {@link UriTokenCertification} it will be applied directly to the URI builder
     * (e.g., to add query‑parameter‑based authentication). Then any standard
     * {@link TokenCertification} headers are applied to the HTTP request.
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
                });
        llmChatRequestData.getTokenCertification().ifPresent(cert -> applyStandardTokenCertification(requestBodySpec, cert));
        return requestBodySpec;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Builds the DeepSeek request DTO from the internal {@link LlmChatRequestData}
     * and converts it into a Jackson {@link ObjectNode} that can be sent as the
     * HTTP request body.
     */
    @Override
    public ObjectNode initializeRequestBody(@NonNull LlmChatRequestData llmChatRequestData) {
        return OBJECT_MAPPER.valueToTree(this.buildRequest(llmChatRequestData));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Determines the type of each streaming chunk by inspecting the JSON structure
     * of the raw SSE event. The DeepSeek API does not provide a dedicated event
     * type field; chunk identification relies on the presence of specific keys:
     * <ul>
     *   <li>{@code /usage} → {@link StreamDataType#USAGE}</li>
     *   <li>{@code /choices/0/finish_reason} (present and non‑null) → {@link StreamDataType#FINISHED}</li>
     *   <li>{@code /choices/0/delta/role} → {@link StreamDataType#ROLE}</li>
     *   <li>{@code /choices/0/delta/reasoning_content} → {@link StreamDataType#REASONING_CONTENT}</li>
     *   <li>{@code /choices/0/delta/content} → {@link StreamDataType#ANSWER_CONTENT}</li>
     *   <li>{@code /choices/0/delta/tool_calls} (array) → {@link StreamDataType#TOOL_CALL}</li>
     * </ul>
     * If none of these match, the chunk is labeled {@link StreamDataType#UNKNOWN}.
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * Aggregates multiple separate SSE tool‑call chunks that belong to different
     * indices of a single assistant turn. The DeepSeek API streams tool call details
     * incrementally (id, type, function name, function arguments) across multiple
     * events. This method merges them into coherent {@link ObjectNode} representations,
     * optionally removing duplicate tool calls based on function name when
     * {@code distinctToolCalls} is {@code true}.
     */
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

    /**
     * Merges a single value from a new node into an existing node for the specified field.
     * <p>
     * If the field already exists in the existing node, the new value is appended
     * (concatenated) to the old value. Otherwise, the field is set to the new value.
     * This is used to accumulate incremental tool call fragments (e.g., function
     * arguments arriving in multiple pieces).
     *
     * @param existNode the existing node to update
     * @param newNode   the node providing the new value
     * @param fieldName the JSON field name to merge
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * Converts the raw DeepSeek JSON response into a {@link GeneralResponse}. This
     * includes parsing the assistant message, optional reasoning content, tool calls,
     * and usage statistics. If the response contains tool calls, they are matched
     * against the provided {@code toolDefinitions} to enrich the result with tool
     * metadata. If parsing fails, an error is propagated via the reactive sink.
     */
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

    /**
     * Parses a JSON array of tool calls from a DeepSeek response into a list of
     * {@link AssistantToolCall} objects, matching each tool by name to a
     * {@link ToolDefinition} from the provided list.
     * <p>
     * Tool definitions are indexed by name for fast lookup. If a tool call references
     * a function not present in the definitions, an {@link IllegalStateException}
     * is thrown, because the invocation would be unsafe.
     *
     * @param toolDefinitions     the list of tool definitions that describe available tools
     * @param toolCallsArrayNode  the JSON array of tool call objects from the response
     * @return a list of parsed and validated assistant tool calls
     * @throws IllegalStateException if a tool call's function name is not in the definitions
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * Translates each streamed chunk (identified by its {@link StreamDataType})
     * into a corresponding {@link StreamResponse} publisher. For example:
     * <ul>
     *   <li>{@link StreamDataType#ROLE} → a {@link TextStreamDataChunk} with the role string</li>
     *   <li>{@link StreamDataType#ANSWER_CONTENT} → a {@link TextStreamDataChunk} with the delta content</li>
     *   <li>{@link StreamDataType#REASONING_CONTENT} → a {@link TextStreamDataChunk} with the reasoning delta</li>
     *   <li>{@link StreamDataType#TOOL_CALL} → a {@link ToolCallStreamDataChunk} containing parsed tool calls (merged earlier)</li>
     *   <li>{@link StreamDataType#USAGE} → a {@link UsageStreamDataChunk} with token usage info</li>
     *   <li>{@link StreamDataType#FINISHED} → a {@link TextStreamDataChunk} with the finish reason</li>
     *   <li>{@link StreamDataType#UNKNOWN} → a raw {@link RawStreamDataChunk} for debugging</li>
     * </ul>
     * If the chunk type is unsupported, an empty {@link Mono} is returned to skip it gracefully.
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "DeepseekChatProviderDelegate{" +
                "llmProviderInfo=" + llmProviderInfo +
                ", baseUrL='" + baseUrL + '\'' +
                ", chatCompletionEndpoint='" + chatCompletionEndpoint + '\'' +
                '}';
    }

    /**
     * Builds a {@link DeepseekChatRequest} object that represents the full API call
     * payload, based on the generic {@link LlmChatRequestData} and DeepSeek‑specific
     * options such as stream options, reasoning mode, and function tools.
     * <p>
     * The method handles:
     * <ul>
     *   <li>model selection</li>
     *   <li>response format (text vs. JSON schema / structured output)</li>
     *   <li>temperature, top_p, max_tokens</li>
     *   <li>stream and <code>stream_options</code> when usage inclusion is requested</li>
     *   <li>reasoning effort through the <code>thinking</code> and <code>reasoning_effort</code> fields</li>
     *   <li>tool definitions and tool choice</li>
     *   <li>construction of the message list: system, historical, tool results, and current user message</li>
     * </ul>
     *
     * @param llmChatRequestData the internal request data
     * @return a fully populated {@link DeepseekChatRequest} ready for serialization
     */
    protected DeepseekChatRequest buildRequest(LlmChatRequestData llmChatRequestData) {
        var deepseekChatRequestBuilder = DeepseekChatRequest.builder()
                .model(llmChatRequestData.getModelName());
        if (llmChatRequestData.getResponseJsonSchema().isEmpty() && llmChatRequestData.getStructuredOutputType().isEmpty()) {
            deepseekChatRequestBuilder.responseFormat(ResponseFormat.builder()
                    .type(Type.TEXT)
                    .build()
            );
        } else {
            Map<String, Object> jsonSchemaMap = Map.of();
            if (llmChatRequestData.getResponseJsonSchema().isPresent()) {
                jsonSchemaMap = JsonRelatedUtil.jsonToMap(llmChatRequestData.getResponseJsonSchema().get());
            } else if (llmChatRequestData.getStructuredOutputType().isPresent()) {
                var structuredOutputType = llmChatRequestData.getStructuredOutputType().get();
                jsonSchemaMap = JsonRelatedUtil.jsonToMap(JsonSchemaUtil.generateForType(structuredOutputType));
            }
            deepseekChatRequestBuilder.responseFormat(ResponseFormat.builder()
                    .type(Type.JSON_OBJECT)
                    .jsonSchema(JsonSchema.builder()
                            .schema(jsonSchemaMap)
                            .strict(true)
                            .build())
                    .build()
            );
        }
        llmChatRequestData.getTemperature().ifPresent(deepseekChatRequestBuilder::temperature);
        llmChatRequestData.getTopP().ifPresent(deepseekChatRequestBuilder::topP);
        if (llmChatRequestData.isStream() && llmChatRequestData.isIncludeUsage()) {
            deepseekChatRequestBuilder.streamOptions(INCLUDE_USAGE);
        }
        llmChatRequestData.getReasoning().ifPresent(reasoning -> {
            String[] deepseekReasoning = reasoning.split(":");
            String enabledOrDisabled = deepseekReasoning[0];
            if ("true".equalsIgnoreCase(enabledOrDisabled) || "enabled".equalsIgnoreCase(enabledOrDisabled) || "1".equalsIgnoreCase(enabledOrDisabled)) {
                deepseekChatRequestBuilder.thinking(Thinking.ENABLED);
                if (deepseekReasoning.length > 1) {
                    deepseekChatRequestBuilder.reasoningEffort(deepseekReasoning[1]);
                } else {
                    log.debug("No reasoning effort configured in thinking.");
                }
            } else if ("false".equalsIgnoreCase(enabledOrDisabled) || "disabled".equalsIgnoreCase(enabledOrDisabled) || "0".equalsIgnoreCase(enabledOrDisabled)) {
                deepseekChatRequestBuilder.thinking(Thinking.DISABLED);
            } else {
                log.warn("Invalid reasoning and reasoning effort value: {}", reasoning);
            }
        });
        llmChatRequestData.getMaxCompletionTokens().ifPresent(deepseekChatRequestBuilder::maxTokens);
        var functionTools = buildFunctionTools(llmChatRequestData.getToolDefinitions());
        boolean isEmptyTools = Objects.isNull(functionTools) || functionTools.isEmpty();
        llmChatRequestData.getToolChoice().ifPresent(toolChoice -> {
            if (isEmptyTools) {
                deepseekChatRequestBuilder.toolChoice("none");
                return;
            }
            if (toolChoice.equalsIgnoreCase("none") || toolChoice.equalsIgnoreCase("auto") || toolChoice.equalsIgnoreCase("required")) {
                deepseekChatRequestBuilder.toolChoice(toolChoice);
                return;
            }
            functionTools.stream()
                    .filter(functionTool -> toolChoice.equalsIgnoreCase(functionTool.getFunction().getName()))
                    .findFirst()
                    .ifPresentOrElse(
                            functionTool -> {
                                FunctionTool nameOnlyFunctionTool = FunctionTool.builder()
                                        .type(functionTool.getType())
                                        .function(Function.builder()
                                                .name(functionTool.getFunction().getName())
                                                .build()
                                        )
                                        .build();
                                deepseekChatRequestBuilder.toolChoice(nameOnlyFunctionTool);
                            }
                            , () -> {
                                log.warn("No tool found for name: {}", toolChoice);
                            }
                    );
        });
        var systemMessage = buildSystemMessage(llmChatRequestData.getSystemMessage());
        var userMessage = this.buildTextMessage(Role.USER, llmChatRequestData.getUserTextMessage());
        llmChatRequestData.getUserMediaMessage()
                .ifPresent(mediaMessage -> {
                    log.warn("Media message is not supported, only text messages are supported. Skipping media message: {}", mediaMessage);
                });
        var historicalMessages = buildHistoricalMessages(llmChatRequestData.getHistoricalMessages());
        var toolMessages = buildToolMessages(llmChatRequestData.getToolResultMessages());
        var allMessages = Stream.of(Stream.of(systemMessage),
                        historicalMessages.stream(),
                        toolMessages.stream(),
                        toolMessages.isEmpty() ? Stream.of(userMessage) : Stream.<ChatCompletionMessage>empty()
                )
                .flatMap(java.util.function.Function.identity())
                .toList();
        return deepseekChatRequestBuilder
                .stream(llmChatRequestData.isStream())
                .messages(allMessages)
                .tools(isEmptyTools ? null : functionTools)
                .build();
    }

    /**
     * Converts a list of {@link ToolDefinition}s into the DeepSeek function‑tool
     * format expected by the API.
     * <p>
     * Each tool definition carries a JSON schema describing its input parameters,
     * which is embedded directly. The {@code strict} flag is passed through to
     * enforce schema compliance if supported.
     *
     * @param toolDefinitions the list of tool definitions; if empty, an empty list is returned
     * @return the list of {@link FunctionTool} objects
     */
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

    /**
     * Transforms a list of {@link ToolResultMessage} instances into DeepSeek
     * {@link ChatCompletionMessage} objects with role <code>tool</code>.
     * <p>
     * Each message includes the tool call id and the raw content (tool result).
     *
     * @param toolResultMessages the list of tool result messages
     * @return the corresponding list of DeepSeek messages; empty if the input is empty
     */
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

    /**
     * Builds a system‑role {@link ChatCompletionMessage} from a {@link TextMessage}.
     *
     * @param textMessage the text message that constitutes the system prompt
     * @return the DeepSeek system message
     */
    protected ChatCompletionMessage buildSystemMessage(TextMessage textMessage) {
        return ChatCompletionMessage.builder()
                .role(Role.SYSTEM)
                .rawContent(textMessage.getContent())
                .build();
    }

    /**
     * Converts a list of historical {@link Message} objects into their DeepSeek
     * equivalents. The method distinguishes between text messages, assistant
     * messages (including those with tool calls), tool result messages, and
     * media messages (which are skipped with a warning because DeepSeek does not
     * currently support them).
     * <p>
     * Tool calls within assistant messages are converted into the
     * {@link ToolCall} format expected by DeepSeek.
     *
     * @param historicalMessages the list of generic historical messages
     * @return a filtered and transformed list of DeepSeek chat completion messages
     */
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
                        log.warn("Media message is not supported, only text messages are supported. Skipping media message: {}", mediaMessage);
                        return null;
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

    /**
     * Creates a simple {@link ChatCompletionMessage} for a given role and text.
     * <p>
     * This is used for both user messages and generic text messages in history.
     *
     * @param role        the role for the message (e.g., USER)
     * @param textMessage the text content
     * @return a DeepSeek message with the given role and content
     */
    protected ChatCompletionMessage buildTextMessage(Role role, TextMessage textMessage) {
        return ChatCompletionMessage.builder()
                .role(role)
                .rawContent(textMessage.getContent())
                .build();
    }

}