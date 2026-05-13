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
package pro.chenggang.project.reactive.ai.lite.core.message.attachment;

import org.springframework.util.MimeType;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;

/**
 * Represents an attachment to a {@link Message}, allowing for the inclusion of non-textual content
 * such as images, documents, or other media.
 * <p>
 * This interface provides a common structure for different types of attachments, enabling
 * multi-modal interactions with AI models. Implementations define how the content is actually
 * stored and transmitted (e.g., via URL or Base64 encoding).
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface Attachment {

    /**
     * Returns the MIME type of the attachment's content.
     * <p>
     * This helps the AI model correctly interpret the data format (e.g., "image/jpeg",
     * "application/pdf").
     * </p>
     *
     * @return the {@link MimeType} of the attachment
     */
    MimeType mimeType();

    /**
     * Returns the name of the attachment.
     * <p>
     * This can be a filename or a descriptive identifier that provides context about the
     * attachment's purpose or origin.
     * </p>
     *
     * @return the name of the attachment
     */
    String name();

    /**
     * Returns the content of the attachment as a string.
     * <p>
     * The specific format of the string depends on the implementation. For example, it could be
     * a publicly accessible URL pointing to a resource, or a Base64-encoded string containing
     * the raw data.
     * </p>
     *
     * @return the string representation of the attachment's content
     */
    String content();

}
