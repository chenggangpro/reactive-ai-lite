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
package pro.chenggang.project.reactive.ai.lite.core.execution.response;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.message.AssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolCallMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolCallMessage.AssistantToolCall;

import java.util.List;
import java.util.Optional;

/**
 * Represents the standard, unified response object for non-streaming chat requests.
 * <p>
 * This class abstracts away the provider-specific JSON structure and provides
 * a clean interface to access the generated text message (and potentially any tool
 * calls) alongside extracted usage and metadata from {@link ExtractedLlmResponse}.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@SuperBuilder
public class GeneralResponse extends ExtractedLlmResponse {

    /**
     * The parsed assistant message.
     * <p>
     * This could be a plain {@link AssistantTextMessage} or an instance of
     * {@link ToolCallMessage} if the model chose to invoke tools.
     * </p>
     */
    protected final AssistantTextMessage assistantTextMessage;

    /**
     * Convenience method to safely extract tool calls if the assistant message
     * is a {@link ToolCallMessage}.
     *
     * @return an {@link Optional} containing a list of tool calls, or empty if none exist
     */
    public Optional<List<AssistantToolCall>> getToolCalls() {
        if (!(assistantTextMessage instanceof ToolCallMessage toolCallMessage)) {
            return Optional.empty();
        }
        return Optional.ofNullable(toolCallMessage.getToolCalls());
    }
}
