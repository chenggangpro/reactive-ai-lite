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

import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;

/**
 * Represents a parsed fragment or "chunk" of data emitted by an AI model during a streaming response.
 * <p>
 * In reactive streaming interactions, AI responses are delivered piece by piece.
 * This interface provides a common abstraction for the various types of data
 * that might be streamed, such as text segments, tool call requests, or usage metrics.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface StreamDataChunk {

    /**
     * Retrieves the specific type of data contained in this stream chunk.
     * <p>
     * The data type allows the consuming application to understand how to process
     * or cast the specific chunk implementation.
     * </p>
     *
     * @return the {@link StreamDataType} representing the category of this chunk
     */
    StreamDataType getDataType();
}
