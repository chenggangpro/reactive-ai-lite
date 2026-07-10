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
package pro.chenggang.project.reactive.ai.lite.core.message.defaults;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;
import org.springframework.lang.Nullable;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.AbstractAttribute;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolCallMessage;

import java.util.List;

/**
 * Default, immutable implementation of {@link ToolCallMessage} representing an AI assistant's
 * request for one or more tool invocations.
 * <p>
 * This message type is typically generated when the AI model decides that external tool execution
 * is needed to fulfill the user's request. It carries the list of {@link AssistantToolCall tool calls}
 * and optionally includes textual content and/or reasoning content.
 * </p>
 * <p>
 * The class is designed as an immutable value object using Lombok's {@code @Builder} and
 * {@code @RequiredArgsConstructor}. The builder pattern, combined with {@code @Singular} on
 * the {@code toolCalls} list, allows flexible construction of the message without exposing
 * mutable setters. It is also annotated with {@code @Jacksonized} to support JSON
 * serialization/deserialization, enabling seamless integration with REST APIs and message
 * stores.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ToolCallMessage
 * @see AssistantToolCall
 * @since 0.1.0
 */
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultToolCallMessage extends AbstractAttribute implements ToolCallMessage {

    /**
     * Optional primary textual content of the message.
     * <p>
     * In many cases the AI model may provide a brief textual statement along with tool calls
     * (e.g., "I'll look up the weather for you"). However, when the response consists solely
     * of tool invocations, this field may be {@code null}, indicating that only the tool calls
     * should be processed.
     * </p>
     */
    @Nullable
    private final String content;

    /**
     * Optional reasoning content that may accompany the message.
     * <p>
     * Some AI models (such as those with chain-of-thought or introspection capabilities) can
     * provide an internal reasoning trace. This field captures that reasoning, if available,
     * while remaining {@code null} for models or scenarios that do not expose it.
     * </p>
     */
    @Nullable
    private final String reasoningContent;

    /**
     * The required list of tool calls requested by the assistant.
     * <p>
     * Each entry is an {@link AssistantToolCall} detailing the function name, arguments,
     * and the unique call ID. Lombok's {@code @Singular} on this field enables the builder
     * to accumulate tool calls one at a time (via {@code toolCall(...)}) or as a collection,
     * ensuring that an empty list is never used (the field is {@code @NonNull}).
     * </p>
     */
    @NonNull
    @Singular
    private final List<AssistantToolCall> toolCalls;

    /**
     * Returns the list of tool calls requested in this message.
     * <p>
     * The returned list is a direct reference to the internal immutable collection. The caller
     * should not attempt to modify it.
     * </p>
     *
     * @return a non-null list of {@link AssistantToolCall} instances
     */
    @Override
    public List<AssistantToolCall> getToolCalls() {
        return this.toolCalls;
    }

    /**
     * Retrieves the primary textual content of the message, if present.
     * <p>
     * The content often complements the tool calls by providing a human-readable explanation
     * or summary. A {@code null} return value is allowed and indicates that the message
     * consists only of tool calls.
     * </p>
     *
     * @return the message text, or {@code null} if none
     */
    @Override
    @Nullable
    public String getContent() {
        return this.content;
    }

    /**
     * Retrieves the reasoning content associated with this message, if any.
     * <p>
     * Reasoning content represents the model's internal commentary that led to the decision
     * to invoke certain tools. It is optional and may be {@code null} when the underlying
     * AI provider does not supply it.
     * </p>
     *
     * @return the reasoning text, or {@code null} if unavailable
     */
    @Nullable
    @Override
    public String getReasoningContent() {
        return this.reasoningContent;
    }

    /**
     * Returns the concrete runtime class of this message.
     * <p>
     * This method is used to determine the exact message type for routing, logging,
     * or polymorphic deserialization purposes. Always returns {@code DefaultToolCallMessage.class}
     * for this implementation.
     * </p>
     *
     * @return the class object representing {@link DefaultToolCallMessage}
     */
    @Override
    public Class<? extends Message> getActualType() {
        return DefaultToolCallMessage.class;
    }
}