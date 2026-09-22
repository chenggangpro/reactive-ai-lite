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
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Map;
import java.util.Objects;

/**
 * Standardized response container for SystemOne operations.
 * <p>
 * Encapsulates the evaluated answers mapped by question identifier, along with common
 * response metadata (token usage, raw JSON body) inherited from {@link ExtractedLlmResponse}.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ExtractedLlmResponse
 * @see SystemOneAnswer
 * @since 0.1.0
 */
@Getter
@ToString(callSuper = true)
@SuperBuilder
public class SystemOneResponse extends ExtractedLlmResponse {

    /**
     * Map of evaluated answers keyed by their respective question identifiers.
     */
    private final Map<String, SystemOneAnswer> answers;

    /**
     * Retrieves an answer by its question identifier.
     *
     * @param questionId the unique question identifier; must not be null
     * @return the {@link SystemOneAnswer} corresponding to the identifier, or {@code null} if not found
     */
    public SystemOneAnswer getAnswer(@NonNull String questionId) {
        if (Objects.isNull(answers)) {
            return null;
        }
        return answers.get(questionId);
    }

    /**
     * Retrieves an answer by its question identifier and casts it to the specified answer subtype.
     *
     * @param <T>         the target {@link SystemOneAnswer} subtype
     * @param questionId  the unique question identifier; must not be null
     * @param answerClass the target class type to cast the answer into; must not be null
     * @return the cast answer instance, or {@code null} if no answer was found for the question ID
     * @throws ClassCastException if the answer is present but cannot be cast to the specified type
     */
    public <T extends SystemOneAnswer> T getAnswer(@NonNull String questionId, @NonNull Class<T> answerClass) {
        SystemOneAnswer answer = getAnswer(questionId);
        if (Objects.isNull(answer)) {
            return null;
        }
        return answerClass.cast(answer);
    }
}
