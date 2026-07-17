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
package pro.chenggang.project.reactive.ai.lite.client.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Represents a request to the OpenAI speech generation API.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenaiSpeechRequest {

    /**
     * One of the available TTS models: tts-1 or tts-1-hd.
     */
    @JsonProperty("model")
    String model;

    /**
     * The text to generate audio for. The maximum length is 4096 characters.
     */
    @JsonProperty("input")
    String input;

    /**
     * The voice to use when generating the audio. Supported voices are alloy, echo, fable, onyx, nova, and shimmer.
     */
    @JsonProperty("voice")
    String voice;

    /**
     * The format to audio in. Supported formats are mp3, opus, aac, flac, wav, and pcm.
     */
    @JsonProperty("response_format")
    String responseFormat;

    /**
     * The speed of the generated audio. Select a value from 0.25 to 4.0. 1.0 is the default.
     */
    @JsonProperty("speed")
    Double speed;
}
