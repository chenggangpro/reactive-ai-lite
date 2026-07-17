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

/**
 * Represents a chunk of response from a streaming text-to-speech request.
 * <p>
 * This class extends {@link LlmResponse} to include a chunk of the resulting audio data.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@SuperBuilder
public class SpeechStreamResponse extends LlmResponse {

    /**
     * A chunk of the synthesized audio data as a byte array.
     */
    protected final byte[] chunk;

}
