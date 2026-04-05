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

import org.junit.jupiter.api.Test;
import org.springframework.util.MimeTypeUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Base64AttachmentTest {

    @Test
    void testBase64Attachment() {
        Base64Attachment attachment = Base64Attachment.builder()
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .name("test.png")
                .base64Content("SGVsbG8=")
                .charset(StandardCharsets.UTF_8)
                .build();
        
        assertThat(attachment.mimeType()).isEqualTo(MimeTypeUtils.IMAGE_PNG);
        assertThat(attachment.name()).isEqualTo("test.png");
        assertThat(attachment.content()).contains("SGVsbG8=");
        assertThat(attachment.content()).contains("charset=UTF-8");
    }
}
