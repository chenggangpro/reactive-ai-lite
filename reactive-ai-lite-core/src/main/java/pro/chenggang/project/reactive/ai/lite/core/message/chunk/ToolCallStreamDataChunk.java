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
package pro.chenggang.project.reactive.ai.lite.core.message.chunk;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolCallMessage.AssistantToolCall;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;

import java.util.List;

/**
 * Represents a stream data chunk containing one or more tool calls.
 * <p>
 * This chunk is emitted when the AI model decides to invoke tools or functions.
 * Because tool calls can be streamed progressively, this chunk typically contains
 * the fully aggregated and parsed details of the tool invocations once the model
 * has finished outputting the tool call arguments for the current stream slide.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolCallStreamDataChunk implements StreamDataChunk {

    /**
     * A list of fully formed tool call objects extracted from the stream.
     */
    private final List<AssistantToolCall> toolCalls;

    /**
     * Retrieves the data type of this stream chunk.
     * <p>
     * Always returns {@link StreamDataType#TOOL_CALL} to explicitly mark this chunk
     * as carrying tool invocation requests.
     * </p>
     *
     * @return the {@link StreamDataType#TOOL_CALL} constant
     */
    @Override
    public StreamDataType getDataType() {
        return StreamDataType.TOOL_CALL;
    }

}
