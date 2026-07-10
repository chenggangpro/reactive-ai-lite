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
 * reasoning content produced during generation. The reasoning content is particularly
 * useful for models that expose a "chain-of-thought" style output (e.g., o1, Claude 3.5),
 * enabling transparency and debug of the assistant's thinking process.
 * <p>
 * The instance is created via a Lombok-generated builder, ensuring a fluent construction
 * pattern. Immutability is enforced by the private final fields and the private constructor.
 * Jackson's {@link Jacksonized} annotation ensures seamless serialization/deserialization,
 * making this message suitable for REST APIs and messaging systems.
 * <p>
 * By extending {@link AbstractAttribute}, the message can carry additional metadata,
 * while the {@code getActualType()} method returns the concrete class, enabling type‑safe
 * message routing and processing in reactive pipelines.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultAssistantTextMessage extends AbstractAttribute implements AssistantTextMessage {

    /**
     * The final textual content of the assistant's response, presented to the end user.
     * <p>
     * This field corresponds to the standard {@code content} property of an assistant message
     * and represents the fully processed, human‑readable output of the model. It is
     * {@code null} when the message does not contain a visible answer (e.g., if only
     * reasoning content is provided). In practice, most text‑based interactions will
     * include a non‑null value here.
     */
    @Nullable
    private final String content;

    /**
     * The internal reasoning or “chain‑of‑thought” content produced during generation.
     * <p>
     * Some advanced language models generate an intermediate reasoning trace before
     * formulating the final answer. This field stores that trace, allowing downstream
     * consumers to inspect the model's step‑by‑step logic. It is {@code null} for models
     * that do not emit reasoning content or when the reasoning is not captured.
     */
    @Nullable
    private final String reasoningContent;

    /**
     * Retrieves the primary textual content of the message.
     * <p>
     * The returned value is the assistant's final, polished answer, suitable for
     * direct display to a human. In a typical conversation, this is the actual
     * answer; in specialized use cases it may be empty or {@code null} when only
     * reasoning content is exposed. Implementors of message consumers should
     * handle both scenarios gracefully.
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
     * <p>
     * This content represents the model's internal deliberation and is often
     * formatted as a thought process or a chain of intermediate steps. It is
     * valuable for debugging, auditing, and providing transparency in AI‑assisted
     * applications. When not supported by the model, this method returns {@code null}.
     *
     * @return the reasoning content, or {@code null} if not available
     */
    @Nullable
    @Override
    public String getReasoningContent() {
        return this.reasoningContent;
    }

    /**
     * Returns the concrete runtime type of this message.
     * <p>
     * This method is used for polymorphic message identification, allowing
     * reactive pipelines, serializers, and dispatchers to correctly down‑cast
     * or select processing logic based on the exact message class. The return
     * value is always {@code DefaultAssistantTextMessage.class}.
     *
     * @return the specific class type of this message implementation
     */
    @Override
    public Class<? extends Message> getActualType() {
        return DefaultAssistantTextMessage.class;
    }

}