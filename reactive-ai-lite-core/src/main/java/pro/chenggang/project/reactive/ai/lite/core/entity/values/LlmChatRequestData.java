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
package pro.chenggang.project.reactive.ai.lite.core.entity.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.TextMessage;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.tool.LlmToolCallResponse;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static pro.chenggang.project.reactive.ai.lite.core.message.Message.EMPTY_MESSAGE;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LlmChatRequestData implements LlmRequestData {

    @Getter
    @NonNull
    private final ExecutionContextView executionContextView;
    private final TokenCertification tokenCertification;
    @Getter
    private final String modelName;
    @Getter
    private final List<ToolDefinition> toolDefinitions;
    @Getter
    private final List<LlmToolCallResponse> llmToolCallResponse;
    @Getter
    private final boolean isStream;
    @Getter
    private final boolean distinctToolCalls;
    private final String toolChoice;
    private final Type structuredOutputType;
    private final String responseJsonSchema;
    private final Double temperature;
    private final Double topP;
    @Getter
    private final boolean includeUsage;
    private final String reasoning;
    private final Integer maxCompletionTokens;
    private final TextMessage userTextMessage;
    @Getter
    private final List<Message> historicalMessages;
    private final ObjectNode latestAssistantMessage;
    private final MediaMessage userMediaMessage;
    private final TextMessage systemMessage;
    @Getter
    private final BiConsumer<ExecutionContextView, ObjectNode> rawRequestCustomizer;
    @Getter
    private final BiConsumer<ExecutionContextView, RawResponse> rawResponseCustomizer;
    @Getter
    private final BiConsumer<ExecutionContextView, RawStreamResponse> rawStreamResponseCustomizer;

    public Optional<TokenCertification> getTokenCertification() {
        return Optional.ofNullable(tokenCertification);
    }

    public Optional<Double> getTemperature() {
        return Optional.ofNullable(this.temperature);
    }

    public Optional<Double> getTopP() {
        return Optional.ofNullable(this.topP);
    }

    public Optional<String> getReasoning() {
        return Optional.ofNullable(this.reasoning);
    }

    public Optional<Integer> getMaxCompletionTokens() {
        return Optional.ofNullable(this.maxCompletionTokens);
    }

    public TextMessage getSystemMessage() {
        return Optional.ofNullable(systemMessage).orElse(Message.EMPTY_MESSAGE);
    }

    public TextMessage getUserTextMessage() {
        return Optional.ofNullable(userTextMessage).orElse(Message.EMPTY_MESSAGE);
    }

    public Optional<MediaMessage> getUserMediaMessage() {
        return Optional.ofNullable(userMediaMessage);
    }

    public Optional<Type> getStructuredOutputType() {
        return Optional.ofNullable(structuredOutputType);
    }

    public Optional<String> getResponseJsonSchema() {
        return Optional.ofNullable(responseJsonSchema);
    }

    public Optional<String> getToolChoice() {
        return Optional.ofNullable(toolChoice);
    }

    public Optional<ObjectNode> getLatestAssistantMessage() {
        return Optional.ofNullable(latestAssistantMessage);
    }

    @Override
    public TraceId getTraceId() {
        return executionContextView.getTraceId();
    }

    @Override
    public List<String> getSummary() {
        List<String> summary = new ArrayList<>();
        summary.add("Model Name: " + getModelName());
        getReasoning().ifPresent(reasoning -> summary.add("Reasoning: " + reasoning));
        summary.add("Is Stream: " + isStream());
        summary.add("Tool Definitions: " + getToolDefinitions().size());
        getToolChoice().ifPresent(toolChoice -> summary.add("Tool Choice: " + toolChoice));
        getTemperature().ifPresent(temperature -> summary.add("Temperature: " + temperature));
        getTopP().ifPresent(topP -> summary.add("Top P: " + topP));
        getMaxCompletionTokens().ifPresent(maxCompletionTokens -> summary.add("Max Completion Tokens: " + maxCompletionTokens));
        return summary;
    }

    @Slf4j
    public static class LlmChatRequestDataInitializer {

        private final Map<String, TokenCertification> certificationMap;
        private final TokenCertification defaultCertification;
        private final LlmProviderInfo llmProviderInfo;
        private final ExecutionInfo executionInfo;
        private final boolean isStream;
        private final Type structuredOutputType;
        private final String responseJsonSchema;

        private LlmChatRequestDataInitializer(@NonNull Map<String, TokenCertification> certificationMap,
                                              TokenCertification defaultCertification,
                                              @NonNull LlmProviderInfo llmProviderInfo,
                                              @NonNull ExecutionInfo executionInfo,
                                              boolean isStream,
                                              Type structuredOutputType,
                                              String responseJsonSchema) {
            this.certificationMap = certificationMap;
            this.defaultCertification = defaultCertification;
            this.llmProviderInfo = llmProviderInfo;
            this.executionInfo = executionInfo;
            this.isStream = isStream;
            this.structuredOutputType = structuredOutputType;
            this.responseJsonSchema = responseJsonSchema;
        }

        public static LlmChatRequestDataInitializer of(@NonNull Map<String, TokenCertification> certificationMap,
                                                       TokenCertification defaultCertification,
                                                       @NonNull LlmProviderInfo llmProviderInfo,
                                                       @NonNull ExecutionInfo executionInfo,
                                                       boolean isStream,
                                                       Type structuredOutputType,
                                                       String responseJsonSchema) {
            return new LlmChatRequestDataInitializer(certificationMap, defaultCertification, llmProviderInfo, executionInfo, isStream, structuredOutputType, responseJsonSchema);
        }

        public LlmChatRequestData initialize() {
            return LlmChatRequestData.builder()
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
                    .includeUsage(this.loadIncludeUsage(executionInfo))
                    .reasoning(this.loadReasoning(executionInfo))
                    .maxCompletionTokens(this.loadMaxCompletionTokens(executionInfo))
                    .toolDefinitions(this.loadToolDefinitions(executionInfo))
                    .llmToolCallResponse(this.loadToolResponse(executionInfo))
                    .rawRequestCustomizer(executionInfo.getRawRequestCustomizer())
                    .isStream(isStream)
                    .distinctToolCalls(executionInfo.isDistinctToolCalls())
                    .toolChoice(this.loadToolChoice(executionInfo))
                    .structuredOutputType(structuredOutputType)
                    .responseJsonSchema(responseJsonSchema)
                    .build();
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

        protected boolean loadIncludeUsage(@NonNull ExecutionInfo executionInfo) {
            Function<ExecutionContextView, Boolean> includeUsageConfigure = executionInfo.getIncludeUsageConfigure();
            if (Objects.isNull(includeUsageConfigure)) {
                return false;
            }
            Boolean includeUsage = includeUsageConfigure.apply(executionInfo.getExecutionContext().getContextView());
            return Boolean.TRUE.equals(includeUsage);
        }

        protected String loadReasoning(@NonNull ExecutionInfo executionInfo) {
            Function<ExecutionContextView, String> reasoningConfigure = executionInfo.getReasoningConfigure();
            if (Objects.isNull(reasoningConfigure)) {
                return null;
            }
            return reasoningConfigure.apply(executionInfo.getExecutionContext().getContextView());
        }

        protected Integer loadMaxCompletionTokens(@NonNull ExecutionInfo executionInfo) {
            Function<ExecutionContextView, Integer> maxCompletionTokensConfigure = executionInfo.getMaxCompletionTokensConfigure();
            Integer maxCompletionTokens = null;
            if (Objects.nonNull(maxCompletionTokensConfigure)) {
                maxCompletionTokens = maxCompletionTokensConfigure.apply(executionInfo.getExecutionContext().getContextView());
            }
            return maxCompletionTokens;
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
            Set<String> identifiers = new HashSet<>();
            List<ToolDefinition> toolDefinitionList = toolDefinitions.stream()
                    .map(toolDefinition -> {
                        if (!StringUtils.hasText(toolDefinition.identifier())) {
                            log.warn("Invalid tool definition, identifier is missing: {}", toolDefinition);
                            return null;
                        }
                        if (identifiers.contains(toolDefinition.identifier())) {
                            log.warn("Invalid tool definition, identifier is duplicated: {}", toolDefinition);
                            return null;
                        }
                        identifiers.add(toolDefinition.identifier());
                        return toolDefinition;
                    })
                    .filter(Objects::nonNull)
                    .toList();
            identifiers.clear();
            return toolDefinitionList;
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
    }
}
