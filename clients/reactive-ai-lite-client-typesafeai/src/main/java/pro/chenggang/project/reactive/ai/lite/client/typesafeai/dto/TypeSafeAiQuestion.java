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
package pro.chenggang.project.reactive.ai.lite.client.typesafeai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Data transfer object representing an individual typed question sent to TypeSafe AI's
 * SystemOne evaluation endpoint.
 * <p>
 * According to TypeSafe AI API specifications, each question must specify a question type
 * ({@code "noul"}, {@code "choice"}, or {@code "score"}), guidelines or instructions
 * to be evaluated, and optional or required evaluation criteria.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see TypeSafeAiSystemOneRequest
 * @since 0.1.0
 */
@Getter
@ToString
@EqualsAndHashCode
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TypeSafeAiQuestion {

    /**
     * The question type discriminator: {@code "noul"}, {@code "choice"}, or {@code "score"}.
     */
    @NonNull
    private final String type;

    /**
     * The evaluation guidelines or question statement. Can be plain text (String) or
     * structured data (Map or List).
     */
    @NonNull
    private final Object instructions;

    /**
     * Rubric criteria defining the evaluation outcomes:
     * <ul>
     *   <li>For {@code "noul"}: optional object with {@code "true"} and {@code "false"} descriptions.</li>
     *   <li>For {@code "choice"}: map of option identifiers to option descriptions.</li>
     *   <li>For {@code "score"}: ordered array of level descriptions.</li>
     * </ul>
     */
    private final Object criteria;

}
