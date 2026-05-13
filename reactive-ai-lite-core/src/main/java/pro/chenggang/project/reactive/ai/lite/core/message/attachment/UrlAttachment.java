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

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.util.MimeType;

/**
 * An implementation of {@link Attachment} where the content is referenced by a URL.
 * <p>
 * This class is useful for including web-hosted images or other resources in a message
 * without embedding the actual binary data directly, reducing the payload size. The
 * underlying AI provider is responsible for fetching the resource from the URL.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class UrlAttachment implements Attachment {

    /**
     * The MIME type of the content located at the URL.
     */
    @NonNull
    private final MimeType mimeType;

    /**
     * The URL pointing to the actual content.
     */
    @NonNull
    private final String url;

    /**
     * The descriptive name or identifier for this attachment.
     */
    @NonNull
    private final String name;

    /**
     * Retrieves the MIME type of the attachment.
     *
     * @return the {@link MimeType}
     */
    @Override
    public MimeType mimeType() {
        return mimeType;
    }

    /**
     * Retrieves the name of the attachment.
     *
     * @return the name string
     */
    @Override
    public String name() {
        return this.name;
    }

    /**
     * Retrieves the content of the attachment.
     * <p>
     * For a URL attachment, the content is the URL string itself.
     * </p>
     *
     * @return the URL string
     */
    @Override
    public String content() {
        return this.url;
    }
}
