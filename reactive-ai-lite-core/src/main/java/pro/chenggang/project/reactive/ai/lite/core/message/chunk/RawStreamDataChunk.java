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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;

/**
 * Represents a raw, untyped JSON stream data chunk that could not be matched to any standard AI response content type.
 * <p>
 * In streaming AI interactions (e.g., chat completions with tool calls, reasoning, etc.), the provider may emit
 * JSON structures that do not conform to any predefined {@link StreamDataType}. Rather than dropping or failing on
 * such data, the system encapsulates it as a {@code RawStreamDataChunk}, preserving the raw {@link ObjectNode}
 * for custom processing. This ensures forward-compatibility with new or provider-specific response fields without
 * disrupting the stream.
 * </p>
 * <p>
 * The chunk is immutable, constructed via a builder, and supports Jackson deserialization from JSON
 * (via {@link Jacksonized @Jacksonized}). It stores the entire raw JSON object node as {@link #value}.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see StreamDataChunk
 * @see StreamDataType
 */
@Getter
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RawStreamDataChunk implements StreamDataChunk {

    /**
     * The raw JSON value emitted by the stream, stored as a Jackson {@link ObjectNode}.
     * <p>
     * Using the tree model allows arbitrary navigation and dynamic extraction of fields that are not
     * part of the standard data model. For example, a provider might include a debug section
     * or an experimental field that the framework does not recognize. Consumers can inspect
     * or transform the node without prior knowledge of its structure.
     * </p>
     */
    private final ObjectNode value;

    /**
     * Returns the data type identifier for this chunk.
     * <p>
     * Because the chunk contains raw, untyped JSON, it always reports {@link StreamDataType#UNKNOWN}.
     * This signals to stream consumers that the chunk does not correspond to any of the standard
     * content categories (e.g., text, tool execution, reasoning) and should be treated as opaque data,
     * typically handed off to custom adapters or logging facilities.
     * </p>
     *
     * @return {@link StreamDataType#UNKNOWN}
     */
    @Override
    public StreamDataType getDataType() {
        return StreamDataType.UNKNOWN;
    }

}