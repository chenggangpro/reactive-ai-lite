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

/**
 * Data transfer object representing an individual typed question sent to Ollama's
 * SystemOne decision endpoint ({@code POST /v1/systemone}).
 * <p>
 * Supported question types include {@code "choice"}, {@code "noul"}, and {@code "score"}.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see OllamaSystemOneRequest
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
public class OllamaSystemOneQuestion {

    /**
     * The question type discriminator: {@code "choice"}, {@code "noul"}, or {@code "score"}.
     */
    @JsonProperty("type")
    @NonNull
    private final String type;

    /**
     * The evaluation guidelines or question statement. Can be plain text (String) or
     * structured data (Map or List).
     */
    @JsonProperty("instructions")
    @NonNull
    private final Object instructions;

    /**
     * Rubric criteria defining the evaluation outcomes:
     * <ul>
     *   <li>For {@code "choice"}: map of option identifiers to option descriptions.</li>
     *   <li>For {@code "noul"}: optional object with {@code "true"} and {@code "false"} descriptions.</li>
     *   <li>For {@code "score"}: ordered array of level descriptions.</li>
     * </ul>
     */
    @JsonProperty("criteria")
    private final Object criteria;

}
