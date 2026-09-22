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
package pro.chenggang.project.reactive.ai.lite.core.message.systemone;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Container holding the collection of {@link SystemOneQuestion} instances to be evaluated in parallel.
 * <p>
 * SystemOne models evaluate multiple questions simultaneously against an input payload. This container
 * ensures that at least one question is provided and preserves the mapping between question identifiers
 * (keys) and their respective question schemas.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see SystemOneQuestion
 * @since 0.1.0
 */
@Getter
public final class SystemOneQuestions {

    /**
     * Internal ordered list of question entries.
     */
    private final List<Entry<String, SystemOneQuestion>> questionList;

    private SystemOneQuestions(@NonNull List<Entry<String, SystemOneQuestion>> questionList) {
        if (questionList.isEmpty()) {
            throw new IllegalArgumentException("At least one question should be configured.");
        }
        this.questionList = questionList;
    }

    /**
     * Returns the questions mapped by their identifier preserving insertion order.
     *
     * @return an unmodifiable {@link Map} of question identifiers to {@link SystemOneQuestion} instances
     */
    public Map<String, SystemOneQuestion> getAllQuestions() {
        return this.questionList.stream()
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (existing, replacement) -> replacement, LinkedHashMap::new));
    }

    /**
     * Creates a new builder for constructing {@link SystemOneQuestions}.
     *
     * @return a new {@link Builder} instance
     */
    public static SystemOneQuestions.Builder newBuilder() {
        return new SystemOneQuestions.Builder();
    }

    /**
     * Creates a {@link SystemOneQuestions} instance holding a single question.
     *
     * @param questionId the question identifier; must not be null
     * @param question   the question definition; must not be null
     * @return a new {@link SystemOneQuestions} instance
     */
    public static SystemOneQuestions of(@NonNull String questionId, @NonNull SystemOneQuestion question) {
        return newBuilder().question(questionId, question).build();
    }

    /**
     * Creates a {@link SystemOneQuestions} instance from a map of questions.
     *
     * @param questionMap the map of question identifiers to question definitions; must not be null
     * @return a new {@link SystemOneQuestions} instance
     */
    public static SystemOneQuestions of(@NonNull Map<String, SystemOneQuestion> questionMap) {
        return newBuilder().questions(questionMap).build();
    }

    /**
     * Fluent builder for constructing {@link SystemOneQuestions} instances.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {

        private final List<Entry<String, SystemOneQuestion>> questionList = new ArrayList<>();

        /**
         * Adds a question with its unique identifier.
         *
         * @param questionId the question identifier; must not be null
         * @param question   the question definition; must not be null
         * @return this builder for method chaining
         */
        public Builder question(@NonNull String questionId, @NonNull SystemOneQuestion question) {
            this.questionList.add(Map.entry(questionId, question));
            return this;
        }

        /**
         * Adds all questions from the given map.
         *
         * @param questionMap the map of questions to add; must not be null
         * @return this builder for method chaining
         */
        public Builder questions(@NonNull Map<String, SystemOneQuestion> questionMap) {
            this.questionList.addAll(questionMap.entrySet());
            return this;
        }

        /**
         * Builds and returns a new {@link SystemOneQuestions} instance.
         *
         * @return a new {@link SystemOneQuestions} instance
         */
        public SystemOneQuestions build() {
            return new SystemOneQuestions(questionList);
        }
    }
}
