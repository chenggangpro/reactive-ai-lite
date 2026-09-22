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
package pro.chenggang.project.reactive.ai.lite.core.option;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.ChoiceAnswer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.NoulAnswer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.ScoreAnswer;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.ChoiceQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.NoulQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.ScoreQuestion;

/**
 * Enumeration of supported question and answer evaluation types for SystemOne operations.
 * <p>
 * Binds each question classification schema (e.g. {@link NoulQuestion}) to its corresponding
 * structured answer representation (e.g. {@link NoulAnswer}) and wire format identifier.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum SystemOneType {

    /**
     * Boolean (Yes/No) evaluation producing calibrated true/false probabilities.
     */
    NOUL("noul", NoulQuestion.class, NoulAnswer.class),

    /**
     * Multi-option classification producing probability distributions across choices.
     */
    CHOICE("choice", ChoiceQuestion.class, ChoiceAnswer.class),

    /**
     * Ordered scale rating producing level distributions or ordinal evaluation.
     */
    SCORE("score", ScoreQuestion.class, ScoreAnswer.class),
    ;

    /**
     * The string identifier expected by downstream SystemOne providers.
     */
    private final String value;

    /**
     * The class type representing the question definition schema for this type.
     */
    private final Class<? extends SystemOneQuestion> questionClass;

    /**
     * The class type representing the evaluated answer result for this type.
     */
    private final Class<? extends SystemOneAnswer> answerClass;

}