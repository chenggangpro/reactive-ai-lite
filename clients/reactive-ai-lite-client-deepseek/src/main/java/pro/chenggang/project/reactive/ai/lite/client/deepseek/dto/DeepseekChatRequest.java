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
 * @author Cheng Gang
 * @version 0.1.0
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DeepseekChatRequest {

    @JsonProperty("messages")
    private final List<ChatCompletionMessage> messages;
    @JsonProperty("model")
    private final String model;
    @JsonProperty("response_format")
    private final ResponseFormat responseFormat;
    @JsonProperty("stream")
    private final Boolean stream;
    @JsonProperty("stream_options")
    private final StreamOptions streamOptions;
    @JsonProperty("temperature")
    private final Double temperature;
    @JsonProperty("top_p")
    private final Double topP;
    @JsonProperty("tools")
    private final List<FunctionTool> tools;
    @JsonProperty("tool_choice")
    private final Object toolChoice;
    @JsonProperty("max_tokens")
    private final Integer maxTokens;
    @JsonProperty("thinking")
    private final Thinking thinking;

    @JsonInclude(Include.NON_NULL)
    public record StreamOptions(
            @JsonProperty("include_usage") Boolean includeUsage) {

        public static StreamOptions INCLUDE_USAGE = new StreamOptions(true);
    }

    @JsonInclude(Include.NON_NULL)
    public record Thinking(
            @JsonProperty("type") String thinkingType) {

        public static Thinking ENABLED = new Thinking("enabled");

        public static Thinking DISABLED = new Thinking("disabled");
    }
}
