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
import org.springframework.util.MimeType;
import pro.chenggang.project.reactive.ai.lite.core.message.Attachment;

/**
 * An implementation of {@link Attachment} for content referenced by a URL.
 * This is useful for including web-hosted images or other resources in a message
 * without embedding the data directly.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class UrlAttachment implements Attachment {

    @NonNull
    private final MimeType mimeType;
    @NonNull
    private final String url;

    @NonNull
    private final String name;

    @Override
    public MimeType mimeType() {
        return mimeType;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String content() {
        return this.url;
    }
}
