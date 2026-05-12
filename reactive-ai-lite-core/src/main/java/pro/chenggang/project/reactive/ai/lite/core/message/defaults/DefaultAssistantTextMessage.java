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
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import org.springframework.lang.Nullable;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.AbstractAttribute;
import pro.chenggang.project.reactive.ai.lite.core.message.AssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;

/**
 * The default, immutable implementation of the {@link AssistantTextMessage} interface.
 * <p>
 * This class provides a concrete representation of a text-based response generated
 * by an AI model. It inherently possesses the "assistant" role (via the interface
 * default method) and may contain both the final response text and any internal
 * reasoning content produced during generation. It is designed to be easily
 * serializable and deserializable using Jackson.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultAssistantTextMessage extends AbstractAttribute implements AssistantTextMessage {

    /**
     * The primary textual content of the message.
     */
    @Nullable
    private final String content;

    /**
     * The reasoning content associated with the message, if any.
     */
    @Nullable
    private final String reasoningContent;

    /**
     * Retrieves the primary textual content.
     *
     * @return the text content, or {@code null} if not available
     */
    @Nullable
    @Override
    public String getContent() {
        return this.content;
    }

    /**
     * Retrieves the reasoning content associated with the message.
     *
     * @return the reasoning content, or {@code null} if not available
     */
    @Nullable
    @Override
    public String getReasoningContent() {
        return this.reasoningContent;
    }

    /**
     * Retrieves the actual concrete type of this message.
     *
     * @return the {@link DefaultAssistantTextMessage} class type
     */
    @Override
    public Class<? extends Message> getActualType() {
        return DefaultAssistantTextMessage.class;
    }

}
