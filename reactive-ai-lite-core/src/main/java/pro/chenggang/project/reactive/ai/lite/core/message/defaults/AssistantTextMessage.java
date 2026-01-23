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

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a text message from the AI assistant.
 * This class extends {@link TextMessage} to include optional reasoning content,
 * which can provide insights into the model's thought process.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
public class AssistantTextMessage extends TextMessage {

    /**
     * The reasoning content or thought process of the AI model, which may precede the final text content.
     * This can be null if no reasoning content is available.
     */
    private final String reasoningContent;

    /**
     * Private constructor to create a new AssistantTextMessage.
     *
     * @param textContent The main textual content of the message. Must not be null.
     * @param reasoningContent The supplementary reasoning content. Can be null.
     */
    private AssistantTextMessage(@NonNull String textContent, String reasoningContent) {
        super(textContent);
        this.reasoningContent = reasoningContent;
    }

    /**
     * Factory method to create an {@link AssistantTextMessage} with only text content.
     *
     * @param textContent The main textual content of the message. Must not be null.
     * @return A new instance of {@link AssistantTextMessage}.
     */
    public static AssistantTextMessage of(@NonNull String textContent) {
        return of(textContent, null);
    }

    /**
     * Factory method to create an {@link AssistantTextMessage} with both text and reasoning content.
     *
     * @param textContent The main textual content of the message. Must not be null.
     * @param reasoningContent The supplementary reasoning content.
     * @return A new instance of {@link AssistantTextMessage}.
     */
    public static AssistantTextMessage of(@NonNull String textContent, String reasoningContent) {
        return new AssistantTextMessage(textContent, reasoningContent);
    }

}
