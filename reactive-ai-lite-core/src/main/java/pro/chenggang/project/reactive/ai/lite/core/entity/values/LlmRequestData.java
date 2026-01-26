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
package pro.chenggang.project.reactive.ai.lite.core.entity.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.TextMessage;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.tool.LlmToolCallResponse;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LlmRequestData {

    @Getter
    @NonNull
    private final LlmProviderInfo llmProviderInfo;
    @Getter
    @NonNull
    private final ExecutionContextView executionContextView;
    private final TokenCertification tokenCertification;
    @Getter
    private final String modelName;
    @Getter
    private final List<ToolDefinition> toolDefinitions;
    @Getter
    private final List<LlmToolCallResponse> llmToolCallResponse;
    @Getter
    private final boolean isStream;
    @Getter
    private final boolean distinctToolCalls;
    private final String toolChoice;
    private final Type structuredOutputType;
    private final String responseJsonSchema;
    private final Double temperature;
    private final Double topP;
    @Getter
    private final boolean includeUsage;
    private final String reasoning;
    private final Integer maxCompletionTokens;
    private final TextMessage userTextMessage;
    @Getter
    private final List<Message> historicalMessages;
    private final ObjectNode latestAssistantMessage;
    private final MediaMessage userMediaMessage;
    private final TextMessage systemMessage;
    @Getter
    private final BiConsumer<ExecutionContextView, ObjectNode> rawRequestCustomizer;
    @Getter
    private final BiConsumer<ExecutionContextView, RawResponse> rawResponseCustomizer;
    @Getter
    private final BiConsumer<ExecutionContextView, RawStreamResponse> rawStreamResponseCustomizer;

    public Optional<TokenCertification> getTokenCertification() {
        return Optional.ofNullable(tokenCertification);
    }

    public Optional<Double> getTemperature() {
        return Optional.ofNullable(this.temperature);
    }

    public Optional<Double> getTopP() {
        return Optional.ofNullable(this.topP);
    }

    public Optional<String> getReasoning() {
        return Optional.ofNullable(this.reasoning);
    }

    public Optional<Integer> getMaxCompletionTokens() {
        return Optional.ofNullable(this.maxCompletionTokens);
    }

    public TextMessage getSystemMessage() {
        return Optional.ofNullable(systemMessage).orElse(Message.EMPTY_MESSAGE);
    }

    public TextMessage getUserTextMessage() {
        return Optional.ofNullable(userTextMessage).orElse(Message.EMPTY_MESSAGE);
    }

    public Optional<MediaMessage> getUserMediaMessage() {
        return Optional.ofNullable(userMediaMessage);
    }

    public Optional<Type> getStructuredOutputType() {
        return Optional.ofNullable(structuredOutputType);
    }

    public Optional<String> getResponseJsonSchema() {
        return Optional.ofNullable(responseJsonSchema);
    }

    public Optional<String> getToolChoice() {
        return Optional.ofNullable(toolChoice);
    }

    public Optional<ObjectNode> getLatestAssistantMessage() {
        return Optional.ofNullable(latestAssistantMessage);
    }
}
