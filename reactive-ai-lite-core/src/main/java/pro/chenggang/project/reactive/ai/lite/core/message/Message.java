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

/**
 * Represents a single message in a conversation with an AI model.
 * <p>
 * A message is the fundamental unit of data exchanged, containing content such as text,
 * media, or tool interactions. This interface provides a common abstraction for different
 * types of messages, allowing them to be processed generically.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface Message {

    /**
     * Returns the role of the message sender (e.g., "user", "assistant", "system").
     *
     * @return the role of the message sender
     */
    String getRole();

    /**
     * Returns the actual concrete type of the message.
     * <p>
     * This is useful for safely downcasting the generic Message interface to a
     * specific implementation (like TextMessage or ToolCallMessage) when specific
     * processing is required based on the message type.
     * </p>
     *
     * @return the actual class type of the message
     */
    Class<? extends Message> getActualType();

}
