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
 * Represents a raw JSON stream data chunk that could not be categorized into a specific type.
 * <p>
 * This chunk typically contains unprocessed JSON nodes, allowing for flexible handling
 * of unknown, experimental, or provider-custom data structures without dropping the data
 * during the streaming process.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RawStreamDataChunk implements StreamDataChunk {

    /**
     * The raw, unprocessed JSON value emitted by the stream.
     */
    private final ObjectNode value;

    /**
     * Retrieves the data type of this stream chunk.
     * <p>
     * Always returns {@link StreamDataType#UNKNOWN} for raw chunks to indicate that
     * the system could not map it to a standard content type.
     * </p>
     *
     * @return the {@link StreamDataType#UNKNOWN} constant
     */
    @Override
    public StreamDataType getDataType() {
        return StreamDataType.UNKNOWN;
    }

}
