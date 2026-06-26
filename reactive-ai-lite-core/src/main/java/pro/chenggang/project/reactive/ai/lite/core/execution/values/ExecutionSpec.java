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
import pro.chenggang.project.reactive.ai.lite.core.message.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolResultMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.spec.ExecutionContextSpec.ContextMerger;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Represents the complete execution specification for an LLM request.
 * <p>
 * This class aggregates all the configuration settings collected through the fluent API
 * (like {@link pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableChatSpec}).
 * It holds the provider selection logic, context setup functions, and message generation functions.
 * Before an actual request is made, this spec is used to instantiate the runtime
 * {@link ExecutionContext} and {@link ExecutionInfo}.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionSpec {

    /**
     * The type of LLM client required (e.g., CHAT).
     */
    @NonNull
    private final LlmClientType llmClientType;

    /**
     * Whether to use the default provider.
     */
    private final boolean defaultProvider;

    /**
     * Whether to use the default profile of the selected provider.
     */
    private final boolean defaultProfile;

    /**
     * Attributes inherited from a parent execution context.
     */
    private final Map<String, Object> parentAttributes;

    /**
     * A consumer to perform custom configuration on the execution context.
     */
    private final ContextMerger contextConfigure;

    /**
     * A predicate to dynamically filter and select an LLM provider.
     */
    private final BiPredicate<LlmProviderInfo, ExecutionContext> providerFilter;

    /**
     * A function to dynamically select a profile for the chosen provider.
     */
    private final BiFunction<ExecutionContext, Set<String>, String> profilePicker;

    /**
     * A function to dynamically generate a default system message.
     */
    private final Function<ExecutionContext, String> defaultSystemMessageConfigure;

    /**
     * A function to dynamically configure the specific model name.
     */
    @NonNull
    private final Function<ExecutionContext, String> modelNameConfigure;

    /**
     * A function to dynamically configure the temperature setting.
     */
    private final Function<ExecutionContext, Double> temperatureConfigure;

    /**
     * A function to dynamically configure the Top-P sampling parameter.
     */
    private final Function<ExecutionContext, Double> topPConfigure;

    /**
     * A function to dynamically determine whether usage metrics should be requested.
     */
    private final Function<ExecutionContext, Boolean> includeUsageConfigure;

    /**
     * A function to dynamically configure reasoning parameters.
     */
    private final Function<ExecutionContext, String> reasoningConfigure;

    /**
     * A function to dynamically configure the maximum number of completion tokens.
     */
    private final Function<ExecutionContext, Integer> maxCompletionTokensConfigure;

    /**
     * A function to dynamically configure the user's text message.
     */
    private final Function<ExecutionContext, String> textMessageConfigure;

    /**
     * A function to dynamically configure a user's media message.
     */
    private final Function<ExecutionContext, MediaMessage> mediaMessageConfigure;

    /**
     * A function to dynamically configure the system message.
     */
    private final Function<ExecutionContext, String> systemMessageConfigure;

    /**
     * A function to dynamically configure the conversation history.
     */
    private final Function<ExecutionContext, List<Message>> historicalMessageConfigure;

    /**
     * A function to dynamically configure the available tools.
     */
    private final Function<ExecutionContext, Collection<ToolDefinition>> toolsConfigure;

    /**
     * A function to dynamically configure the tool choice behavior. Defaults to returning "auto".
     */
    @Builder.Default
    private final Function<ExecutionContext, String> toolChoiceConfigure = __ -> "auto";

    /**
     * A function to dynamically configure the results of previous tool calls.
     */
    private final Function<ExecutionContext, Collection<ToolResultMessage>> toolResultMessageConfigure;

    /**
     * A consumer to dynamically customize the raw request JSON node before it is sent to the LLM provider.
     */
    @Builder.Default
    private final BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure = ((executionContext, jsonNodes) -> {});

    /**
     * Whether to filter distinct tool calls from the provider's response.
     */
    private final boolean distinctToolCalls;

    /**
     * Creates a new {@link ExecutionInfo} object binding this specification's
     * dynamic configuration functions to the given runtime execution context.
     *
     * @param executionContext the runtime execution context
     * @return a new {@link ExecutionInfo} instance ready for execution
     */
    public ExecutionInfo newExecutionInfo(@NonNull ExecutionContext executionContext) {
        return ExecutionInfo.builder()
                .profilePicker(this.profilePicker)
                .defaultProfile(this.defaultProfile)
                .defaultSystemMessageConfigure(this.defaultSystemMessageConfigure)
                .modelNameConfigure(this.modelNameConfigure)
                .temperatureConfigure(this.temperatureConfigure)
                .topPConfigure(this.topPConfigure)
                .includeUsageConfigure(this.includeUsageConfigure)
                .reasoningConfigure(this.reasoningConfigure)
                .maxCompletionTokensConfigure(this.maxCompletionTokensConfigure)
                .textMessageConfigure(this.textMessageConfigure)
                .mediaMessageConfigure(this.mediaMessageConfigure)
                .systemMessageConfigure(this.systemMessageConfigure)
                .historicalMessageConfigure(this.historicalMessageConfigure)
                .toolsConfigure(this.toolsConfigure)
                .toolChoiceConfigure(this.toolChoiceConfigure)
                .toolResultMessageConfigure(this.toolResultMessageConfigure)
                .rawRequestCustomizerConfigure(this.rawRequestCustomizerConfigure)
                .distinctToolCalls(this.distinctToolCalls)
                .build();
    }
}
