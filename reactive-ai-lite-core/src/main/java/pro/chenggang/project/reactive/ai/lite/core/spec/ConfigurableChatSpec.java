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
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.message.Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.TextMessage;
import pro.chenggang.project.reactive.ai.lite.core.tool.LlmToolCallResponse;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An extension of {@link ChatSpec} that provides a fluent API for configuring
 * the parameters of a chat request. This interface allows for setting various
 * options such as the model, temperature, messages, and tools.
 * <p>
 * Most configuration methods come in two forms: one that accepts a static value,
 * and another that accepts a {@code Function<ExecutionContextView, T>} for dynamic
 * configuration based on the request context.
 *
 * @author Cheng Gang
 * @version 0.1.0
 * @since 0.1.0
 */
public interface ConfigurableChatSpec extends ChatSpec {

    /**
     * Dynamically configures the model name to be used for the chat request.
     *
     * @param modelNameConfigure A function that returns the model name based on the execution context.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec model(@NonNull Function<ExecutionContextView, String> modelNameConfigure);

    /**
     * Sets a static model name for the chat request.
     *
     * @param modelName The name of the model to use.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec model(@NonNull String modelName) {
        return model(contextView -> modelName);
    }

    /**
     * Dynamically configures the temperature for the chat request. Temperature controls randomness.
     *
     * @param temperatureConfigure A function that returns the temperature value based on the execution context.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec temperature(@NonNull Function<ExecutionContextView, Double> temperatureConfigure);

    /**
     * Sets a static temperature for the chat request.
     *
     * @param temperature The temperature value.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec temperature(@NonNull Double temperature) {
        return temperature(contextView -> temperature);
    }

    /**
     * Dynamically configures the Top-P (nucleus sampling) value for the chat request.
     *
     * @param topPConfigure A function that returns the Top-P value based on the execution context.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */

    ConfigurableChatSpec topP(@NonNull Function<ExecutionContextView, Double> topPConfigure);

    /**
     * Sets a static Top-P value for the chat request.
     *
     * @param topP The Top-P value.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec topP(@NonNull Double topP) {
        return topP(contextView -> topP);
    }

    /**
     * Dynamically configures extra, provider-specific data for the chat request.
     *
     * @param extraDataConfigure A function that returns a map of extra data based on the execution context.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec extraData(@NonNull Function<ExecutionContextView, Map<String, Object>> extraDataConfigure);

    /**
     * Sets a static map of extra, provider-specific data for the chat request.
     *
     * @param extraData A map containing extra data.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec extraData(@NonNull Map<String, Object> extraData) {
        return extraData(contextView -> extraData);
    }

    /**
     * Dynamically configures the system message for the chat request.
     *
     * @param systemMessageConfigure A function that returns the system message based on the execution context.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec systemMessage(@NonNull Function<ExecutionContextView, TextMessage> systemMessageConfigure);

    /**
     * Sets a static system message for the chat request.
     *
     * @param systemMessage The content of the system message.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec systemMessage(@NonNull String systemMessage) {
        return systemMessage(contextView -> TextMessage.of(systemMessage));
    }

    /**
     * Dynamically configures the historical messages (conversation history) for the chat request.
     *
     * @param historicalMessageConfigure A function that returns a collection of historical messages.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec historicalMessage(@NonNull Function<ExecutionContextView, Collection<Message>> historicalMessageConfigure);

    /**
     * Sets a static collection of historical messages for the chat request.
     *
     * @param historicalMessage A collection of historical messages.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec historicalMessage(@NonNull Collection<Message> historicalMessage) {
        return historicalMessage(contextView -> historicalMessage);
    }

    /**
     * Dynamically configures the latest assistant message. This is often used for tool-related follow-ups.
     *
     * @param latestAssistantMessageConfigure A function that returns the latest assistant message as an {@link ObjectNode}.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec latestAssistantMessage(@NonNull Function<ExecutionContextView, ObjectNode> latestAssistantMessageConfigure);

    /**
     * Sets the latest assistant message.
     *
     * @param latestAssistantMessage The latest assistant message as an {@link ObjectNode}.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec latestAssistantMessage(@NonNull ObjectNode latestAssistantMessage) {
        return latestAssistantMessage(contextView -> latestAssistantMessage);
    }

    /**
     * Dynamically configures the user's text message for the chat request.
     *
     * @param textMessageConfigure A function that returns the user's text message.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec textMessage(@NonNull Function<ExecutionContextView, TextMessage> textMessageConfigure);

    /**
     * Sets the user's text message for the chat request.
     *
     * @param textContent The content of the user's message.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec textMessage(@NonNull String textContent) {
        return textMessage(contextView -> TextMessage.of(textContent));
    }

    /**
     * Dynamically configures a media message (text + attachments) for the chat request.
     *
     * @param mediaMessageConfigure A function that returns the media message.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec mediaMessage(@NonNull Function<ExecutionContextView, MediaMessage> mediaMessageConfigure);

    /**
     * Sets a media message with text content and a list of attachments.
     *
     * @param textContent The text part of the message.
     * @param attachments A list of media attachments.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec mediaMessage(@NonNull String textContent, @NonNull List<Attachment> attachments) {
        return mediaMessage(contextView -> MediaMessage.of(textContent, attachments));
    }

    /**
     * Dynamically configures the maximum number of tokens to generate in the completion.
     *
     * @param maxCompletionTokensConfigure A function that returns the maximum number of tokens.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec maxCompletionTokens(@NonNull Function<ExecutionContextView, Integer> maxCompletionTokensConfigure);

    /**
     * Sets the maximum number of tokens to generate in the completion.
     *
     * @param maxCompletionTokens The maximum number of tokens.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec maxCompletionTokens(@NonNull Integer maxCompletionTokens) {
        return maxCompletionTokens(contextView -> maxCompletionTokens);
    }

    /**
     * Dynamically configures the tools (e.g., functions) available to the model.
     *
     * @param toolsConfigure A function that returns a collection of tool definitions.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec tools(@NonNull Function<ExecutionContextView, Collection<ToolDefinition>> toolsConfigure);

    default ConfigurableChatSpec tools(@NonNull Collection<ToolDefinition> toolDefinitions) {
        return tools(contextView -> toolDefinitions);
    }

    /**
     * Dynamically configures the tool choice behavior for the model.
     * This can be "auto", "none", or a specific tool name to force its use.
     *
     * @param toolChoiceConfigure A function that returns the tool choice string.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec toolChoice(@NonNull Function<ExecutionContextView, String> toolChoiceConfigure);

    /**
     * Sets a static tool choice behavior for the model.
     *
     * @param toolChoice The tool choice string (e.g., "auto", "none").
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec toolChoice(@NonNull String toolChoice) {
        return toolChoice(contextView -> toolChoice);
    }

    /**
     * Dynamically provides the responses from tool calls that the model previously requested.
     *
     * @param toolsResponseConfigure A function that returns a collection of tool call responses.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec toolsResponse(@NonNull Function<ExecutionContextView, Collection<LlmToolCallResponse>> toolsResponseConfigure);

    /**
     * Provides a static collection of responses from tool calls.
     *
     * @param toolsResponses A collection of tool call responses.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec toolsResponse(@NonNull Collection<LlmToolCallResponse> toolsResponses) {
        return toolsResponse(contextView -> toolsResponses);
    }

    /**
     * Provides a {@link BiConsumer} to customize the raw request payload before it is sent to the AI provider.
     * This allows for adding provider-specific, non-standard parameters.
     *
     * @param rawRequestCustomizer A biconsumer that accepts the execution context and the request payload as an {@link ObjectNode}.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec rawRequestCustomizer(@NonNull BiConsumer<ExecutionContextView, ObjectNode> rawRequestCustomizer);

    /**
     * Provides a {@link Consumer} to customize the raw request payload.
     *
     * @param rawRequestCustomizer A consumer that accepts the request payload as an {@link ObjectNode}.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec rawRequestCustomizer(@NonNull Consumer<ObjectNode> rawRequestCustomizer) {
        return rawRequestCustomizer((contextView, jsonNode) -> rawRequestCustomizer.accept(jsonNode));
    }

    /**
     * Registers a {@link BiConsumer} to process the raw, non-streaming response from the AI provider.
     *
     * @param rawResponseCustomizer A biconsumer that accepts the execution context and the {@link RawResponse}.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec rawResponseCustomizer(@NonNull BiConsumer<ExecutionContextView, RawResponse> rawResponseCustomizer);

    /**
     * Registers a {@link Consumer} to process the raw, non-streaming response.
     *
     * @param rawResponseCustomizer A consumer that accepts the {@link RawResponse}.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec rawResponseCustomizer(@NonNull Consumer<RawResponse> rawResponseCustomizer) {
        return rawResponseCustomizer((contextView, response) -> rawResponseCustomizer.accept(response));
    }

    /**
     * Registers a {@link BiConsumer} to process each chunk of a raw, streaming response from the AI provider.
     *
     * @param rawStreamResponseCustomizer A biconsumer that accepts the execution context and each {@link RawStreamResponse} chunk.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    ConfigurableChatSpec rawStreamResponseCustomizer(@NonNull BiConsumer<ExecutionContextView, RawStreamResponse> rawStreamResponseCustomizer);

    /**
     * Registers a {@link Consumer} to process each chunk of a raw, streaming response.
     *
     * @param rawStreamResponseCustomizer A consumer that accepts each {@link RawStreamResponse} chunk.
     * @return This {@link ConfigurableChatSpec} instance for method chaining.
     */
    default ConfigurableChatSpec rawStreamResponseCustomizer(@NonNull Consumer<RawStreamResponse> rawStreamResponseCustomizer) {
        return rawStreamResponseCustomizer((contextView, response) -> rawStreamResponseCustomizer.accept(response));
    }
}
