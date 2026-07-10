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
 * Immutable default implementation of {@link TextMessage} designed for textual
 * communication within the reactive AI framework. This class is intended to serve as
 * the standard concrete representation for messages that consist solely of a text
 * payload, such as common user or assistant utterances.
 * <p>
 * <strong>Design decisions and context:</strong>
 * <ul>
 *     <li><em>Immutability</em> – all fields are {@code final} and no setters are
 *         provided. This guarantees thread‑safety and simplifies reasoning about
 *         message state in reactive pipelines.</li>
 *     <li><em>Builder pattern</em> – annotated with {@link Builder} to enable fluent,
 *         readable construction without long argument lists. The generated builder
 *         respects {@link NonNull} constraints, ensuring mandatory fields are set.</li>
 *     <li><em>Jacksonized</em> – the {@link Jacksonized} annotation instructs Jackson
 *         to use the builder for deserialization, preserving immutability while
 *         achieving seamless JSON/XML (de)serialization. This is essential when
 *         messages are transferred over the network or stored externally.</li>
 *     <li><em>Private constructor</em> – access to the all‑args constructor is
 *         restricted to {@link AccessLevel#PRIVATE} so that object creation can only
 *         happen through the builder, centralising validation logic.</li>
 * </ul>
 * The class exposes type information via {@link #getActualType()} to support dynamic
 * dispatch or type‑specific handling in generic message processing flows.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 * @see TextMessage
 * @see Message
 */
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultTextMessage implements TextMessage {

    /**
     * The role of the entity that produced the message (e.g., {@code "user"},
     * {@code "assistant"}, {@code "system"}). In conversational AI models, the role
     * identifies the originator and determines how the content is interpreted in
     * the dialog history. This field is mandatory ({@link NonNull}) and cannot be
     * omitted.
     */
    @NonNull
    private final String role;

    /**
     * The primary textual payload of the message. This is the core information that
     * the sender wishes to convey, e.g., a user’s query or an assistant’s answer.
     * The field is mandatory ({@link NonNull}) and must not be empty in typical
     * usage.
     */
    @NonNull
    private final String content;

    /**
     * An optional human‑readable name for the sender, useful in multi‑user or
     * multi‑agent scenarios where the role alone is insufficient to distinguish
     * participants. When present, it can be displayed in interfaces or logged for
     * traceability. A {@code null} value means no explicit name is provided.
     */
    @Nullable
    private final String name;

    /**
     * Retrieves the role of the message sender.
     *
     * @return the role, never {@code null} (enforced by {@link NonNull} + builder)
     */
    @Override
    public String getRole() {
        return this.role;
    }

    /**
     * Provides the textual content of the message, which constitutes the main body
     * of the communication.
     *
     * @return the text content, never {@code null} (enforced by {@link NonNull} +
     *         builder)
     */
    @Override
    public String getContent() {
        return this.content;
    }

    /**
     * Returns the optional sender name, which can be used to distinguish between
     * multiple participants sharing the same role.
     *
     * @return the sender name, or {@code null} if none was specified during
     *         construction
     */
    @Nullable
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Exposes the concrete runtime type of this message implementation. This
     * information is valuable in generic message processing frameworks that
     * operate on {@link Message} references but need to perform type‑specific
     * actions, such as serialisation routing or validation.
     *
     * @return always {@code DefaultTextMessage.class}, representing this immutably
     *         implemented text message type
     */
    @Override
    public Class<? extends Message> getActualType() {
        return DefaultTextMessage.class;
    }

}