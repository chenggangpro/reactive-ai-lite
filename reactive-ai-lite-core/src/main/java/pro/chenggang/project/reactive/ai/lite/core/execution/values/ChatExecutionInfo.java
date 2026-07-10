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
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An immutable container that holds all the dynamic configuration functions
 * necessary to construct an LLM Chat request. Each function receives the current
 * {@link ExecutionContext}, enabling per-invocation resolution of parameters
 * like model name, temperature, tool set, message history, and more. The builder
 * pattern (via {@code @Builder}) creates instances with a fluent API; the generated
 * private constructor takes all final fields and is used by the builder.
 *
 * <p>Field values can be fixed {@code boolean} flags, plain objects, or functional
 * interfaces that allow late-binding of configuration at request time. This design
 * separates the <em>what</em> (the configuration itself) from the <em>when</em>
 * (the evaluation against a concrete {@link ExecutionContext}), promoting reuse
 * and testability.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatExecutionInfo implements ExecutionInfo {

    /**
     * Whether to use a pre-configured default profile. When {@code true}, the
     * {@link #profilePicker} might be ignored and a built-in profile selection
     * logic is used. Defaults to {@code false} when omitted from the builder.
     */
    private final boolean defaultProfile;

    /**
     * A function that selects a named profile (e.g., "conversational", "code") 
     * given the execution context and a set of available profile names.
     * Allows dynamic profile resolution based on user identity, conversation
     * state, or any other contextual data.
     */
    private final BiFunction<ExecutionContext, Set<String>, String> profilePicker;

    /**
     * Provides the LLM model identifier (e.g., "gpt-4o", "claude-3-opus") based
     * on the execution context. This function is <strong>required</strong> (see
     * {@code @NonNull}) and must never return {@code null}.
     */
    @NonNull
    private final Function<ExecutionContext, String> modelNameConfigure;

    /**
     * A consumer that receives the raw JSON request body (as a Jackson
     * {@link ObjectNode}) right before it is sent to the LLM provider.
     * Allows injection of provider-specific parameters or overrides that are
     * not directly modelled by other fields in this class.
     */
    private final BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;

    /**
     * Resolves the sampling temperature (typically a value between 0.0 and 1.0)
     * for the request. Higher values increase randomness; lower values make
     * outputs more deterministic.
     */
    private final Function<ExecutionContext, Double> temperatureConfigure;

    /**
     * Resolves the nucleus sampling probability (top-P). Only tokens with
     * cumulative probability up to this threshold are considered.
     */
    private final Function<ExecutionContext, Double> topPConfigure;

    /**
     * Resolves whether the provider should include detailed token usage
     * statistics in the response. Useful for monitoring and cost calculation.
     */
    private final Function<ExecutionContext, Boolean> includeUsageConfigure;

    /**
     * Resolves a provider-specific parameter for "reasoning mode"
     * (e.g., "thought", "deepseek-reasoner"). Its exact semantics depend on
     * the LLM provider in use.
     */
    private final Function<ExecutionContext, String> reasoningConfigure;

    /**
     * Resolves the maximum number of tokens allowed for the model's
     * completion. Helps constrain response length and control costs.
     */
    private final Function<ExecutionContext, Integer> maxCompletionTokensConfigure;

    /**
     * Supplies the main user prompt as a plain text string. This function may
     * read from a template or compose messages from contextual data.
     */
    private final Function<ExecutionContext, String> textMessageConfigure;

    /**
     * Supplies a {@link MediaMessage} (e.g., an image, audio, or video)
     * that is attached to the user prompt. Typically used for multi-modal
     * LLM requests.
     */
    private final Function<ExecutionContext, MediaMessage> mediaMessageConfigure;

    /**
     * Supplies a system-level instruction message that sets the behaviour,
     * tone, or constraints for the LLM. Evaluated for every request,
     * allowing dynamic system prompts.
     */
    private final Function<ExecutionContext, String> systemMessageConfigure;

    /**
     * Provides the conversation history as a list of {@link Message}
     * objects. Enables multi-turn interactions where previous turns are
     * fed back into the new request contextually.
     */
    private final Function<ExecutionContext, List<Message>> historicalMessageConfigure;

    /**
     * Provides the collection of {@link ToolDefinition} that the LLM can
     * invoke during the conversation. Tools are typically registered
     * once and resolved dynamically, e.g., based on user role or session.
     */
    private final Function<ExecutionContext, Collection<ToolDefinition>> toolsConfigure;

    /**
     * Provides a tool choice specification string (e.g., "auto",
     * "required", or {@code null} for default). Determines how the LLM
     * decides whether to call tools in its response.
     */
    private final Function<ExecutionContext, String> toolChoiceConfigure;

    /**
     * Supplies a collection of {@link ToolResultMessage} instances that
     * incorporate the results of previously executed tool calls. These
     * are merged into the request to give the LLM the outcome of tool
     * invocations.
     */
    private final Function<ExecutionContext, Collection<ToolResultMessage>> toolResultMessageConfigure;

    /**
     * When {@code true}, duplicate tool call definitions are filtered out
     * before sending the request to the LLM. This can prevent the LLM
     * from making redundant calls when multiple tool definitions with the
     * same name are registered. Defaults to {@code false}.
     */
    private final boolean distinctToolCalls;

    /**
     * An optional JSON Schema (as a string) that describes the expected
     * structure of the LLM's output. When present, the LLM is asked to
     * obey the schema, enabling structured data extraction.
     */
    private final String responseJsonSchema;

    /**
     * The Java {@link java.lang.reflect.Type} that the structured output
     * (if any) should be deserialized to. Used together with
     * {@link #responseJsonSchema} for automatic conversion of the LLM's
     * JSON response into a typed object.
     */
    private final java.lang.reflect.Type structuredOutputType;

}