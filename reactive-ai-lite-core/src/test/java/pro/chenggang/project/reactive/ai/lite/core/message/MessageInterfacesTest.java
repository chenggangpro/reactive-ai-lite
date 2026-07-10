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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MessageInterfacesTest {

    @Test
    void testMediaMessageBuilder() {
        assertThatThrownBy(() -> MediaMessage.newMediaMessage((Role) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MediaMessage.newMediaMessage((String) null))
                .isInstanceOf(IllegalArgumentException.class);

        Attachment attachment1 = mock(Attachment.class);
        Attachment attachment2 = mock(Attachment.class);
        
        MediaMessage msg1 = MediaMessage.newMediaMessage(Role.USER)
                .name("testName")
                .content("hello")
                .attachments(attachment1, attachment2)
                .build();
                
        assertThat(msg1.getRole()).isEqualTo(Role.USER.getValue());
        assertThat(msg1.getName()).isEqualTo("testName");
        assertThat(msg1.getContent()).isEqualTo("hello");
        assertThat(msg1.getAttachments()).containsExactly(attachment1, attachment2);

        MediaMessage msg2 = MediaMessage.newMediaMessage("custom_role")
                .name(null)
                .content(null)
                .attachments((Attachment[]) null)
                .build();
                
        assertThat(msg2.getRole()).isEqualTo("custom_role");
        assertThat(msg2.getName()).isNull();
        assertThat(msg2.getContent()).isEmpty(); // default
        assertThat(msg2.getAttachments()).isEmpty();
        
        MediaMessage msg3 = MediaMessage.newMediaMessage("role")
                .attachments(List.of(attachment1))
                .build();
        assertThat(msg3.getAttachments()).containsExactly(attachment1);
        
        MediaMessage msg4 = MediaMessage.newMediaMessage("role")
                .attachments((List<Attachment>) null)
                .build();
        assertThat(msg4.getAttachments()).isEmpty();
    }
    
    @Test
    void testTextMessageBuilder() {
        assertThatThrownBy(() -> TextMessage.newTextMessage((Role) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TextMessage.newTextMessage((String) null))
                .isInstanceOf(IllegalArgumentException.class);
                
        TextMessage msg1 = TextMessage.newTextMessage(Role.ASSISTANT)
                .name("sys")
                .content("hey")
                .build();
                
        assertThat(msg1.getRole()).isEqualTo(Role.ASSISTANT.getValue());
        assertThat(msg1.getName()).isEqualTo("sys");
        assertThat(msg1.getContent()).isEqualTo("hey");
        
        TextMessage msg2 = TextMessage.newTextMessage("some_role")
                .name(null)
                .content(null)
                .build();
                
        assertThat(msg2.getRole()).isEqualTo("some_role");
        assertThat(msg2.getName()).isNull();
        assertThat(msg2.getContent()).isEmpty();
    }
}
