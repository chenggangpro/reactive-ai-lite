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
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.exception.ExecutionContextLossException;
import pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.message.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.TextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolResultMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents the comprehensive set of data required to make a chat request to an LLM provider.
 * <p>
 * This class encapsulates all configuration parameters, messages, tools, and execution context
 * details necessary for an AI model to generate a response. It acts as an immutable payload
 * passed from the framework to the specific provider implementation.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LlmChatRequestData {

    /**
     * The execution context.
     */
    @Getter
    @NonNull
    private final ExecutionContext executionContext;

    /**
     * The authentication certification required for the provider API.
     */
    private final TokenCertification tokenCertification;

    /**
     * The name of the AI model to be used.
     */
    @Getter
    private final String modelName;

    /**
     * A list of tools (functions) available for the model to call.
     */
    @Getter
    private final List<ToolDefinition> toolDefinitions;

    /**
     * A list of result messages from previous tool calls.
     */
    @Getter
    private final List<ToolResultMessage> toolResultMessages;

    /**
     * Indicates whether the response should be streamed.
     */
    @Getter
    private final boolean isStream;

    /**
     * Indicates whether the provider should filter out duplicate tool calls in a single response turn.
     */
    @Getter
    private final boolean distinctToolCalls;

    /**
     * Specifies the tool choice behavior (e.g., "auto", "none", or a specific tool).
     */
    private final String toolChoice;

    /**
     * The expected Java type for a structured output response.
     */
    private final Type structuredOutputType;

    /**
     * The expected JSON schema for a structured output response.
     */
    private final String responseJsonSchema;

    /**
     * The sampling temperature for the model.
     */
    private final Double temperature;

    /**
     * The nucleus sampling (Top-P) probability.
     */
    private final Double topP;

    /**
     * Indicates whether to include usage metadata in the response.
     */
    @Getter
    private final boolean includeUsage;

    /**
     * Instructions to guide the model's internal reasoning or thought process.
     */
    private final String reasoning;

    /**
     * The maximum number of tokens to generate in the completion.
     */
    private final Integer maxCompletionTokens;

    /**
     * The system message providing high-level instructions to the model.
     */
    private final TextMessage systemMessage;

    /**
     * A list of historical messages providing conversation context.
     */
    @Getter
    private final List<Message> historicalMessages;

    /**
     * The most recent text message from the user.
     */
    private final TextMessage userTextMessage;

    /**
     * The most recent media message (text with attachments) from the user.
     */
    private final MediaMessage userMediaMessage;

    /**
     * A customizer for the raw request object node.
     */
    @Getter
    private final BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;

    /**
     * Gets the token certification as an Optional.
     *
     * @return an {@link Optional} containing the token certification, or empty if null
     */
    public Optional<TokenCertification> getTokenCertification() {
        return Optional.ofNullable(tokenCertification);
    }

    /**
     * Gets the temperature as an Optional.
     *
     * @return an {@link Optional} containing the temperature, or empty if null
     */
    public Optional<Double> getTemperature() {
        return Optional.ofNullable(this.temperature);
    }

    /**
     * Gets the Top-P value as an Optional.
     *
     * @return an {@link Optional} containing the Top-P value, or empty if null
     */
    public Optional<Double> getTopP() {
        return Optional.ofNullable(this.topP);
    }

    /**
     * Gets the reasoning instructions as an Optional.
     *
     * @return an {@link Optional} containing the reasoning instructions, or empty if null
     */
    public Optional<String> getReasoning() {
        return Optional.ofNullable(this.reasoning);
    }

    /**
     * Gets the maximum completion tokens as an Optional.
     *
     * @return an {@link Optional} containing the maximum completion tokens, or empty if null
     */
    public Optional<Integer> getMaxCompletionTokens() {
        return Optional.ofNullable(this.maxCompletionTokens);
    }

    /**
     * Gets the system message.
     *
     * @return the configured system message, or an empty system message if none was provided
     */
    public TextMessage getSystemMessage() {
        return Optional.ofNullable(systemMessage).orElse(TextMessage.emptySystemTextMessage());
    }

    /**
     * Gets the user text message.
     *
     * @return the configured user text message, or an empty user message if none was provided
     */
    public TextMessage getUserTextMessage() {
        return Optional.ofNullable(userTextMessage).orElse(TextMessage.emptyUserTextMessage());
    }

    /**
     * Gets the user media message as an Optional.
     *
     * @return an {@link Optional} containing the user media message, or empty if null
     */
    public Optional<MediaMessage> getUserMediaMessage() {
        return Optional.ofNullable(userMediaMessage);
    }

    /**
     * Gets the structured output type as an Optional.
     *
     * @return an {@link Optional} containing the expected output type, or empty if null
     */
    public Optional<Type> getStructuredOutputType() {
        return Optional.ofNullable(structuredOutputType);
    }

    /**
     * Gets the response JSON schema as an Optional.
     *
     * @return an {@link Optional} containing the JSON schema string, or empty if null
     */
    public Optional<String> getResponseJsonSchema() {
        return Optional.ofNullable(responseJsonSchema);
    }

    /**
     * Gets the tool choice instruction as an Optional.
     *
     * @return an {@link Optional} containing the tool choice, or empty if null
     */
    public Optional<String> getToolChoice() {
        return Optional.ofNullable(toolChoice);
    }

    /**
     * An initializer class responsible for building an {@link LlmChatRequestData} instance.
     * <p>
     * This class evaluates the dynamically configured functions from an {@link ExecutionInfo} object
     * against the current execution context to produce the static, immutable data required for the request.
     * </p>
     */
    @Slf4j
    public static class LlmChatRequestDataInitializer {

        private final Map<String, TokenCertification> certificationMap;
        private final TokenCertification defaultCertification;
        private final LlmProviderInfo llmProviderInfo;
        private final ExecutionInfo executionInfo;
        private final boolean isStream;

        /**
         * Constructs a new initializer.
         *
         * @param certificationMap     a map of available token certifications
         * @param defaultCertification the default token certification
         * @param llmProviderInfo      information about the LLM provider
         * @param executionInfo        the execution specification holding configuration functions
         * @param isStream             whether the request is a streaming request
         */
        private LlmChatRequestDataInitializer(@NonNull Map<String, TokenCertification> certificationMap,
                                              TokenCertification defaultCertification,
                                              @NonNull LlmProviderInfo llmProviderInfo,
                                              @NonNull ExecutionInfo executionInfo,
                                              boolean isStream) {
            this.certificationMap = certificationMap;
            this.defaultCertification = defaultCertification;
            this.llmProviderInfo = llmProviderInfo;
            this.executionInfo = executionInfo;
            this.isStream = isStream;
        }

        /**
         * Factory method to create a new initializer.
         *
         * @param certificationMap     a map of available token certifications
         * @param defaultCertification the default token certification
         * @param llmProviderInfo      information about the LLM provider
         * @param executionInfo        the execution specification
         * @param isStream             whether the request is a streaming request
         * @return a new {@link LlmChatRequestDataInitializer}
         */
        public static LlmChatRequestDataInitializer of(@NonNull Map<String, TokenCertification> certificationMap,
                                                       TokenCertification defaultCertification,
                                                       @NonNull LlmProviderInfo llmProviderInfo,
                                                       @NonNull ExecutionInfo executionInfo,
                                                       boolean isStream) {
            return new LlmChatRequestDataInitializer(certificationMap, defaultCertification, llmProviderInfo, executionInfo, isStream);
        }

        /**
         * Evaluates all configurations and builds the final {@link LlmChatRequestData}.
         *
         * @return a populated {@link LlmChatRequestData} instance
         */
        public Mono<LlmChatRequestData> initialize() {
            return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(ExecutionContext.class))
                            .ofType(ExecutionContext.class)
                            .switchIfEmpty(Mono.error(new ExecutionContextLossException()))
                    )
                    .flatMap(executionContext -> {
                        return Mono.fromCallable(() -> LlmChatRequestData.builder()
                                .executionContext(executionContext)
                                .modelName(this.loadModelName(executionInfo, executionContext))
                                .tokenCertification(this.loadTokenCertification(executionInfo, executionContext))
                                .systemMessage(this.loadSystemMessage(executionInfo, executionContext))
                                .userTextMessage(this.loadUserMessage(executionInfo, executionContext))
                                .userMediaMessage(this.loadMediaMessage(executionInfo, executionContext))
                                .historicalMessages(this.loadHistoricalMessage(executionInfo, executionContext))
                                .temperature(this.loadTemperature(executionInfo, executionContext))
                                .topP(this.loadTopP(executionInfo, executionContext))
                                .includeUsage(this.loadIncludeUsage(executionInfo, executionContext))
                                .reasoning(this.loadReasoning(executionInfo, executionContext))
                                .maxCompletionTokens(this.loadMaxCompletionTokens(executionInfo, executionContext))
                                .toolDefinitions(this.loadToolDefinitions(executionInfo, executionContext))
                                .toolResultMessages(this.loadToolResultMessage(executionInfo, executionContext))
                                .isStream(isStream)
                                .distinctToolCalls(executionInfo.isDistinctToolCalls())
                                .toolChoice(this.loadToolChoice(executionInfo, executionContext))
                                .structuredOutputType(executionInfo.getStructuredOutputType())
                                .responseJsonSchema(executionInfo.getResponseJsonSchema())
                                .rawRequestCustomizerConfigure(executionInfo.getRawRequestCustomizerConfigure())
                                .build()
                        );
                    });
        }

        /**
         * Resolves the token certification based on the profile configuration.
         *
         * @param executionInfo the execution info containing profile logic
         * @return the resolved {@link TokenCertification}
         * @throws NoProfileFoundLlmClientException if a valid profile cannot be determined
         */
        protected TokenCertification loadTokenCertification(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            if (executionInfo.isDefaultProfile()) {
                return this.defaultCertification;
            }
            BiFunction<ExecutionContext, Set<String>, String> profilePicker = executionInfo.getProfilePicker();
            if (Objects.isNull(profilePicker)) {
                throw new NoProfileFoundLlmClientException(this.llmProviderInfo);
            }
            String pickedProfile = profilePicker.apply(executionContext, this.llmProviderInfo.profiles());
            if (Objects.isNull(pickedProfile) || !this.certificationMap.containsKey(pickedProfile)) {
                throw new NoProfileFoundLlmClientException(this.llmProviderInfo, pickedProfile);
            }
            return certificationMap.get(pickedProfile);
        }

        /**
         * Resolves the model name.
         *
         * @param executionInfo the execution info containing the model configuration function
         * @return the model name
         * @throws IllegalArgumentException if the model name is empty or null
         */
        protected String loadModelName(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            String modelName = executionInfo.getModelNameConfigure().apply(executionContext);
            if (!StringUtils.hasText(modelName)) {
                throw new IllegalArgumentException("Model name cannot be null or empty");
            }
            return modelName;
        }

        /**
         * Resolves the system message.
         *
         * @param executionInfo the execution info containing system message configurations
         * @return the configured system message
         */
        protected TextMessage loadSystemMessage(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, String> defaultSystemMessageConfigure = executionInfo.getDefaultSystemMessageConfigure();
            Function<ExecutionContext, String> systemMessageConfigure = executionInfo.getSystemMessageConfigure();
            String systemMessage = "";
            if (Objects.nonNull(systemMessageConfigure)) {
                systemMessage = systemMessageConfigure.apply(executionContext);
            } else if (Objects.nonNull(defaultSystemMessageConfigure)) {
                systemMessage = defaultSystemMessageConfigure.apply(executionContext);
            }
            return TextMessage.newTextMessage(Role.SYSTEM)
                    .content(systemMessage)
                    .build();
        }

        /**
         * Resolves the historical messages.
         *
         * @param executionInfo the execution info containing historical messages configurations
         * @return the historical messages, or an empty list if none
         */
        protected List<Message> loadHistoricalMessage(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, List<Message>> historicalMessageConfigure = executionInfo.getHistoricalMessageConfigure();
            if (Objects.isNull(historicalMessageConfigure)) {
                return List.of();
            }
            return historicalMessageConfigure.apply(executionContext);
        }

        /**
         * Resolves the user's text message.
         *
         * @param executionInfo the execution info containing the text message configuration
         * @return the user's text message
         */
        protected TextMessage loadUserMessage(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, String> textMessageConfigure = executionInfo.getTextMessageConfigure();
            String userMessage = "";
            if (Objects.nonNull(textMessageConfigure)) {
                userMessage = textMessageConfigure.apply(executionContext);
            }
            return TextMessage.newTextMessage(Role.USER)
                    .content(userMessage)
                    .build();
        }

        /**
         * Resolves the user's media message.
         *
         * @param executionInfo the execution info containing the media message configuration
         * @return the media message, or null if none
         */
        protected MediaMessage loadMediaMessage(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, MediaMessage> mediaMessageConfigure = executionInfo.getMediaMessageConfigure();
            MediaMessage mediaMessage = null;
            if (Objects.nonNull(mediaMessageConfigure)) {
                mediaMessage = mediaMessageConfigure.apply(executionContext);
            }
            return mediaMessage;
        }

        /**
         * Resolves the temperature.
         *
         * @param executionInfo the execution info
         * @return the temperature, or null if not configured
         */
        protected Double loadTemperature(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, Double> temperatureConfigure = executionInfo.getTemperatureConfigure();
            Double temperature = null;
            if (Objects.nonNull(temperatureConfigure)) {
                temperature = temperatureConfigure.apply(executionContext);
            }
            return temperature;
        }

        /**
         * Resolves the Top-P value.
         *
         * @param executionInfo the execution info
         * @return the Top-P value, or null if not configured
         */
        protected Double loadTopP(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, Double> topPConfigure = executionInfo.getTopPConfigure();
            Double topP = null;
            if (Objects.nonNull(topPConfigure)) {
                topP = topPConfigure.apply(executionContext);
            }
            return topP;
        }

        /**
         * Resolves whether to include usage information.
         *
         * @param executionInfo the execution info
         * @return true if usage should be included, false otherwise
         */
        protected boolean loadIncludeUsage(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, Boolean> includeUsageConfigure = executionInfo.getIncludeUsageConfigure();
            if (Objects.isNull(includeUsageConfigure)) {
                return false;
            }
            Boolean includeUsage = includeUsageConfigure.apply(executionContext);
            return Boolean.TRUE.equals(includeUsage);
        }

        /**
         * Resolves the reasoning instructions.
         *
         * @param executionInfo the execution info
         * @return the reasoning instructions, or null if not configured
         */
        protected String loadReasoning(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, String> reasoningConfigure = executionInfo.getReasoningConfigure();
            if (Objects.isNull(reasoningConfigure)) {
                return null;
            }
            return reasoningConfigure.apply(executionContext);
        }

        /**
         * Resolves the maximum number of completion tokens.
         *
         * @param executionInfo the execution info
         * @return the max tokens, or null if not configured
         */
        protected Integer loadMaxCompletionTokens(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, Integer> maxCompletionTokensConfigure = executionInfo.getMaxCompletionTokensConfigure();
            Integer maxCompletionTokens = null;
            if (Objects.nonNull(maxCompletionTokensConfigure)) {
                maxCompletionTokens = maxCompletionTokensConfigure.apply(executionContext);
            }
            return maxCompletionTokens;
        }

        /**
         * Resolves the available tool definitions.
         * <p>
         * It filters out invalid tools (missing name) and duplicates.
         * </p>
         *
         * @param executionInfo the execution info
         * @return a list of valid tool definitions
         */
        protected List<ToolDefinition> loadToolDefinitions(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, Collection<ToolDefinition>> toolsConfigure = executionInfo.getToolsConfigure();
            if (Objects.isNull(toolsConfigure)) {
                return List.of();
            }
            Collection<ToolDefinition> toolDefinitions = toolsConfigure.apply(executionContext);
            if (Objects.isNull(toolDefinitions) || toolDefinitions.isEmpty()) {
                return List.of();
            }
            Set<String> identifiers = new HashSet<>();
            List<ToolDefinition> toolDefinitionList = toolDefinitions.stream()
                    .map(toolDefinition -> {
                        if (!StringUtils.hasText(toolDefinition.name())) {
                            log.warn("Invalid tool definition, tool name is missing: {}", toolDefinition);
                            return null;
                        }
                        if (identifiers.contains(toolDefinition.name())) {
                            log.warn("Invalid tool definition, tool name is duplicated: {}", toolDefinition);
                            return null;
                        }
                        identifiers.add(toolDefinition.name());
                        return toolDefinition;
                    })
                    .filter(Objects::nonNull)
                    .toList();
            identifiers.clear();
            return toolDefinitionList;
        }

        /**
         * Resolves the tool choice behavior.
         *
         * @param executionInfo the execution info
         * @return the tool choice string, or null if not configured
         */
        protected String loadToolChoice(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, String> toolChoiceConfigure = executionInfo.getToolChoiceConfigure();
            if (Objects.isNull(toolChoiceConfigure)) {
                return null;
            }
            return toolChoiceConfigure.apply(executionContext);
        }

        /**
         * Resolves the tool result messages.
         *
         * @param executionInfo the execution info
         * @return a list of tool result messages
         */
        protected List<ToolResultMessage> loadToolResultMessage(@NonNull ExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, Collection<ToolResultMessage>> toolsConfigure = executionInfo.getToolResultMessageConfigure();
            if (Objects.isNull(toolsConfigure)) {
                return List.of();
            }
            Collection<ToolResultMessage> toolCallResponses = toolsConfigure.apply(executionContext);
            if (Objects.isNull(toolCallResponses) || toolCallResponses.isEmpty()) {
                return List.of();
            }
            return List.copyOf(toolCallResponses);
        }
    }
}
