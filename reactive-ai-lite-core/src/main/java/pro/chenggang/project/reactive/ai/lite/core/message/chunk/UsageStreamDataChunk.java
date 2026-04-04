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
import pro.chenggang.project.reactive.ai.lite.core.entity.usage.Usage;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;

/**
 * Represents a stream data chunk containing usage information.
 * <p>
 * This chunk is typically emitted near or at the very end of a streaming response
 * to report the total token usage metrics consumed by the request and response generation.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class UsageStreamDataChunk implements StreamDataChunk {

    /**
     * The token usage metrics parsed from the response stream.
     */
    private final Usage usage;

    /**
     * Retrieves the data type of this stream chunk.
     * <p>
     * Always returns {@link StreamDataType#USAGE} to explicitly mark this chunk
     * as carrying token consumption metrics.
     * </p>
     *
     * @return the {@link StreamDataType#USAGE} constant
     */
    @Override
    public StreamDataType getDataType() {
        return StreamDataType.USAGE;
    }

}
