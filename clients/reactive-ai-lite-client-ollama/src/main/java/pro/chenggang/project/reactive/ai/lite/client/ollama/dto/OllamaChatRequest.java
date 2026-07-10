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

import java.util.List;
import java.util.Map;

/**
 * Immutable request object for the Ollama chat completions API.
 * <p>
 * This DTO maps one-to-one with the JSON payload expected by Ollama's {@code /api/chat}
 * endpoint. It encapsulates the conversation history, model selection, generation parameters,
 * and optional capabilities such as tool calling, structured output formatting, and streaming.
 * <p>
 * The class is designed to be built using the Lombok {@link Builder} pattern; its constructor
 * is private and the instance is created via {@link #builder()}. Jackson deserialization is
 * supported through the {@link Jacksonized} annotation.
 * <p>
 * For detailed API semantics, refer to the
 * <a href="https://github.com/ollama/ollama/blob/main/docs/api.md#generate-a-chat-completion">
 * Ollama API documentation</a>.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OllamaChatRequest {

    /**
     * The name of the Ollama model to use for chat completion.
     * <p>
     * Must correspond to a model that has been pulled to the Ollama instance (e.g.,
     * {@code llama3.2}, {@code mistral}, {@code deepseek-r1}). The server uses this
     * value to select the appropriate inference engine and tokenizer.
     */
    @JsonProperty("model")
    private final String model;

    /**
     * The sequence of messages that form the conversation context.
     * <p>
     * Each entry is an {@link OllamaChatMessage} representing either a user prompt,
     * assistant response, system instruction, or tool call result. The list is ordered
     * chronologically, and the model generates a completion based on this entire context.
     */
    @JsonProperty("messages")
    private final List<OllamaChatMessage> messages;

    /**
     * Controls whether the response is streamed incrementally.
     * <p>
     * When {@code true}, the server sends partial tokens as they are generated,
     * allowing real-time display. When {@code false} or absent, the full response
     * is returned in a single JSON object after generation completes.
     */
    @JsonProperty("stream")
    private final Boolean stream;

    /**
     * Specifies the output format constraint for the generated response.
     * <p>
     * Accepts either a simple string (e.g., {@code "json"}) that instructs the model
     * to produce valid JSON, or a JSON Schema object that strictly defines the expected
     * structure. The exact value is passed as-is to the Ollama API, allowing users to
     * enforce structured outputs.
     *
     * @see <a href="https://github.com/ollama/ollama/blob/main/docs/api.md#json-mode">
     * Ollama JSON mode</a>
     * @see <a href="https://json-schema.org/">JSON Schema</a>
     */
    @JsonProperty("format")
    private final Object format;

    /**
     * A list of function (tool) definitions available to the model.
     * <p>
     * When provided, the model is prompted to decide whether to call one of these tools
     * and generate the appropriate arguments. Each entry is a {@link FunctionTool} object
     * that contains the function name, description, and parameter schema.
     */
    @JsonProperty("tools")
    private final List<FunctionTool> tools;

    /**
     * Additional model parameters that control generation behavior.
     * <p>
     * Common options include {@code temperature}, {@code top_p}, {@code top_k},
     * {@code num_predict}, and {@code stop} sequences. All values are passed directly
     * to the Ollama backend; unsupported keys are typically ignored.
     */
    @JsonProperty("options")
    private final Map<String, Object> options;

    /**
     * Experimental "thinking" mode configuration for certain models (e.g., DeepSeek R1).
     * <p>
     * When enabled, the model is instructed to produce an internal chain-of-thought
     * before delivering the final answer. The value can be a simple {@code Boolean}
     * to toggle the feature, or a more complex structure (e.g., {@code {"thoughts": true}})
     * depending on the model's capabilities.
     */
    @JsonProperty("think")
    private final Object think;
}