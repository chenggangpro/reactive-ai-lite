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
package pro.chenggang.project.reactive.ai.lite.client.openai.provider.chat;

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
import pro.chenggang.project.reactive.ai.lite.client.openai.provider.OpenaiLlmProviderInfo;
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
 * Default implementation of {@link LlmChatProviderDelegate} for OpenAI chat completions.
 * <p>
 * Translates generic {@link LlmChatRequestData} into OpenAI-specific API requests,
 * handles streaming and non-streaming responses, extracts token usage, and reconciles tool calls.
 * This delegate centralizes the mapping between the framework’s unified message types and the
 * OpenAI chat message model, enabling seamless interaction with OpenAI’s chat completion endpoints.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class OpenaiChatProviderDelegate implements LlmChatProviderDelegate {

    /**
     * The unified LLM provider information representing this OpenAI chat provider instance.
     * <p>
     * Encapsulates metadata such as base URL, supported models, and authentication profiles,
     * which are used by the framework to route requests and display provider details.
     * </p>
     */
    private final LlmProviderInfo llmProviderInfo;

    /**
     * The base URL used for all API requests to the OpenAI chat service.
     * <p>
     * It serves as the common root for constructing request URIs, allowing different endpoints
     * (e.g., chat completions) to be appended without hardcoding the full URL.
     * </p>
     */
    private final String baseUrL;

    /**
     * The specific endpoint path appended to the base URL for executing chat completions.
     * <p>
     * This field separates the endpoint from the base URL, giving flexibility to switch paths
     * or support multiple API versions if needed.
     * </p>
     */
    private final String chatCompletionEndpoint;

    /**
     * The configured {@link WebClient} instance responsible for executing HTTP requests to the OpenAI API.
     * <p>
     * Pre-built with the base URL, it is reused across all chat completion invocations to
     * benefit from connection pooling and consistent configuration.
     * </p>
     */
    private final WebClient webClient;

    /**
     * Constructs a new {@code OpenaiChatProviderDelegate} with the given configuration.
     * <p>
     * Uses Lombok's {@link Builder} to create the instance. The constructor assembles the
     * {@link LlmProviderInfo} and pre‑configures the {@link WebClient} with the supplied base URL.
     * </p>
     *
     * @param webClientBuilder       the {@link WebClient.Builder} used to create the underlying HTTP client;
     *                               must not be {@code null}
     * @param baseUrL                the base URL for the OpenAI API; must not be {@code null}
     * @param chatCompletionEndpoint the endpoint path for chat completions; must not be {@code null}
     * @param isDefault              when {@code true}, this provider is marked as the default chat provider
     * @param name                   the internal name identifying this provider instance; must not be {@code null}
     * @param supportedModels        the set of OpenAI model identifiers supported by this provider
     * @param certifications         the list of valid {@link TokenCertification} implementations for
     *                               authenticating API requests; must not be {@code null}
     */
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

    /**
     * Returns the provider metadata for this OpenAI chat implementation.
     * <p>
     * This includes the base URL, supported models, authentication profiles, and a flag
     * indicating whether it is the default provider. Used by the framework to display
     * provider details and select the appropriate delegate.
     * </p>
     *
     * @return the {@link LlmProviderInfo} that describes this provider
     */
    @Override
    public LlmProviderInfo providerInfo() {
        return this.llmProviderInfo;
    }

    /**
     * Extracts the number of prompt tokens used from the raw usage JSON data.
     * <p>
     * This method navigates to the {@code prompt_tokens} field in the OpenAI response's
     * {@code usage} object. If the field is present and numeric, its integer value is returned;
     * otherwise, {@code 0} is returned to ensure token accounting doesn't break.
     * </p>
     *
     * @param rawUsage the raw JSON object representing the usage statistics (cannot be {@code null})
     * @return the number of prompt tokens consumed, or 0 if the field is missing or invalid
     */
    protected Integer extractPromptTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/prompt_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    /**
     * Extracts the number of completion tokens used from the raw usage JSON data.
     * <p>
     * This field corresponds to the tokens generated by the model as part of the answer.
     * If the {@code completion_tokens} field is missing or not a valid integer, this method
     * returns 0 to avoid null token statistics.
     * </p>
     *
     * @param rawUsage the raw JSON object representing the usage statistics (cannot be {@code null})
     * @return the number of completion tokens consumed, or 0 if the field is missing or invalid
     */
    protected Integer extractCompletionTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/completion_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    /**
     * Extracts any other token usage not accounted for by prompt and completion tokens.
     * <p>
     * OpenAI's usage object provides a {@code total_tokens} field. By subtracting the
     * previously extracted prompt and completion tokens from the total, we derive the number
     * of tokens spent on other activities (e.g., tool call overhead). This ensures that
     * the sum of the three categories always matches the reported total.
     * </p>
     *
     * @param rawUsage the raw JSON object representing the usage statistics (cannot be {@code null})
     * @return the number of other tokens consumed; guaranteed to be non-negative
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
     * Initializes the JSON request body representation from the generic chat request data.
     * <p>
     * Converts the unified {@link LlmChatRequestData} into an OpenAI-specific
     * {@link OpenaiChatRequest} using {@link #buildRequest(LlmChatRequestData)} and then
     * serializes it to an {@link ObjectNode} for inclusion in the HTTP request body.
     * </p>
     *
     * @param llmChatRequestData the unified chat request data; must not be {@code null}
     * @return the constructed JSON request body as an {@link ObjectNode}
     */
    @Override
    public ObjectNode initializeRequestBody(@NonNull LlmChatRequestData llmChatRequestData) {
        return OBJECT_MAPPER.valueToTree(this.buildRequest(llmChatRequestData));
    }

    /**
     * Extracts categorized stream chunks from a raw JSON streaming response.
     * <p>
     * Each incoming SSE event is parsed to determine its nature:
     * <ul>
     *   <li>A {@code usage} object signals token usage updates.</li>
     *   <li>A {@code finish_reason} node indicates stream completion.</li>
     *   <li>The {@code delta} object may carry a role, reasoning content, textual answer,
     *       or tool call definitions.</li>
     * </ul>
     * This method returns an array of {@link JsonStreamChunkSlide} objects, each tagged with
     * a {@link StreamDataType} to drive downstream processing.
     * </p>
     *
     * @param jsonChunkParsingData the raw JSON chunk data containing delta or usage info
     * @return an array of extracted stream chunk slides
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
     * Merges a series of streaming tool call chunks into a single unified JSON representation.
     * <p>
     * OpenAI streams tool call information across multiple delta events, each carrying an index
     * and partial data (id, type, function name, function arguments). This method reassembles
     * these fragments by accumulating them into a list of tool call objects keyed by their index.
     * If {@code distinctToolCalls} is {@code true}, duplicate tool calls (based on function name)
     * are filtered out to avoid redundant executions.
     * </p>
     *
     * @param rawToolCallMessages the list of accumulated raw tool call message chunks
     * @param distinctToolCalls   whether to filter out duplicate tool calls by name
     * @return the merged tool calls enclosed in an {@link ObjectNode} under the "tool_calls" key
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
     * Helper method to merge a specific field from a new JSON node into an existing one.
     * <p>
     * When streaming tool call arguments, each delta carries a fragment of a field value
     * (e.g., the function name or the arguments string). This method concatenates those
     * fragments by appending the new value to the existing content. If the field does not
     * yet exist in the accumulator, it is simply set.
     * </p>
     *
     * @param existNode the existing accumulated JSON node
     * @param newNode   the incoming delta JSON node
     * @param fieldName the specific field name to merge
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
     * Extracts a unified {@link GeneralResponse} from a standard (non‑streaming) raw response.
     * <p>
     * The method parses the OpenAI response JSON to locate the assistant’s message, token usage,
     * and optional tool calls. If a tool call array is present, it is converted into a
     * {@link ToolCallMessage} using {@link #parseToolCallList}; otherwise a plain
     * {@link AssistantTextMessage} is created. The assembled response includes the original
     * execution context and the raw response body for traceability.
     * </p>
     *
     * @param toolDefinitions the available tool definitions applicable for this request; must not be {@code null}
     * @param rawResponse     the raw HTTP response; must not be {@code null}
     * @return a {@link Mono} emitting the normalized {@link GeneralResponse}
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
     * Parses an array of raw JSON tool calls into internal {@link AssistantToolCall} representations.
     * <p>
     * Each element in the JSON array is expected to contain an {@code id}, a {@code type},
     * and a {@code function} sub‑object with {@code name} and {@code arguments}. The tool name
     * is validated against the provided {@code toolDefinitions}; an {@link IllegalStateException}
     * is thrown if an unrecognized tool is referenced. The resulting list preserves the original
     * order and indices from the API response.
     * </p>
     *
     * @param toolDefinitions    the list of allowed tool definitions for cross‑referencing; must not be {@code null}
     * @param toolCallsArrayNode the JSON array node containing the tool calls; must not be {@code null}
     * @return a list of parsed tool calls, each mapped to its corresponding {@link ToolDefinition}
     * @throws IllegalStateException if an unknown tool is referenced
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
     * Extracts categorized stream data chunks from a single raw stream response.
     * <p>
     * Based on the {@link StreamDataType} of the incoming fragment, this method translates
     * the raw JSON content into a domain‑specific chunk implementation (e.g.,
     * {@link TextStreamDataChunk} for role/content/reasoning/finish events,
     * {@link ToolCallStreamDataChunk} for tool call fragments, or
     * {@link UsageStreamDataChunk} for usage updates). Unrecognized types are returned as
     * {@link RawStreamDataChunk} for inspection, while fully unsupported types are silently
     * dropped.
     * </p>
     *
     * @param toolDefinitions   the available tools for parsing tool calls if any; must not be {@code null}
     * @param rawStreamResponse the individual parsed stream response fragment; must not be {@code null}
     * @return a {@link Publisher} emitting the processed {@link StreamResponse}
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
     * Assembles the Spring {@link WebClient} request body specification.
     * <p>
     * Configures the HTTP {@code POST} request with:
     * <ul>
     *   <li>the chat completion endpoint path</li>
     *   <li>application/json content type</li>
     *   <li>appropriate {@code Accept} header (text/event-stream for streaming, application/json otherwise)</li>
     *   <li>token certification via headers or query parameters, depending on the concrete {@link TokenCertification} type</li>
     * </ul>
     * This method does not set the request body; the caller is responsible for supplying it
     * and executing the request. The authentication logic ensures that only one certification
     * method is applied (bearer token > organization token > URI parameter), with a warning
     * logged if none is applied.
     * </p>
     *
     * @param llmChatRequestData the core chat request configuration; must not be {@code null}
     * @return the fully configured {@link RequestBodySpec} ready for body insertion and execution
     * @throws IllegalStateException if no {@link TokenCertification} is present in the request data
     */
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


    /**
     * Returns a string representation of this delegate, encapsulating key properties.
     * <p>
     * Useful for logging and debugging purposes to quickly inspect provider configuration.
     * </p>
     *
     * @return string format of the provider configuration
     */
    @Override
    public String toString() {
        return "OpenaiChatProviderDelegate{" +
                "llmProviderInfo=" + llmProviderInfo +
                ", baseUrL='" + baseUrL + '\'' +
                ", chatCompletionEndpoint='" + chatCompletionEndpoint + '\'' +
                '}';
    }

    /**
     * Maps the generalized {@link LlmChatRequestData} into the OpenAI‑specific request payload.
     * <p>
     * This method carries out the following conversions:
     * <ul>
     *   <li>Sets the model name, temperature, top‑P, and other inference parameters.</li>
     *   <li>Configures response format: plain text, or JSON schema when structured output is
     *       requested (either via a pre‑supplied schema or by auto‑generating one from a Java type).</li>
     *   <li>Builds the message array from system, historical, tool, and user messages,
     *       applying the appropriate OpenAI role and content structure.</li>
     *   <li>Translates tool definitions into {@link FunctionTool} objects and sets them
     *       on the request, unless no tools are provided.</li>
     *   <li>Enables parallel tool calls and applies the tool‑choice policy if specified.</li>
     * </ul>
     * The resulting {@link OpenaiChatRequest} is ready to be serialised and sent to the API.
     * </p>
     *
     * @param llmChatRequestData the generalized chat request parameters; must not be {@code null}
     * @return the specialised {@link OpenaiChatRequest} for OpenAI endpoints
     */
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
        boolean isEmptyTools = Objects.isNull(functionTools) || functionTools.isEmpty();
        llmChatRequestData.getToolChoice().ifPresent(toolChoice -> {
            if (isEmptyTools) {
                openaiChatRequestBuilder.toolChoice("none");
                return;
            }
            if (toolChoice.equalsIgnoreCase("none") || toolChoice.equalsIgnoreCase("auto") || toolChoice.equalsIgnoreCase("required")) {
                openaiChatRequestBuilder.toolChoice(toolChoice);
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
                                openaiChatRequestBuilder.toolChoice(nameOnlyFunctionTool);
                            }
                            , () -> {
                                log.warn("No tool found for name: {}", toolChoice);
                            }
                    );
        });
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
                .tools(isEmptyTools ? null : functionTools)
                .parallelToolCalls(true)
                .build();
    }

    /**
     * Extracts a normalized, safe type name string from a given reflection {@link java.lang.reflect.Type}.
     * <p>
     * When the framework generates a JSON schema automatically for structured output, the schema
     * name must be a valid identifier. This method strips package prefixes and generic type
     * parameters, replacing nesting symbols with underscores. The resulting string is guaranteed
     * to not exceed 64 characters (the maximum allowed by OpenAI) after truncation in
     * {@link #buildRequest(LlmChatRequestData)}.
     * </p>
     *
     * @param type the Java type definition
     * @return a sanitized string representing the type name, suitable for a JSON schema name
     */
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

    /**
     * Converts generalized {@link ToolDefinition} instances into OpenAI‑specific {@link FunctionTool} objects.
     * <p>
     * Each tool definition is mapped to an OpenAI function tool retaining the original name,
     * description, parameters (as a JSON‑friendly map), and strict flag. An empty list is
     * returned if no tools are defined to avoid sending an empty {@code tools} array.
     * </p>
     *
     * @param toolDefinitions the list of generalized tool definitions; must not be {@code null}
     * @return the list of mapped OpenAI function tools, never {@code null}
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
     * Maps generalized {@link ToolResultMessage}s into OpenAI’s {@link ChatCompletionMessage} format.
     * <p>
     * Each tool result is assigned the {@code TOOL} role and includes the tool call ID and the
     * serialized result content. This ensures the assistant can correctly correlate the result
     * with the preceding tool call.
     * </p>
     *
     * @param toolResultMessages the list of tool execution results; must not be {@code null}
     * @return a list of chat completion messages categorized as TOOL role, never {@code null}
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
     * Maps a generalized {@link TextMessage} meant as a system instruction into an OpenAI system message.
     * <p>
     * The {@code SYSTEM} role message sets the overall behaviour and instructions for the assistant.
     * </p>
     *
     * @param textMessage the system text content; must not be {@code null}
     * @return the equivalent OpenAI system chat completion message
     */
    protected ChatCompletionMessage buildSystemMessage(TextMessage textMessage) {
        return ChatCompletionMessage.builder()
                .role(Role.SYSTEM)
                .rawContent(textMessage.getContent())
                .build();
    }

    /**
     * Processes generalized historical messages into OpenAI‑compatible message structures.
     * <p>
     * Iterates over the conversation history and maps each {@link Message} to the appropriate
     * OpenAI role and content format:
     * <ul>
     *   <li>{@link ToolCallMessage} → ASSISTANT with tool calls</li>
     *   <li>{@link ToolResultMessage} → TOOL with result content</li>
     *   <li>{@link MediaMessage} → USER/ASSISTANT with multimodal content</li>
     *   <li>{@link TextMessage} → simple text message in the given role</li>
     *   <li>{@link AssistantTextMessage} → ASSISTANT with text and possibly reasoning</li>
     * </ul>
     * Any unhandled message type is logged and excluded from the resulting list.
     * </p>
     *
     * @param historicalMessages the conversation history; may be empty
     * @return the ordered list of OpenAI chat completion messages, never {@code null}
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

    /**
     * Maps a generalized {@link MediaMessage} (e.g., images, audio, documents) to the OpenAI message format.
     * <p>
     * The method builds a structured content array consisting of:
     * <ul>
     *   <li>A text part from the media message’s textual content.</li>
     *   <li>Image parts for {@link UrlAttachment}s whose MIME type starts with {@code image/}.</li>
     *   <li>Audio parts for {@link Base64Attachment}s with supported audio formats ({@code mp3}, {@code wav}).</li>
     *   <li>File parts (input_file) for any other base‑64 encoded binary data.</li>
     * </ul>
     * Unsupported attachment types are logged and skipped.
     * </p>
     *
     * @param role         the designated role (e.g., {@code USER}); must not be {@code null}
     * @param mediaMessage the media message containing various file attachments; must not be {@code null}
     * @return the assembled OpenAI message accommodating multimedia content
     */
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

    /**
     * Maps a simple generalized {@link TextMessage} to the standard OpenAI text message format.
     * <p>
     * Only the textual content is transferred; no multimedia or tool data is involved.
     * </p>
     *
     * @param role        the associated role (e.g., {@code USER}, {@code ASSISTANT}); must not be {@code null}
     * @param textMessage the text content; must not be {@code null}
     * @return the resulting chat completion message
     */
    protected ChatCompletionMessage buildTextMessage(Role role, TextMessage textMessage) {
        return ChatCompletionMessage.builder()
                .role(role)
                .rawContent(textMessage.getContent())
                .build();
    }

}