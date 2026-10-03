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
package pro.chenggang.project.reactive.ai.lite.client.ollama.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

/**
 * Top-level data transfer object representing a request payload sent to the Ollama
 * SystemOne evaluation endpoint ({@code POST /v1/systemone}).
 * <p>
 * Contains the target model identifier, the input state or unstructured data payload,
 * optional shared Base64-encoded image attachments, a map of typed questions, and an
 * optional keep-alive duration setting.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see OllamaSystemOneQuestion
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@ToString
@EqualsAndHashCode
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OllamaSystemOneRequest {

    /**
     * The model that handles the request (e.g. {@code "clef:latest"}, {@code "clef-flash"}, {@code "nimble"}).
     */
    @JsonProperty("model")
    @NonNull
    private final String model;

    /**
     * The content or state to evaluate. Can be plain text string, structured JSON object,
     * or array of entries.
     */
    @JsonProperty("state")
    @NonNull
    private final Object state;

    /**
     * Optional Base64-encoded images shared by all questions in request order.
     * Supported formats: PNG, JPEG, WebP. URLs and data URLs are not accepted.
     */
    @JsonProperty("images")
    private final List<String> images;

    /**
     * A map of typed {@link OllamaSystemOneQuestion} objects, keyed by question identifier.
     */
    @JsonProperty("questions")
    @NonNull
    private final Map<String, OllamaSystemOneQuestion> questions;

    /**
     * How long to keep the model loaded after the request, as a duration string (such as "5m") or seconds.
     */
    @JsonProperty("keep_alive")
    private final Object keepAlive;

}
