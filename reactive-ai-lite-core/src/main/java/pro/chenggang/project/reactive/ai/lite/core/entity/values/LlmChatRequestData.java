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
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionInfo;
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
     * The execution context containing session-scoped state and variables.
     * <p>
     * This context is used to dynamically resolve configuration values such as the system
     * message, tool definitions, or user input from the current conversation state.
     */
    @Getter
    @NonNull
    private final ExecutionContext executionContext;

    /**
     * The authentication credential required to access the LLM provider API.
     * <p>
     * This certification is resolved dynamically based on the configured profile. It may be
     * the default certification or a profile-specific token, depending on the execution setup.
     */
    private final TokenCertification tokenCertification;

    /**
     * The identifier of the AI model to invoke (e.g., {@code "gpt-4"} or {@code "claude-3"}).
     * <p>
     * This value must be non-null and non-empty; it is determined by the model configuration
     * function provided in the {@link ChatExecutionInfo}.
     */
    @Getter
    private final String modelName;

    /**
     * A list of tools (functions) available for the model to call during the conversation.
     * <p>
     * Each tool definition describes a callable function, its parameters, and any constraints.
     * The provider may use these to perform tool-calling actions when appropriate.
     */
    @Getter
    private final List<ToolDefinition> toolDefinitions;

    /**
     * A list of result messages from previous tool calls.
     * <p>
     * These messages contain the outputs of tools that were invoked in prior turns and are
     * fed back into the model to continue the conversation or complete a task.
     */
    @Getter
    private final List<ToolResultMessage> toolResultMessages;

    /**
     * Indicates whether the response should be streamed back incrementally rather than returned
     * in a single blocking call.
     */
    @Getter
    private final boolean isStream;

    /**
     * Indicates whether the provider should filter out duplicate tool calls within a single
     * response turn, preventing the model from invoking the same tool multiple times with
     * identical arguments.
     */
    @Getter
    private final boolean distinctToolCalls;

    /**
     * Specifies the tool choice behavior for the model, governing how it decides to use tools.
     * <p>
     * Typical values are {@code "auto"} (the model decides), {@code "none"} (no tools used),
     * or a specific tool name to force its use. This field is dynamically resolved and may be
     * {@code null} when not explicitly configured.
     */
    private final String toolChoice;

    /**
     * The expected Java {@link Type} for a structured output response.
     * <p>
     * When set, the provider is asked to return a response that conforms to this type,
     * enabling seamless deserialization into a specific object model.
     */
    private final Type structuredOutputType;

    /**
     * A JSON schema string that defines the expected format of a structured output response.
     * <p>
     * Providers that support structured outputs will use this schema to guide the shape of
     * the generated text, ensuring it can be parsed into the corresponding Java type.
     */
    private final String responseJsonSchema;

    /**
     * The sampling temperature (0.0 to 2.0 typically) that controls randomness in token selection.
     * <p>
     * Higher values (e.g., 0.8) make output more random, while lower values (e.g., 0.2) make
     * it more deterministic. This field is optional and may be left {@code null} to use the
     * provider's default.
     */
    private final Double temperature;

    /**
     * The nucleus sampling (Top-P) probability, a parameter that controls diversity via nucleus
     * sampling. The model considers only the smallest set of tokens whose cumulative probability
     * is at least this value.
     * <p>
     * Like temperature, this is optional and provider-specific.
     */
    private final Double topP;

    /**
     * Indicates whether to include usage metadata (e.g., token count) in the response.
     * <p>
     * Setting this to {@code true} can be useful for billing or monitoring but may add extra
     * processing overhead.
     */
    @Getter
    private final boolean includeUsage;

    /**
     * Instructions that guide the model’s internal reasoning or thought process, often used
     * for chain-of-thought prompting.
     * <p>
     * This string is provider-specific and may not be supported by all models. It is optional
     * and can be {@code null}.
     */
    private final String reasoning;

    /**
     * The maximum number of tokens to generate in the completion.
     * <p>
     * This value caps the output length and can help control cost and latency. It is optional;
     * if not provided, the provider’s default or model-specific limit is used.
     */
    private final Integer maxCompletionTokens;

    /**
     * The system message that provides high‑level instructions and context to the model.
     * <p>
     * Typically includes the model’s persona, behavior constraints, and ground rules.
     * If not explicitly configured, a default empty system message is used.
     */
    private final TextMessage systemMessage;

    /**
     * A list of historical messages representing the conversation context before the current
     * user input.
     * <p>
     * This includes previous user messages, assistant responses, and tool interaction results.
     * It can be empty if this is the first turn of a conversation.
     */
    @Getter
    private final List<Message> historicalMessages;

    /**
     * The most recent text message from the user, i.e., the current user input for this request.
     * <p>
     * This message is used to continue or start a conversation; it may be empty if the user
     * has not provided explicit text input (e.g., only a media message).
     */
    private final TextMessage userTextMessage;

    /**
     * The most recent media message from the user, combining text with attachments (images,
     * audio, video, etc.).
     * <p>
     * This is only present when the user supplies multimodal content. It is optional and may
     * be {@code null}.
     */
    private final MediaMessage userMediaMessage;

    /**
     * An optional customizer that allows raw modification of the JSON request body before it
     * is sent to the provider.
     * <p>
     * This provides an escape hatch for provider-specific parameters that are not directly
     * supported by the framework. The customizer receives the {@link ExecutionContext} and
     * the mutable {@link ObjectNode} representing the request payload.
     */
    @Getter
    private final BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;

    /**
     * Gets the token certification as an Optional.
     * <p>
     * The certification is resolved based on the execution profile; it may be a default token
     * or a profile-specific credential. If the profile configuration fails to determine a
     * valid certification, the underlying builder will throw an exception, so this optional
     * will always contain a value if the request data was built successfully.
     *
     * @return an {@link Optional} containing the token certification, or empty if null
     */
    public Optional<TokenCertification> getTokenCertification() {
        return Optional.ofNullable(tokenCertification);
    }

    /**
     * Gets the temperature as an Optional.
     * <p>
     * Temperature controls the randomness of the model's output. If not configured, the
     * provider's default is used. This method provides access for building the provider
     * request, allowing providers to omit the parameter when it is null.
     *
     * @return an {@link Optional} containing the temperature, or empty if null
     */
    public Optional<Double> getTemperature() {
        return Optional.ofNullable(this.temperature);
    }

    /**
     * Gets the Top-P value as an Optional.
     * <p>
     * Top-P (nucleus sampling) provides an alternative to temperature for controlling randomness.
     * Similar to temperature, a null value means the provider should use its own default.
     *
     * @return an {@link Optional} containing the Top-P value, or empty if null
     */
    public Optional<Double> getTopP() {
        return Optional.ofNullable(this.topP);
    }

    /**
     * Gets the reasoning instructions as an Optional.
     * <p>
     * Reasoning instructions influence the model's internal thinking process, often used
     * for more deliberate problem solving. This is an advanced feature, and not all
     * providers/models support it; thus its absence is handled gracefully.
     *
     * @return an {@link Optional} containing the reasoning instructions, or empty if null
     */
    public Optional<String> getReasoning() {
        return Optional.ofNullable(this.reasoning);
    }

    /**
     * Gets the maximum completion tokens as an Optional.
     * <p>
     * This value caps the length of the generated response. When empty, the provider will
     * apply its own default limit, which may be model-specific.
     *
     * @return an {@link Optional} containing the maximum completion tokens, or empty if null
     */
    public Optional<Integer> getMaxCompletionTokens() {
        return Optional.ofNullable(this.maxCompletionTokens);
    }

    /**
     * Gets the system message.
     * <p>
     * The system message sets the overall behavior and background for the conversation.
     * If no system message was configured, an empty system message is returned to ensure
     * the provider always receives a well-defined message object (some providers require
     * a system message even if empty).
     *
     * @return the configured system message, or an empty system message if none was provided
     */
    public TextMessage getSystemMessage() {
        return Optional.ofNullable(systemMessage).orElse(TextMessage.emptySystemTextMessage());
    }

    /**
     * Gets the user text message.
     * <p>
     * This is the primary textual input from the user for the current turn. If the caller
     * did not provide a text message (perhaps because they supplied a media message), an
     * empty user message is returned to maintain consistency with the message flow.
     *
     * @return the configured user text message, or an empty user message if none was provided
     */
    public TextMessage getUserTextMessage() {
        return Optional.ofNullable(userTextMessage).orElse(TextMessage.emptyUserTextMessage());
    }

    /**
     * Gets the user media message as an Optional.
     * <p>
     * A media message contains one or more attachments (images, audio, etc.) along with an
     * optional caption. This method returns an empty optional when the user only sent text
     * (or no message at all), allowing the provider to distinguish between pure text and
     * multimodal requests.
     *
     * @return an {@link Optional} containing the user media message, or empty if null
     */
    public Optional<MediaMessage> getUserMediaMessage() {
        return Optional.ofNullable(userMediaMessage);
    }

    /**
     * Gets the structured output type as an Optional.
     * <p>
     * When a structured output type is configured, the provider is expected to generate a
     * response that can be deserialized into that Java type. This is typically used with
     * providers that support function‑calling or JSON mode.
     *
     * @return an {@link Optional} containing the expected output type, or empty if null
     */
    public Optional<Type> getStructuredOutputType() {
        return Optional.ofNullable(structuredOutputType);
    }

    /**
     * Gets the response JSON schema as an Optional.
     * <p>
     * This schema describes the exact JSON structure the model should produce, enabling
     * reliable parsing and validation. It is often paired with a structured output type
     * but can be used independently for providers that only need a schema hint.
     *
     * @return an {@link Optional} containing the JSON schema string, or empty if null
     */
    public Optional<String> getResponseJsonSchema() {
        return Optional.ofNullable(responseJsonSchema);
    }

    /**
     * Gets the tool choice instruction as an Optional.
     * <p>
     * The tool choice controls whether and how the model uses available tools. A value like
     * {@code "auto"} leaves the decision to the model, while {@code "none"} disables tool
     * calls entirely. A specific tool name forces the model to use that tool. If empty, the
     * provider's default behavior applies.
     *
     * @return an {@link Optional} containing the tool choice, or empty if null
     */
    public Optional<String> getToolChoice() {
        return Optional.ofNullable(toolChoice);
    }

    /**
     * An initializer class responsible for building an {@link LlmChatRequestData} instance.
     * <p>
     * This class evaluates the dynamically configured functions from a {@link ChatExecutionInfo}
     * object against the current execution context to produce the static, immutable data required
     * for the request. It resolves model name, token certification, messages, temperature,
     * tool definitions, and all other parameters by applying the provided function configurations
     * to the execution context.
     * </p>
     */
    @Slf4j
    public static class LlmChatRequestDataInitializer {

        /**
         * A map of profile names to token certifications.
         * <p>
         * Each profile represents a distinct authentication identity (e.g., for different
         * API keys or tenants). The initializer selects the appropriate certification
         * based on the execution profile.
         */
        private final Map<String, TokenCertification> certificationMap;

        /**
         * The default token certification to use when no specific profile is configured
         * or when the execution info indicates use of the default profile.
         */
        private final TokenCertification defaultCertification;

        /**
         * Information about the LLM provider, including its name and available profiles.
         * Used for error reporting when no suitable profile can be found.
         */
        private final LlmProviderInfo llmProviderInfo;

        /**
         * The execution specification containing all configuration functions for building
         * the request data. These functions are evaluated lazily with the execution context.
         */
        private final ChatExecutionInfo executionInfo;

        /**
         * Whether the request is a streaming request. This directly affects how the provider
         * processes the call and expects the response.
         */
        private final boolean isStream;

        /**
         * Constructs a new initializer with the required components.
         *
         * @param certificationMap     a map of available token certifications, must not be null
         * @param defaultCertification the default token certification, may be null if no default exists
         * @param llmProviderInfo      information about the LLM provider, must not be null
         * @param executionInfo        the execution specification, must not be null
         * @param isStream             whether the request is a streaming request
         */
        private LlmChatRequestDataInitializer(@NonNull Map<String, TokenCertification> certificationMap,
                                              TokenCertification defaultCertification,
                                              @NonNull LlmProviderInfo llmProviderInfo,
                                              @NonNull ChatExecutionInfo executionInfo,
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
         * @param certificationMap     a map of available token certifications, must not be null
         * @param defaultCertification the default token certification, may be null
         * @param llmProviderInfo      information about the LLM provider, must not be null
         * @param executionInfo        the execution specification, must not be null
         * @param isStream             whether the request is a streaming request
         * @return a new {@link LlmChatRequestDataInitializer}
         */
        public static LlmChatRequestDataInitializer of(@NonNull Map<String, TokenCertification> certificationMap,
                                                       TokenCertification defaultCertification,
                                                       @NonNull LlmProviderInfo llmProviderInfo,
                                                       @NonNull ChatExecutionInfo executionInfo,
                                                       boolean isStream) {
            return new LlmChatRequestDataInitializer(certificationMap, defaultCertification, llmProviderInfo, executionInfo, isStream);
        }

        /**
         * Evaluates all configurations and builds the final {@link LlmChatRequestData}.
         * <p>
         * This method obtains the current {@link ExecutionContext} from the reactive context,
         * then invokes each configuration function from {@link #executionInfo} with that context
         * to resolve the values for the builder. It returns a {@link Mono} that emits the
         * completed request data or signals an appropriate error if the context is missing
         * or any configuration resolution fails.
         *
         * @return a {@link Mono} emitting a populated {@link LlmChatRequestData} instance
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
         * <p>
         * If the execution info indicates use of the default profile, the default certification
         * is returned. Otherwise, a profile picker function is invoked to select a profile name
         * from the provider’s available profiles, and the corresponding certification is retrieved
         * from the certification map.
         * </p>
         *
         * @param executionInfo the execution info containing profile logic, must not be null
         * @param executionContext the current execution context, must not be null
         * @return the resolved {@link TokenCertification}
         * @throws NoProfileFoundLlmClientException if a valid profile cannot be determined
         */
        protected TokenCertification loadTokenCertification(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
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
         * Resolves the model name by applying the model configuration function to the execution context.
         * <p>
         * The model name is a critical parameter that identifies which AI model to use.
         * An exception is thrown if the resolved name is null or empty, ensuring the provider
         * always receives a valid identifier.
         * </p>
         *
         * @param executionInfo the execution info containing the model name configuration function
         * @param executionContext the current execution context
         * @return the model name
         * @throws IllegalArgumentException if the model name is empty or null
         */
        protected String loadModelName(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            String modelName = executionInfo.getModelNameConfigure().apply(executionContext);
            if (!StringUtils.hasText(modelName)) {
                throw new IllegalArgumentException("Model name cannot be null or empty");
            }
            return modelName;
        }

        /**
         * Resolves the system message by evaluating the system message configuration function.
         * <p>
         * If no function is configured, an empty string is used, resulting in a system message
         * with no content but still a valid message wrapped in {@link TextMessage}.
         * </p>
         *
         * @param executionInfo the execution info containing system message configurations
         * @param executionContext the current execution context
         * @return the configured system message
         */
        protected TextMessage loadSystemMessage(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, String> systemMessageConfigure = executionInfo.getSystemMessageConfigure();
            String systemMessage = "";
            if (Objects.nonNull(systemMessageConfigure)) {
                systemMessage = systemMessageConfigure.apply(executionContext);
            }
            return TextMessage.newTextMessage(Role.SYSTEM)
                    .content(systemMessage)
                    .build();
        }

        /**
         * Resolves the historical messages by invoking the historical messages configuration function.
         * <p>
         * Historical messages provide the conversation context. If no function is configured,
         * an empty list is returned, meaning this is treated as a new conversation or a single‑turn request.
         * </p>
         *
         * @param executionInfo the execution info containing historical messages configurations
         * @param executionContext the current execution context
         * @return the historical messages, or an empty list if none
         */
        protected List<Message> loadHistoricalMessage(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, List<Message>> historicalMessageConfigure = executionInfo.getHistoricalMessageConfigure();
            if (Objects.isNull(historicalMessageConfigure)) {
                return List.of();
            }
            return historicalMessageConfigure.apply(executionContext);
        }

        /**
         * Resolves the user's text message for the current turn.
         * <p>
         * This is the user's actual input. If no text message configuration function is provided,
         * an empty user message is created. This ensures the request always contains a user message,
         * which is required by many providers.
         * </p>
         *
         * @param executionInfo the execution info containing the text message configuration
         * @param executionContext the current execution context
         * @return the user's text message
         */
        protected TextMessage loadUserMessage(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
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
         * Resolves the user's media message for the current turn.
         * <p>
         * If a media message configuration function is present, it is invoked to obtain the
         * media message, which may contain images, audio, or video attachments. If no function
         * exists, {@code null} is returned, indicating a pure text interaction.
         * </p>
         *
         * @param executionInfo the execution info containing the media message configuration
         * @param executionContext the current execution context
         * @return the media message, or null if none
         */
        protected MediaMessage loadMediaMessage(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, MediaMessage> mediaMessageConfigure = executionInfo.getMediaMessageConfigure();
            MediaMessage mediaMessage = null;
            if (Objects.nonNull(mediaMessageConfigure)) {
                mediaMessage = mediaMessageConfigure.apply(executionContext);
            }
            return mediaMessage;
        }

        /**
         * Resolves the temperature by evaluating the temperature configuration function.
         * <p>
         * Temperature controls randomness. If no function is configured, this method returns
         * {@code null}, allowing the provider to apply its default behavior.
         * </p>
         *
         * @param executionInfo the execution info containing temperature configuration
         * @param executionContext the current execution context
         * @return the temperature, or null if not configured
         */
        protected Double loadTemperature(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, Double> temperatureConfigure = executionInfo.getTemperatureConfigure();
            Double temperature = null;
            if (Objects.nonNull(temperatureConfigure)) {
                temperature = temperatureConfigure.apply(executionContext);
            }
            return temperature;
        }

        /**
         * Resolves the Top-P value by evaluating the Top-P configuration function.
         * <p>
         * Top‑P is an alternative to temperature for controlling output diversity. If not
         * configured, {@code null} is returned, deferring the parameter to the provider.
         * </p>
         *
         * @param executionInfo the execution info containing Top-P configuration
         * @param executionContext the current execution context
         * @return the Top-P value, or null if not configured
         */
        protected Double loadTopP(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, Double> topPConfigure = executionInfo.getTopPConfigure();
            Double topP = null;
            if (Objects.nonNull(topPConfigure)) {
                topP = topPConfigure.apply(executionContext);
            }
            return topP;
        }

        /**
         * Resolves whether to include usage metadata in the response.
         * <p>
         * If configured, the function returns a {@code Boolean}; only an explicit {@code true}
         * enables usage reporting. Otherwise, usage is not included (default {@code false}).
         * </p>
         *
         * @param executionInfo the execution info holding the include‑usage configuration
         * @param executionContext the current execution context
         * @return {@code true} if usage should be included, {@code false} otherwise
         */
        protected boolean loadIncludeUsage(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, Boolean> includeUsageConfigure = executionInfo.getIncludeUsageConfigure();
            if (Objects.isNull(includeUsageConfigure)) {
                return false;
            }
            Boolean includeUsage = includeUsageConfigure.apply(executionContext);
            return Boolean.TRUE.equals(includeUsage);
        }

        /**
         * Resolves reasoning instructions that guide the model's internal thought process.
         * <p>
         * This is a provider‑specific feature that can improve problem‑solving. If not
         * configured, {@code null} is returned, meaning no special reasoning hint is sent.
         * </p>
         *
         * @param executionInfo the execution info containing reasoning configuration
         * @param executionContext the current execution context
         * @return the reasoning instructions, or null if not configured
         */
        protected String loadReasoning(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, String> reasoningConfigure = executionInfo.getReasoningConfigure();
            if (Objects.isNull(reasoningConfigure)) {
                return null;
            }
            return reasoningConfigure.apply(executionContext);
        }

        /**
         * Resolves the maximum number of completion tokens.
         * <p>
         * This value caps the length of the generated completion. If not configured,
         * {@code null} is returned, allowing the provider to decide the limit.
         * </p>
         *
         * @param executionInfo the execution info containing max tokens configuration
         * @param executionContext the current execution context
         * @return the max tokens, or null if not configured
         */
        protected Integer loadMaxCompletionTokens(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
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
         * The configured function is invoked to obtain a collection of {@link ToolDefinition}
         * instances. The method filters out any tools with missing names or duplicate names
         * (logging a warning for each such case) to ensure the provider receives a consistent,
         * valid list of tools.
         * </p>
         *
         * @param executionInfo the execution info containing the tool configuration function
         * @param executionContext the current execution context
         * @return a list of valid, deduplicated tool definitions, or an empty list if none are configured
         */
        protected List<ToolDefinition> loadToolDefinitions(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
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
         * Resolves the tool choice behavior by applying the tool choice configuration function.
         * <p>
         * Typically returns a string like {@code "auto"}, {@code "none"}, or a specific tool
         * name. If no function is configured, {@code null} is returned, signaling that the
         * provider should use its default behavior.
         * </p>
         *
         * @param executionInfo the execution info containing tool choice configuration
         * @param executionContext the current execution context
         * @return the tool choice string, or null if not configured
         */
        protected String loadToolChoice(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
            Function<ExecutionContext, String> toolChoiceConfigure = executionInfo.getToolChoiceConfigure();
            if (Objects.isNull(toolChoiceConfigure)) {
                return null;
            }
            return toolChoiceConfigure.apply(executionContext);
        }

        /**
         * Resolves the tool result messages by invoking the corresponding configuration function.
         * <p>
         * These messages carry the outputs of tool calls from previous conversation turns.
         * If no function is configured, an empty list is returned.
         * </p>
         *
         * @param executionInfo the execution info containing the tool result messages configuration
         * @param executionContext the current execution context
         * @return a list of tool result messages, or an empty list if none
         */
        protected List<ToolResultMessage> loadToolResultMessage(@NonNull ChatExecutionInfo executionInfo, @NonNull ExecutionContext executionContext) {
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