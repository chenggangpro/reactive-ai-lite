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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;

import java.util.List;

/**
 * @author Gang Cheng
 * @version 0.1.0
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatCompletionMessage {

    @JsonProperty("role")
    private final Role role;
    @JsonProperty("name")
    private final String name;
    @JsonProperty("content")
    private final Object rawContent;
    @JsonProperty("tool_call_id")
    private final String toolCallId;
    @JsonProperty("tool_calls")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private final List<ToolCall> toolCalls;
    @JsonProperty("reasoning_content")
    private final String reasoningContent;


    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ToolCall {

        @JsonProperty("index")
        private final Integer index;
        @JsonProperty("id")
        private final String id;
        @JsonProperty("type")
        private final String type;
        @JsonProperty("function")
        private final ChatCompletionFunction function;

    }

    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ChatCompletionFunction {

        @JsonProperty("name")
        private final String name;
        @JsonProperty("arguments")
        private final String arguments;
    }

}