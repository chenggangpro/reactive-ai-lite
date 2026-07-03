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
 * An extension of {@link ChatSpec} that provides a fluent API for configuring
 * the parameters of a chat request. This interface allows for setting various
 * options such as the model, temperature, messages, and tools.
 * <p>
 * Most configuration methods come in two forms: one that accepts a static value,
 * and another that accepts a {@code Function<ExecutionContext, T>} for dynamic
 * configuration based on the request context.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface ConfigurableChatSpec extends ChatSpec {

    /**
     * Dynamically configures the model name to be used for the chat request.
     *
     * @param modelNameConfigure a function that returns the model name based on the execution context
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec model(@NonNull Function<ExecutionContext, String> modelNameConfigure);

    /**
     * Sets a static model name for the chat request.
     *
     * @param modelName the name of the model to use
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec model(@NonNull String modelName) {
        return model(contextView -> modelName);
    }

    /**
     * Dynamically configures the temperature for the chat request. Temperature controls randomness.
     *
     * @param temperatureConfigure a function that returns the temperature value based on the execution context
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec temperature(@NonNull Function<ExecutionContext, Double> temperatureConfigure);

    /**
     * Sets a static temperature for the chat request.
     *
     * @param temperature the temperature value
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec temperature(Double temperature) {
        if (Objects.nonNull(temperature)) {
            return temperature(contextView -> temperature);
        }
        return this;
    }

    /**
     * Dynamically configures the Top-P (nucleus sampling) value for the chat request.
     *
     * @param topPConfigure a function that returns the Top-P value based on the execution context
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */

    ConfigurableChatSpec topP(@NonNull Function<ExecutionContext, Double> topPConfigure);

    /**
     * Sets a static Top-P value for the chat request.
     *
     * @param topP the Top-P value
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec topP(Double topP) {
        if (Objects.nonNull(topP)) {
            return topP(contextView -> topP);
        }
        return this;
    }

    /**
     * Dynamically configures whether to include usage metadata (e.g., token counts)
     * in the chat response.
     *
     * @param includeUsageConfigure a function that returns {@code true} to include usage
     *                              metadata, or {@code false} to omit it, based on the
     *                              execution context
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec includeUsage(@NonNull Function<ExecutionContext, Boolean> includeUsageConfigure);

    /**
     * Statically configures the chat request to include usage metadata (e.g., token counts)
     * in the response. This is a convenience method that sets the inclusion flag to {@code true}.
     *
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec includeUsage() {
        return includeUsage(contextView -> true);
    }

    /**
     * Dynamically configures the reasoning for the model.
     * This can be used to guide the model's thought process or provide meta-prompts,
     * which might be supported by specific AI providers.
     * <ul>
     * <li>low/medium/high value in openai</li>
     * <li>enabled/disabled in deepseek's thinking param</li>
     * <li>true/false in ollama's think param</li>
     * </ul>
     *
     * @param reasoningConfigure a function that returns the reasoning string based on the execution context
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec reasoning(@NonNull Function<ExecutionContext, String> reasoningConfigure);

    /**
     * Sets a static reasoning or system-level instruction for the model.
     * <ul>
     * <li>openai:
     * <ul>
     * <li>low</li>
     * <li>medium</li>
     * <li>high</li>
     * </ul>
     * <li>deepseek</li>
     * <ul>
     *     <li>enabled</li>
     *     <li>disabled</li>
     *     <li>enabled:high</li>
     *     <li>enabled:max</li>
     * </ul>
     * <li>ollama</li>
     * <ul>
     *     <li>true</li>
     *     <li>false</li>
     * </ul>
     * <li>anthropic</li>
     * <ul>
     *     <li>enabled:budgetTokens(enabled:1024)</li>
     *     <li>false</li>
     * </ul>
     * </ul>
     *
     * @param reasoning the reasoning string to use
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec reasoning(String reasoning) {
        if (StringUtils.hasText(reasoning)) {
            return reasoning(contextView -> reasoning);
        }
        return this;
    }

    /**
     * Dynamically configures the system message for the chat request.
     *
     * @param systemMessageConfigure a function that returns the system message based on the execution context
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec systemMessage(@NonNull Function<ExecutionContext, String> systemMessageConfigure);

    /**
     * Sets a static system message for the chat request.
     *
     * @param systemMessage the content of the system message
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec systemMessage(String systemMessage) {
        if (Objects.nonNull(systemMessage)) {
            return systemMessage(contextView -> systemMessage);
        }
        return this;
    }

    /**
     * Dynamically configures the historical messages (conversation history) for the chat request.
     *
     * @param historicalMessageConfigure a function that returns a collection of historical messages
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec historicalMessage(@NonNull Function<ExecutionContext, List<Message>> historicalMessageConfigure);

    /**
     * Sets a static collection of historical messages for the chat request.
     *
     * @param historicalMessages a collection of historical messages
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec historicalMessage(List<Message> historicalMessages) {
        if (CollectionUtils.isEmpty(historicalMessages)) {
            return this;
        }
        return historicalMessage(contextView -> historicalMessages);
    }

    /**
     * Dynamically configures the user's text message for the chat request.
     *
     * @param textMessageConfigure a function that returns the user's text message
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec textMessage(@NonNull Function<ExecutionContext, String> textMessageConfigure);

    /**
     * Sets the user's text message for the chat request.
     *
     * @param textContent the content of the user's message
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec textMessage(@NonNull String textContent) {
        return textMessage(contextView -> textContent);
    }

    /**
     * Dynamically configures a media message (text + attachments) for the chat request.
     *
     * @param mediaMessageConfigure a function that returns the media message
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec mediaMessage(@NonNull Function<ExecutionContext, MediaMessage> mediaMessageConfigure);

    /**
     * Sets a media message with text content and a list of attachments.
     *
     * @param textContent the text part of the message
     * @param attachments a list of media attachments
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec mediaMessage(@NonNull String textContent, @NonNull List<Attachment> attachments) {
        return mediaMessage(contextView -> MediaMessage.newMediaMessage(Role.USER).content(textContent).attachments(attachments).build());
    }

    /**
     * Dynamically configures the maximum number of tokens to generate in the completion.
     *
     * @param maxCompletionTokensConfigure a function that returns the maximum number of tokens
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec maxCompletionTokens(@NonNull Function<ExecutionContext, Integer> maxCompletionTokensConfigure);

    /**
     * Sets the maximum number of tokens to generate in the completion.
     *
     * @param maxCompletionTokens the maximum number of tokens
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec maxCompletionTokens(@NonNull Integer maxCompletionTokens) {
        return maxCompletionTokens(contextView -> maxCompletionTokens);
    }

    /**
     * Dynamically configures the tools (e.g., functions) available to the model.
     *
     * @param toolsConfigure a function that returns a collection of tool definitions
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec tools(@NonNull Function<ExecutionContext, Collection<ToolDefinition>> toolsConfigure);

    /**
     * Sets a static collection of tools (e.g., functions) available to the model.
     *
     * @param toolDefinitions a collection of tool definitions
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec tools(@NonNull Collection<ToolDefinition> toolDefinitions) {
        return tools(contextView -> toolDefinitions);
    }

    /**
     * Configures whether to filter for distinct tool calls in the model's response.
     * <p>
     * Some models may return multiple, identical tool call requests in a single turn.
     * Setting this to {@code true} ensures that only unique tool calls are processed.
     * </p>
     *
     * @param distinctToolCalls if {@code true}, filters for unique tool calls; if {@code false}, preserves all tool calls
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec distinctToolCalls(boolean distinctToolCalls);

    /**
     * Dynamically configures the tool choice behavior for the model.
     * <p>
     * This can be "auto", "none", or a specific tool name to force its use.
     * </p>
     *
     * @param toolChoiceConfigure a function that returns the tool choice string
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec toolChoice(@NonNull Function<ExecutionContext, String> toolChoiceConfigure);

    /**
     * Sets a static tool choice behavior for the model.
     *
     * @param toolChoice the tool choice string (e.g., "auto", "none")
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec toolChoice(@NonNull String toolChoice) {
        return toolChoice(contextView -> toolChoice);
    }

    /**
     * Dynamically provides the result message from tool calls that the model previously requested.
     *
     * @param toolResultMessageConfigure a function that returns a collection of tool call result messages
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec toolsResponse(@NonNull Function<ExecutionContext, Collection<ToolResultMessage>> toolResultMessageConfigure);

    /**
     * Provides a static collection of result messages from tool calls.
     *
     * @param toolResultMessages a collection of tool call result messages
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec toolsResponse(@NonNull Collection<ToolResultMessage> toolResultMessages) {
        return toolsResponse(contextView -> toolResultMessages);
    }

    /**
     * Configures a customizer for the raw request object (JSON node).
     * This allows for low-level manipulation of the request payload before it is sent to the AI provider,
     * enabling support for provider-specific parameters not explicitly covered by this API.
     *
     * @param rawRequestCustomizerConfigure a consumer that accepts the execution context and the raw request ObjectNode
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    ConfigurableChatSpec rawRequestCustomizer(@NonNull BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure);

    /**
     * Configures a customizer for the raw request object (JSON node).
     * This is a convenience method that ignores the execution context.
     *
     * @param rawRequestCustomizerConfigure a consumer that accepts the raw request ObjectNode
     * @return this {@link ConfigurableChatSpec} instance for method chaining
     */
    default ConfigurableChatSpec rawRequestCustomizer(@NonNull Consumer<ObjectNode> rawRequestCustomizerConfigure) {
        return rawRequestCustomizer((contextView, jsonNode) -> rawRequestCustomizerConfigure.accept(jsonNode));
    }
}
