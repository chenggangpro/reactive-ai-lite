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
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.TextMessage;
import pro.chenggang.project.reactive.ai.lite.core.tool.LlmToolCallResponse;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionInfo {

    @NonNull
    private final ExecutionContext executionContext;
    private final boolean defaultProfile;
    private final BiFunction<ExecutionContextView, Set<String>, String> profilePicker;
    private final Function<ExecutionContextView, TextMessage> defaultSystemMessageConfigure;
    @NonNull
    private final Function<ExecutionContextView, String> modelNameConfigure;
    private final Function<ExecutionContextView, Double> temperatureConfigure;
    private final Function<ExecutionContextView, Double> topPConfigure;
    private final Function<ExecutionContextView, Boolean> includeUsageConfigure;
    private final Function<ExecutionContextView, String> reasoningConfigure;
    private final Function<ExecutionContextView, Integer> maxCompletionTokensConfigure;
    private final Function<ExecutionContextView, TextMessage> textMessageConfigure;
    private final Function<ExecutionContextView, MediaMessage> mediaMessageConfigure;
    private final Function<ExecutionContextView, TextMessage> systemMessageConfigure;
    private final Function<ExecutionContextView, Collection<Message>> historicalMessageConfigure;
    private final Function<ExecutionContextView, ObjectNode> latestAssistantMessageConfigure;
    private final BiConsumer<ExecutionContextView, ObjectNode> rawRequestCustomizer;
    private final BiConsumer<ExecutionContextView, RawResponse> rawResponseCustomizer;
    private final BiConsumer<ExecutionContextView, RawStreamResponse> rawStreamResponseCustomizer;
    private final Function<ExecutionContextView, Collection<ToolDefinition>> toolsConfigure;
    private final Function<ExecutionContextView, String> toolChoiceConfigure;
    private final Function<ExecutionContextView, Collection<LlmToolCallResponse>> toolsResponseConfigure;
    private final boolean distinctToolCalls;

}
