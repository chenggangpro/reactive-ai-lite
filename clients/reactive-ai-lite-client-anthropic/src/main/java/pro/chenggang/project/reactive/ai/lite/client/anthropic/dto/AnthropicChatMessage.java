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
 * Represents a single message in an Anthropic conversation.
 * <p>
 * According to the Anthropic Messages API, a message consists of a {@link Role}
 * (either "user" or "assistant") and a polymorphic {@code content} field that
 * can be a simple string or an array of {@link ContentBlock} objects. This
 * class uses Jackson's {@code @JsonTypeInfo} on the content block hierarchy to
 * support the different block types (text, image, tool_use, tool_result,
 * thinking) that the API may send or expect.
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
public class AnthropicChatMessage {

    /**
     * The role of the message sender (e.g., "user" or "assistant").
     */
    @JsonProperty("role")
    private final Role role;

    /**
     * The message content.
     * <p>
     * When serializing to the Anthropic API, this field accepts either a plain
     * string (for simple text messages) or a list of {@link ContentBlock}
     * instances. Deserialization handles both formats automatically.
     * </p>
     */
    @JsonProperty("content")
    private final Object content;

    /**
     * A polymorphic interface for content blocks within an Anthropic message.
     * <p>
     * The Anthropic API structures rich messages as an array of typed blocks.
     * This interface is the base for all such blocks and uses Jackson
     * polymorphism ({@code type} property) to distinguish between text,
     * image, tool_use, tool_result, and thinking blocks.
     * </p>
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

        /**
         * Returns the content block type identifier (e.g., "text", "image",
         * "tool_use", "tool_result", "thinking"). This value is serialized as
         * the {@code type} JSON property used in the polymorphic deserialization.
         *
         * @return the type string of this content block
         */
        String getType();
    }

    /**
     * A simple text content block.
     * <p>
     * Contains a single text value. When sent to the API, this is one element
     * in the content array. When received, it often represents the assistant's
     * reply or a user's text prompt.
     * </p>
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TextBlock implements ContentBlock {

        /**
         * The fixed block type identifier for text blocks.
         */
        @JsonProperty("type")
        @Builder.Default
        private final String type = "text";

        /**
         * The textual content of this block.
         */
        @JsonProperty("text")
        private final String text;
    }

    /**
     * An image content block.
     * <p>
     * Represents an image attachment in a message. The actual image data is
     * provided through the {@link ImageSource} sub‑objects, which support
     * base64‑encoded images or URL references.
     * </p>
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ImageBlock implements ContentBlock {

        /**
         * The fixed block type identifier for image blocks.
         */
        @JsonProperty("type")
        @Builder.Default
        private final String type = "image";

        /**
         * The source of the image data (base64 or url).
         */
        @JsonProperty("source")
        private final ImageSource source;

        /**
         * Interface for image sources within an {@code ImageBlock}.
         * <p>
         * Subtypes determine how the image bytes are retrieved. This interface
         * exists to allow Jackson to properly handle polymorphic source types
         * (base64 and url) based on the {@code type} property.
         * </p>
         */
        interface ImageSource {

            /**
             * Returns the source type identifier (e.g., "base64" or "url").
             *
             * @return the type string of this image source
             */
            String getType();
        }

        /**
         * A base64‑encoded image source.
         * <p>
         * Provides inline image data by specifying the media type and the
         * base64‑encoded string. This is convenient when the image is
         * available in memory or when a URL is not persistent.
         * </p>
         */
        @Getter
        @Builder
        @Jacksonized
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Base64ImageSource implements ImageSource {

            /**
             * The fixed source type identifier (always "base64").
             */
            @JsonProperty("type")
            @Builder.Default
            private final String type = "base64";

            /**
             * The media (MIME) type of the image (e.g., "image/png",
             * "image/jpeg").
             */
            @JsonProperty("media_type")
            private final String mediaType;

            /**
             * The base64‑encoded image data (without the data URI prefix).
             */
            @JsonProperty("data")
            private final String data;
        }

        /**
         * A URL‑based image source.
         * <p>
         * References an image by its URL. The Anthropic API will fetch the
         * image from the given URL when processing the message.
         * </p>
         */
        @Getter
        @Builder
        @Jacksonized
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        public static class UrlImageSource implements ImageSource {

            /**
             * The fixed source type identifier (always "url").
             */
            @JsonProperty("type")
            @Builder.Default
            private final String type = "url";

            /**
             * The image URL.
             */
            @JsonProperty("url")
            private final String url;
        }
    }

    /**
     * A tool use request block.
     * <p>
     * When the assistant decides to call a tool, it emits this content block
     * containing the tool invocation details. It includes a unique identifier,
     * the tool name, and the arguments for the call.
     * </p>
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ToolUseBlock implements ContentBlock {

        /**
         * The fixed block type identifier ("tool_use").
         */
        @JsonProperty("type")
        @Builder.Default
        private final String type = "tool_use";

        /**
         * A unique identifier for this tool call, generated by the model.
         * Used to match the call with its later {@link ToolResultBlock}.
         */
        @JsonProperty("id")
        private final String id;

        /**
         * The name of the tool being invoked.
         */
        @JsonProperty("name")
        private final String name;

        /**
         * A JSON object containing the arguments to pass to the tool.
         */
        @JsonProperty("input")
        private final Map<String, Object> input;
    }

    /**
     * A tool result block, sent back to the model after executing a tool.
     * <p>
     * This block conveys the result of a previously requested tool use. It
     * references the same {@code tool_use_id} from the corresponding
     * {@link ToolUseBlock} and may optionally indicate an error.
     * </p>
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ToolResultBlock implements ContentBlock {

        /**
         * The fixed block type identifier ("tool_result").
         */
        @JsonProperty("type")
        @Builder.Default
        private final String type = "tool_result";

        /**
         * The ID of the tool use request this result corresponds to.
         */
        @JsonProperty("tool_use_id")
        private final String toolUseId;

        /**
         * The result content. This can be a string or structured data,
         * depending on the tool output format expected by the API.
         */
        @JsonProperty("content")
        private final Object content;

        /**
         * If {@code true}, indicates that the tool execution resulted in an error.
         * This helps the model differentiate between successful and failed calls.
         */
        @JsonProperty("is_error")
        private final Boolean isError;
    }

    /**
     * A thinking block that exposes the model's internal reasoning (if enabled).
     * <p>
     * When extended thinking is turned on, the API may return a thinking block
     * containing the raw reasoning text and a cryptographic signature used
     * to verify that the model indeed produced that reasoning.
     * </p>
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ThinkingBlock implements ContentBlock {

        /**
         * The fixed block type identifier ("thinking").
         */
        @JsonProperty("type")
        @Builder.Default
        private final String type = "thinking";

        /**
         * The model's reasoning text.
         */
        @JsonProperty("thinking")
        private final String thinking;

        /**
         * The cryptographic signature that verifies the thinking content
         * was generated by the model.
         */
        @JsonProperty("signature")
        private final String signature;
    }
}