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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;

import java.util.Map;

/**
 * Anthropic Message DTO.
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
public class AnthropicChatMessage {

    @JsonProperty("role")
    private final Role role;

    @JsonProperty("content")
    private final Object content;

    /**
     * Base interface for Anthropic content blocks.
     */
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TextBlock.class, name = "text"),
            @JsonSubTypes.Type(value = ImageBlock.class, name = "image"),
            @JsonSubTypes.Type(value = ToolUseBlock.class, name = "tool_use"),
            @JsonSubTypes.Type(value = ToolResultBlock.class, name = "tool_result"),
            @JsonSubTypes.Type(value = ThinkingBlock.class, name = "thinking")
    })
    public interface ContentBlock {
        String getType();
    }

    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TextBlock implements ContentBlock {
        @JsonProperty("type")
        @Builder.Default
        private final String type = "text";

        @JsonProperty("text")
        private final String text;
    }

    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ImageBlock implements ContentBlock {
        @JsonProperty("type")
        @Builder.Default
        private final String type = "image";

        @JsonProperty("source")
        private final ImageSource source;

        interface ImageSource {

            String getType();
        }

        @Getter
        @Builder
        @Jacksonized
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Base64ImageSource implements ImageSource {

            @JsonProperty("type")
            @Builder.Default
            private final String type = "base64";

            @JsonProperty("media_type")
            private final String mediaType;

            @JsonProperty("data")
            private final String data;
        }

        @Getter
        @Builder
        @Jacksonized
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        public static class UrlImageSource implements ImageSource {

            @JsonProperty("type")
            @Builder.Default
            private final String type = "url";

            @JsonProperty("url")
            private final String url;
        }
    }

    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ToolUseBlock implements ContentBlock {
        @JsonProperty("type")
        @Builder.Default
        private final String type = "tool_use";

        @JsonProperty("id")
        private final String id;

        @JsonProperty("name")
        private final String name;

        @JsonProperty("input")
        private final Map<String, Object> input;
    }

    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ToolResultBlock implements ContentBlock {
        @JsonProperty("type")
        @Builder.Default
        private final String type = "tool_result";

        @JsonProperty("tool_use_id")
        private final String toolUseId;

        @JsonProperty("content")
        private final Object content;

        @JsonProperty("is_error")
        private final Boolean isError;
    }

    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ThinkingBlock implements ContentBlock {
        @JsonProperty("type")
        @Builder.Default
        private final String type = "thinking";

        @JsonProperty("thinking")
        private final String thinking;

        @JsonProperty("signature")
        private final String signature;
    }
}
