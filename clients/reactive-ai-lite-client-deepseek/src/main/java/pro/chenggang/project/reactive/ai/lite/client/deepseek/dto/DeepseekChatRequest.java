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
package pro.chenggang.project.reactive.ai.lite.client.deepseek.dto;

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

/**
 * Request payload for the Deepseek chat completion API.
 * <p>
 * This immutable DTO maps directly to the JSON structure expected by the Deepseek endpoint.
 * It encapsulates all configurable parameters for a chat completion call, including the
 * conversation messages, model selection, response format constraints, streaming options,
 * temperature and top_p sampling controls, function/tool calling schema, and advanced
 * thinking/reasoning settings. Lombok annotations drive the generation of getters, a builder,
 * a Jackson-friendly deserializer, and a private all-args constructor, ensuring that instances
 * can only be created via the builder and remain fully immutable.
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
public class DeepseekChatRequest {

    /**
     * The list of messages that form the conversation context.
     * <p>
     * Typically includes alternating {@code system}, {@code user}, and {@code assistant} messages.
     * The array order represents the chronological flow of the conversation. At least one
     * message is required; the system message (if present) sets the behavior of the assistant.
     */
    @JsonProperty("messages")
    private final List<ChatCompletionMessage> messages;

    /**
     * The identifier of the Deepseek model to invoke (e.g., {@code "deepseek-chat"}).
     * <p>
     * Different model IDs correspond to different capabilities, token limits, and pricing.
     * This field is mandatory for every request.
     */
    @JsonProperty("model")
    private final String model;

    /**
     * Optional specification of the desired response format.
     * <p>
     * When set, it instructs the model to produce output that adheres to a predefined structure,
     * such as JSON mode. This is particularly useful when integrating with downstream systems
     * that expect structured data.
     */
    @JsonProperty("response_format")
    private final ResponseFormat responseFormat;

    /**
     * Flag indicating whether the response should be streamed.
     * <p>
     * If {@code true}, the server returns a stream of server-sent events (SSE) as tokens are generated,
     * enabling incremental processing. If {@code false} or absent, the entire response is delivered
     * at once after generation completes.
     */
    @JsonProperty("stream")
    private final Boolean stream;

    /**
     * Additional options governing streaming behavior.
     * <p>
     * Allows fine-tuning of what information is included in each stream chunk. For example,
     * {@link StreamOptions#INCLUDE_USAGE} requests that token usage statistics be appended
     * at the end of the stream.
     */
    @JsonProperty("stream_options")
    private final StreamOptions streamOptions;

    /**
     * Sampling temperature, a value between 0 and 2.
     * <p>
     * Higher values (e.g., 0.8) make the output more random, while lower values (e.g., 0.2)
     * make it more focused and deterministic. It controls the sharpness of the probability
     * distribution over candidate tokens.
     */
    @JsonProperty("temperature")
    private final Double temperature;

    /**
     * Nucleus sampling parameter (top_p), a value between 0 and 1.
     * <p>
     * Instead of considering all possible tokens, the model only considers the smallest set
     * of tokens whose cumulative probability mass equals or exceeds {@code top_p}. This provides
     * a balance between randomness and coherence, often used as an alternative to temperature.
     */
    @JsonProperty("top_p")
    private final Double topP;

    /**
     * A list of function tools that the model may invoke during generation.
     * <p>
     * Each tool is a JSON Schema definition describing a callable function. When tools are provided,
     * the model can output a function call request instead of plain text, enabling the integration
     * of external services.
     */
    @JsonProperty("tools")
    private final List<FunctionTool> tools;

    /**
     * Controls which tool (if any) the model selects.
     * <p>
     * Acceptable values are {@code "none"} (no tool call), {@code "auto"} (model decides),
     * or a specific tool object that forces the model to call that particular function.
     * This field is only relevant when {@link #tools} is non-empty.
     */
    @JsonProperty("tool_choice")
    private final Object toolChoice;

    /**
     * The maximum number of tokens to generate in the completion.
     * <p>
     * This limits the length of the assistant's reply. The total tokens consumed (prompt + completion)
     * cannot exceed the model's context window. Setting an appropriate value helps control cost
     * and prevent excessively long responses.
     */
    @JsonProperty("max_tokens")
    private final Integer maxTokens;

    /**
     * Configuration for advanced thinking mode (Deepseek-specific feature).
     * <p>
     * Enables extended reasoning capabilities that allow the model to “think” before responding.
     * Use predefined constants {@link Thinking#ENABLED} or {@link Thinking#DISABLED} to toggle
     * this behavior.
     */
    @JsonProperty("thinking")
    private final Thinking thinking;

    /**
     * Optional effort level for the reasoning process (Deepseek-specific).
     * <p>
     * Values such as {@code "low"}, {@code "medium"}, or {@code "high"} control the depth
     * of the model's internal chain-of-thought. This field is typically used in conjunction
     * with {@link #thinking} to fine-tune reasoning quality versus latency.
     */
    @JsonProperty("reasoning_effort")
    private final String reasoningEffort;

    /**
     * Immutable record capturing options that fine-tune the server-sent event stream.
     *
     * @param includeUsage whether to include token usage statistics in the final stream chunk
     */
    @JsonInclude(Include.NON_NULL)
    public record StreamOptions(
            @JsonProperty("include_usage") Boolean includeUsage) {

        /**
         * Pre-built instance that requests the inclusion of token usage information.
         * <p>
         * Using this constant ensures that the API appends usage counts (prompt, completion, total tokens)
         * at the end of the stream, which is valuable for monitoring and cost tracking.
         */
        public static StreamOptions INCLUDE_USAGE = new StreamOptions(true);
    }

    /**
     * Immutable record representing the thinking mode toggle.
     *
     * @param thinkingType the type of thinking mode, typically {@code "enabled"} or {@code "disabled"}
     */
    @JsonInclude(Include.NON_NULL)
    public record Thinking(
            @JsonProperty("type") String thinkingType) {

        /**
         * Convenience instance that enables thinking mode.
         */
        public static Thinking ENABLED = new Thinking("enabled");

        /**
         * Convenience instance that disables thinking mode.
         */
        public static Thinking DISABLED = new Thinking("disabled");
    }
}