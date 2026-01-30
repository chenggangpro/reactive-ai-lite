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
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.execution.GeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.StreamExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.StructuredExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat.ChatGeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat.ChatStreamExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat.ChatStructuredExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.TextMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableChatSpec;
import pro.chenggang.project.reactive.ai.lite.core.tool.LlmToolCallResponse;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * This class represents a default implementation of the ConfigurableChatSpec interface.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter(AccessLevel.PROTECTED)
public class DefaultConfigurableChatSpec implements ConfigurableChatSpec {

    private final LlmClientType llmClientType;
    private final LlmProviderRegistry llmProviderRegistry;
    private final DefaultExecutionContextSpec defaultExecutionContextSpec;
    private final DefaultProviderSpec defaultProviderSpec;

    private Function<ExecutionContextView, String> toolChoiceConfigure;
    private Function<ExecutionContextView, String> modelNameConfigure;
    private Function<ExecutionContextView, Double> temperatureConfigure;
    private Function<ExecutionContextView, Double> topPConfigure;
    private Function<ExecutionContextView, Boolean> includeUsageConfigure;
    private Function<ExecutionContextView, String> reasoningConfigure;
    private Function<ExecutionContextView, TextMessage> textMessageConfigure;
    private Function<ExecutionContextView, MediaMessage> mediaMessageConfigure;
    private Function<ExecutionContextView, TextMessage> systemMessageConfigure;
    private Function<ExecutionContextView, Collection<Message>> historicalMessageConfigure;
    private Function<ExecutionContextView, ObjectNode> latestAssistantMessageConfigure;
    private Function<ExecutionContextView, Integer> maxCompletionTokensConfigure;
    private BiConsumer<ExecutionContextView, ObjectNode> rawRequestCustomizer;
    private BiConsumer<ExecutionContextView, RawResponse> rawResponseCustomizer;
    private BiConsumer<ExecutionContextView, RawStreamResponse> rawStreamResponseCustomizer;
    private Function<ExecutionContextView, Collection<ToolDefinition>> toolsConfigure;
    private Function<ExecutionContextView, Collection<LlmToolCallResponse>> toolsResponseConfigure;
    private boolean distinctToolCalls;

    protected DefaultConfigurableChatSpec(@NonNull LlmClientType llmClientType,
                                          @NonNull LlmProviderRegistry llmProviderRegistry,
                                          @NonNull DefaultExecutionContextSpec defaultExecutionContextSpec,
                                          @NonNull DefaultProviderSpec defaultProviderSpec) {
        this.llmClientType = llmClientType;
        this.llmProviderRegistry = llmProviderRegistry;
        this.defaultExecutionContextSpec = defaultExecutionContextSpec;
        this.defaultProviderSpec = defaultProviderSpec;
    }

    @Override
    public ConfigurableChatSpec model(@NonNull Function<ExecutionContextView, String> modelNameConfigure) {
        this.modelNameConfigure = modelNameConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec temperature(@NonNull Function<ExecutionContextView, Double> temperatureConfigure) {
        this.temperatureConfigure = temperatureConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec topP(@NonNull Function<ExecutionContextView, Double> topPConfigure) {
        this.topPConfigure = topPConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec includeUsage(@NonNull Function<ExecutionContextView, Boolean> includeUsageConfigure) {
        this.includeUsageConfigure = includeUsageConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec reasoning(@NonNull Function<ExecutionContextView, String> reasoningConfigure) {
        this.reasoningConfigure = reasoningConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec textMessage(@NonNull Function<ExecutionContextView, TextMessage> textMessageConfigure) {
        this.textMessageConfigure = textMessageConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec mediaMessage(@NonNull Function<ExecutionContextView, MediaMessage> mediaMessageConfigure) {
        this.mediaMessageConfigure = mediaMessageConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec systemMessage(@NonNull Function<ExecutionContextView, TextMessage> systemMessageConfigure) {
        this.systemMessageConfigure = systemMessageConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec historicalMessage(@NonNull Function<ExecutionContextView, Collection<Message>> historicalMessageConfigure) {
        this.historicalMessageConfigure = historicalMessageConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec latestAssistantMessage(@NonNull Function<ExecutionContextView, ObjectNode> latestAssistantMessageConfigure) {
        this.latestAssistantMessageConfigure = latestAssistantMessageConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec maxCompletionTokens(@NonNull Function<ExecutionContextView, Integer> maxCompletionTokensConfigure) {
        this.maxCompletionTokensConfigure = maxCompletionTokensConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec tools(@NonNull Function<ExecutionContextView, Collection<ToolDefinition>> toolsConfigure) {
        this.toolsConfigure = toolsConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec distinctToolCalls(boolean distinctToolCalls) {
        this.distinctToolCalls = distinctToolCalls;
        return this;
    }

    @Override
    public ConfigurableChatSpec toolChoice(@NonNull Function<ExecutionContextView, String> toolChoiceConfigure) {
        this.toolChoiceConfigure = toolChoiceConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec toolsResponse(@NonNull Function<ExecutionContextView, Collection<LlmToolCallResponse>> toolsResponseConfigure) {
        this.toolsResponseConfigure = toolsResponseConfigure;
        return this;
    }

    @Override
    public ConfigurableChatSpec rawRequestCustomizer(@NonNull BiConsumer<ExecutionContextView, ObjectNode> rawRequestCustomizer) {
        this.rawRequestCustomizer = rawRequestCustomizer;
        return this;
    }

    @Override
    public ConfigurableChatSpec rawResponseCustomizer(@NonNull BiConsumer<ExecutionContextView, RawResponse> rawResponseCustomizer) {
        this.rawResponseCustomizer = rawResponseCustomizer;
        return this;
    }

    @Override
    public ConfigurableChatSpec rawStreamResponseCustomizer(@NonNull BiConsumer<ExecutionContextView, RawStreamResponse> rawStreamResponseCustomizer) {
        this.rawStreamResponseCustomizer = rawStreamResponseCustomizer;
        return this;
    }

    @Override
    public GeneralExecution general() {
        return ChatGeneralExecution.of(this.llmProviderRegistry, this.toExecutionSpec());
    }

    @Override
    public StreamExecution stream() {
        return ChatStreamExecution.of(this.llmProviderRegistry, this.toExecutionSpec());
    }

    @Override
    public StructuredExecution structured() {
        return ChatStructuredExecution.of(this.llmProviderRegistry, this.toExecutionSpec());
    }

    protected ExecutionSpec toExecutionSpec() {
        var builder = ExecutionSpec.builder();
        if (Objects.nonNull(defaultExecutionContextSpec.getTraceIdGenerator())) {
            builder.traceIdGenerator(defaultExecutionContextSpec.getTraceIdGenerator());
        }
        if (Objects.nonNull(this.toolChoiceConfigure)) {
            builder.toolChoiceConfigure(this.toolChoiceConfigure);
        }
        return builder.llmClientType(llmClientType)
                .parentTraceId(defaultExecutionContextSpec.getParentTraceId())
                .parentAttributes(defaultExecutionContextSpec.getParentAttributes())
                .contextConfigure(defaultExecutionContextSpec.getContextConfigure())
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
                .latestAssistantMessageConfigure(this.latestAssistantMessageConfigure)
                .textMessageConfigure(this.textMessageConfigure)
                .mediaMessageConfigure(this.mediaMessageConfigure)
                .rawRequestCustomizer(this.rawRequestCustomizer)
                .rawResponseCustomizer(this.rawResponseCustomizer)
                .rawStreamResponseCustomizer(this.rawStreamResponseCustomizer)
                .toolsConfigure(this.toolsConfigure)
                .toolsResponseConfigure(this.toolsResponseConfigure)
                .distinctToolCalls(this.distinctToolCalls)
                .build();
    }
}
