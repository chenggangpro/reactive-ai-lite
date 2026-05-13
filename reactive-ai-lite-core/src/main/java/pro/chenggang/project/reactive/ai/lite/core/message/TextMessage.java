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
 * Represents a text-based message within a conversation.
 * <p>
 * This is the most common type of message, typically containing the instructions
 * from a user or system prompt. It can optionally include a name to identify
 * the specific sender in multi-user or multi-agent scenarios.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface TextMessage extends Message {

    /**
     * Retrieves the optional name of the user or entity who sent the message.
     * <p>
     * This is useful in scenarios where multiple users are interacting in the same context,
     * allowing the AI model to distinguish between different participants.
     * </p>
     *
     * @return the optional name of the sender, or {@code null} if not specified
     */
    @Nullable
    default String getName() {
        return null;
    }

    /**
     * Returns the primary textual content of the message.
     *
     * @return the text content of the message
     */
    String getContent();

    /**
     * Creates an empty text message with the USER role.
     *
     * @return an empty text message instance assigned to the user role
     */
    static TextMessage emptyUserTextMessage() {
        return TextMessageBuilder.EMPTY_USER_MESSAGE;
    }

    /**
     * Creates an empty text message with the SYSTEM role.
     *
     * @return an empty text message instance assigned to the system role
     */
    static TextMessage emptySystemTextMessage() {
        return TextMessageBuilder.EMPTY_SYSTEM_MESSAGE;
    }

    /**
     * Creates a new builder for constructing a {@link TextMessage} with the specified role.
     *
     * @param role the {@link Role} of the message sender
     * @return a new {@link TextMessageBuilder} instance
     */
    static TextMessageBuilder newTextMessage(@NonNull Role role) {
        return new TextMessageBuilder(role.getValue());
    }

    /**
     * Creates a new builder for constructing a {@link TextMessage} with the specified role string.
     *
     * @param role the role of the message sender as a string
     * @return a new {@link TextMessageBuilder} instance
     */
    static TextMessageBuilder newTextMessage(@NonNull String role) {
        return new TextMessageBuilder(role);
    }

    /**
     * Builder class for constructing {@link TextMessage} instances.
     * <p>
     * This builder provides a fluent API for creating text messages with configurable
     * role, name, and content properties.
     * </p>
     *
     * @author Gang Cheng
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    class TextMessageBuilder {

        /**
         * A pre-configured empty text message with the USER role and empty content.
         */
        public static final TextMessage EMPTY_USER_MESSAGE = DefaultTextMessage.builder()
                .role(Role.USER.getValue())
                .content("")
                .build();

        /**
         * A pre-configured empty text message with the SYSTEM role and empty content.
         */
        public static final TextMessage EMPTY_SYSTEM_MESSAGE = DefaultTextMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content("")
                .build();

        @NonNull
        private final String role;
        private String name;
        private String content = "";

        /**
         * Sets the optional name of the user who sent the message.
         *
         * @param name the name of the sender, can be null
         * @return this builder instance for method chaining
         */
        public TextMessageBuilder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the content of the text message.
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
         * Builds and returns a new {@link TextMessage} instance with the configured properties.
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
