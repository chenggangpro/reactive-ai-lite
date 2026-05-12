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
import pro.chenggang.project.reactive.ai.lite.core.message.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolResultMessage;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An immutable container that holds all the dynamic configuration functions
 * and the actual runtime context necessary to construct an LLM request.
 * <p>
 * During the execution phase, the provider iterates over the functions defined in this
 * object, applying them to the encapsulated {@link ExecutionContext} to resolve
 * the static values needed for the final payload (e.g., resolving the specific model name,
 * temperature, or list of historical messages).
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionInfo {

    /**
     * The mutable execution context that tracks state and parsingAttributes for this specific run.
     */
    @NonNull
    private final ExecutionContext executionContext;

    /**
     * Indicates whether the default profile should be used.
     */
    private final boolean defaultProfile;

    /**
     * A function to dynamically pick a profile from a set of available profiles.
     */
    private final BiFunction<ExecutionContextView, Set<String>, String> profilePicker;

    /**
     * A function to dynamically configure the default system message.
     */
    private final Function<ExecutionContextView, String> defaultSystemMessageConfigure;

    /**
     * A function to dynamically configure the specific model name to use.
     */
    @NonNull
    private final Function<ExecutionContextView, String> modelNameConfigure;

    /**
     * A function to dynamically configure the temperature setting.
     */
    private final Function<ExecutionContextView, Double> temperatureConfigure;

    /**
     * A function to dynamically configure the Top-P sampling parameter.
     */
    private final Function<ExecutionContextView, Double> topPConfigure;

    /**
     * A function to dynamically determine whether usage metrics should be requested.
     */
    private final Function<ExecutionContextView, Boolean> includeUsageConfigure;

    /**
     * A function to dynamically configure reasoning or thinking parameters.
     */
    private final Function<ExecutionContextView, String> reasoningConfigure;

    /**
     * A function to dynamically configure the maximum number of completion tokens to generate.
     */
    private final Function<ExecutionContextView, Integer> maxCompletionTokensConfigure;

    /**
     * A function to dynamically configure the user's text message.
     */
    private final Function<ExecutionContextView, String> textMessageConfigure;

    /**
     * A function to dynamically configure a user's media message.
     */
    private final Function<ExecutionContextView, MediaMessage> mediaMessageConfigure;

    /**
     * A function to dynamically configure the system message.
     */
    private final Function<ExecutionContextView, String> systemMessageConfigure;

    /**
     * A function to dynamically configure the conversation history.
     */
    private final Function<ExecutionContextView, List<Message>> historicalMessageConfigure;

    /**
     * A function to dynamically configure the set of available tools.
     */
    private final Function<ExecutionContextView, Collection<ToolDefinition>> toolsConfigure;

    /**
     * A function to dynamically configure the tool choice behavior.
     */
    private final Function<ExecutionContextView, String> toolChoiceConfigure;

    /**
     * A function to dynamically configure the tool execution results to send back to the model.
     */
    private final Function<ExecutionContextView, Collection<ToolResultMessage>> toolResultMessageConfigure;

    /**
     * A consumer to dynamically customize the raw request JSON object before it is sent.
     */
    private final BiConsumer<ExecutionContextView, ObjectNode> rawRequestCustomizerConfigure;

    /**
     * A flag indicating whether distinct tool calls should be enforced.
     */
    private final boolean distinctToolCalls;

}
