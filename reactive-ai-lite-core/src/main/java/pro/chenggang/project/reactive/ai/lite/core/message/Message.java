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
 * The fundamental contract for any message exchanged within an AI conversation.
 * <p>
 * In a reactive AI pipeline, messages flow as the primary unit of information between
 * the user, the system, and the AI model. This interface abstracts the common structure,
 * enabling polymorphic processing and type-safe handling across different message variants
 * (e.g., plain text, tool invocation results, multimedia content). By exposing the
 * sender's role and the concrete Java type, components can route, transform, or react
 * to messages without coupling to specific implementations.
 * </p>
 * <p>
 * Typical roles include {@code "user"} for human input, {@code "assistant"} for model
 * responses, and {@code "system"} for instructions that steer the conversation. The
 * {@link #getActualType()} method is particularly important for frameworks that need to
 * deserialize or dispatch messages based on their true runtime type, avoiding unsafe casts
 * and enabling compile-time safety where possible.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface Message {

    /**
     * Retrieves the conversational role of the entity that produced this message.
     * <p>
     * The role acts as a discriminator that signals how the message should be interpreted
     * by downstream processors. For example, a message with role {@code "system"} might
     * influence the model's behavior globally, while a {@code "user"} message represents
     * a direct query. The exact set of allowed roles is defined by the AI model and the
     * higher-level orchestration logic; this method simply returns the assigned role as a
     * plain string, allowing maximum flexibility.
     * </p>
     *
     * @return the role identifier, never null, typically one of "user", "assistant", "system", or a model-specific value
     */
    String getRole();

    /**
     * Returns the specific concrete class that implements this message at runtime.
     * <p>
     * Because the {@code Message} interface is generic, consumers often need to downcast
     * to access traits like the text payload of a {@code TextMessage} or the tool call
     * details of a {@code ToolCallMessage}. This method exposes the actual type in a
     * type-safe manner, enabling patterns such as:
     * </p>
     * <pre>{@code
     * if (message.getActualType() == TextMessage.class) {
     *     TextMessage tm = (TextMessage) message;
     *     // process text
     * }
     * }</pre>
     * <p>
     * It is also instrumental for serialization frameworks and dynamic dispatchers that
     * reconstruct messages from a wire format and need to instantiate the correct subclass.
     * The returned class is guaranteed to be an implementation of {@code Message}, and it
     * is typically a more specific subtype that carries meaningful data.
     * </p>
     *
     * @return the runtime class of this message, never null, extending {@code Message}
     */
    Class<? extends Message> getActualType();

}