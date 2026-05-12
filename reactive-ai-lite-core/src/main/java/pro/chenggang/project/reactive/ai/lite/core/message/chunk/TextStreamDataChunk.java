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
import pro.chenggang.project.reactive.ai.lite.core.entity.AttributesAbility;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.AbstractAttribute;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;

/**
 * Represents a text-based stream data chunk.
 * <p>
 * This chunk holds textual data, typically representing fragments of a generated
 * message, reasoning content, or metadata like the role. The exact nature of the
 * text is determined by its {@link StreamDataType}.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TextStreamDataChunk extends AbstractAttribute implements StreamDataChunk, AttributesAbility {

    /**
     * The specific category of this text chunk (e.g., ANSWER_CONTENT, REASONING_CONTENT).
     */
    private final StreamDataType dataType;

    /**
     * The textual fragment value emitted in this chunk.
     */
    @Getter
    private final String value;

    /**
     * Retrieves the category of data this stream chunk contains.
     *
     * @return the {@link StreamDataType} specifying the text content's purpose
     */
    @Override
    public StreamDataType getDataType() {
        return this.dataType;
    }

}
