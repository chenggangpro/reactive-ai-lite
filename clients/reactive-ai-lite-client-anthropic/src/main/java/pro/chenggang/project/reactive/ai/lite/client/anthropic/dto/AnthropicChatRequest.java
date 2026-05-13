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
 * Anthropic Messages API Request.
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
public class AnthropicChatRequest {

    @JsonProperty("model")
    private final String model;

    @JsonProperty("messages")
    private final List<AnthropicChatMessage> messages;

    @JsonProperty("max_tokens")
    private final Integer maxTokens;

    @JsonProperty("output_config")
    private final OutputConfig outputConfig;

    @JsonProperty("system")
    private final Object system;

    @JsonProperty("stop_sequences")
    private final List<String> stopSequences;

    @JsonProperty("stream")
    private final Boolean stream;

    @JsonProperty("temperature")
    private final Double temperature;

    @JsonProperty("top_p")
    private final Double topP;

    @JsonProperty("top_k")
    private final Integer topK;

    @JsonProperty("metadata")
    private final Metadata metadata;

    @JsonProperty("tools")
    private final List<Tool> tools;

    @JsonProperty("tool_choice")
    private final ToolChoice toolChoice;

    @JsonProperty("thinking")
    private final ThinkingConfig thinking;

    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Metadata {
        @JsonProperty("user_id")
        private final String userId;
    }

    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Tool {
        @JsonProperty("name")
        private final String name;

        @JsonProperty("description")
        private final String description;

        @JsonProperty("input_schema")
        private final Map<String, Object> inputSchema;

        @JsonProperty("strict")
        private final Boolean strict;
    }

    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ToolChoice {
        @JsonProperty("type")
        private final String type;

        @JsonProperty("name")
        private final String name;

        public static ToolChoice AUTO = ToolChoice.builder().type("auto").build();
        public static ToolChoice ANY = ToolChoice.builder().type("any").build();
        public static ToolChoice NONE = ToolChoice.builder().type("none").build();

        public static ToolChoice tool(String name) {
            return ToolChoice.builder().type("tool").name(name).build();
        }
    }

    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ThinkingConfig {
        @JsonProperty("type")
        @Builder.Default
        private final String type = "enabled";

        @JsonProperty("budget_tokens")
        private final Integer budgetTokens;
    }

}
