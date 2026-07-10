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
import lombok.extern.jackson.Jacksonized;
import org.springframework.lang.Nullable;
import pro.chenggang.project.reactive.ai.lite.core.message.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Attachment;

/**
 * The default, immutable implementation of the {@link MediaMessage} interface,
 * designed for use within the reactive AI lite messaging framework.
 * <p>
 * This class represents a complete message entity that can carry both textual content and
 * an arbitrary number of media attachments (images, files, etc.). It is used as a standard
 * message carrier in conversational pipelines and is fully JSON‑friendly thanks to the
 * {@link Jacksonized} annotation, which enables deserialization via the generated builder.
 * </p>
 * <p>
 * <b>Immutability:</b> All fields are declared {@code final}, and the class provides no
 * setters, ensuring thread‑safety and predictability. The builder pattern (via
 * {@code @Builder}) is the only way to construct an instance.
 * </p>
 * <p>
 * <b>Defaults:</b> The {@code attachments} array defaults to an empty array rather than
 * {@code null}, simplifying client code by avoiding ubiquitous null‑checks.
 * </p>
 * <p>
 * <b>Type safety:</b> Overriding {@link #getActualType()} allows runtime inspection of the
 * concrete message type, which is crucial when messages are passed through generic
 * interfaces or collections.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see MediaMessage
 * @see Message
 */
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultMediaMessage implements MediaMessage {

    /**
     * The role of the message sender (for example, {@code "user"} or {@code "assistant"}).
     * This field must never be {@code null} and is used to distinguish the origin of the
     * message within a conversation. It follows the conventions defined by the reactive AI
     * lite protocol.
     */
    @NonNull
    private final String role;

    /**
     * The optional name of the sender (e.g., a specific user alias or model variant). This
     * field provides a more granular identity on top of the role. It can be {@code null}
     * when not needed.
     */
    @Nullable
    private final String name;

    /**
     * The primary textual content of the message. This field holds the main payload that
     * is processed by the language model or displayed to the user. It must never be
     * {@code null}, though it may be an empty string.
     */
    @NonNull
    private final String content;

    /**
     * Optional reasoning content, often used for chain‑of‑thought or explainability
     * features. When present, it represents the model's internal reasoning steps. This
     * field is separate from the main {@link #content} to allow a clean separation of
     * output and thought process. It may be {@code null}.
     */
    @Nullable
    private final String reasoningContent;

    /**
     * An array of media attachments (images, audio, files, etc.) associated with this
     * message. The builder defaults this to an empty array rather than {@code null},
     * guaranteeing that client code always receives a non‑null value. This field is
     * declared as {@code @NonNull} to enforce that invariant.
     */
    @NonNull
    @Builder.Default
    private final Attachment[] attachments = new Attachment[0];

    /**
     * Returns the role of the message sender. This value identifies the participant
     * category (user, assistant, system, etc.) and is used for routing and context
     * management in the conversation pipeline.
     *
     * @return the role string, never {@code null}
     */
    @Override
    public String getRole() {
        return this.role;
    }

    /**
     * Returns the optional name of the sender. The name can be used to distinguish
     * between multiple participants sharing the same role (e.g., two different user
     * profiles) or to identify a specific model version.
     *
     * @return the sender's name, or {@code null} if none was set
     */
    @Nullable
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Returns the main textual content of the message. This is the actual information
     * exchanged between the user and the AI model.
     *
     * @return the text content, never {@code null}
     */
    @Override
    public String getContent() {
        return this.content;
    }

    /**
     * Returns any reasoning content that accompanies the message. In advanced AI
     * interactions (e.g., with o‑series models), the model may provide a separate
     * “thinking” output. This method exposes that auxiliary information.
     *
     * @return the reasoning text, or {@code null} if absent
     */
    @Nullable
    @Override
    public String getReasoningContent() {
        return this.reasoningContent;
    }

    /**
     * Returns the media attachments of this message. The returned array is never
     * {@code null} (defaults to an empty array), enabling safe iteration without
     * null‑checks.
     *
     * @return an array of {@link Attachment} objects, possibly empty
     */
    @Override
    public Attachment[] getAttachments() {
        return this.attachments;
    }

    /**
     * Returns the concrete Java type of this message implementation.
     * <p>
     * This method is used for type introspection in generic message processing logic
     * (e.g., routing based on message type or serialization). It always returns
     * {@code DefaultMediaMessage.class}.
     * </p>
     *
     * @return {@code DefaultMediaMessage.class}
     */
    @Override
    public Class<? extends Message> getActualType() {
        return DefaultMediaMessage.class;
    }
}