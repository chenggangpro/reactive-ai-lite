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

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;


/**
 * A basic implementation of the {@link Message} interface that represents a simple text message.
 * This class is immutable and serves as a foundational building block for conversations.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public class TextMessage implements Message {

    /**
     * The textual content of the message.
     */
    protected final String textContent;

    protected TextMessage(@NonNull String textContent) {
        this.textContent = textContent;
    }

    /**
     * Factory method to create a new {@link TextMessage}.
     *
     * @param textContent The text for the message. Must not be null.
     * @return A new instance of {@link TextMessage}.
     */
    public static TextMessage of(@NonNull String textContent) {
        return new TextMessage(textContent);
    }

    @Override
    public String text() {
        return this.textContent;
    }

}
