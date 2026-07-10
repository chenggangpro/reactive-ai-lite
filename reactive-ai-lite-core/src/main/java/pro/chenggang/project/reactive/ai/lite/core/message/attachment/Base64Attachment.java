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
 * This class embeds media directly within a message payload by formatting the content as a
 * <a href="https://datatracker.ietf.org/doc/html/rfc2397">data URI scheme</a>. It uses a builder
 * pattern for convenient, immutable instantiation. The typical workflow is:
 * <ol>
 *   <li>Specify the media's MIME type via {@code mimeType} (e.g., {@code image/png}).</li>
 *   <li>Optionally include a {@link Charset} when the media is textual (e.g., {@code text/plain}).</li>
 *   <li>Provide a human‑readable {@code name} for the attachment.</li>
 *   <li>Pass the raw content as a Base64‑encoded string in {@code base64Content}.</li>
 *   <li>The {@link #content()} method then dynamically builds the data URI:
 *       {@code data:<mime_type>[;charset=<charset>];base64,<base64_data>}.</li>
 * </ol>
 * This approach avoids external URL dependencies and keeps the message self‑contained.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Base64Attachment implements Attachment {

    /**
     * The MIME type of the media content, e.g., {@code "image/png"} or {@code "application/pdf"}.
     * <p>
     * This value is required because it describes the data format, enabling consumers to
     * correctly interpret the Base64 payload. It appears in the data URI's type segment
     * right after the {@code data:} prefix.
     * </p>
     */
    @NonNull
    private final MimeType mimeType;

    /**
     * An optional character set identifier.
     * <p>
     * Typically provided for text‑based MIME types (e.g., {@code text/html; charset=UTF-8}).
     * If present, it generates a {@code ;charset=<name>} parameter in the data URI. When
     * {@code null}, the parameter is omitted, leaving the consumer to assume a default
     * charset. Used by {@link #content()} to compose the full URI.
     * </p>
     */
    private final Charset charset;

    /**
     * A human‑readable, descriptive name for the attachment.
     * <p>
     * This identifier is useful for logging, debugging, and context within the message.
     * It does not influence the data URI but serves as a label for the attached media,
     * e.g., "invoice.png" or "transcript.txt".
     * </p>
     */
    @NonNull
    private final String name;

    /**
     * The raw content encoded as a Base64 string.
     * <p>
     * This field holds the media payload. The Base64 encoding ensures safe transport
     * within text‑based protocols and is mandatory for a data URI. The string must
     * represent valid Base64 without line breaks or whitespace. It is directly appended
     * after the {@code ;base64,} prefix in the generated URI.
     * </p>
     */
    @NonNull
    private final String base64Content;

    /**
     * Returns the MIME type of this attachment.
     *
     * @return the {@link MimeType} set at construction time
     */
    @Override
    public MimeType mimeType() {
        return mimeType;
    }

    /**
     * Returns the descriptive name of this attachment.
     *
     * @return the name string set at construction time
     */
    @Override
    public String name() {
        return this.name;
    }

    /**
     * Generates the complete data URI representation of this attachment.
     * <p>
     * The format follows the data URI scheme:
     * {@code data:[<mime_type>][;charset=<charset>];base64,<base64_content>}.
     * <ul>
     *   <li>The MIME type is obtained from {@link #mimeType}.</li>
     *   <li>If {@link #charset} is not null, a {@code ;charset=...} parameter is appended.</li>
     *   <li>The constant {@code ;base64,} signals the encoding scheme.</li>
     *   <li>Finally, the {@link #base64Content} is concatenated, resulting in a self‑contained
     *       data URI that can be embedded directly in HTML, JSON, or similar text formats.</li>
     * </ul>
     * This method avoids the overhead of streaming or external storage by
     * constructing the URI on demand each time it is called.
     * </p>
     *
     * @return a String containing the data URI with the embedded Base64 content
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