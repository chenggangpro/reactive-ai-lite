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
 * A final {@link StreamDataChunk} that carries the aggregated token usage
 * metrics for a completed streaming response.
 * <p>
 * This chunk is typically produced once at the very end of a reactive stream
 * of chunks, after all text or content chunks have been emitted. It encapsulates
 * a {@link Usage} instance that reports how many prompt tokens and completion
 * tokens were consumed, along with the total. Because this chunk signals the
 * end of the stream’s data payload, downstream consumers can rely on it to
 * definitively obtain the overall cost metrics for the request.
 * </p>
 * <p>
 * The class is designed to be immutable and constructed via its Lombok-based
 * builder ({@code UsageStreamDataChunk.builder().usage(…).build()}). It uses
 * {@code @Jacksonized} to support JSON deserialization in AI service
 * integrations. The private no-args constructor enforced by
 * {@link RequiredArgsConstructor} ensures that the builder is the only
 * normal creation path, preserving consistency and immutability.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see Usage
 * @see StreamDataChunk
 */
@Getter
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class UsageStreamDataChunk implements StreamDataChunk {

    /**
     * The complete token usage details from the AI service response.
     * <p>
     * This object summarizes the tokens used in the prompt and in the generated
     * completion, as well as the total tokens consumed. It is never {@code null}
     * because a valid usage chunk must always report its metrics. The data is
     * extracted from the final part of the stream and can be used for billing,
     * rate limiting, or monitoring purposes.
     * </p>
     */
    private final Usage usage;

    /**
     * Returns the type identifier for this chunk, which is always
     * {@link StreamDataType#USAGE}.
     * <p>
     * This method overrides {@link StreamDataChunk#getDataType()} to indicate
     * that the chunk holds token consumption information rather than content
     * text, commands, or other stream data. By returning the
     * {@code USAGE} constant, the framework and custom handlers can
     * unambiguously determine the chunk’s payload without performing an
     * {@code instanceof} check, enabling cleaner reactive stream processing.
     * </p>
     *
     * @return the constant {@link StreamDataType#USAGE}
     */
    @Override
    public StreamDataType getDataType() {
        return StreamDataType.USAGE;
    }

}