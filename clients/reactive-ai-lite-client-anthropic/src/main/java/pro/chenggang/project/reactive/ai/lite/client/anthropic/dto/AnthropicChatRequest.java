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
package pro.chenggang.project.reactive.ai.lite.client.anthropic.dto;

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
 * Encapsulates all parameters for a call to the Anthropic Messages API.
 * <p>
 * This request object supports both standard and extended capabilities such as
 * streaming responses, tool use, structured output configuration, and
 * extended thinking. The builder pattern is used for convenient construction
 * with optional parameters, using Lombok's {@link Builder &#64;Builder} and
 * Jackson's {@link Jacksonized &#64;Jacksonized} for seamless JSON serialization.
 * <p>
 * Note that {@code maxTokens} is mandatory for every request, while other fields
 * are optional and will be omitted from the JSON if not set.
 * 
 * @author Gang Cheng
 * @version 0.1.0
 * @see <a href="https://docs.anthropic.com/en/api/messages">Anthropic Messages API</a>
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AnthropicChatRequest {

    /**
     * The model that will complete your prompt. See
     * <a href="https://docs.anthropic.com/en/docs/about-claude/models">Anthropic models</a>
     * for additional details and options. This value is required.
     */
    @JsonProperty("model")
    private final String model;

    /**
     * The conversation history, consisting of an array of message objects.
     * Each message must have a {@code role} and {@code content}. The API expects
     * at least one message with {@code role} "user".
     */
    @JsonProperty("messages")
    private final List<AnthropicChatMessage> messages;

    /**
     * The maximum number of tokens to generate before stopping.
     * Required parameter; note that different models have different maximum
     * token limits (e.g., 8192 for Claude 3.5 Sonnet).
     */
    @JsonProperty("max_tokens")
    private final Integer maxTokens;

    /**
     * Configuration for structured output via the {@code output_schema} extension.
     * When set, the model will generate JSON that conforms to the specified
     * JSON Schema. The {@code type} field must be {@code "json_schema"} and
     * a {@code schema} must be provided. Only available with certain model versions.
     */
    @JsonProperty("output_config")
    private final OutputConfig outputConfig;

    /**
     * System prompt, which can be either a plain string or an array of
     * content blocks (text objects). A string is the simplest form; an array
     * allows multiple text blocks or other media types.
     */
    @JsonProperty("system")
    private final Object system;

    /**
     * Custom text sequences that will cause the model to stop generating.
     * The response will contain the stop reason {@code "stop_sequence"} if one
     * of these strings is matched.
     */
    @JsonProperty("stop_sequences")
    private final List<String> stopSequences;

    /**
     * Whether to stream the response via server‑sent events (SSE).
     * When {@code true}, the response will be delivered incrementally
     * as {@code content_block_delta} and {@code message_delta} events.
     */
    @JsonProperty("stream")
    private final Boolean stream;

    /**
     * Controls randomness: lowering results in less random output.
     * Ranges from 0.0 (deterministic) to 1.0 (maximum randomness).
     * It is recommended to set either this or {@code topP}, not both.
     */
    @JsonProperty("temperature")
    private final Double temperature;

    /**
     * Nucleus sampling parameter. The model considers the smallest set of
     * tokens whose cumulative probability exceeds {@code topP}.
     * Value between 0.0 and 1.0. Often used as an alternative to temperature.
     */
    @JsonProperty("top_p")
    private final Double topP;

    /**
     * Only sample from the top K most probable tokens. Reduces the
     * distribution for each step, limiting the model's choices.
     */
    @JsonProperty("top_k")
    private final Integer topK;

    /**
     * An opaque field to help Anthropic track and manage usage.
     * The {@code userId} can be used for abuse detection or internal
     * monitoring. This field is optional.
     */
    @JsonProperty("metadata")
    private final Metadata metadata;

    /**
     * Definitions of custom tools (functions) that the model may call.
     * Each tool includes a name, description, and a JSON Schema for
     * the input parameters. Enable tool use by also setting {@link #toolChoice}.
     */
    @JsonProperty("tools")
    private final List<Tool> tools;

    /**
     * Controls how the model should use tools defined in {@link #tools}.
     * <ul>
     *   <li>{@code "auto"} – model decides whether to call a tool</li>
     *   <li>{@code "any"} – model must call at least one tool</li>
     *   <li>{@code "tool"} – model must call a specific named tool</li>
     *   <li>{@code "none"} – model must not call any tool</li>
     * </ul>
     */
    @JsonProperty("tool_choice")
    private final ToolChoice toolChoice;

    /**
     * Configuration for extended thinking (beta feature).
     * When enabled, the model is given a budget of tokens for internal reasoning
     * before producing a final response. This can improve quality on complex tasks.
     */
    @JsonProperty("thinking")
    private final ThinkingConfig thinking;

    /**
     * Optional metadata for the request, currently only supporting a user identifier.
     * This helps Anthropic with abuse prevention and usage analysis.
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Metadata {
        /**
         * An external identifier for the user associated with the request.
         * Can be any string; Anthropic may use it for abuse detection.
         */
        @JsonProperty("user_id")
        private final String userId;
    }

    /**
     * Defines a tool (function) that can be called by the model during a
     * conversation. It includes a name, a human‑readable description, the
     * expected input schema (as a JSON Schema object), and a flag to enable
     * strict schema validation.
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Tool {
        /**
         * The name of the function to be called. Must be unique within a request.
         */
        @JsonProperty("name")
        private final String name;

        /**
         * A human‑readable description of what the tool does. The model uses this
         * to decide when and how to call the function.
         */
        @JsonProperty("description")
        private final String description;

        /**
         * A JSON Schema object describing the expected parameters for the function.
         * The schema should follow standard JSON Schema validation rules.
         */
        @JsonProperty("input_schema")
        private final Map<String, Object> inputSchema;

        /**
         * If {@code true}, the model's output for the tool will strictly adhere
         * to the provided {@link #inputSchema}. When {@code false} (default),
         * the model may deviate slightly from the schema.
         */
        @JsonProperty("strict")
        private final Boolean strict;
    }

    /**
     * Specifies how the model should use tools provided in the {@link AnthropicChatRequest#tools} list.
     * The choice is either a general mode ({@code auto}, {@code any}, {@code none}) or a
     * specific tool name with type {@code "tool"}.
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ToolChoice {
        /**
         * The tool‑choice mode. One of {@code "auto"}, {@code "any"}, {@code "tool"}, {@code "none"}.
         */
        @JsonProperty("type")
        private final String type;

        /**
         * The name of the specific tool to call. Required only when {@code type} is {@code "tool"}.
         */
        @JsonProperty("name")
        private final String name;

        /**
         * Pre‑built constant for {@code {type: "auto"}}. The model decides whether
         * to call a tool.
         */
        public static ToolChoice AUTO = ToolChoice.builder().type("auto").build();

        /**
         * Pre‑built constant for {@code {type: "any"}}. The model must call at
         * least one of the provided tools.
         */
        public static ToolChoice ANY = ToolChoice.builder().type("any").build();

        /**
         * Pre‑built constant for {@code {type: "none"}}. The model must not call
         * any tool, producing a standard text response.
         */
        public static ToolChoice NONE = ToolChoice.builder().type("none").build();

        /**
         * Creates a tool‑choice that instructs the model to use a specific tool
         * with the given name. The resulting object has {@code type="tool"} and
         * the supplied name.
         *
         * @param name the tool name as defined in {@link AnthropicChatRequest#tools}
         * @return a ToolChoice instance with {@code type="tool"} and the specified name
         */
        public static ToolChoice tool(String name) {
            return ToolChoice.builder().type("tool").name(name).build();
        }
    }

    /**
     * Configuration for extended thinking. When enabled, the model is allocated
     * a budget of tokens for internal reasoning before generating the final answer.
     * The thinking block itself is excluded from the output but can be retrieved
     * in the response for inspection if desired.
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ThinkingConfig {
        /**
         * Type of thinking configuration. Currently only {@code "enabled"} is supported.
         * Default value is {@code "enabled"}.
         */
        @JsonProperty("type")
        @Builder.Default
        private final String type = "enabled";

        /**
         * The maximum number of tokens the model is allowed to “think” before
         * producing a final response. Must be less than {@link AnthropicChatRequest#maxTokens}
         * and at least 1024.
         */
        @JsonProperty("budget_tokens")
        private final Integer budgetTokens;
    }

}