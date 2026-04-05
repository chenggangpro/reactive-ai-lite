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

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Attachment;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MediaMessageTest {

    @Test
    void testNewMediaMessageWithRole() {
        MediaMessage message = MediaMessage.newMediaMessage(Role.USER)
                .content("hello")
                .name("user")
                .reasoningContent("reason")
                .attachments(mock(Attachment.class))
                .build();
        
        assertThat(message.getRole()).isEqualTo(Role.USER.getValue());
        assertThat(message.getContent()).isEqualTo("hello");
        assertThat(message.getName()).isEqualTo("user");
        assertThat(message.getReasoningContent()).isEqualTo("reason");
        assertThat(message.getAttachments()).hasSize(1);
    }

    @Test
    void testNewMediaMessageWithStringRole() {
        MediaMessage message = MediaMessage.newMediaMessage("user")
                .content("hello")
                .build();
        
        assertThat(message.getRole()).isEqualTo("user");
    }

    @Test
    void testMediaMessageBuilderWithList() {
        Attachment attachment = mock(Attachment.class);
        MediaMessage message = MediaMessage.newMediaMessage(Role.USER)
                .attachments(Collections.singletonList(attachment))
                .build();
        
        assertThat(message.getAttachments()).containsExactly(attachment);
    }

    @Test
    void testMediaMessageBuilderWithNulls() {
        MediaMessage message = MediaMessage.newMediaMessage(Role.USER)
                .content(null)
                .reasoningContent(null)
                .attachments((Attachment[]) null)
                .build();
        
        assertThat(message.getContent()).isEmpty();
        assertThat(message.getReasoningContent()).isNull();
        assertThat(message.getAttachments()).isEmpty();
    }
}
