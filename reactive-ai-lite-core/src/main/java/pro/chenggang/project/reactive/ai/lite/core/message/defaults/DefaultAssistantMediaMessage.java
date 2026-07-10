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
import pro.chenggang.project.reactive.ai.lite.core.message.AssistantMediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Attachment;

/**
 * The default, immutable implementation of the {@link AssistantMediaMessage} interface,
 * representing a message produced by an AI assistant that includes both textual content
 * (with optional reasoning) and one or more media attachments.
 *
 * <p>This class is designed to be constructed exclusively through its generated builder,
 * which is enabled by the {@link Builder} and {@link Jacksonized} Lombok annotations.
 * Immutability is enforced by the {@link RequiredArgsConstructor} with private access,
 * ensuring that instances are fully initialized at construction time and cannot be modified
 * afterward. This is critical for thread safety and reliable message routing in reactive
 * pipelines.
 *
 * <p>The assistant role is implicit: unlike generic messages, this class always represents
 * an assistant's output. The {@code content} field holds the main textual response,
 * {@code reasoningContent} carries internal chain-of-thought or explanation (if the model
 * supports it), and {@code attachments} stores any associated media (images, files, etc.).
 * The attachments array defaults to an empty array via {@code @Builder.Default} to
 * avoid null handling in downstream consumers.
 *
 * <p>For serialization, the {@link Jacksonized} annotation integrates the builder with
 * JSON deserialization, allowing the object to be reconstructed from JSON without
 * compromising immutability.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see AssistantMediaMessage
 * @see Attachment
 */
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultAssistantMediaMessage implements AssistantMediaMessage {

    /**
     * The optional name of the assistant sender.
     * <p>
     * In multi‑agent scenarios or when the assistant has a designated persona,
     * this field can be populated with a human‑readable identifier. If not provided,
     * it remains {@code null}, which is acceptable for standard single‑assistant
     * interactions. Its nullability avoids forcing a naming convention while
     * preserving extensibility.
     */
    @Nullable
    private final String name;

    /**
     * The mandatory primary textual content of the message.
     * <p>
     * This field must never be {@code null} (enforced by {@link NonNull}),
     * ensuring that every assistant response carries a textual payload even
     * when attachments are present. It serves as the fallback representation
     * for clients that cannot process media.
     */
    @NonNull
    private final String content;

    /**
     * The optional reasoning content associated with the message.
     * <p>
     * When the underlying AI model emits a "chain‑of‑thought" or step‑by‑step
     * explanation, this field captures that output. It is kept separate from
     * the main content to allow UI layers to show or hide reasoning on demand.
     * A {@code null} value indicates that no reasoning was generated.
     */
    @Nullable
    private final String reasoningContent;

    /**
     * The array of media attachments included in the message.
     * <p>
     * Attachments represent any non‑textual data, such as images, audio, or
     * file references. The field defaults to an empty array via {@code Builder.Default}
     * to guarantee a non‑null value and simplify consumption. The order of
     * attachments is preserved, enabling deterministic rendering when multiple
     * media items are returned.
     */
    @NonNull
    @Builder.Default
    private final Attachment[] attachments = new Attachment[0];

    /**
     * Returns the optional sender name.
     * <p>
     * The name is typically not required for standard assistant responses;
     * however, it can be used to differentiate between multiple assistant
     * instances or to inject a persona label.
     *
     * @return the assistant's name, or {@code null} if not set
     */
    @Nullable
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Returns the primary textual content of the message.
     * <p>
     * This is the core assistant answer, guaranteed to be non‑null. It should
     * be used as the main display text or as the text part in mixed‑media
     * presentations.
     *
     * @return the non‑null textual content
     */
    @Override
    public String getContent() {
        return this.content;
    }

    /**
     * Returns any reasoning content associated with this message.
     * <p>
     * Reasoning content provides insight into the model's thought process
     * and can be displayed to users (e.g., in an expandable panel) or logged
     * for debugging and audit purposes. If no such content exists, the method
     * returns {@code null}.
     *
     * @return the reasoning text, or {@code null} if not available
     */
    @Nullable
    @Override
    public String getReasoningContent() {
        return this.reasoningContent;
    }

    /**
     * Returns the array of media attachments.
     * <p>
     * The returned array is a direct reference to the internal field; this class
     * does not offer a defensive copy because the builder enforces that attachments
     * are provided as an immutable snapshot during construction. Consumers must
     * not mutate the array. An empty array is returned when no attachments were
     * included.
     *
     * @return a non‑null, potentially empty array of {@link Attachment} objects
     */
    @Override
    public Attachment[] getAttachments() {
        return this.attachments;
    }

    /**
     * Identifies the concrete runtime type of this message.
     * <p>
     * This method is used by the message framework to determine the exact
     * implementation class for type‑based filtering, routing, or serialization
     * strategies. In a polymorphic message handling scenario, relying on
     * {@code instanceof} checks may be fragile; this method provides a stable,
     * hard‑coded reference to {@code DefaultAssistantMediaMessage}, simplifying
     * dispatcher logic and avoiding reflection.
     *
     * @return the {@link DefaultAssistantMediaMessage} class literal
     */
    @Override
    public Class<? extends Message> getActualType() {
        return DefaultAssistantMediaMessage.class;
    }
}