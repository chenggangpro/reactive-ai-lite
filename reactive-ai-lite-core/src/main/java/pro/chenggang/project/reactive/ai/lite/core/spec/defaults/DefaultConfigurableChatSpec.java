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
 * This class collects all the configuration parameters for a chat request,
 * such as the model name, temperature, messages, and tools, either as static
 * values or dynamic functions. It then uses this configuration to build an
 * {@link ExecutionSpec} and create the appropriate execution handler (general,
 * stream, or structured).
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter(AccessLevel.PROTECTED)
public class DefaultConfigurableChatSpec implements ConfigurableChatSpec {

    @NonNull
    private final LlmClientType llmClientType;
    @NonNull
    private final LlmProviderRegistry llmProviderRegistry;
    @NonNull
    private final DefaultProviderSpec defaultProviderSpec;

    private Function<ExecutionContext, String> toolChoiceConfigure;
    private Function<ExecutionContext, String> modelNameConfigure;
    private Function<ExecutionContext, Double> temperatureConfigure;
    private Function<ExecutionContext, Double> topPConfigure;
    private Function<ExecutionContext, Boolean> includeUsageConfigure;
    private Function<ExecutionContext, String> reasoningConfigure;
    private Function<ExecutionContext, String> textMessageConfigure;
    private Function<ExecutionContext, MediaMessage> mediaMessageConfigure;
    private Function<ExecutionContext, String> systemMessageConfigure;
    private Function<ExecutionContext, List<Message>> historicalMessageConfigure;
    private Function<ExecutionContext, Integer> maxCompletionTokensConfigure;
    private Function<ExecutionContext, Collection<ToolDefinition>> toolsConfigure;
    private Function<ExecutionContext, Collection<ToolResultMessage>> toolsResultMessageConfigure;
    private BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;
    private boolean distinctToolCalls;

    /**
     * Constructs a new {@link DefaultConfigurableChatSpec}.
     *
     * @param llmClientType               the type of client
     * @param llmProviderRegistry         the registry for looking up providers
     * @param defaultProviderSpec         the preceding provider specification
     */
    public DefaultConfigurableChatSpec(@NonNull LlmClientType llmClientType,
                                       @NonNull LlmProviderRegistry llmProviderRegistry,
                                       @NonNull DefaultProviderSpec defaultProviderSpec) {
        this.llmClientType = llmClientType;
        this.llmProviderRegistry = llmProviderRegistry;
        this.defaultProviderSpec = defaultProviderSpec;
    }

    /**
     * Dynamically configures the model name.
     *
     * @param modelNameConfigure a function that returns the model name
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec model(@NonNull Function<ExecutionContext, String> modelNameConfigure) {
        this.modelNameConfigure = modelNameConfigure;
        return this;
    }

    /**
     * Dynamically configures the temperature.
     *
     * @param temperatureConfigure a function that returns the temperature
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec temperature(@NonNull Function<ExecutionContext, Double> temperatureConfigure) {
        this.temperatureConfigure = temperatureConfigure;
        return this;
    }

    /**
     * Dynamically configures the Top-P value.
     *
     * @param topPConfigure a function that returns the Top-P value
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec topP(@NonNull Function<ExecutionContext, Double> topPConfigure) {
        this.topPConfigure = topPConfigure;
        return this;
    }

    /**
     * Dynamically configures whether to include usage metadata.
     *
     * @param includeUsageConfigure a function that returns a boolean indicating inclusion
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec includeUsage(@NonNull Function<ExecutionContext, Boolean> includeUsageConfigure) {
        this.includeUsageConfigure = includeUsageConfigure;
        return this;
    }

    /**
     * Dynamically configures reasoning/system instructions.
     *
     * @param reasoningConfigure a function that returns the reasoning string
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec reasoning(@NonNull Function<ExecutionContext, String> reasoningConfigure) {
        this.reasoningConfigure = reasoningConfigure;
        return this;
    }

    /**
     * Dynamically configures the user's text message.
     *
     * @param textMessageConfigure a function that returns the text message
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec textMessage(@NonNull Function<ExecutionContext, String> textMessageConfigure) {
        this.textMessageConfigure = textMessageConfigure;
        return this;
    }

    /**
     * Dynamically configures a media message.
     *
     * @param mediaMessageConfigure a function that returns a media message
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec mediaMessage(@NonNull Function<ExecutionContext, MediaMessage> mediaMessageConfigure) {
        this.mediaMessageConfigure = mediaMessageConfigure;
        return this;
    }

    /**
     * Dynamically configures the system message.
     *
     * @param systemMessageConfigure a function that returns the system message
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec systemMessage(@NonNull Function<ExecutionContext, String> systemMessageConfigure) {
        this.systemMessageConfigure = systemMessageConfigure;
        return this;
    }

    /**
     * Dynamically configures historical messages.
     *
     * @param historicalMessageConfigure a function that returns a list of historical messages
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec historicalMessage(@NonNull Function<ExecutionContext, List<Message>> historicalMessageConfigure) {
        this.historicalMessageConfigure = historicalMessageConfigure;
        return this;
    }

    /**
     * Dynamically configures the max completion tokens.
     *
     * @param maxCompletionTokensConfigure a function that returns the max completion tokens
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec maxCompletionTokens(@NonNull Function<ExecutionContext, Integer> maxCompletionTokensConfigure) {
        this.maxCompletionTokensConfigure = maxCompletionTokensConfigure;
        return this;
    }

    /**
     * Dynamically configures the available tools.
     *
     * @param toolsConfigure a function that returns a collection of tool definitions
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec tools(@NonNull Function<ExecutionContext, Collection<ToolDefinition>> toolsConfigure) {
        this.toolsConfigure = toolsConfigure;
        return this;
    }

    /**
     * Configures whether to filter for distinct tool calls.
     *
     * @param distinctToolCalls boolean flag indicating whether to filter distinct tool calls
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec distinctToolCalls(boolean distinctToolCalls) {
        this.distinctToolCalls = distinctToolCalls;
        return this;
    }

    /**
     * Dynamically configures the tool choice behavior.
     *
     * @param toolChoiceConfigure a function that returns the tool choice
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec toolChoice(@NonNull Function<ExecutionContext, String> toolChoiceConfigure) {
        this.toolChoiceConfigure = toolChoiceConfigure;
        return this;
    }

    /**
     * Dynamically configures the result messages from tool calls.
     *
     * @param toolsResultMessageConfigure a function that returns a collection of tool result messages
     * @return this instance for method chaining
     */
    @Override
    public ConfigurableChatSpec toolsResponse(@NonNull Function<ExecutionContext, Collection<ToolResultMessage>> toolsResultMessageConfigure) {
        this.toolsResultMessageConfigure = toolsResultMessageConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec rawRequestCustomizer(@NonNull BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure) {
        this.rawRequestCustomizerConfigure = rawRequestCustomizerConfigure;
        return this;
    }

    /**
     * Creates and returns a handler for general chat execution.
     *
     * @return a {@link GeneralExecution} instance
     */
    @Override
    public GeneralExecution general() {
        return ChatGeneralExecution.of(this.llmProviderRegistry, this.toExecutionSpec());
    }

    /**
     * Creates and returns a handler for streaming chat execution.
     *
     * @return a {@link StreamExecution} instance
     */
    @Override
    public StreamExecution stream() {
        return ChatStreamExecution.of(this.llmProviderRegistry, this.toExecutionSpec());
    }

    /**
     * Creates and returns a handler for structured chat execution.
     *
     * @return a {@link StructuredExecution} instance
     */
    @Override
    public StructuredExecution structured() {
        return ChatStructuredExecution.of(this.llmProviderRegistry, this.toExecutionSpec());
    }

    /**
     * Consolidates all the configured parameters into a single {@link ExecutionSpec} object.
     *
     * @return the consolidated {@link ExecutionSpec}
     */
    protected ExecutionSpec toExecutionSpec() {
        var builder = ExecutionSpec.builder();
        if (Objects.nonNull(this.toolChoiceConfigure)) {
            builder.toolChoiceConfigure(this.toolChoiceConfigure);
        }
        if (Objects.nonNull(this.rawRequestCustomizerConfigure)) {
            builder.rawRequestCustomizerConfigure(this.rawRequestCustomizerConfigure);
        }
        return builder.llmClientType(llmClientType)
                .parentAttributes(defaultProviderSpec.getParentAttributes())
                .contextConfigure(defaultProviderSpec.getContextConfigure())
                .defaultProvider(defaultProviderSpec.isDefaultProvider())
                .providerFilter(defaultProviderSpec.getProviderFilter())
                .defaultProfile(defaultProviderSpec.isDefaultProfile())
                .profilePicker(defaultProviderSpec.getProfilePicker())
                .defaultSystemMessageConfigure(defaultProviderSpec.getDefaultSystemMessageProvider())
                .modelNameConfigure(this.modelNameConfigure)
                .temperatureConfigure(this.temperatureConfigure)
                .topPConfigure(this.topPConfigure)
                .includeUsageConfigure(this.includeUsageConfigure)
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
