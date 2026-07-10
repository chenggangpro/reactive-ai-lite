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
package pro.chenggang.project.reactive.ai.lite.core.spec.defaults;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.GeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.StreamExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.StructuredExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat.ChatGeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat.ChatStreamExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat.ChatStructuredExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.message.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolResultMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableChatSpec;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The default implementation of the {@link ConfigurableChatSpec} interface.
 * <p>
 * This class serves as a mutable, chainable builder that collects all configuration
 * parameters for a chat request. Each configuration can be provided either as a static
 * value or, more commonly in a reactive pipeline, as a function that will be evaluated
 * later against an {@link ExecutionContext}. This deferred configuration pattern
 * allows the same spec to be reused across different chat interactions where the actual
 * values (like the current user message or historical data) are only known at execution time.
 * </p>
 * <p>
 * Once all parameters have been set, calling one of the terminal methods
 * ({@link #general()}, {@link #stream()}, or {@link #structured()}) aggregates the
 * configurations into an {@link ExecutionSpec} (specifically a {@link ChatExecutionSpec}),
 * looks up the appropriate provider via the {@link #llmProviderRegistry}, and creates the
 * corresponding execution handler. Thus, this class acts as a bridge between the high-level
 * configuration DSL and the low-level execution invoker.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ConfigurableChatSpec
 * @see ChatExecutionSpec
 * @see ProviderConfigureInfo
 */
@Getter(AccessLevel.PROTECTED)
public class DefaultConfigurableChatSpec implements ConfigurableChatSpec {

    /**
     * The type of LLM client (e.g., CHAT, EMBEDDING) that determines the category of the request.
     * This is fixed at construction time and cannot be changed later.
     */
    @NonNull
    private final LlmClientType llmClientType;

    /**
     * The central registry for LLM providers, used to resolve a concrete provider implementation
     * when building the execution handler.
     */
    @NonNull
    private final LlmProviderRegistry llmProviderRegistry;

    /**
     * Information carried over from a previous provider selection step, such as parent attributes,
     * provider filters, and profile pickers. This data will be merged into the final {@link ExecutionSpec}.
     */
    @NonNull
    private final ProviderConfigureInfo providerConfigureInfo;

    /**
     * Function to dynamically provide the tool‑choice instruction (e.g., "auto", "none", or a specific tool ID).
     * If set, it will be evaluated during execution to influence the LLM's tool‑calling behavior.
     */
    private Function<ExecutionContext, String> toolChoiceConfigure;

    /**
     * Function to dynamically provide the model name or identifier (e.g., "gpt-4", "claude-2").
     * This is typically the most important decision point in the spec.
     */
    private Function<ExecutionContext, String> modelNameConfigure;

    /**
     * Function to dynamically provide the sampling temperature (a double between 0.0 and 2.0).
     * Controls the randomness of the output.
     */
    private Function<ExecutionContext, Double> temperatureConfigure;

    /**
     * Function to dynamically provide the top‑p (nucleus sampling) value.
     * An alternative to temperature that limits the vocabulary to the most probable tokens.
     */
    private Function<ExecutionContext, Double> topPConfigure;

    /**
     * Function to dynamically request metadata about token usage in the response.
     * Many providers can include this information if asked.
     */
    private Function<ExecutionContext, Boolean> includeUsageConfigure;

    /**
     * Function to dynamically provide a reasoning string or system‑level instruction
     * that guides the overall behavior of the model. Often used for chain‑of‑thought
     * or role definition.
     */
    private Function<ExecutionContext, String> reasoningConfigure;

    /**
     * Function to dynamically provide the main user text message for the current turn.
     * This is normally the core input of a conversation.
     */
    private Function<ExecutionContext, String> textMessageConfigure;

    /**
     * Function to dynamically provide a media message (e.g., an image or audio) for multimodal models.
     */
    private Function<ExecutionContext, MediaMessage> mediaMessageConfigure;

    /**
     * Function to dynamically provide the system message, which sets the assistant's persona
     * or high‑level instructions.
     */
    private Function<ExecutionContext, String> systemMessageConfigure;

    /**
     * Function to dynamically provide the conversation history (list of previous {@link Message}s).
     * This enables stateful multi‑turn exchanges.
     */
    private Function<ExecutionContext, List<Message>> historicalMessageConfigure;

    /**
     * Function to dynamically provide the maximum number of tokens the model should generate.
     * Limits the length of the response.
     */
    private Function<ExecutionContext, Integer> maxCompletionTokensConfigure;

    /**
     * Function to dynamically provide the collection of {@link ToolDefinition}s available to the model.
     * Tools enable the model to request external function calls.
     */
    private Function<ExecutionContext, Collection<ToolDefinition>> toolsConfigure;

    /**
     * Function to dynamically provide the results of previous tool calls, allowing the model
     * to continue a conversation based on tool outputs.
     */
    private Function<ExecutionContext, Collection<ToolResultMessage>> toolsResultMessageConfigure;

    /**
     * Consumer to customize the raw JSON request body right before it is sent to the provider.
     * This is an escape hatch to add provider‑specific fields that are not covered by the high‑level API.
     */
    private BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;

    /**
     * Flag indicating whether the execution should filter out duplicate tool calls when processing
     * a list of requested tool invocations.
     */
    private boolean distinctToolCalls;

    /**
     * Constructs a new {@code DefaultConfigurableChatSpec} with mandatory fixed parameters.
     * <p>
     * The object is initially empty (except for the three non‑null arguments) and ready to be
     * configured via the chainable methods of {@link ConfigurableChatSpec}.
     *
     * @param llmClientType         the category of the LLM client (must not be null)
     * @param llmProviderRegistry   the registry that provides access to concrete provider implementations
     * @param providerConfigureInfo the configuration carried over from a prior provider selection step
     */
    public DefaultConfigurableChatSpec(@NonNull LlmClientType llmClientType,
                                       @NonNull LlmProviderRegistry llmProviderRegistry,
                                       @NonNull ProviderConfigureInfo providerConfigureInfo) {
        this.llmClientType = llmClientType;
        this.llmProviderRegistry = llmProviderRegistry;
        this.providerConfigureInfo = providerConfigureInfo;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Stores the given {@code modelNameConfigure} function; later, when the spec is finalized,
     * it will be placed into the {@link ChatExecutionSpec#getModelNameConfigure()} slot.
     */
    @Override
    public ConfigurableChatSpec model(@NonNull Function<ExecutionContext, String> modelNameConfigure) {
        this.modelNameConfigure = modelNameConfigure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The temperature value is commonly used to control the creativity of the response.
     */
    @Override
    public ConfigurableChatSpec temperature(@NonNull Function<ExecutionContext, Double> temperatureConfigure) {
        this.temperatureConfigure = temperatureConfigure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Top‑p is an alternative sampling parameter; smaller values produce more focused outputs.
     */
    @Override
    public ConfigurableChatSpec topP(@NonNull Function<ExecutionContext, Double> topPConfigure) {
        this.topPConfigure = topPConfigure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * When enabled, the response object (if supported by the provider) will include
     * detailed token usage information that can be useful for monitoring or billing.
     */
    @Override
    public ConfigurableChatSpec includeUsage(@NonNull Function<ExecutionContext, Boolean> includeUsageConfigure) {
        this.includeUsageConfigure = includeUsageConfigure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The reasoning string can be used to inject a chain‑of‑thought or a pre‑prompt
     * that is passed directly to the model (if the provider supports it).
     */
    @Override
    public ConfigurableChatSpec reasoning(@NonNull Function<ExecutionContext, String> reasoningConfigure) {
        this.reasoningConfigure = reasoningConfigure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This is the primary user message of the current conversation turn.
     */
    @Override
    public ConfigurableChatSpec textMessage(@NonNull Function<ExecutionContext, String> textMessageConfigure) {
        this.textMessageConfigure = textMessageConfigure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * For multimodal models, this function can supply an image, audio, or other media
     * that the model should process together with the text message.
     */
    @Override
    public ConfigurableChatSpec mediaMessage(@NonNull Function<ExecutionContext, MediaMessage> mediaMessageConfigure) {
        this.mediaMessageConfigure = mediaMessageConfigure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The system message typically defines the assistant's persona or global rules.
     */
    @Override
    public ConfigurableChatSpec systemMessage(@NonNull Function<ExecutionContext, String> systemMessageConfigure) {
        this.systemMessageConfigure = systemMessageConfigure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Providing a list of historical turns enables the LLM to understand the ongoing dialog.
     */
    @Override
    public ConfigurableChatSpec historicalMessage(@NonNull Function<ExecutionContext, List<Message>> historicalMessageConfigure) {
        this.historicalMessageConfigure = historicalMessageConfigure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Limiting the completion length helps control cost and prevent overly long outputs.
     */
    @Override
    public ConfigurableChatSpec maxCompletionTokens(@NonNull Function<ExecutionContext, Integer> maxCompletionTokensConfigure) {
        this.maxCompletionTokensConfigure = maxCompletionTokensConfigure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned collection typically includes both the tool definitions and their
     * JSON‑Schema representations so that the LLM can decide when to invoke them.
     */
    @Override
    public ConfigurableChatSpec tools(@NonNull Function<ExecutionContext, Collection<ToolDefinition>> toolsConfigure) {
        this.toolsConfigure = toolsConfigure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * When set to {@code true}, the execution will automatically remove duplicate tool requests
     * that refer to the same tool with identical arguments, reducing redundancy.
     */
    @Override
    public ConfigurableChatSpec distinctToolCalls(boolean distinctToolCalls) {
        this.distinctToolCalls = distinctToolCalls;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned string typically maps to one of the standard tool‑choice modes
     * ("none", "auto", "required", or a specific tool name) supported by the provider.
     */
    @Override
    public ConfigurableChatSpec toolChoice(@NonNull Function<ExecutionContext, String> toolChoiceConfigure) {
        this.toolChoiceConfigure = toolChoiceConfigure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * After a tool has been executed, its result must be fed back to the model so that
     * the conversation can continue. This function supplies those results dynamically.
     */
    @Override
    public ConfigurableChatSpec toolsResponse(@NonNull Function<ExecutionContext, Collection<ToolResultMessage>> toolsResultMessageConfigure) {
        this.toolsResultMessageConfigure = toolsResultMessageConfigure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This customizer gives direct access to the raw JSON payload (as an {@link ObjectNode})
     * before it is serialized and sent. It is intended for provider‑specific extensions
     * that are not covered by the standard configuration API.
     */
    @Override
    public ConfigurableChatSpec rawRequestCustomizer(@NonNull BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure) {
        this.rawRequestCustomizerConfigure = rawRequestCustomizerConfigure;
        return this;
    }

    /**
     * Creates and returns a handler for general (synchronous, non‑streaming) chat execution.
     * <p>
     * Internally, it first consolidates all configured values into a {@link ChatExecutionSpec}.
     * Then it locates an appropriate provider (using the spec's filtering and provider‑selection
     * rules) and wraps it in a {@link ChatGeneralExecution} that implements {@link GeneralExecution}.
     *
     * @return a new {@code GeneralExecution} ready to process a chat request
     */
    @Override
    public GeneralExecution general() {
        return ChatGeneralExecution.of(this.llmProviderRegistry, this.toChatExecutionSpec());
    }

    /**
     * Creates and returns a handler for streaming chat execution.
     * <p>
     * Similarly to {@link #general()}, but the created {@link ChatStreamExecution}
     * produces a reactive stream of response chunks, enabling real‑time display.
     *
     * @return a new {@code StreamExecution} ready to stream chat responses
     */
    @Override
    public StreamExecution stream() {
        return ChatStreamExecution.of(this.llmProviderRegistry, this.toChatExecutionSpec());
    }

    /**
     * Creates and returns a handler for structured chat execution.
     * <p>
     * This handler expects the LLM to return a structured output (e.g., JSON that conforms
     * to a predefined schema) and automatically maps it to a Java object via a type reference
     * that must be supplied at execution time.
     *
     * @return a new {@code StructuredExecution} ready to process a structured chat request
     */
    @Override
    public StructuredExecution structured() {
        return ChatStructuredExecution.of(this.llmProviderRegistry, this.toChatExecutionSpec());
    }

    /**
     * Aggregates all the configured parameters into a single {@link ChatExecutionSpec} instance.
     * <p>
     * This method is the key internal step that translates the chainable builder's state into an
     * immutable, self‑contained specification object. It also merges the pre‑existing
     * {@link #providerConfigureInfo} data, ensuring that provider selection hints (like default
     * provider flags or profile pickers) are carried forward. Null checks are performed on
     * optional configuration functions so that only non‑null functions are included in the
     * builder, preserving the ability for downstream code to distinguish between "not configured"
     * and "configured with a null‑returning function".
     *
     * @return a fully populated {@link ChatExecutionSpec} ready for use with an execution handler
     */
    protected ChatExecutionSpec toChatExecutionSpec() {
        var builder = ChatExecutionSpec.builder();
        if (Objects.nonNull(this.toolChoiceConfigure)) {
            builder.toolChoiceConfigure(this.toolChoiceConfigure);
        }
        if (Objects.nonNull(this.rawRequestCustomizerConfigure)) {
            builder.rawRequestCustomizerConfigure(this.rawRequestCustomizerConfigure);
        }
        if (Objects.nonNull(this.includeUsageConfigure)) {
            builder.includeUsageConfigure(this.includeUsageConfigure);
        }
        return builder.llmClientType(llmClientType)
                .parentAttributes(providerConfigureInfo.getParentAttributes())
                .contextConfigure(providerConfigureInfo.getContextConfigure())
                .defaultProvider(providerConfigureInfo.isDefaultProvider())
                .providerFilter(providerConfigureInfo.getProviderFilter())
                .defaultProfile(providerConfigureInfo.isDefaultProfile())
                .profilePicker(providerConfigureInfo.getProfilePicker())
                .modelNameConfigure(this.modelNameConfigure)
                .temperatureConfigure(this.temperatureConfigure)
                .topPConfigure(this.topPConfigure)
                .reasoningConfigure(this.reasoningConfigure)
                .maxCompletionTokensConfigure(this.maxCompletionTokensConfigure)
                .systemMessageConfigure(this.systemMessageConfigure)
                .historicalMessageConfigure(this.historicalMessageConfigure)
                .textMessageConfigure(this.textMessageConfigure)
                .mediaMessageConfigure(this.mediaMessageConfigure)
                .toolsConfigure(this.toolsConfigure)
                .toolResultMessageConfigure(this.toolsResultMessageConfigure)
                .distinctToolCalls(this.distinctToolCalls)
                .build();
    }
}