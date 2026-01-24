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
package pro.chenggang.project.reactive.ai.lite.core.execution.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.TraceId;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.TextMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.tool.LlmToolCallResponse;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionSpec {

    @NonNull
    private final LlmClientType llmClientType;
    private final boolean defaultProvider;
    private final boolean defaultProfile;
    private final TraceId parentTraceId;
    private final Map<String, Object> parentAttributes;
    @NonNull
    @Builder.Default
    private final Supplier<String> traceIdGenerator = () -> UUID.randomUUID().toString();
    private final Consumer<ExecutionContext> contextConfigure;
    private final BiPredicate<LlmProviderInfo, ExecutionContextView> providerFilter;
    private final BiFunction<ExecutionContextView, Set<String>, String> profilePicker;
    private final Function<ExecutionContextView, TextMessage> defaultSystemMessageConfigure;
    @NonNull
    private final Function<ExecutionContextView, String> modelNameConfigure;
    private final Function<ExecutionContextView, Double> temperatureConfigure;
    private final Function<ExecutionContextView, Double> topPConfigure;
    private final Function<ExecutionContextView, Integer> maxCompletionTokensConfigure;
    private final Function<ExecutionContextView, Map<String, Object>> extraDataConfigure;
    private final Function<ExecutionContextView, TextMessage> textMessageConfigure;
    private final Function<ExecutionContextView, MediaMessage> mediaMessageConfigure;
    private final Function<ExecutionContextView, TextMessage> systemMessageConfigure;
    private final Function<ExecutionContextView, Collection<Message>> historicalMessageConfigure;
    private final Function<ExecutionContextView, ObjectNode> latestAssistantMessageConfigure;
    private final BiConsumer<ExecutionContextView, ObjectNode> rawRequestCustomizer;
    private final BiConsumer<ExecutionContextView, RawResponse> rawResponseCustomizer;
    private final BiConsumer<ExecutionContextView, RawStreamResponse> rawStreamResponseCustomizer;
    private final Function<ExecutionContextView, Collection<ToolDefinition>> toolsConfigure;
    @Builder.Default
    private final Function<ExecutionContextView, String> toolChoiceConfigure = __ -> "auto";
    private final Function<ExecutionContextView, Collection<LlmToolCallResponse>> toolsResponseConfigure;

    public ExecutionContext newExecutionContext() {
        String currentTraceId = this.traceIdGenerator.get();
        if (Objects.isNull(currentTraceId)) {
            throw new IllegalArgumentException("TraceId generator must not return null.");
        }
        String parentTraceId = Objects.isNull(this.parentTraceId) ? null : this.parentTraceId.getCurrentId();
        TraceId traceId = TraceId.of(parentTraceId, currentTraceId);
        ExecutionContext executionContext = ExecutionContext.newContextWith(traceId);
        if (Objects.nonNull(this.parentAttributes)) {
            executionContext.getAttributes().putAll(this.parentAttributes);
        }
        if (Objects.nonNull(this.contextConfigure)) {
            contextConfigure.accept(executionContext);
        }
        return executionContext;
    }

    public ExecutionInfo newExecutionInfo(@NonNull ExecutionContext executionContext) {
        return ExecutionInfo.builder()
                .executionContext(executionContext)
                .profilePicker(this.profilePicker)
                .defaultProfile(this.defaultProfile)
                .defaultSystemMessageConfigure(this.defaultSystemMessageConfigure)
                .modelNameConfigure(this.modelNameConfigure)
                .temperatureConfigure(this.temperatureConfigure)
                .topPConfigure(this.topPConfigure)
                .maxCompletionTokensConfigure(this.maxCompletionTokensConfigure)
                .extraDataConfigure(this.extraDataConfigure)
                .textMessageConfigure(this.textMessageConfigure)
                .mediaMessageConfigure(this.mediaMessageConfigure)
                .systemMessageConfigure(this.systemMessageConfigure)
                .historicalMessageConfigure(this.historicalMessageConfigure)
                .latestAssistantMessageConfigure(this.latestAssistantMessageConfigure)
                .rawRequestCustomizer(this.rawRequestCustomizer)
                .rawResponseCustomizer(this.rawResponseCustomizer)
                .rawStreamResponseCustomizer(this.rawStreamResponseCustomizer)
                .toolsConfigure(this.toolsConfigure)
                .toolChoiceConfigure(this.toolChoiceConfigure)
                .toolsResponseConfigure(this.toolsResponseConfigure)
                .build();
    }
}
