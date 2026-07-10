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
package pro.chenggang.project.reactive.ai.lite.core.execution.response;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.message.AssistantTextMessage;

/**
 * Represents the final, deserialized object resulting from a structured chat request.
 * <p>
 * This class extends {@link ExtractedLlmResponse}, inheriting common metadata such as
 * token usage, finish reason, and timing information. It augments the base with two
 * essential pieces of structured response data:
 * <ul>
 *   <li>The original {@link AssistantTextMessage} emitted by the language model,
 *       containing the raw JSON string before any deserialization.</li>
 *   <li>The strongly-typed Java object ({@code T}) obtained by deserializing that JSON
 *       using a pre-configured object mapper or serialization framework.</li>
 * </ul>
 * This dual representation is designed to satisfy both type-safe programmatic consumption
 * and scenarios where the untouched JSON is needed (e.g., logging, auditing, or manual
 * fallback processing).
 * </p>
 * <p>
 * Instances of this class are intended to be created via the builder pattern provided by
 * {@code @SuperBuilder}, ensuring fluent and immutable construction of the entire hierarchy.
 * </p>
 *
 * @param <T> the type of the deserialized structured content
 * @author Gang Cheng
 * @version 0.1.0
 * @see ExtractedLlmResponse
 * @see AssistantTextMessage
 */
@Getter
@SuperBuilder
public class StructuredResponse<T> extends ExtractedLlmResponse {

    /**
     * The assistant message as returned by the language model.
     * <p>
     * This message holds the exact JSON string that the model produced in response to
     * the structured request. While the {@link #structuredContent} field provides
     * convenient typed access, this field preserves the original serialized form for
     * scenarios that require raw JSON, such as detailed logging, manual verification,
     * or fallback parsing.
     * </p>
     */
    private final AssistantTextMessage assistantTextMessage;

    /**
     * The deserialized structured content of type {@code T}.
     * <p>
     * This is the primary output of the structured chat, automatically converted from
     * the JSON string contained in {@link #assistantTextMessage} using an object mapper
     * (e.g., Jackson) configured within the framework. It enables direct, type-safe
     * integration with business logic, eliminating the need for manual JSON parsing and
     * casting. If deserialization fails, appropriate exceptions are thrown by the
     * underlying framework and should be handled by the caller.
     * </p>
     */
    private final T structuredContent;

}