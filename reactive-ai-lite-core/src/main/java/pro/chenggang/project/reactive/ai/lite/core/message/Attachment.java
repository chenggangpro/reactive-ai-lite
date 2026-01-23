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

import org.springframework.util.MimeType;

/**
 * Represents an attachment to a {@link Message}, allowing for the inclusion of non-textual content
 * such as images, documents, or other media. This interface provides a common structure for
 * different types of attachments.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface Attachment {

    /**
     * Returns the MIME type of the attachment's content. This helps the AI model
     * correctly interpret the data (e.g., "image/jpeg", "application/pdf").
     *
     * @return The {@link MimeType} of the attachment.
     */
    MimeType mimeType();

    /**
     * Returns the name of the attachment, which can be a filename or a descriptive identifier.
     *
     * @return The name of the attachment.
     */
    String name();

    /**
     * Returns the content of the attachment. The format of the content depends on the
     * implementation; for example, it could be a URL pointing to a resource or a
     * Base64-encoded string representing the data.
     *
     * @return The content of the attachment as a string.
     */
    String content();

}
