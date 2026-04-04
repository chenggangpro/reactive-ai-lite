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
import org.springframework.util.CollectionUtils;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultMediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;

import java.util.List;
import java.util.Objects;

/**
 * Represents a message that can contain both text and media attachments.
 * <p>
 * This type of message is typically used when a user or assistant needs to send
 * multi-modal content, such as an image alongside a text prompt.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface MediaMessage extends Message {

    /**
     * Retrieves the optional name of the user or entity who sent the message.
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
     * Returns the reasoning content associated with the message, if any.
     *
     * @return the reasoning content, or {@code null} if not available
     */
    @Nullable
    String getReasoningContent();

    /**
     * Returns the array of media attachments associated with the message.
     *
     * @return an array of {@link Attachment} objects
     */
    Attachment[] getAttachments();

    /**
     * Creates a new builder for constructing a {@link MediaMessage} with the specified role.
     *
     * @param role the {@link Role} of the message sender
     * @return a new {@link MediaMessageBuilder} instance
     */
    static MediaMessageBuilder newMediaMessage(@NonNull Role role) {
        return new MediaMessageBuilder(role.getValue());
    }

    /**
     * Creates a new builder for constructing a {@link MediaMessage} with the specified role string.
     *
     * @param role the role of the message sender as a string
     * @return a new {@link MediaMessageBuilder} instance
     */
    static MediaMessageBuilder newMediaMessage(@NonNull String role) {
        return new MediaMessageBuilder(role);
    }

    /**
     * Builder class for constructing {@link MediaMessage} instances.
     * <p>
     * This builder provides a fluent API for creating media messages with various properties
     * including role, name, content, reasoning content, and attachments.
     * </p>
     *
     * @author Cheng Gang
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    class MediaMessageBuilder {

        @NonNull
        private final String role;
        private String name;
        private String content = "";
        private String reasoningContent;
        private Attachment[] attachments = new Attachment[0];

        /**
         * Sets the optional name of the sender.
         *
         * @param name the name to associate with the message, can be null
         * @return this builder instance for method chaining
         */
        public MediaMessageBuilder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the text content for the media message.
         *
         * @param content the text content, ignored if null
         * @return this builder instance for method chaining
         */
        public MediaMessageBuilder content(@Nullable String content) {
            if (Objects.nonNull(content)) {
                this.content = content;
            }
            return this;
        }

        /**
         * Sets the reasoning content for the media message.
         *
         * @param reasoningContent the reasoning content, ignored if null
         * @return this builder instance for method chaining
         */
        public MediaMessageBuilder reasoningContent(@Nullable String reasoningContent) {
            if (Objects.nonNull(reasoningContent)) {
                this.reasoningContent = reasoningContent;
            }
            return this;
        }

        /**
         * Sets the attachments for the media message using a varargs array.
         *
         * @param attachments the attachments to include, ignored if null or empty
         * @return this builder instance for method chaining
         */
        public MediaMessageBuilder attachments(Attachment... attachments) {
            if (Objects.nonNull(attachments) && attachments.length > 0) {
                this.attachments = attachments;
            }
            return this;
        }

        /**
         * Sets the attachments for the media message using a List.
         *
         * @param attachments the list of attachments, ignored if null or empty
         * @return this builder instance for method chaining
         */
        public MediaMessageBuilder attachments(List<Attachment> attachments) {
            if (!CollectionUtils.isEmpty(attachments)) {
                this.attachments = attachments.toArray(new Attachment[0]);
            }
            return this;
        }

        /**
         * Builds and returns a new {@link MediaMessage} instance with the configured properties.
         *
         * @return a new {@link DefaultMediaMessage} instance
         */
        public MediaMessage build() {
            return DefaultMediaMessage.builder()
                    .role(role)
                    .name(name)
                    .content(content)
                    .reasoningContent(reasoningContent)
                    .attachments(attachments)
                    .build();
        }

    }
}
