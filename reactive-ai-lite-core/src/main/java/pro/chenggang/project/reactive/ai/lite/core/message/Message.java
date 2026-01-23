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

import pro.chenggang.project.reactive.ai.lite.core.message.defaults.TextMessage;

/**
 * Represents a single message in a conversation with an AI model.
 * A message is the fundamental unit of data exchanged, containing content such as text.
 * This interface provides a common abstraction for different types of messages.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface Message {

    /**
     * A constant representing an empty text message.
     * Useful for initialization or as a default value.
     */
    TextMessage EMPTY_MESSAGE = TextMessage.of("");

    /**
     * Returns the primary textual content of the message.
     *
     * @return The text content of the message.
     */
    String text();

}
