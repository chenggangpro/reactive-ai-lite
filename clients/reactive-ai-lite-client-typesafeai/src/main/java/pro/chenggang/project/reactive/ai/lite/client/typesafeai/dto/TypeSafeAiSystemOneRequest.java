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

import java.util.Map;

/**
 * Top-level data transfer object representing a request payload sent to the TypeSafe AI
 * SystemOne evaluation endpoint ({@code POST https://api.typesafe.ai/v1/systemone}).
 * <p>
 * Contains the target model identifier, the input state or unstructured data payload,
 * and a map of typed questions evaluated concurrently in parallel.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see TypeSafeAiQuestion
 * @since 0.1.0
 */
@Getter
@ToString
@EqualsAndHashCode
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TypeSafeAiSystemOneRequest {

    /**
     * The model that handles the request (e.g. {@code "jev-latest"}).
     */
    @NonNull
    private final String model;

    /**
     * The content or state to evaluate. Can be plain text string, structured JSON object,
     * array of entries, or null.
     */
    private final Object state;

    /**
     * A map of typed {@link TypeSafeAiQuestion} objects, keyed by question identifier.
     * Answers from TypeSafe AI are returned under the matching identifiers.
     */
    @NonNull
    private final Map<String, TypeSafeAiQuestion> questions;

}
