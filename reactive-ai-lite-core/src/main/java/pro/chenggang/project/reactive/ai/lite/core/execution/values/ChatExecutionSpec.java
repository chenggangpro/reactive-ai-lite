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

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.message.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolResultMessage;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Defines the specification for an LLM chat execution, encapsulating all configurable aspects
 * of a chat completion request. Each configuration parameter is provided as a
 * {@link Function} that accepts the current {@link ExecutionContext} and returns the
 * corresponding value. This allows dynamic configuration based on runtime context
 * (e.g., different temperatures per session, varying tool sets per user).
 * <p>
 * The specification is intended to be built using the Lombok {@code @SuperBuilder}
 * extension, which supports inheritance from {@link ExecutionSpec}. A builder
 * will typically set static values (e.g., {@code .temperatureConfigure(ctx -> 0.7)})
 * or more complex logic.
 * <p>
 * An instance of this spec can be used to generate a {@link ChatExecutionInfo}
 * via {@link #newExecutionInfo(ExecutionContext)}. The generated info holds
 * resolved functions and metadata that the actual execution handler (e.g., an
 * LLM client) will evaluate at invocation time.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ExecutionSpec
 * @see ChatExecutionInfo
 */
@Getter
@SuperBuilder
public class ChatExecutionSpec extends ExecutionSpec<ChatExecutionInfo> {

    /**
     * Supplies the temperature value to control randomness in generated text.
     * <p>
     * A lower temperature (closer to 0) makes the output more deterministic and focused,
     * while a higher temperature (up to 2 typically) increases diversity and creativity.
     * This function receives the current {@link ExecutionContext}, enabling dynamic
     * temperature selection based on user preferences or session state.
     */
    private final Function<ExecutionContext, Double> temperatureConfigure;

    /**
     * Supplies the top-p (nucleus sampling) parameter.
     * <p>
     * An alternative to temperature, top-p considers the smallest set of tokens whose
     * cumulative probability exceeds the given value (e.g., 0.9). This function is
     * evaluated during execution, allowing runtime tuning of output diversity.
     */
    private final Function<ExecutionContext, Double> topPConfigure;

    /**
     * Determines whether the model response should include token usage statistics.
     * <p>
     * Defaults to a function that always returns {@code true}, ensuring usage information
     * (prompt tokens, completion tokens, total tokens) is returned in the response.
     * Override this to disable usage reporting for specific scenarios, e.g., to reduce
     * overhead or because the LLM provider does not support it.
     */
    @Builder.Default
    private final Function<ExecutionContext, Boolean> includeUsageConfigure = __ -> true;

    /**
     * Provides an optional reasoning/thought process directive for the model.
     * <p>
     * This can be used to inject a specific chain-of-thought instruction or a
     * reasoning depth setting. The exact interpretation depends on the underlying
     * LLM provider. Return {@code null} to omit any reasoning configuration.
     */
    private final Function<ExecutionContext, String> reasoningConfigure;

    /**
     * Defines the maximum number of tokens allowed in the generated completion.
     * <p>
     * This upper bound helps control response length and API costs. The function
     * allows adapting the limit based on the execution context (e.g., shorter answers
     * for mobile interfaces). Returning {@code null} uses the provider default.
     */
    private final Function<ExecutionContext, Integer> maxCompletionTokensConfigure;

    /**
     * Supplies the primary user message text for the chat conversation.
     * <p>
     * This is the main prompt input. It can be derived from the context,
     * possibly combining multiple sources or applying templates. Must not return
     * {@code null} if the chat is initiated without a media message.
     */
    private final Function<ExecutionContext, String> textMessageConfigure;

    /**
     * Provides a media message (e.g., an image or audio clip) to be included in the
     * multimodal prompt.
     * <p>
     * In multimodal scenarios, this is used alongside or instead of a text message.
     * The function may return {@code null} if no media is required for the request.
     */
    private final Function<ExecutionContext, MediaMessage> mediaMessageConfigure;

    /**
     * Supplies the system message that sets the behavior and context for the model.
     * <p>
     * System messages are typically used to provide instructions, tone, or rules
     * (e.g., "You are a helpful assistant that answers questions about science").
     * If {@code null} is returned, the system message is omitted.
     */
    private final Function<ExecutionContext, String> systemMessageConfigure;

    /**
     * Provides a list of previous messages to maintain conversation history in a
     * multi-turn chat.
     * <p>
     * These are injected into the prompt to give the model context about earlier
     * exchanges. The returned list may be empty, signifying a fresh conversation.
     * The function receives the {@link ExecutionContext}, which could contain a
     * session ID or stored history.
     */
    private final Function<ExecutionContext, List<Message>> historicalMessageConfigure;

    /**
     * Supplies the collection of available tool definitions for function calling.
     * <p>
     * Tools describe functions that the model may request to invoke, along with their
     * parameters. The function is evaluated during each execution, allowing dynamic
     * registration of tools based on context (e.g., user permissions, active plugins).
     * An empty collection means no tools are offered.
     */
    private final Function<ExecutionContext, Collection<ToolDefinition>> toolsConfigure;

    /**
     * Controls how the model selects from the available tools.
     * <p>
     * Common values are:
     * <ul>
     *   <li>{@code "auto"}: the model decides automatically (default)</li>
     *   <li>{@code "none"}: the model will not call any functions</li>
     *   <li>{@code "required"}: the model must call one or more functions</li>
     *   <li>A specific function name: force the model to call that function</li>
     * </ul>
     * The default function always returns {@code "auto"}. Override to enforce a strict
     * tool use policy.
     */
    @Builder.Default
    private final Function<ExecutionContext, String> toolChoiceConfigure = __ -> "auto";

    /**
     * Provides a collection of tool execution results to be sent back to the model
     * after a previous function call.
     * <p>
     * These messages contain the outputs of tool invocations and are necessary for
     * the model to generate a final response that incorporates tool results. May be
     * {@code null} or empty if no tool calls have been performed.
     */
    private final Function<ExecutionContext, Collection<ToolResultMessage>> toolResultMessageConfigure;

    /**
     * Indicates whether duplicate tool calls should be removed from the conversation
     * history when building the request payload.
     * <p>
     * When {@code true}, the execution handler will filter out repeated function calls
     * (same name and arguments) that might have occurred in the same turn. This prevents
     * redundant tool invocations and reduces token usage.
     */
    private final boolean distinctToolCalls;

    /**
     * An optional JSON schema string defining the expected structured output format.
     * <p>
     * Supported by some providers (e.g., OpenAI's {@code response_format} with
     * {@code json_schema}) to enforce that the generated content adheres to a specific
     * JSON structure. When {@code null}, no schema constraint is applied.
     */
    private final String responseJsonSchema;

    /**
     * The target Java type for deserializing a structured (JSON) response.
     * <p>
     * If set, the executor will attempt to convert the response text into an instance
     * of this type (e.g., using a Jackson ObjectMapper). This works in conjunction with
     * {@link #responseJsonSchema} or provider-specific structured output modes. A
     * {@code null} value disables typed response mapping.
     */
    private final java.lang.reflect.Type structuredOutputType;

    /**
     * Creates a new {@link ChatExecutionInfo} instance configured with all the functions
     * defined in this specification, bound to the given execution context.
     * <p>
     * The resulting {@code ChatExecutionInfo} is a snapshot that can be passed to an
     * executor. Each field captures the corresponding function from this spec, so that
     * the executor can evaluate them when needed, ensuring thread-safe, context-aware
     * configuration.
     *
     * @param executionContext the current execution context, never {@code null}
     * @return a fully populated {@link ChatExecutionInfo} builder result
     */
    @Override
    public ChatExecutionInfo newExecutionInfo(@NonNull ExecutionContext executionContext) {
        return ChatExecutionInfo.builder()
                .profilePicker(this.getProfilePicker())
                .defaultProfile(this.isDefaultProfile())
                .modelNameConfigure(this.getModelNameConfigure())
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
                .rawRequestCustomizerConfigure(this.getRawRequestCustomizerConfigure())
                .distinctToolCalls(this.distinctToolCalls)
                .responseJsonSchema(this.responseJsonSchema)
                .structuredOutputType(this.structuredOutputType)
                .build();
    }
}