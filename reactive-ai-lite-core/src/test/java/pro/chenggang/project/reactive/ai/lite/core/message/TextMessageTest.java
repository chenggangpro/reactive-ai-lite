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
import pro.chenggang.project.reactive.ai.lite.core.option.Role;

import static org.assertj.core.api.Assertions.assertThat;

class TextMessageTest {

    @Test
    void testNewTextMessage() {
        TextMessage message = TextMessage.newTextMessage(Role.USER)
                .content("Hello")
                .name("Alice")
                .build();

        assertThat(message.getRole()).isEqualTo(Role.USER.getValue());
        assertThat(message.getContent()).isEqualTo("Hello");
        assertThat(message.getName()).isEqualTo("Alice");
    }

    @Test
    void testEmptyUserTextMessage() {
        TextMessage message = TextMessage.emptyUserTextMessage();
        assertThat(message.getRole()).isEqualTo(Role.USER.getValue());
        assertThat(message.getContent()).isEmpty();
    }

    @Test
    void testEmptySystemTextMessage() {
        TextMessage message = TextMessage.emptySystemTextMessage();
        assertThat(message.getRole()).isEqualTo(Role.SYSTEM.getValue());
        assertThat(message.getContent()).isEmpty();
    }
}
