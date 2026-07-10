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
package pro.chenggang.project.reactive.ai.lite.core.message;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.util.MimeTypeUtils;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Base64Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.UrlAttachment;
import pro.chenggang.project.reactive.ai.lite.core.message.chunk.RawStreamDataChunk;
import pro.chenggang.project.reactive.ai.lite.core.message.chunk.TextStreamDataChunk;
import pro.chenggang.project.reactive.ai.lite.core.message.chunk.ToolCallStreamDataChunk;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultAssistantMediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultAssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultMediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultToolCallMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultToolResultMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MessagePackageTest {

    @Test
    void testTextMessage() {
        TextMessage message = DefaultTextMessage.builder().role(Role.SYSTEM.getValue()).content("System rules").build();
        assertThat(message.getRole()).isEqualTo(Role.SYSTEM.getValue());
        assertThat(message.getContent()).isEqualTo("System rules");
        assertThat(message.getActualType()).isEqualTo(DefaultTextMessage.class);

        TextMessage userMessage = DefaultTextMessage.builder().role(Role.USER.getValue()).content("Hello").build();
        assertThat(userMessage.getRole()).isEqualTo(Role.USER.getValue());
        assertThat(userMessage.getContent()).isEqualTo("Hello");
    }

    @Test
    void testMediaMessage() {
        Attachment attachment = UrlAttachment.builder().mimeType(MimeTypeUtils.IMAGE_PNG).url("https://example.com/image.png").name("testImage").build();
        MediaMessage mediaMessage = DefaultMediaMessage.builder().role(Role.USER.getValue()).content("").attachments(new Attachment[]{attachment}).build();
        assertThat(mediaMessage.getRole()).isEqualTo(Role.USER.getValue());
        assertThat(mediaMessage.getContent()).isEmpty();
        assertThat(mediaMessage.getAttachments()).containsExactly(attachment);
        assertThat(mediaMessage.getActualType()).isEqualTo(DefaultMediaMessage.class);

        MediaMessage userMessage = DefaultMediaMessage.builder().role(Role.USER.getValue()).content("Look at this").attachments(new Attachment[]{attachment}).build();
        assertThat(userMessage.getRole()).isEqualTo(Role.USER.getValue());
        assertThat(userMessage.getContent()).isEqualTo("Look at this");
        assertThat(userMessage.getAttachments()).containsExactly(attachment);
    }

    @Test
    void testAssistantTextMessage() {
        AssistantTextMessage assistantTextMessage = DefaultAssistantTextMessage.builder().content("Hello, User").build();
        assertThat(assistantTextMessage.getRole()).isEqualTo(Role.ASSISTANT.getValue());
        assertThat(assistantTextMessage.getContent()).isEqualTo("Hello, User");
        assertThat(assistantTextMessage.getActualType()).isEqualTo(DefaultAssistantTextMessage.class);
    }

    @Test
    void testAssistantMediaMessage() {
        Attachment attachment = UrlAttachment.builder().mimeType(MimeTypeUtils.IMAGE_PNG).url("https://example.com/response.png").name("respImage").build();
        AssistantMediaMessage assistantMediaMessage = DefaultAssistantMediaMessage.builder().content("Here is the image").attachments(new Attachment[]{attachment}).build();
        assertThat(assistantMediaMessage.getRole()).isEqualTo(Role.ASSISTANT.getValue());
        assertThat(assistantMediaMessage.getContent()).isEqualTo("Here is the image");
        assertThat(assistantMediaMessage.getAttachments()).containsExactly(attachment);
        assertThat(assistantMediaMessage.getActualType()).isEqualTo(DefaultAssistantMediaMessage.class);

        AssistantMediaMessage noText = DefaultAssistantMediaMessage.builder().content("").attachments(new Attachment[]{attachment}).build();
        assertThat(noText.getContent()).isEmpty();
        assertThat(noText.getAttachments()).containsExactly(attachment);
    }

    @Test
    void testToolCallMessage() {
        ToolCallMessage.AssistantToolCallFunction function = ToolCallMessage.AssistantToolCallFunction.builder()
                .name("weatherTool")
                .arguments("{\"location\":\"London\"}")
                .build();
        ToolCallMessage.AssistantToolCall toolCall = ToolCallMessage.AssistantToolCall.builder()
                .index(0)
                .id("call_1")
                .type("function")
                .function(function)
                .toolDefinition(mock(ToolDefinition.class))
                .build();
        ToolCallMessage message = DefaultToolCallMessage.builder().toolCalls(List.of(toolCall)).build();
        assertThat(message.getRole()).isEqualTo(Role.ASSISTANT.getValue());
        assertThat(message.getToolCalls()).containsExactly(toolCall);
        assertThat(message.getToolCalls().getFirst().getId()).isEqualTo("call_1");
        assertThat(message.getToolCalls().getFirst().getFunction().getName()).isEqualTo("weatherTool");
        assertThat(message.getToolCalls().getFirst().getFunction().getArguments()).isEqualTo("{\"location\":\"London\"}");
        assertThat(message.getActualType()).isEqualTo(DefaultToolCallMessage.class);

        ToolCallMessage messageWithText = DefaultToolCallMessage.builder().content("Calling tool").toolCalls(List.of(toolCall)).build();
        assertThat(messageWithText.getContent()).isEqualTo("Calling tool");
    }

    @Test
    void testToolResultMessage() {
        ToolResultMessage message = DefaultToolResultMessage.builder().toolCallId("call_1").content("Sunny").build();
        assertThat(message.getRole()).isEqualTo(Role.TOOL.getValue());
        assertThat(message.toolCallId()).isEqualTo("call_1");
        assertThat(message.content()).isEqualTo("Sunny");
        assertThat(message.getActualType()).isEqualTo(DefaultToolResultMessage.class);
    }

    @Test
    void testBase64Attachment() {
        Base64Attachment attachment = Base64Attachment.builder().mimeType(MimeTypeUtils.IMAGE_PNG).base64Content("iVBORw0K...").name("base64").build();
        assertThat(attachment.mimeType()).isEqualTo(MimeTypeUtils.IMAGE_PNG);
        assertThat(attachment.content()).contains("base64,iVBORw0K...");
        assertThat(attachment.name()).isEqualTo("base64");
    }

    @Test
    void testUrlAttachment() {
        UrlAttachment attachment = UrlAttachment.builder().mimeType(MimeTypeUtils.IMAGE_PNG).url("https://example.com/image.png").name("urlImage").build();
        assertThat(attachment.content()).isEqualTo("https://example.com/image.png");
        assertThat(attachment.name()).isEqualTo("urlImage");
    }

    @Test
    void testChunks() {
        TextStreamDataChunk textChunk = TextStreamDataChunk.builder().dataType(StreamDataType.ANSWER_CONTENT).value("chunk of text").build();
        assertThat(textChunk.getDataType()).isEqualTo(StreamDataType.ANSWER_CONTENT);
        assertThat(textChunk.getValue()).isEqualTo("chunk of text");

        ObjectNode node = mock(ObjectNode.class);
        RawStreamDataChunk rawChunk = RawStreamDataChunk.builder().value(node).build();
        assertThat(rawChunk.getDataType()).isEqualTo(StreamDataType.UNKNOWN);
        assertThat(rawChunk.getValue()).isEqualTo(node);

        ToolCallMessage.AssistantToolCallFunction function = ToolCallMessage.AssistantToolCallFunction.builder()
                .name("weatherTool")
                .arguments("{}")
                .build();
        ToolCallMessage.AssistantToolCall toolCall = ToolCallMessage.AssistantToolCall.builder()
                .index(0)
                .id("call_1")
                .type("function")
                .function(function)
                .toolDefinition(mock(ToolDefinition.class))
                .build();
        ToolCallStreamDataChunk toolChunk = ToolCallStreamDataChunk.builder().toolCalls(List.of(toolCall)).build();
        assertThat(toolChunk.getDataType()).isEqualTo(StreamDataType.TOOL_CALL);
        assertThat(toolChunk.getToolCalls()).containsExactly(toolCall);
    }
}
