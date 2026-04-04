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
 * The default, immutable implementation of the {@link MediaMessage} interface.
 * <p>
 * This class provides a concrete representation of a message that can contain both text
 * and media attachments. It uses Lombok's {@code @Builder} for construction and is
 * configured for Jackson JSON serialization/deserialization.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultMediaMessage implements MediaMessage {

    /**
     * The role of the message sender.
     */
    @NonNull
    private final String role;

    /**
     * The optional name of the sender.
     */
    @Nullable
    private final String name;

    /**
     * The primary textual content of the message.
     */
    @NonNull
    private final String content;

    /**
     * The optional reasoning content associated with the message.
     */
    @Nullable
    private final String reasoningContent;

    /**
     * An array of media attachments included in the message.
     * Defaults to an empty array.
     */
    @NonNull
    @Builder.Default
    private final Attachment[] attachments = new Attachment[0];

    /**
     * Retrieves the role of the message sender.
     *
     * @return the role as a string
     */
    @Override
    public String getRole() {
        return this.role;
    }

    /**
     * Retrieves the optional name of the sender.
     *
     * @return the name, or {@code null} if not provided
     */
    @Nullable
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Retrieves the primary textual content of the message.
     *
     * @return the text content
     */
    @Override
    public String getContent() {
        return this.content;
    }

    /**
     * Retrieves the reasoning content associated with the message.
     *
     * @return the reasoning content, or {@code null} if not available
     */
    @Nullable
    @Override
    public String getReasoningContent() {
        return this.reasoningContent;
    }

    /**
     * Retrieves the array of media attachments.
     *
     * @return an array of {@link Attachment} objects
     */
    @Override
    public Attachment[] getAttachments() {
        return this.attachments;
    }

    /**
     * Retrieves the actual concrete type of this message.
     *
     * @return the {@link DefaultMediaMessage} class type
     */
    @Override
    public Class<? extends Message> getActualType() {
        return DefaultMediaMessage.class;
    }
}
