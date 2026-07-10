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
package pro.chenggang.project.reactive.ai.lite.core.message;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;

import java.util.Objects;

/**
 * Represents a specialized {@link Message} conveying textual content, typically used for user instructions,
 * system prompts, or assistant responses. It is the most common message type in conversational AI,
 * designed to hold a plain text string and optionally a sender name for disambiguation in multi-user scenarios.
 * <p>
 * This interface simplifies the representation of text-only interactions, ensuring a consistent contract
 * for content retrieval and optional naming, which downstream processing can rely upon.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface TextMessage extends Message {

    /**
     * Returns the optional name of the sender. This property is useful in advanced conversations where
     * the AI model needs to differentiate between multiple participants (e.g., a user and a system).
     * By providing a name, the model can tailor responses or maintain context per participant.
     * Defaults to {@code null} if not needed, preserving backward compatibility.
     *
     * @return the optional name of the sender, or {@code null} if not specified
     */
    @Nullable
    default String getName() {
        return null;
    }

    /**
     * Returns the core textual payload of the message. This is the natural language input (or output)
     * that the AI engine processes. It is never null; implementations should guarantee a non-null value.
     *
     * @return the text content of the message
     */
    String getContent();

    /**
     * Returns a pre-built empty text message with the {@link Role#USER} role.
     * This is used as a placeholder or starting point for building user messages without requiring manual
     * construction each time. It helps avoid null and provides a canonical empty instance for assertions
     * or initial states.
     *
     * @return an empty text message instance assigned to the user role
     */
    static TextMessage emptyUserTextMessage() {
        return TextMessageBuilder.EMPTY_USER_MESSAGE;
    }

    /**
     * Returns a pre-built empty text message with the {@link Role#SYSTEM} role.
     * Similar to {@link #emptyUserTextMessage()}, this provides a convenient empty message placeholder
     * for system roles, useful in conversation setup or fallback logic.
     *
     * @return an empty text message instance assigned to the system role
     */
    static TextMessage emptySystemTextMessage() {
        return TextMessageBuilder.EMPTY_SYSTEM_MESSAGE;
    }

    /**
     * Creates a new builder initialized with the given {@link Role}. The builder provides a fluent API
     * to set optional name and content before building an immutable instance. Using the role as a starting
     * point enforces that every message has a defined sender role, which is critical for AI model behavior.
     *
     * @param role the {@link Role} of the message sender (must not be null)
     * @return a new {@link TextMessageBuilder} instance configured with the specified role
     */
    static TextMessageBuilder newTextMessage(@NonNull Role role) {
        return new TextMessageBuilder(role.getValue());
    }

    /**
     * Creates a new builder initialized with a role string. This allows integrating custom or non-standard
     * roles (e.g., 'agent', 'tool') while still leveraging the builder pattern. The role is treated as
     * an opaque identifier.
     *
     * @param role the role of the message sender as a string (must not be null)
     * @return a new {@link TextMessageBuilder} instance configured with the given role
     */
    static TextMessageBuilder newTextMessage(@NonNull String role) {
        return new TextMessageBuilder(role);
    }

    /**
     * A fluent builder for constructing {@link TextMessage} instances. It encapsulates the logic of
     * assembling a message, ensuring the role is always defined and content is non-null. The default
     * content is an empty string, preventing null issues. The builder is designed to be used via the
     * static factory methods {@link #newTextMessage(Role)} and {@link #newTextMessage(String)},
     * which inject the mandatory role.
     * <p>
     * The constructor is private, only accessible within the surrounding interface, and is typically
     * used through the static factory methods.
     * </p>
     *
     * @author Gang Cheng
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    class TextMessageBuilder {

        /**
         * A pre-configured empty text message with the {@link Role#USER} role and empty content.
         * Useful when a placeholder user message is needed without creating a new instance each time,
         * such as in unit tests or fallback logic.
         */
        public static final TextMessage EMPTY_USER_MESSAGE = DefaultTextMessage.builder()
                .role(Role.USER.getValue())
                .content("")
                .build();

        /**
         * A pre-configured empty text message with the {@link Role#SYSTEM} role and empty content.
         * Analogous to {@link #EMPTY_USER_MESSAGE} but for system roles, providing a reusable instance
         * for consistency.
         */
        public static final TextMessage EMPTY_SYSTEM_MESSAGE = DefaultTextMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content("")
                .build();

        /**
         * The role of the message sender; never null. This determines how the AI interprets the message
         * (e.g., system instruction, user query).
         */
        @NonNull
        private final String role;

        /**
         * Optional name of the sender to distinguish participants; may be null.
         */
        private String name;

        /**
         * The textual content, defaulting to empty string to avoid null. The builder ensures non-null
         * by replacing null with empty string in {@link #content(String)}.
         */
        private String content = "";

        /**
         * Sets the optional sender name. Pass null to indicate no name. This value is directly stored;
         * no validation is performed.
         *
         * @param name the name of the sender, can be null
         * @return this builder instance for method chaining
         */
        public TextMessageBuilder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the message content. If the provided string is null, the content remains the default
         * empty string, guaranteeing a non-null value. This simplifies downstream handling.
         *
         * @param content the message content, defaults to an empty string if null is provided
         * @return this builder instance for method chaining
         */
        public TextMessageBuilder content(@Nullable String content) {
            if (Objects.nonNull(content)) {
                this.content = content;
            }
            return this;
        }

        /**
         * Assembles and returns a new immutable {@link DefaultTextMessage} using the configured role,
         * name, and content. The resulting message is ready for use in a conversation.
         *
         * @return a new {@link DefaultTextMessage} instance
         */
        public TextMessage build() {
            return DefaultTextMessage.builder()
                    .role(role)
                    .name(name)
                    .content(content)
                    .build();
        }

    }
}