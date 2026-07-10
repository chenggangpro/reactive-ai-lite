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
 * A message that can carry both textual content and media attachments.
 * <p>
 * This interface extends {@link Message} to support multi‑modal interactions, where
 * a message may include images, audio, video, or other files alongside a text prompt.
 * Implementations such as {@link DefaultMediaMessage} provide immutable, complete
 * representations of such messages.
 * </p>
 * <p>
 * <strong>Why use this?</strong> Modern AI models often require the ability to process
 * both text and binary data. This abstraction cleanly separates concerns, allowing
 * clients to work with a uniform message type regardless of the underlying transport
 * or model implementation.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface MediaMessage extends Message {

    /**
     * Retrieves the optional name of the sender.
     * <p>
     * In chatbot or multi‑turn conversation scenarios, the sender name allows
     * distinguishing between different participants (e.g. "User123", "Assistant-Analyst").
     * The default implementation returns {@code null}, meaning no specific name is
     * associated; subclasses may override to supply a name when needed.
     * </p>
     *
     * @return the name of the sender, or {@code null} if not available
     */
    @Nullable
    default String getName() {
        return null;
    }

    /**
     * Returns the primary textual content of the message.
     * <p>
     * For a user message, this is typically the prompt or query. For an assistant
     * message, this holds the generated response. When attachments are present,
     * this content may describe or reference them.
     * </p>
     *
     * @return the non‑null text content; may be empty if the message consists
     *         entirely of attachments
     */
    String getContent();

    /**
     * Returns the reasoning content produced by the assistant, if any.
     * <p>
     * Reasoning content represents the internal chain‑of‑thought or step‑by‑step
     * logic that an AI model generated while deriving the final answer. This is
     * separate from the {@link #getContent() main text} and is often used to
     * explain or debug the model's behavior. Returns {@code null} when no
     * reasoning was emitted or the feature is not supported.
     * </p>
     *
     * @return the reasoning text, or {@code null} if absent
     */
    @Nullable
    String getReasoningContent();

    /**
     * Returns all media attachments bundled with this message.
     * <p>
     * Attachments can be images, audio files, documents, or any other binary
     * payload represented as {@link Attachment} objects. The returned array is
     * never {@code null}; an empty array indicates that no attachments are
     * associated with the message.
     * </p>
     *
     * @return an array of attachments; length 0 if none
     */
    Attachment[] getAttachments();

    /**
     * Creates a new builder for constructing a {@link MediaMessage} with the
     * given {@link Role}.
     * <p>
     * This factory method provides a fluent starting point for building
     * multi‑modal messages. The supplied role is required and determines
     * the message's origin (e.g. user, assistant, system).
     * </p>
     *
     * @param role the role of the message sender, must not be null
     * @return a fresh {@link MediaMessageBuilder} instance
     */
    static MediaMessageBuilder newMediaMessage(@NonNull Role role) {
        return new MediaMessageBuilder(role.getValue());
    }

    /**
     * Creates a new builder for constructing a {@link MediaMessage} with a
     * custom role string.
     * <p>
     * Use this overload when the role is not one of the standard {@link Role}
     * constants or when integrating with providers that support custom roles.
     * </p>
     *
     * @param role the role as a string, must not be null
     * @return a new {@link MediaMessageBuilder} instance
     */
    static MediaMessageBuilder newMediaMessage(@NonNull String role) {
        return new MediaMessageBuilder(role);
    }

    /**
     * Fluent builder for {@link MediaMessage} instances.
     * <p>
     * This builder collects all pieces needed to construct a complete media message:
     * a mandatory role, an optional sender name, textual content, reasoning content,
     * and attachments. Once configured, calling {@link #build()} yields an immutable
     * {@link DefaultMediaMessage}.
     * </p>
     * <p>
     * <strong>Usage notes:</strong>
     * <ul>
     *   <li>Content and reasoning content setters safely ignore {@code null} arguments,
     *       preserving previously set values.</li>
     *   <li>Attachments setters also ignore empty or {@code null} inputs.</li>
     *   <li>The {@link #name(String)} method allows setting the name to {@code null},
     *       which is equivalent to “no name”.</li>
     * </ul>
     * </p>
     *
     * @author Gang Cheng
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    class MediaMessageBuilder {

        /** The role of the sender, never null. */
        @NonNull
        private final String role;

        /** Optional name of the sender; may be null to indicate no name. */
        private String name;

        /** Primary text content; defaults to an empty string. */
        private String content = "";

        /** Reasoning content; null if no reasoning is available. */
        private String reasoningContent;

        /** Attachments bundle; defaults to an empty array. */
        private Attachment[] attachments = new Attachment[0];

        /**
         * Sets the optional name of the sender.
         * <p>
         * Passing {@code null} explicitly removes any previously set name,
         * indicating that the message has no associated sender name. This is
         * meaningful in anonymous or machine‑generated messages.
         * </p>
         *
         * @param name the name to associate, or {@code null} for none
         * @return this builder instance
         */
        public MediaMessageBuilder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the textual content of the message.
         * <p>
         * If the provided {@code content} is {@code null}, the call is ignored
         * and the previously set content (or the default empty string) remains
         * unchanged. This design avoids accidental overwrites when assembling
         * content conditionally.
         * </p>
         *
         * @param content the text content; ignored if null
         * @return this builder instance
         */
        public MediaMessageBuilder content(@Nullable String content) {
            if (Objects.nonNull(content)) {
                this.content = content;
            }
            return this;
        }

        /**
         * Sets the reasoning content.
         * <p>
         * Reasoning content typically appears in assistant messages and contains
         * the model's step‑by‑step thought process. Providing a {@code null}
         * argument has no effect; use an empty string to explicitly clear
         * any previously set reasoning.
         * </p>
         *
         * @param reasoningContent the reasoning text; ignored if null
         * @return this builder instance
         */
        public MediaMessageBuilder reasoningContent(@Nullable String reasoningContent) {
            if (Objects.nonNull(reasoningContent)) {
                this.reasoningContent = reasoningContent;
            }
            return this;
        }

        /**
         * Assigns attachments from a varargs array.
         * <p>
         * If the array is {@code null} or empty, the call is ignored and any
         * previously set attachments are preserved. This allows partial
         * configuration without fear of mistakenly discarding existing data.
         * </p>
         *
         * @param attachments attachments to include; null or empty arrays are ignored
         * @return this builder instance
         */
        public MediaMessageBuilder attachments(Attachment... attachments) {
            if (Objects.nonNull(attachments) && attachments.length > 0) {
                this.attachments = attachments;
            }
            return this;
        }

        /**
         * Assigns attachments from a {@link List}.
         * <p>
         * Convenience overload that accepts a list. If the list is
         * {@code null} or empty, the call is ignored, preserving any
         * previously set attachments.
         * </p>
         *
         * @param attachments list of attachments; null or empty lists are ignored
         * @return this builder instance
         */
        public MediaMessageBuilder attachments(List<Attachment> attachments) {
            if (!CollectionUtils.isEmpty(attachments)) {
                this.attachments = attachments.toArray(new Attachment[0]);
            }
            return this;
        }

        /**
         * Builds and returns a new {@link MediaMessage} with all configured properties.
         * <p>
         * The resulting message is immutable and safe for sharing across threads.
         * The implementation uses {@link DefaultMediaMessage} as the concrete type.
         * </p>
         *
         * @return a fully‑configured, immutable {@link DefaultMediaMessage}
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