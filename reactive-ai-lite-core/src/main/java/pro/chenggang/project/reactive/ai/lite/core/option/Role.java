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
package pro.chenggang.project.reactive.ai.lite.core.option;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;

import java.util.Locale;

/**
 * Defines the role of the entity that authored a message in a conversation.
 * <p>
 * In LLM-based chat interactions, every message is assigned a role that dictates how the model interprets and handles it.
 * This enum standardizes those roles to match the common convention used by OpenAI and other providers:
 * <ul>
 *   <li>{@link #SYSTEM} – provides high‑level behavioral guidance and context for the entire conversation.</li>
 *   <li>{@link #USER} – represents the end‑user's input or query.</li>
 *   <li>{@link #ASSISTANT} – the model's generated response; essential for multi‑turn dialogue history.</li>
 *   <li>{@link #TOOL} – carries the result of a function/tool call back to the model, enabling tool‑augmented
 *   reasoning.</li>
 * </ul>
 * Each constant is annotated with {@link JsonProperty} so that Jackson serialization/deserialization uses the
 * lowercase names (e.g., "system", "user") expected by the public LLM APIs.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see #getValue()
 * @see #fromValue(String)
 */
public enum Role {

    /**
     * System message – used to set the assistant’s persona, behavior, tone, and constraints.
     * <p>
     * Typically placed at the very beginning of a conversation. The model gives higher priority to system
     * messages when generating responses, effectively shaping the entire interaction. This role does not
     * correspond to user input but to the developer’s instructions.
     * </p>
     */
    @JsonProperty("system")
    SYSTEM,

    /**
     * User message – originates from the end‑user interacting with the AI.
     * <p>
     * Represents questions, statements, or any input the human provides. In a multi‑turn conversation,
     * consecutive user messages are interleaved with assistant and tool messages to maintain context.
     * </p>
     */
    @JsonProperty("user")
    USER,

    /**
     * Assistant message – the model’s own output during a conversation turn.
     * <p>
     * Storing assistant messages in the history is crucial for maintaining state across turns; without them,
     * the model would have no recollection of its previous responses. This role also appears when the model
     * decides to invoke a tool, where the content may be empty and a tool call payload is attached instead.
     * </p>
     */
    @JsonProperty("assistant")
    ASSISTANT,

    /**
     * Tool message – contains the execution result of a function/tool that the model requested.
     * <p>
     * After the model emits a {@link #ASSISTANT} message with a tool call, the external system executes the
     * tool and returns its output using this role. The model then uses the tool message’s content to formulate
     * a final user‑facing answer, allowing it to incorporate real‑time data or computations beyond its training
     * cutoff.
     * </p>
     */
    @JsonProperty("tool")
    TOOL,

    ;

    /**
     * Returns the lowercase string representation of this role, suitable for JSON serialization.
     * <p>
     * API contracts (OpenAI, Azure, etc.) expect role names in lowercase. Using {@link Locale#ROOT} ensures
     * a locale‑independent conversion, preventing unexpected behavior in regions with special casing rules.
     * </p>
     *
     * @return the lowercased role name, e.g. "system", "user", "assistant", "tool"
     */
    public String getValue() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Creates a {@link Role} from a case‑insensitive string value.
     * <p>
     * This method is typically used when parsing role information from JSON responses or configuration files,
     * where the case may vary. It converts the input to uppercase using {@link Locale#ROOT} and then delegates
     * to {@link Enum#valueOf(Class, String)}. The {@link NonNull} annotation on the parameter enforces that a
     * non‑null value is supplied at compile time (via Lombok).
     * </p>
     *
     * @param value the role string; must not be {@code null} and should match one of the enum names
     *              (case‑insensitive, e.g., "SYSTEM", "user", "Assistant")
     * @return the corresponding role constant
     * @throws IllegalArgumentException if the given string does not correspond to any role
     */
    public static Role fromValue(@NonNull String value) {
        return Role.valueOf(value.toUpperCase(Locale.ROOT));
    }

}