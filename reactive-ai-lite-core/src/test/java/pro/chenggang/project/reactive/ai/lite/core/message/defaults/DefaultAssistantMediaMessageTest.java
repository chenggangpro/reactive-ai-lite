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

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Attachment;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DefaultAssistantMediaMessageTest {

    @Test
    void testBuilderAndGetters() {
        String content = "Here is an analysis of the image";
        String name = "assistant-1";
        String reasoningContent = "Analyzing pixel data";
        Attachment attachment = mock(Attachment.class);
        Attachment[] attachments = new Attachment[]{attachment};

        DefaultAssistantMediaMessage message = DefaultAssistantMediaMessage.builder()
                .content(content)
                .name(name)
                .reasoningContent(reasoningContent)
                .attachments(attachments)
                .build();

        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getName()).isEqualTo(name);
        assertThat(message.getReasoningContent()).isEqualTo(reasoningContent);
        assertThat(message.getAttachments()).containsExactly(attachment);
        assertThat(message.getRole()).isEqualTo(Role.ASSISTANT.name().toLowerCase());
        assertThat(message.getActualType()).isEqualTo(DefaultAssistantMediaMessage.class);
    }
}
