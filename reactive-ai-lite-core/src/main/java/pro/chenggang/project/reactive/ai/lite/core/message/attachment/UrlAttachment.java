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
 * An {@link Attachment} implementation that references external content via a URL.
 * <p>
 * Instead of embedding large binary data directly into the message, this class stores
 * a remote URL from which the AI provider will download the actual content. This
 * significantly reduces the payload size of each interaction and offloads network and
 * storage responsibilities to the provider. The provider uses the {@link #mimeType}
 * to understand the format of the remote resource (e.g., {@code image/png}) without
 * needing to inspect the bytes first.
 * <p>
 * Potential downsides include increased latency because of the extra fetch step and
 * a dependency on the availability of the external resource. It is recommended to use
 * URL attachments when the content is hosted on a stable, publicly accessible server.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class UrlAttachment implements Attachment {

    /**
     * The MIME type that describes the format of the content located at {@link #url}.
     * <p>
     * This information allows the AI provider to correctly interpret the downloaded
     * bytes without having to guess or probe the content. Common values include
     * {@code image/png}, {@code image/jpeg}, and {@code audio/wav}.
     */
    @NonNull
    private final MimeType mimeType;

    /**
     * The fully qualified URL pointing to the external content.
     * <p>
     * The AI provider will issue a GET request to this URL to retrieve the resource.
     * For security and reliability, this should be a URL served by a trusted host with
     * proper access controls and availability guarantees.
     */
    @NonNull
    private final String url;

    /**
     * A human-readable name or logical identifier for this attachment.
     * <p>
     * This name is not used for retrieval but can serve as a label in logs, debugging
     * output, or user interfaces, making it easier to distinguish multiple attachments
     * in a single message.
     */
    @NonNull
    private final String name;

    /**
     * Returns the MIME type that instructs the provider how to interpret the fetched content.
     *
     * @return the {@link MimeType} associated with the external resource
     */
    @Override
    public MimeType mimeType() {
        return mimeType;
    }

    /**
     * Returns the descriptive name assigned to this attachment.
     *
     * @return the attachment name; never null
     */
    @Override
    public String name() {
        return this.name;
    }

    /**
     * Returns the content representation of this URL attachment.
     * <p>
     * Because the actual bytes are not held locally, this method returns the raw
     * {@link #url} string. The AI provider is responsible for resolving the URL
     * and downloading the content. This approach keeps the in-memory footprint
     * extremely small.
     *
     * @return the URL string that points to the remote resource
     */
    @Override
    public String content() {
        return this.url;
    }
}