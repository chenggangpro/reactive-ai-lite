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

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.ChoiceAnswer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.NoulAnswer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.ScoreAnswer;
import pro.chenggang.project.reactive.ai.lite.core.option.SystemOneType;

import java.util.List;

/**
 * Sealed interface representing an evaluated answer produced by a SystemOne AI model.
 * <p>
 * Corresponding to the three question types defined in {@link SystemOneType}, an answer
 * may be:
 * <ul>
 *   <li>{@link NoulAnswer}: Calibrated boolean probability / score scale.</li>
 *   <li>{@link ChoiceAnswer}: Categorical decision with confidence score and probability distribution.</li>
 *   <li>{@link ScoreAnswer}: Continuous or ordinal score with confidence and level probabilities.</li>
 * </ul>
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see SystemOneType
 * @since 0.1.0
 */
public sealed interface SystemOneAnswer permits NoulAnswer, ChoiceAnswer, ScoreAnswer {

    /**
     * Returns the type of this answer.
     *
     * @return the {@link SystemOneType} enum constant
     */
    SystemOneType type();

    /**
     * Casts this answer to the specific implementation corresponding to its {@link #type()}.
     *
     * @param <T> the target {@link SystemOneAnswer} implementation type
     * @return this instance cast to {@code T}
     * @throws ClassCastException if the cast is not possible
     */
    @SuppressWarnings("unchecked")
    default <T extends SystemOneAnswer> T cast() {
        return (T) type().getAnswerClass().cast(this);
    }

    /**
     * Answer payload for a boolean proposition evaluation (NOUL).
     */
    @Getter
    @Builder
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class NoulAnswer implements SystemOneAnswer {

        /**
         * The calibrated probability scale (between 0.0 and 1.0) indicating how likely the proposition is true.
         */
        private final Double scale;

        @Override
        public SystemOneType type() {
            return SystemOneType.NOUL;
        }
    }

    /**
     * Answer payload for a categorical classification evaluation (CHOICE).
     */
    @Getter
    @Builder
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class ChoiceAnswer implements SystemOneAnswer {

        /**
         * The chosen option identifier selected by the model.
         */
        private final String choice;

        /**
         * The model's confidence rating for the selected choice.
         */
        private final Double confidence;

        /**
         * The calibrated probability distribution across all evaluated choices.
         */
        private final List<Probability> probabilities;

        @Override
        public SystemOneType type() {
            return SystemOneType.CHOICE;
        }

        /**
         * Represents the probability assigned to an individual choice key.
         */
        @Getter
        @Builder
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Probability {

            /**
             * The choice identifier key.
             */
            private final String key;

            /**
             * The calibrated probability value for this key.
             */
            private final Double probability;
        }

    }

    /**
     * Answer payload for an ordered rating scale evaluation (SCORE).
     */
    @Getter
    @Builder
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class ScoreAnswer implements SystemOneAnswer {

        /**
         * The aggregated numeric score assigned by the model.
         */
        private final Double score;

        /**
         * The model's confidence rating in the assigned score.
         */
        private final Double confidence;

        /**
         * The calibrated probability distribution across the defined legend rating levels.
         */
        private final List<LegendProbability> legendProbabilities;

        @Override
        public SystemOneType type() {
            return SystemOneType.SCORE;
        }

        /**
         * Represents the probability assigned to an individual rating legend level.
         */
        @Getter
        @Builder
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        public static class LegendProbability {

            /**
             * The descriptive rating level identifier.
             */
            private final String level;

            /**
             * The calibrated probability value for this level.
             */
            private final double probability;
        }
    }
}
