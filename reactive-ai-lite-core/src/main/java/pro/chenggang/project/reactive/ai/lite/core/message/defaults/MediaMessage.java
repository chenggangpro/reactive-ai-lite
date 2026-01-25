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

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.springframework.util.Assert;
import pro.chenggang.project.reactive.ai.lite.core.message.Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;

import java.util.List;

/**
 * Represents a multi-modal message that combines textual content with one or more media attachments.
 * This class implements the {@link Message} interface and is designed to handle complex inputs,
 * such as an image with a descriptive text prompt.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@ToString
public class MediaMessage implements Message {

    /**
     * The textual part of the message, which can be a prompt, a question, or a description
     * related to the media attachments.
     */
    private final String textContent;
    /**
     * A list of {@link Attachment} objects associated with the message.
     * This list must not be empty.
     */
    @Getter
    private final List<Attachment> attachments;

    /**
     * Private constructor to create a new {@link MediaMessage}.
     * It ensures that the list of attachments is not empty.
     *
     * @param textContent The textual content of the message. Can be empty but not null.
     * @param attachments A non-empty list of {@link Attachment} objects.
     */
    private MediaMessage(@NonNull String textContent, @NonNull List<Attachment> attachments) {
        Assert.notEmpty(attachments, "Attachments cannot be empty in media message.");
        this.textContent = textContent;
        this.attachments = attachments;
    }

    /**
     * Factory method to create a {@link MediaMessage} with attachments and an empty text part.
     *
     * @param attachments A non-empty list of {@link Attachment} objects.
     * @return A new instance of {@link MediaMessage}.
     */
    public static MediaMessage of(@NonNull List<Attachment> attachments) {
        return of("", attachments);
    }

    /**
     * Factory method to create a {@link MediaMessage} with both textual content and attachments.
     *
     * @param textContent The textual content for the message.
     * @param attachments A non-empty list of {@link Attachment} objects.
     * @return A new instance of {@link MediaMessage}.
     */
    public static MediaMessage of(@NonNull String textContent, @NonNull List<Attachment> attachments) {
        return new MediaMessage(textContent, attachments);
    }

    @Override
    public String text() {
        return this.textContent;
    }

}
