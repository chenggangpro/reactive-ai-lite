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
package pro.chenggang.project.reactive.ai.lite.client.openai.dto;

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
 * Represents a chat completion request specifically formatted for the OpenAI API.
 * Encapsulates all configuration parameters supported by OpenAI, including messages, 
 * tool choices, stream options, and specific model configurations.
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
public class OpenaiChatRequest {

    /**
     * A list of messages comprising the conversation so far.
     */
    @JsonProperty("messages")
    private final List<ChatCompletionMessage> messages;

    /**
     * ID of the model to use (e.g., "gpt-4o").
     */
    @JsonProperty("model")
    private final String model;

    /**
     * Specifies the format that the model must output.
     * Compatible with JSON mode or structured outputs.
     */
    @JsonProperty("response_format")
    private final ResponseFormat responseFormat;

    /**
     * If true, partial message deltas will be sent via Server-Sent Events (SSE).
     */
    @JsonProperty("stream")
    private final Boolean stream;

    /**
     * Options for streaming responses, such as requesting usage metrics.
     */
    @JsonProperty("stream_options")
    private final StreamOptions streamOptions;

    /**
     * The sampling temperature to use, between 0 and 2.
     * Higher values make the output more random.
     */
    @JsonProperty("temperature")
    private final Double temperature;

    /**
     * An alternative to sampling with temperature, called nucleus sampling,
     * where the model considers the results of the tokens with top_p probability mass.
     */
    @JsonProperty("top_p")
    private final Double topP;

    /**
     * A list of tools the model may call. Currently, only functions are supported.
     */
    @JsonProperty("tools")
    private final List<FunctionTool> tools;

    /**
     * Controls which (if any) tool is called by the model.
     * "none" means the model will not call a tool, "auto" means it can.
     */
    @JsonProperty("tool_choice")
    private final Object toolChoice;

    /**
     * Whether to enable parallel function calling during tool use.
     */
    @JsonProperty("parallel_tool_calls")
    private final Boolean parallelToolCalls;

    /**
     * The maximum number of tokens that can be generated in the chat completion.
     */
    @JsonProperty("max_completion_tokens")
    private final Integer maxCompletionTokens;

    /**
     * Specifies the reasoning effort level for reasoning models (e.g., o1 models).
     */
    @JsonProperty("reasoning_effort")
    private final String reasoningEffort;

    /**
     * Represents additional options available when streaming is enabled.
     *
     * @param includeUsage Whether to include token usage metrics at the end of the stream.
     */
    @JsonInclude(Include.NON_NULL)
    public record StreamOptions(
            @JsonProperty("include_usage") Boolean includeUsage) {

        /**
         * A predefined singleton for enabling usage metrics in streams.
         */
        public static StreamOptions INCLUDE_USAGE = new StreamOptions(true);
    }

}
