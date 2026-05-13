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
import lombok.extern.jackson.Jacksonized;
import org.springframework.lang.Nullable;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.TextMessage;

/**
 * The default, immutable implementation of the {@link TextMessage} interface.
 * <p>
 * This class provides a concrete representation of a text-based message within
 * the framework. It is designed to be immutable and easily serializable/deserializable
 * using Jackson.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultTextMessage implements TextMessage {

    /**
     * The role of the message sender.
     */
    @NonNull
    private final String role;

    /**
     * The primary textual content of the message.
     */
    @NonNull
    private final String content;

    /**
     * The optional name of the sender.
     */
    @Nullable
    private final String name;

    /**
     * Retrieves the role of the message sender.
     *
     * @return the role as a string
     */
    @Override
    public String getRole() {
        return this.role;
    }

    /**
     * Retrieves the primary textual content of the message.
     *
     * @return the text content
     */
    @Override
    public String getContent() {
        return this.content;
    }

    /**
     * Retrieves the optional name of the sender.
     *
     * @return the name, or {@code null} if not provided
     */
    @Nullable
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Retrieves the actual concrete type of this message.
     *
     * @return the {@link DefaultTextMessage} class type
     */
    @Override
    public Class<? extends Message> getActualType() {
        return DefaultTextMessage.class;
    }

}
