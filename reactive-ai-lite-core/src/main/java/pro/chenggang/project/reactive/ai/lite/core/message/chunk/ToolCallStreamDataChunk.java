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
 * A stream data chunk that carries a list of tool call requests emitted by the AI model.
 * <p>
 * During a streaming interaction the model may decide to invoke external tools or functions.
 * Because tool call descriptions (name, parameters) can be transmitted progressively,
 * this chunk is produced only after the model has fully serialized all argument details for
 * the current set of tool invocations. It guarantees that every {@link AssistantToolCall}
 * contained in the chunk is complete and immediately usable.
 * </p>
 * <p>
 * The consumer of this chunk can react to it by:
 * <ul>
 *   <li>executing the requested tools locally or remotely,</li>
 *   <li>returning the results back to the model conversation,</li>
 *   <li>or processing the tool calls asynchronously.</li>
 * </ul>
 * The stream data type is fixed to {@link StreamDataType#TOOL_CALL}, enabling unambiguous
 * routing in generic stream handlers.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see StreamDataChunk
 * @see AssistantToolCall
 */
@Getter
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolCallStreamDataChunk implements StreamDataChunk {

    /**
     * The fully aggregated tool call objects extracted from the current streaming window.
     * <p>
     * Each item represents one distinct tool invocation that the model decided to initiate.
     * The list may contain multiple entries when the model requests several tools
     * simultaneously. Because the chunk is emitted only after the model has finished
     * writing the arguments, every {@link AssistantToolCall} in this list is guaranteed
     * to have a complete set of parameters and is ready for execution.
     * </p>
     * <p>
     * The field is immutable and populated through the {@link Builder} interface.
     * </p>
     */
    private final List<AssistantToolCall> toolCalls;

    /**
     * Always returns {@link StreamDataType#TOOL_CALL} to identify this chunk as a
     * tool invocation request.
     * <p>
     * This constant return value makes it possible for stream consumers to handle
     * tool call chunks in a type-safe manner without relying on {@code instanceof}
     * checks or class mapping. It acts as a discriminator in the reactive pipeline
     * and allows centralized dispatching logic.
     * </p>
     *
     * @return the predefined data type constant for tool call chunks
     */
    @Override
    public StreamDataType getDataType() {
        return StreamDataType.TOOL_CALL;
    }

}