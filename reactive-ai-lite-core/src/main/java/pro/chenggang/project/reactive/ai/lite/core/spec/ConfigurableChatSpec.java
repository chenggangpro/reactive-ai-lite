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
package pro.chenggang.project.reactive.ai.lite.core.spec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.message.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolResultMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Attachment;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Extension of {@link ChatSpec} that provides a fluent configuration API for
 * building a chat request. Implementations collect parameters such as model,
 * temperature, messages, tools, and provider‑specific options.
 * <p>
 * The fluent design relies on two complementary patterns:
 * <ul>
 *   <li><strong>static values:</strong> shortcut methods that accept a plain value
 *       and delegate to a {@code Function} returning the same value.</li>
 *   <li><strong>dynamic factories:</strong> methods that accept a
 *       {@link Function}{@code <}{@link ExecutionContext}{@code , T>} so that the
 *       value can be computed at request time based on pipeline state. This
 *       allows reusable, context‑aware specs.</li>
 * </ul>
 * </p>
 *
 * <p>
 * After configuration the spec exposes its accumulated parameters via the
 * {@link ChatSpec} interface, making it suitable for use in higher‑level
 * components such as {@code ChatClient}.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ChatSpec
 * @see ExecutionContext
 */
public interface ConfigurableChatSpec extends ChatSpec {

    /**
     * Registers a function that provides the model name to be used for the
     * chat request. The function receives the current {@link ExecutionContext}
     * and must return a non‑null model identifier.
     * <p>
     * This is the dynamic version; use {@link #model(String)} for static
     * configuration.
     * </p>
     *
     * @param modelNameConfigure a context‑aware supplier for the model name
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec model(@NonNull Function<ExecutionContext, String> modelNameConfigure);

    /**
     * Sets a fixed model name for the chat request. Internally delegates to
     * {@link #model(Function)} with a function that ignores the context and
     * returns the given string.
     *
     * @param modelName the model identifier (e.g. "gpt-4o", "claude-3-haiku")
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec model(@NonNull String modelName) {
        return model(contextView -> modelName);
    }

    /**
     * Registers a function that controls the temperature parameter sent to the
     * AI provider. Temperature influences response randomness: values closer to 0
     * make output deterministic, values closer to 2 increase variability.
     * <p>
     * If the function returns {@code null} the parameter is omitted, allowing
     * the provider to use its default.
     * </p>
     *
     * @param temperatureConfigure a context‑aware supplier for the temperature
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec temperature(@NonNull Function<ExecutionContext, Double> temperatureConfigure);

    /**
     * Sets a static temperature value. Delegates to the dynamic version only if
     * the argument is non‑null, preserving the ability to later provide a
     * context‑aware function.
     *
     * @param temperature a value typically between 0.0 and 2.0
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec temperature(Double temperature) {
        if (Objects.nonNull(temperature)) {
            return temperature(contextView -> temperature);
        }
        return this;
    }

    /**
     * Registers a function that provides the Top‑P (nucleus sampling) value.
     * Top‑P restricts the model to consider only the most probable tokens whose
     * cumulative probability exceeds the given threshold. Generally used as an
     * alternative to temperature for controlling randomness.
     *
     * @param topPConfigure a context‑aware supplier for the Top‑P value
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec topP(@NonNull Function<ExecutionContext, Double> topPConfigure);

    /**
     * Sets a static Top‑P value. If non‑null, delegates to the dynamic version.
     *
     * @param topP the nucleus sampling threshold (commonly 0.0–1.0)
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec topP(Double topP) {
        if (Objects.nonNull(topP)) {
            return topP(contextView -> topP);
        }
        return this;
    }

    /**
     * Registers a function that decides whether the response should include
     * usage metadata (token counts, etc.). Providers often expose this as a
     * separate flag; setting it to {@code true} adds a {@code stream_options}
     * or equivalent field in the request.
     *
     * @param includeUsageConfigure a context‑aware predicate for including usage
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec includeUsage(@NonNull Function<ExecutionContext, Boolean> includeUsageConfigure);

    /**
     * Convenience method that forces the inclusion of usage metadata by
     * delegating to {@link #includeUsage(Function)} with a constant {@code true}
     * supplier.
     *
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec includeUsage() {
        return includeUsage(contextView -> true);
    }

    /**
     * Registers a function that supplies the reasoning or thinking configuration
     * string for the model. Different providers interpret this value differently:
     * <ul>
     * <li><b>OpenAI o‑series models:</b> {@code "low"}, {@code "medium"},
     *     {@code "high"}</li>
     * <li><b>DeepSeek R1:</b> {@code "enabled"}, {@code "disabled"},
     *     {@code "enabled:high"}, {@code "enabled:max"}</li>
     * <li><b>Ollama:</b> {@code "true"}, {@code "false"}</li>
     * <li><b>Anthropic:</b> {@code "enabled:budgetTokens"}
     *     (e.g. {@code "enabled:1024"})</li>
     * </ul>
     * The string is passed unchanged into the provider‑specific request body
     * when supported.
     *
     * @param reasoningConfigure a context‑aware supplier for the reasoning string
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec reasoning(@NonNull Function<ExecutionContext, String> reasoningConfigure);

    /**
     * Sets a static reasoning/thinking string. Only applies if the argument is
     * non‑empty, otherwise leaves the spec unchanged.
     *
     * @param reasoning a provider‑recognised reasoning value
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec reasoning(String reasoning) {
        if (StringUtils.hasText(reasoning)) {
            return reasoning(contextView -> reasoning);
        }
        return this;
    }

    /**
     * Registers a function that returns the system message emitted at the
     * beginning of the conversation. System messages set the assistant’s
     * persona, behaviour constraints, or response format.
     *
     * @param systemMessageConfigure a context‑aware supplier for the system message text
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec systemMessage(@NonNull Function<ExecutionContext, String> systemMessageConfigure);

    /**
     * Sets a static system message. If the argument is non‑null, delegates to
     * the dynamic version.
     *
     * @param systemMessage the system prompt content
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec systemMessage(String systemMessage) {
        if (Objects.nonNull(systemMessage)) {
            return systemMessage(contextView -> systemMessage);
        }
        return this;
    }

    /**
     * Registers a function that supplies the conversation history (previous
     * turns) as a list of {@link Message} objects. These messages are sent
     * before the current user request, allowing the model to maintain context.
     *
     * @param historicalMessageConfigure a context‑aware supplier of the history
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec historicalMessage(@NonNull Function<ExecutionContext, List<Message>> historicalMessageConfigure);

    /**
     * Sets a static list of historical messages. If the list is empty or
     * {@code null} the call is ignored, preserving previously registered
     * dynamic history.
     *
     * @param historicalMessages previous assistant/user messages
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec historicalMessage(List<Message> historicalMessages) {
        if (CollectionUtils.isEmpty(historicalMessages)) {
            return this;
        }
        return historicalMessage(contextView -> historicalMessages);
    }

    /**
     * Registers a function that provides the textual content of the user’s
     * message. This is the simplest way to send a prompt.
     *
     * @param textMessageConfigure a context‑aware supplier for the user’s text
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec textMessage(@NonNull Function<ExecutionContext, String> textMessageConfigure);

    /**
     * Sets a static user text message.
     *
     * @param textContent the user input
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec textMessage(@NonNull String textContent) {
        return textMessage(contextView -> textContent);
    }

    /**
     * Registers a function that supplies a multimedia user message, which
     * includes text and attached files (images, audio, video, etc.).
     *
     * @param mediaMessageConfigure a context‑aware supplier for the media message
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec mediaMessage(@NonNull Function<ExecutionContext, MediaMessage> mediaMessageConfigure);

    /**
     * Builds a media message with the given text and attachments and registers
     * it statically. The message role is automatically set to {@link Role#USER}.
     *
     * @param textContent the explanatory text
     * @param attachments the media files to attach
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec mediaMessage(@NonNull String textContent, @NonNull List<Attachment> attachments) {
        return mediaMessage(contextView -> MediaMessage.newMediaMessage(Role.USER).content(textContent).attachments(attachments).build());
    }

    /**
     * Registers a function that provides the maximum number of tokens the model
     * is allowed to generate in the completion. This parameter maps directly to
     * most providers’ {@code max_tokens} or equivalent field.
     *
     * @param maxCompletionTokensConfigure a context‑aware supplier for the cap
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec maxCompletionTokens(@NonNull Function<ExecutionContext, Integer> maxCompletionTokensConfigure);

    /**
     * Sets a static maximum token count.
     *
     * @param maxCompletionTokens the generation limit
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec maxCompletionTokens(@NonNull Integer maxCompletionTokens) {
        return maxCompletionTokens(contextView -> maxCompletionTokens);
    }

    /**
     * Registers a function that supplies the tool (function) definitions
     * available to the model. Each {@link ToolDefinition} describes a callable
     * function, including its name, description, and parameters schema.
     *
     * @param toolsConfigure a context‑aware supplier for the tool definitions
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec tools(@NonNull Function<ExecutionContext, Collection<ToolDefinition>> toolsConfigure);

    /**
     * Registers a static collection of tool definitions.
     *
     * @param toolDefinitions the tools the model may invoke
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec tools(@NonNull Collection<ToolDefinition> toolDefinitions) {
        return tools(contextView -> toolDefinitions);
    }

    /**
     * Controls whether duplicate tool calls returned by the model should be
     * filtered out before processing. Some models may emit identical tool
     * requests multiple times; enabling this flag ensures only the first
     * occurrence is acted upon, preventing redundant executions.
     *
     * @param distinctToolCalls {@code true} to deduplicate, {@code false} to keep all
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec distinctToolCalls(boolean distinctToolCalls);

    /**
     * Registers a function that selects the tool choice mode. Typical values
     * include:
     * <ul>
     *   <li>{@code "auto"} – the model decides whether to call a tool</li>
     *   <li>{@code "none"} – the model must not call any tool</li>
     *   <li>{@code "required"} – the model must call at least one tool</li>
     *   <li>a specific tool name – forces the model to call that tool</li>
     * </ul>
     * The exact format depends on the provider; the string is passed through
     * without validation.
     *
     * @param toolChoiceConfigure a context‑aware supplier for the tool choice string
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec toolChoice(@NonNull Function<ExecutionContext, String> toolChoiceConfigure);

    /**
     * Sets a static tool choice string.
     *
     * @param toolChoice the mode or tool name
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec toolChoice(@NonNull String toolChoice) {
        return toolChoice(contextView -> toolChoice);
    }

    /**
     * Registers a function that supplies the results of previously requested
     * tool calls. These {@link ToolResultMessage} objects are inserted into the
     * conversation so the model can use their outputs to generate a final
     * answer or request additional tools.
     *
     * @param toolResultMessageConfigure a context‑aware supplier for tool results
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec toolsResponse(@NonNull Function<ExecutionContext, Collection<ToolResultMessage>> toolResultMessageConfigure);

    /**
     * Registers a static collection of tool result messages.
     *
     * @param toolResultMessages the outcomes of executed tools
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec toolsResponse(@NonNull Collection<ToolResultMessage> toolResultMessages) {
        return toolsResponse(contextView -> toolResultMessages);
    }

    /**
     * Registers a raw request customizer that accepts both the
     * {@link ExecutionContext} and the JSON {@link ObjectNode} representing the
     * provider request payload. This hook allows injecting or overriding
     * provider‑specific fields that are not exposed by the declarative
     * configuration methods.
     * <p>
     * Example: setting an Ollama‑specific {@code keep_alive} field.
     * </p>
     *
     * @param rawRequestCustomizerConfigure a bi‑consumer that modifies the request
     * @return this spec, for method chaining
     */
    ConfigurableChatSpec rawRequestCustomizer(@NonNull BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure);

    /**
     * Convenience version of {@link #rawRequestCustomizer(BiConsumer)} that
     * ignores the execution context.
     *
     * @param rawRequestCustomizerConfigure a consumer that operates on the raw request JSON
     * @return this spec, for method chaining
     */
    default ConfigurableChatSpec rawRequestCustomizer(@NonNull Consumer<ObjectNode> rawRequestCustomizerConfigure) {
        return rawRequestCustomizer((contextView, jsonNode) -> rawRequestCustomizerConfigure.accept(jsonNode));
    }
}