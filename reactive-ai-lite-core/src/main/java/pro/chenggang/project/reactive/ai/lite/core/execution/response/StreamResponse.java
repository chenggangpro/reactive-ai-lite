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
import pro.chenggang.project.reactive.ai.lite.core.message.chunk.StreamDataChunk;

/**
 * Represents a standardized, parsed chunk of data emitted during a streaming LLM response.
 * <p>
 * This class extends {@link LlmResponse} to wrap a specific {@link StreamDataChunk},
 * which encapsulates the actual type (e.g., text fragment, tool call request, usage stats)
 * and content generated in this particular slide of the stream. It abstracts away
 * the underlying Server-Sent Events (SSE) JSON structure from the framework's consumers.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
@SuperBuilder
public class StreamResponse extends LlmResponse {

    /**
     * The parsed chunk of streaming data.
     */
    private final StreamDataChunk dataChunk;

}
