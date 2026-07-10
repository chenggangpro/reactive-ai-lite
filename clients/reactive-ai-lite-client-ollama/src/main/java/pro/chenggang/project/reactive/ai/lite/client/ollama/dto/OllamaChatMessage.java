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
package pro.chenggang.project.reactive.ai.lite.client.ollama.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaChatMessage.ToolCall.ToolCallFunction;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;

import java.util.List;
import java.util.Map;

/**
 * Represents a single message in a chat conversation with the Ollama API.
 * <p>
 * This class is a data transfer object (DTO) that maps to the JSON structure
 * of a chat completion message. It supports all common message properties:
 * role, content, optional images for multimodal models, tool calls for function
 * calling, and a separate {@code thinking} field for models that expose internal
 * reasoning. The class is fully Jackson-annotated for seamless serialization
 * and deserialization when communicating with the Ollama HTTP endpoints.
 * </p>
 * <p>
 * The {@code role} field uses the {@link Role} enum, which standardizes sender
 * categories (system, user, assistant, tool). The {@code content} field carries
 * the textual payload, while {@code images} can hold base64-encoded image data
 * for vision-capable models. Tool‑calling is modeled through {@link ToolCall}
 * and its nested {@link ToolCallFunction}, allowing the assistant to propose
 * function invocations. The {@code toolName} property identifies the tool when
 * the message is a tool‑call result (typically role = tool). Finally, {@code thinking}
 * captures optional chain‑of‑thought text that some models output alongside the
 * final answer.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OllamaChatMessage {

    /**
     * The role of the message sender (e.g., system, user, assistant, tool).
     * Maps to the {@code role} field in the Ollama chat request/response JSON.
     */
    @JsonProperty("role")
    private final Role role;

    /**
     * The textual content of the message.
     * May be {@code null} for messages that contain only tool calls or images.
     */
    @JsonProperty("content")
    private final String content;

    /**
     * Optional list of images attached to the message.
     * Each entry is typically a base64-encoded image string. Only supported
     * by multimodal models.
     */
    @JsonProperty("images")
    private final List<String> images;

    /**
     * List of tool calls issued by the assistant.
     * Each {@link ToolCall} specifies a function name and arguments to be
     * executed. Used for function/tool calling features.
     */
    @JsonProperty("tool_calls")
    private final List<ToolCall> toolCalls;

    /**
     * The name of the tool when this message is a tool result.
     * Typically used with role = tool to identify which tool produced the result.
     */
    @JsonProperty("tool_name")
    private final String toolName;

    /**
     * Optional field representing the model's internal thinking or chain‑of‑thought.
     * Some Ollama models (e.g., those with a "thinking" feature) output reasoning
     * steps in this field, often alongside or instead of the final {@code content}.
     */
    @JsonProperty("thinking")
    private final String thinking;

    /**
     * Represents a single tool call requested by the assistant in a chat message.
     * <p>
     * A tool call is a structured request to invoke a specific function with
     * concrete arguments. This class mirrors the {@code tool_calls} array entries
     * in the Ollama API. Each {@code ToolCall} contains a {@link ToolCallFunction}
     * that details which function to call and what arguments to pass.
     * </p>
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ToolCall {

        /**
         * The function specification for this tool call.
         * Contains the function's name, a map of argument name‑value pairs,
         * and an optional index to distinguish multiple calls.
         */
        @JsonProperty("function")
        private final ToolCallFunction function;

        /**
         * Describes a function to be called as part of a tool call.
         * <p>
         * Includes the function's name, a map of argument names to their values
         * (as generic objects), and an optional index. The argument types are
         * determined by the function definition registered with the model.
         * </p>
         */
        @JsonInclude(Include.NON_NULL)
        @JsonIgnoreProperties(ignoreUnknown = true)
        @Getter
        @Builder
        @Jacksonized
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        public static class ToolCallFunction {
            /**
             * The name of the function to invoke.
             */
            @JsonProperty("name")
            private final String name;

            /**
             * A map of argument names to their values.
             * The values are represented as {@link Object} because their actual
             * types depend on the function's parameter definitions.
             */
            @JsonProperty("arguments")
            private final Map<String, Object> arguments;

            /**
             * Optional index to differentiate between multiple tool calls in a single message.
             * May be {@code null} if not provided by the API.
             */
            @JsonProperty("index")
            private final Integer index;
        }
    }
}