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
package pro.chenggang.project.reactive.ai.lite.client.ollama.provider.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.FunctionTool;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.FunctionTool.Function;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaChatMessage;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaChatMessage.ToolCall;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaChatMessage.ToolCall.ToolCallFunction;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaChatRequest;
import pro.chenggang.project.reactive.ai.lite.client.ollama.provider.OllamaLlmProviderInfo;
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

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * Default implementation of {@link LlmChatProviderDelegate} for the Ollama LLM provider.
 * This class handles the entire lifecycle of chatting with the Ollama API, including
 * building HTTP requests, parsing streaming and non-streaming responses, extracting token
 * usage, merging tool call chunks, and converting between the framework's internal message
 * model and Ollama's specific JSON protocol.
 * <p>
 * Ollama's chat completion API supports features such as structured JSON output, image
 * attachments (via base64), function/tool calls, reasoning content ("thinking"), and
 * streaming with per-chunk events. This delegate translates the framework's
 * {@link LlmChatRequestData} into an {@link OllamaChatRequest} and processes the raw
 * JSON responses accordingly.
 * <p>
 * No API key certification is required by default; the {@link #checkTokenCertification}
 * method simply logs this fact. Token certifications (e.g., via URI query parameters) are
 * handled when provided.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class OllamaChatProviderDelegate implements LlmChatProviderDelegate {

    /**
     * Metadata describing this provider instance, including base URL, endpoint,
     * supported models, and the default flag.
     */
    private final LlmProviderInfo llmProviderInfo;

    /**
     * The set of JSON field names in Ollama's API response that contain token usage
     * metrics. These are extracted from the root-level of the response and mapped into
     * a {@link Usage} object.
     * <p>
     * Ollama uses non-standard fields: {@code total_duration}, {@code load_duration},
     * {@code prompt_eval_count}, {@code prompt_eval_duration}, {@code eval_count},
     * {@code eval_duration}. Among these, only {@code prompt_eval_count} and
     * {@code eval_count} correspond to prompt and completion tokens respectively.
     */
    private final List<String> usageJsonField = List.of(
            "total_duration",
            "load_duration",
            "prompt_eval_count",
            "prompt_eval_duration",
            "eval_count",
            "eval_duration"
    );

    /**
     * The base URL of the Ollama API instance, e.g., {@code http://localhost:11434}.
     * This is used to initialize the {@link WebClient} and is stored for debugging.
     */
    private final String baseUrL;

    /**
     * The chat completion endpoint path relative to the base URL, typically {@code /api/chat}.
     */
    private final String chatCompletionEndpoint;

    /**
     * Pre-configured Spring WebClient used for all HTTP interactions with the Ollama server.
     */
    private final WebClient webClient;

    /**
     * Constructs a new OllamaChatProviderDelegate using the provided configuration.
     * The constructor is generated by Lombok's {@link Builder} annotation and is normally
     * invoked via the builder pattern. It initializes the provider metadata, stores the
     * endpoint and base URL, and builds a {@link WebClient} bound to the base URL.
     *
     * @param webClientBuilder    the Spring {@link WebClient.Builder} to configure
     * @param baseUrL             the Ollama API base URL (e.g. http://localhost:11434)
     * @param chatCompletionEndpoint the endpoint path for chat completions (e.g. /api/chat)
     * @param isDefault           whether this provider should be considered the default
     * @param name                a human-readable provider name
     * @param supportedModels     the set of model names this provider supports
     * @param certifications      list of token certifications to apply (URI or header-based)
     */
    @Builder
    protected OllamaChatProviderDelegate(@NonNull WebClient.Builder webClientBuilder,
                                 @NonNull String baseUrL,
                                 @NonNull String chatCompletionEndpoint,
                                 boolean isDefault,
                                 @NonNull String name,
                                 Set<String> supportedModels,
                                 @NonNull List<TokenCertification> certifications) {
        this.baseUrL = baseUrL;
        this.chatCompletionEndpoint = chatCompletionEndpoint;
        this.webClient = webClientBuilder.baseUrl(baseUrL).build();
        this.llmProviderInfo = OllamaLlmProviderInfo.builder()
                        .isDefault(isDefault)
                        .name(name)
                        .supportedModels(supportedModels)
                        .profiles(certifications.stream().map(TokenCertification::profile).collect(Collectors.toSet()))
                        .baseUrl(baseUrL)
                        .endpoint(chatCompletionEndpoint)
                        .build();
    }

    /**
     * Returns the immutable provider metadata, exposing base URL, supported models,
     * and other configuration.
     *
     * @return the provider information instance
     */
    @Override
    public LlmProviderInfo providerInfo() {
        return this.llmProviderInfo;
    }

    /**
     * Extracts the number of prompt tokens from an Ollama response usage node.
     * In Ollama, this corresponds to the {@code prompt_eval_count} field.
     * If the field is missing or not a number, 0 is returned.
     *
     * @param rawUsage a JSON object containing raw usage fields
     * @return the prompt token count, or 0
     */
    protected Integer extractPromptTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/prompt_eval_count");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    /**
     * Extracts the number of completion tokens from an Ollama response usage node.
     * In Ollama, this is the {@code eval_count} field.
     * If the field is missing or not a number, 0 is returned.
     *
     * @param rawUsage a JSON object containing raw usage fields
     * @return the completion token count, or 0
     */
    protected Integer extractCompletionTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/eval_count");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    /**
     * Ollama's API does not currently provide a separate "other" token count,
     * so this method always returns 0.
     *
     * @param rawUsage a JSON object containing raw usage fields
     * @return always 0
     */
    protected Integer extractOtherTokenUsage(ObjectNode rawUsage) {
        return 0;
    }

    /**
     * Prepares the HTTP request specification (POST to the chat endpoint) with
     * the appropriate URI and any token certifications. If a {@link UriTokenCertification}
     * is present, it is applied to the URI builder (e.g., appends an API key as a query parameter).
     * Other certifications are applied as standard request headers via {@link #applyStandardTokenCertification}.
     *
     * @param llmChatRequestData the request data containing token certification and context
     * @return the configured {@link RequestBodySpec} ready to receive the body
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
     * Builds the request body as a JSON tree from the {@link OllamaChatRequest} DTO.
     * This is the full payload that will be sent to the Ollama /api/chat endpoint.
     *
     * @param llmChatRequestData the request data with model, messages, tools, etc.
     * @return an {@link ObjectNode} representing the JSON request body
     */
    @Override
    public ObjectNode initializeRequestBody(@NonNull LlmChatRequestData llmChatRequestData) {
        return OBJECT_MAPPER.valueToTree(this.buildRequest(llmChatRequestData));
    }

    /**
     * Parses a single SSE chunk from Ollama's streaming response and classifies it into
     * one or more {@link JsonStreamChunkSlide} objects with a known {@link StreamDataType}.
     * <p>
     * The method first inspects the {@code /message} sub-object:
     * <ul>
     *   <li>If missing/invalid, returns an UNKNOWN chunk.</li>
     *   <li>If {@code /thinking} is present, classifies as REASONING_CONTENT.</li>
     *   <li>If {@code /content} is present, classifies as ANSWER_CONTENT.</li>
     *   <li>If {@code /tool_calls} is present and {@code done} is false, classifies as TOOL_CALL.</li>
     *   <li>If {@code /tool_calls} is present and {@code done} is true, it may produce a TOOL_CALL chunk
     *       with usage removed from the root, and a separate USAGE or FINISHED chunk depending on
     *       whether usage fields exist.</li>
     *   <li>If {@code done} is true and no tool calls, checks for usage fields and returns USAGE if present,
     *       otherwise UNKNOWN.</li>
     * </ul>
     * This careful logic is necessary because Ollama sometimes bundles usage metrics and the "done" flag
     * in the same chunk as the final tool call array, causing duplication.
     *
     * @param jsonChunkParsingData the parsed chunk data
     * @return an array of slides, each with a data type and content node
     */
    @Override
    public JsonStreamChunkSlide[] extractStreamChunks(@NonNull StreamResponseParser.JsonChunkParsingData jsonChunkParsingData) {
        // ... implementation unchanged
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

    /**
     * Merges a list of raw tool call message chunks (typically from streaming) into a single
     * aggregated JSON object containing a {@code tool_calls} array. This is required because
     * Ollama streams multiple chunks each containing a partial array of tool calls; they must be
     * combined into one final tool call request. When {@code distinctToolCalls} is true, duplicate
     * tool call entries (by function name) are removed, keeping only the first occurrence.
     *
     * @param rawToolCallMessages the raw message nodes (with {@code /message/tool_calls})
     * @param distinctToolCalls   if true, deduplicate tool calls by function name
     * @return an {@link ObjectNode} with a single {@code tool_calls} array containing all merged tool calls
     */
    @Override
    public ObjectNode mergeRawToolCallMessages(@NonNull List<ObjectNode> rawToolCallMessages, boolean distinctToolCalls) {
        // ... implementation unchanged
        if (rawToolCallMessages.size() == 1) {
            return rawToolCallMessages.getFirst();
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

    /**
     * Processes a complete non-streaming response from Ollama to produce a {@link GeneralResponse}.
     * It extracts the assistant's message (content, reasoning, and/or tool calls) and token usage
     * from the response body. If tool calls are present, a {@link ToolCallMessage} is created;
     * otherwise, an {@link AssistantTextMessage}.
     * <p>
     * The method first parses the {@code /message} subtree. If the response contains tool calls,
     * they are converted using {@link #parseToolCallList}. The root-level usage fields are collected
     * and converted into a {@link Usage} object using the token extractors.
     *
     * @param toolDefinitions the list of defined tools to validate tool call names against
     * @param rawResponse     the raw HTTP response container
     * @return a Mono emitting the assembled GeneralResponse
     */
    @Override
    public Mono<GeneralResponse> extractGeneralResponse(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse) {
        return Mono.fromCallable(rawResponse::getResponseBody)
                .handle((rawResponseBody, syncSink) -> {
                    var generalResponseBuilder = GeneralResponse.builder()
                            .executionContext(rawResponse.getExecutionContext())
                            .rawResponseBody(rawResponseBody);
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

    /**
     * Converts the raw JSON array of tool calls returned by Ollama into a list of
     * {@link AssistantToolCall} instances, validating that each tool name is present in the
     * provided {@code toolDefinitions}. Tool definitions are looked up by name, and the
     * matched {@link ToolDefinition} is embedded in the resulting {@code AssistantToolCall}
     * for further processing.
     *
     * @param toolDefinitions    the registered tool definitions
     * @param toolCallsArrayNode the JSON array of tool call objects from the response
     * @return a list of parsed assistant tool calls
     * @throws IllegalStateException if a tool name is not found in the definitions
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
                    .type("function")
                    .toolDefinition(toolDefinition)
                    .function(toolCallFunction)
                    .build();
            toolCallList.add(toolCall);
        }
        return List.copyOf(toolCallList);
    }

    /**
     * Processes a single classified streaming response chunk into the corresponding
     * {@link StreamResponse} with the appropriate data chunk type. This method handles
     * all supported {@link StreamDataType} values:
     * <ul>
     *   <li>UNKNOWN → wraps raw data in a {@link RawStreamDataChunk}.</li>
     *   <li>ROLE → extracts role from /message/role into a {@link TextStreamDataChunk}.</li>
     *   <li>ANSWER_CONTENT → extracts text from /message/content.</li>
     *   <li>REASONING_CONTENT → extracts thinking from /message/thinking.</li>
     *   <li>TOOL_CALL → parses tool calls via {@link #parseToolCallList} and creates a
     *       {@link ToolCallStreamDataChunk}.</li>
     *   <li>USAGE → builds a {@link Usage} object from the root fields and emits a
     *       {@link UsageStreamDataChunk}.</li>
     *   <li>FINISHED → extracts the done_reason into a {@link TextStreamDataChunk}.</li>
     * </ul>
     * If the data type is not recognized, the method returns an empty publisher and logs a warning.
     *
     * @param toolDefinitions   the registered tool definitions for tool call parsing
     * @param rawStreamResponse the already-classified raw stream response chunk
     * @return a publisher emitting the corresponding StreamResponse, or empty if unsupported
     */
    @Override
    public Publisher<StreamResponse> extractStreamResponseContent(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawStreamResponse rawStreamResponse) {
        // ... implementation unchanged
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
                JsonNode contentNode = dataContent.at("/message/content");
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
                JsonNode contentNode = dataContent.at("/message/thinking");
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
                        .executionContext(rawStreamResponse.getExecutionContext())
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
                        .executionContext(rawStreamResponse.getExecutionContext())
                        .dataChunk(textStreamDataChunk)
                        .build();
            });
        }
        log.warn("Unsupported response data type: {}, this raw response slice will be discard", streamDataType);
        return Mono.empty();
    }

    /**
     * Ollama does not require any API key or token certification by default.
     * This implementation simply logs that fact. If a certification is provided
     * it will be applied during request building in {@link #loadRequestBodySpec}.
     *
     * @param llmChatRequestData the request data (ignored)
     */
    @Override
    public void checkTokenCertification(@NonNull LlmChatRequestData llmChatRequestData) {
        log.debug("Ollama chat provider can work without api certification");
    }

    /**
     * Returns a string representation of this delegate, useful for debugging.
     *
     * @return a string containing the provider info, base URL, and endpoint
     */
    @Override
    public String toString() {
        return "OllamaChatProviderDelegate{" +
                "llmProviderInfo=" + llmProviderInfo +
                ", baseUrL='" + baseUrL + '\'' +
                ", chatCompletionEndpoint='" + chatCompletionEndpoint + '\'' +
                '}';
    }

    /**
     * Builds the complete {@link OllamaChatRequest} DTO from the framework's
     * {@link LlmChatRequestData}. This method handles:
     * <ul>
     *   <li>Setting the model name.</li>
     *   <li>Configuring the output format: either raw JSON mode (by setting format="json")
     *       or a custom JSON schema for structured output. If no schema is provided, defaults
     *       to JSON mode.</li>
     *   <li>Collecting generation options: temperature, top_p, reasoning (think) mode.</li>
     *   <li>Converting tool definitions to Ollama's function tool format.</li>
     *   <li>Assembling the message sequence: system message, historical messages, tool result
     *       messages, and the user message (only if no tool result messages exist to avoid
     *       consuming the original user query).</li>
     * </ul>
     *
     * @param llmChatRequestData the input request data
     * @return the fully prepared Ollama chat request
     */
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

    /**
     * Converts the framework's {@link ToolDefinition} list into Ollama's function tool DTOs.
     * Each tool definition is mapped to a {@link FunctionTool} with its name, description,
     * parameter JSON schema (as a Map), and strict mode flag.
     *
     * @param toolDefinitions the list of tool definitions from the request
     * @return a list of Ollama function tools, empty if no tools are defined
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
     * Converts a list of {@link ToolResultMessage} instances into Ollama tool messages.
     * In Ollama's protocol, tool results are represented as messages with role=TOOL,
     * the tool call ID as the {@code toolName}, and the result string as {@code content}.
     *
     * @param toolResultMessages the tool result messages to convert
     * @return a list of Ollama chat messages for tool results
     */
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

    /**
     * Builds a system message from the given {@link TextMessage}. The system message sets the
     * overall behavior and context for the model.
     *
     * @param textMessage the system prompt text
     * @return an Ollama chat message with role SYSTEM
     */
    protected OllamaChatMessage buildSystemMessage(TextMessage textMessage) {
        return OllamaChatMessage.builder()
                .role(Role.SYSTEM)
                .content(textMessage.getContent())
                .build();
    }

    /**
     * Converts a list of historical {@link Message} objects into the corresponding Ollama
     * chat message format. This method handles:
     * <ul>
     *   <li>{@link ToolCallMessage} → assistant message with tool calls array</li>
     *   <li>{@link ToolResultMessage} → tool role message</li>
     *   <li>{@link MediaMessage} → message with images (base64)</li>
     *   <li>{@link TextMessage} → user or assistant text message</li>
     *   <li>{@link AssistantTextMessage} → assistant text with reasoning/thinking</li>
     * </ul>
     * Any unrecognized message type is logged and skipped.
     *
     * @param historicalMessages the list of prior messages
     * @return the converted list of Ollama messages, excluding any null entries
     */
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
                        return this.buildMediaMessage(Role.fromValue(mediaMessage.getRole()), mediaMessage);
                    }
                    if (message instanceof TextMessage textMessage) {
                        return this.buildTextMessage(Role.fromValue(textMessage.getRole()), textMessage);
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

    /**
     * Builds an {@link OllamaChatMessage} with media (images) from a {@link MediaMessage}.
     * Only {@link Base64Attachment} attachments are supported; they are collected as a list
     * of base64-encoded image strings that Ollama accepts in the {@code images} field.
     * Other attachment types are logged and ignored.
     *
     * @param role         the role (usually USER) for this message
     * @param mediaMessage the media message containing attachments
     * @return an Ollama chat message with optionally populated {@code images} array
     */
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

    /**
     * Builds a simple text-only {@link OllamaChatMessage} from a {@link TextMessage}.
     *
     * @param role        the role for this message
     * @param textMessage the text message content
     * @return an Ollama chat message with the given role and content
     */
    protected OllamaChatMessage buildTextMessage(Role role, TextMessage textMessage) {
        return OllamaChatMessage.builder()
                .role(role)
                .content(textMessage.getContent())
                .build();
    }

}