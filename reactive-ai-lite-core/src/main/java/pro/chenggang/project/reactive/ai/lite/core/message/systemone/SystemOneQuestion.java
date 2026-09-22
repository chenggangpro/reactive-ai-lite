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
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.ChoiceQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.NoulQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.NoulQuestion.QuestionBuilder;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.ScoreQuestion;
import pro.chenggang.project.reactive.ai.lite.core.option.SystemOneType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Sealed interface representing an atomic question schema evaluated by a SystemOne AI model.
 * <p>
 * In SystemOne models (e.g. TypeSafe AI's Jev), questions are evaluated in parallel against an input
 * content payload. The model outputs structured, calibrated probabilities without token-generation overhead.
 * The three canonical question types are:
 * <ul>
 *   <li>{@link NoulQuestion}: Answers a boolean proposition by outputting the calibrated probability that the proposition is true.</li>
 *   <li>{@link ChoiceQuestion}: Evaluates multiple mutually exclusive options and produces a probability distribution.</li>
 *   <li>{@link ScoreQuestion}: Evaluates the input along an ordered scale of descriptive criteria or rating levels.</li>
 * </ul>
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see SystemOneContent
 * @see SystemOneQuestions
 * @since 0.1.0
 */
public sealed interface SystemOneQuestion permits NoulQuestion, ChoiceQuestion, ScoreQuestion {

    /**
     * Returns the type of this question.
     *
     * @return the non-null {@link SystemOneType}
     */
    SystemOneType type();

    /**
     * Returns the instructional guidelines or prompt content for evaluating this question.
     *
     * @return the instructions as {@link SystemOneContent}
     */
    SystemOneContent<?> instructions();

    /**
     * Casts this question to the specific implementation corresponding to its {@link #type()}.
     *
     * @param <T> the target {@link SystemOneQuestion} implementation type
     * @return this instance cast to {@code T}
     * @throws ClassCastException if the cast is not possible
     */
    @SuppressWarnings("unchecked")
    default <T extends SystemOneQuestion> T cast() {
        return (T) type().getQuestionClass().cast(this);
    }

    /**
     * Creates a new builder for a {@link NoulQuestion} (Yes/No boolean) question with the given instructions.
     *
     * @param instructions the question instructions; must not be null
     * @return a new {@link NoulQuestion.QuestionBuilder}
     */
    static NoulQuestion.QuestionBuilder newNoulBuilder(@NonNull SystemOneContent<?> instructions) {
        return new QuestionBuilder(instructions);
    }

    /**
     * Creates a new builder for a {@link NoulQuestion} (Yes/No boolean) question with plain text instructions.
     *
     * @param instructions the question instructions text; must not be null
     * @return a new {@link NoulQuestion.QuestionBuilder}
     */
    static NoulQuestion.QuestionBuilder newNoulBuilder(@NonNull String instructions) {
        return newNoulBuilder(SystemOneContent.text(instructions));
    }

    /**
     * Creates a new builder for a {@link ChoiceQuestion} question with the given instructions.
     *
     * @param instructions the question instructions; must not be null
     * @return a new {@link ChoiceQuestion.QuestionBuilder}
     */
    static ChoiceQuestion.QuestionBuilder newChoiceBuilder(@NonNull SystemOneContent<?> instructions) {
        return new ChoiceQuestion.QuestionBuilder(instructions);
    }

    /**
     * Creates a new builder for a {@link ChoiceQuestion} question with plain text instructions.
     *
     * @param instructions the question instructions text; must not be null
     * @return a new {@link ChoiceQuestion.QuestionBuilder}
     */
    static ChoiceQuestion.QuestionBuilder newChoiceBuilder(@NonNull String instructions) {
        return newChoiceBuilder(SystemOneContent.text(instructions));
    }

    /**
     * Creates a new builder for a {@link ScoreQuestion} question with the given instructions.
     *
     * @param instructions the question instructions; must not be null
     * @return a new {@link ScoreQuestion.QuestionBuilder}
     */
    static ScoreQuestion.QuestionBuilder newScoreBuilder(@NonNull SystemOneContent<?> instructions) {
        return new ScoreQuestion.QuestionBuilder(instructions);
    }

    /**
     * Creates a new builder for a {@link ScoreQuestion} question with plain text instructions.
     *
     * @param instructions the question instructions text; must not be null
     * @return a new {@link ScoreQuestion.QuestionBuilder}
     */
    static ScoreQuestion.QuestionBuilder newScoreBuilder(@NonNull String instructions) {
        return newScoreBuilder(SystemOneContent.text(instructions));
    }

    /**
     * A boolean (Yes/No) question evaluated by a SystemOne model, producing calibrated true/false probabilities.
     */
    @Getter
    final class NoulQuestion implements SystemOneQuestion {

        /**
         * The guidelines or statement to be evaluated.
         */
        @NonNull
        private final SystemOneContent<?> instructions;

        /**
         * Optional criteria configuring the semantics of the true and false outcomes.
         */
        private final NoulCriteria criteria;

        private NoulQuestion(@NonNull SystemOneContent<?> instructions, NoulCriteria criteria) {
            if (instructions instanceof SystemOneContent.NullContent) {
                throw new IllegalArgumentException("Instructions cannot be null content");
            }
            if (instructions instanceof SystemOneContent.TextContent textContent && Objects.isNull(textContent.getValue())) {
                throw new IllegalArgumentException("Instructions cannot be null text content");
            }
            this.instructions = instructions;
            this.criteria = criteria;
        }

        @Override
        public SystemOneType type() {
            return SystemOneType.NOUL;
        }

        @Override
        public SystemOneContent<?> instructions() {
            return this.instructions;
        }

        /**
         * Returns the optional criteria defining the true/false options.
         *
         * @return the {@link NoulCriteria}, or {@code null} if default boolean semantics apply
         */
        public NoulCriteria criteria() {
            return this.criteria;
        }

        /**
         * Criteria defining the descriptive options for true and false outcomes in a {@link NoulQuestion} question.
         */
        @Getter
        @Builder
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        public static class NoulCriteria {

            /**
             * Content description for the true outcome.
             */
            private final SystemOneContent<?> trueOption;

            /**
             * Content description for the false outcome.
             */
            private final SystemOneContent<?> falseOption;

        }

        /**
         * Builder for constructing immutable {@link NoulQuestion} question instances.
         */
        public static class QuestionBuilder {

            private final SystemOneContent<?> instructions;
            private SystemOneContent<?> trueOption;
            private SystemOneContent<?> falseOption;

            private QuestionBuilder(@NonNull SystemOneContent<?> instructions) {
                this.instructions = instructions;
            }

            /**
             * Sets the description content for the true option.
             *
             * @param trueOption the true option content
             * @return this builder for method chaining
             */
            public QuestionBuilder trueOption(SystemOneContent<?> trueOption) {
                this.trueOption = trueOption;
                return this;
            }

            /**
             * Sets the description text for the true option.
             *
             * @param trueOption the true option text
             * @return this builder for method chaining
             */
            public QuestionBuilder trueOption(String trueOption) {
                return trueOption(Objects.nonNull(trueOption) ? SystemOneContent.text(trueOption) : null);
            }

            /**
             * Sets the description content for the false option.
             *
             * @param falseOption the false option content
             * @return this builder for method chaining
             */
            public QuestionBuilder falseOption(SystemOneContent<?> falseOption) {
                this.falseOption = falseOption;
                return this;
            }

            /**
             * Sets the description text for the false option.
             *
             * @param falseOption the false option text
             * @return this builder for method chaining
             */
            public QuestionBuilder falseOption(String falseOption) {
                return falseOption(Objects.nonNull(falseOption) ? SystemOneContent.text(falseOption) : null);
            }

            /**
             * Builds and returns a new immutable {@link NoulQuestion} question.
             *
             * @return a new {@link NoulQuestion} instance
             */
            public NoulQuestion build() {
                NoulCriteria criteria = null;
                if (Objects.nonNull(trueOption) || Objects.nonNull(falseOption)) {
                    criteria = NoulCriteria.builder()
                            .trueOption(trueOption)
                            .falseOption(falseOption)
                            .build();
                }
                return new NoulQuestion(instructions, criteria);
            }
        }
    }

    /**
     * A multi-option categorical question evaluated by a SystemOne model, producing a probability distribution.
     */
    final class ChoiceQuestion implements SystemOneQuestion {

        /**
         * The guidelines or instructions to be evaluated.
         */
        @NonNull
        private final SystemOneContent<?> instructions;

        /**
         * The options available for selection, mapped by option identifier.
         */
        private final Map<String, SystemOneContent<?>> criteria;

        @SuppressWarnings("unchecked")
        private ChoiceQuestion(@NonNull SystemOneContent<?> instructions, List<Entry<String, SystemOneContent<?>>> criteria) {
            if (instructions instanceof SystemOneContent.NullContent) {
                throw new IllegalArgumentException("Instructions cannot be null content");
            }
            if (instructions instanceof SystemOneContent.TextContent textContent && Objects.isNull(textContent.getValue())) {
                throw new IllegalArgumentException("Instructions cannot be null text content");
            }
            this.instructions = instructions;
            if (Objects.nonNull(criteria) && !criteria.isEmpty()) {
                this.criteria = Map.ofEntries(criteria.toArray(new Entry[0]));
            } else {
                this.criteria = null;
            }
        }

        @Override
        public SystemOneType type() {
            return SystemOneType.CHOICE;
        }

        @Override
        public SystemOneContent<?> instructions() {
            return this.instructions;
        }

        /**
         * Returns the options map for this choice question.
         *
         * @return an immutable map of option key to option description, or {@code null} if none
         */
        public Map<String, SystemOneContent<?>> criteria() {
            return this.criteria;
        }

        /**
         * Builder for constructing immutable {@link ChoiceQuestion} question instances.
         */
        public static class QuestionBuilder {

            private final SystemOneContent<?> instructions;
            private final List<Map.Entry<String, SystemOneContent<?>>> optionList = new ArrayList<>();

            private QuestionBuilder(@NonNull SystemOneContent<?> instructions) {
                this.instructions = instructions;
            }

            /**
             * Adds an option with the given key and description content.
             *
             * @param key         the option identifier; must not be null
             * @param description the option description; must not be null
             * @return this builder for method chaining
             */
            public QuestionBuilder option(@NonNull String key, @NonNull SystemOneContent<?> description) {
                optionList.add(Map.entry(key, description));
                return this;
            }

            /**
             * Adds an option with the given key and text description.
             *
             * @param key         the option identifier; must not be null
             * @param description the option description text; must not be null
             * @return this builder for method chaining
             */
            public QuestionBuilder option(@NonNull String key, @NonNull String description) {
                return option(key, SystemOneContent.text(description));
            }

            /**
             * Adds multiple options from a map of keys to description content.
             *
             * @param options the map of options; must not be null
             * @return this builder for method chaining
             */
            public QuestionBuilder options(@NonNull Map<String, SystemOneContent<?>> options) {
                optionList.addAll(options.entrySet());
                return this;
            }

            /**
             * Builds and returns a new immutable {@link ChoiceQuestion} question.
             *
             * @return a new {@link ChoiceQuestion} instance
             */
            public ChoiceQuestion build() {
                return new ChoiceQuestion(instructions, optionList);
            }

        }
    }

    /**
     * An ordered scale rating question evaluated by a SystemOne model along defined criteria levels.
     */
    final class ScoreQuestion implements SystemOneQuestion {

        /**
         * The guidelines or instructions to be evaluated.
         */
        @NonNull
        private final SystemOneContent<?> instructions;

        /**
         * The ordered scale levels or criteria.
         */
        private final List<SystemOneContent<?>> criteria;

        private ScoreQuestion(@NonNull SystemOneContent<?> instructions, List<SystemOneContent<?>> criteria) {
            if (instructions instanceof SystemOneContent.NullContent) {
                throw new IllegalArgumentException("Instructions cannot be null content");
            }
            if (instructions instanceof SystemOneContent.TextContent textContent && Objects.isNull(textContent.getValue())) {
                throw new IllegalArgumentException("Instructions cannot be null text content");
            }
            this.instructions = instructions;
            this.criteria = Objects.nonNull(criteria) ? List.copyOf(criteria) : List.of();
        }

        @Override
        public SystemOneType type() {
            return SystemOneType.SCORE;
        }

        @Override
        public SystemOneContent<?> instructions() {
            return this.instructions;
        }

        /**
         * Returns the ordered list of scale level criteria.
         *
         * @return an immutable list of scale levels
         */
        public List<SystemOneContent<?>> criteria() {
            return this.criteria;
        }

        /**
         * Builder for constructing immutable {@link ScoreQuestion} question instances.
         */
        public static class QuestionBuilder {

            private final SystemOneContent<?> instructions;
            private final List<SystemOneContent<?>> levelList = new ArrayList<>();

            private QuestionBuilder(@NonNull SystemOneContent<?> instructions) {
                this.instructions = instructions;
            }

            /**
             * Adds a scale level with content description.
             *
             * @param level the level content; must not be null
             * @return this builder for method chaining
             */
            public QuestionBuilder level(@NonNull SystemOneContent<?> level) {
                this.levelList.add(level);
                return this;
            }

            /**
             * Adds a scale level with text description.
             *
             * @param level the level text; must not be null
             * @return this builder for method chaining
             */
            public QuestionBuilder level(@NonNull String level) {
                return level(SystemOneContent.text(level));
            }

            /**
             * Adds multiple scale levels.
             *
             * @param levels the collection of levels; must not be null
             * @return this builder for method chaining
             */
            public QuestionBuilder levels(@NonNull Collection<SystemOneContent<?>> levels) {
                this.levelList.addAll(levels);
                return this;
            }

            /**
             * Adds multiple scale levels using text strings.
             *
             * @param levels varargs text descriptions for each level
             * @return this builder for method chaining
             */
            public QuestionBuilder levels(@NonNull String... levels) {
                for (String lvl : levels) {
                    level(lvl);
                }
                return this;
            }

            /**
             * Builds and returns a new immutable {@link ScoreQuestion} question.
             *
             * @return a new {@link ScoreQuestion} instance
             */
            public ScoreQuestion build() {
                return new ScoreQuestion(instructions, levelList);
            }

        }

    }

}
