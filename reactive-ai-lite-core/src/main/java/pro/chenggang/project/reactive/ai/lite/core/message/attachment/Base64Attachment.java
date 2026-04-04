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

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * An implementation of the {@link Attachment} interface that represents media content encoded in Base64.
 * <p>
 * This class is designed to embed media directly within a message payload by formatting
 * the content as a data URI scheme. It uses a builder pattern for easy instantiation.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Base64Attachment implements Attachment {

    /**
     * The MIME type of the media content (e.g., "image/png"). Must not be null.
     */
    @NonNull
    private final MimeType mimeType;

    /**
     * The character set of the content, optional and typically used for text-based MIME types.
     */
    private final Charset charset;

    /**
     * A descriptive name for the attachment. Must not be null.
     */
    @NonNull
    private final String name;

    /**
     * The Base64-encoded string representing the raw media content. Must not be null.
     */
    @NonNull
    private final String base64Content;

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
     * Retrieves the content of the attachment formatted as a data URI.
     * <p>
     * The format follows the standard data URI scheme:
     * {@code data:[<mime_type>][;charset=<charset>];base64,<base64_encoded_data>}
     * </p>
     *
     * @return the data URI string containing the Base64 content
     */
    @Override
    public String content() {
        StringBuilder builder = new StringBuilder("data:");
        builder.append(mimeType.toString());
        if (Objects.nonNull(charset)) {
            builder.append(";charset=").append(charset.name());
        }
        builder.append(";base64,").append(base64Content);
        return builder.toString();
    }
}
