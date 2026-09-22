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

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.ChoiceQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.NoulQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.ScoreQuestion;
import pro.chenggang.project.reactive.ai.lite.core.option.SystemOneType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SystemOneQuestion} and its schema builders.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
class SystemOneQuestionTest {

    @Test
    void testNoulQuestionDefaultCriteria() {
        NoulQuestion noulQuestion = SystemOneQuestion.newNoulBuilder("Is this an urgent request?")
                .build();

        assertThat(noulQuestion.type()).isEqualTo(SystemOneType.NOUL);
        assertThat(noulQuestion.instructions().getValue()).isEqualTo("Is this an urgent request?");
        assertThat(noulQuestion.criteria()).isNull();
        NoulQuestion cast = noulQuestion.cast();
        assertThat(cast).isSameAs(noulQuestion);
    }

    @Test
    void testNoulQuestionCustomCriteria() {
        NoulQuestion noulQuestion = SystemOneQuestion.newNoulBuilder("Check compliance")
                .trueOption("Compliant")
                .falseOption("Non-compliant")
                .build();

        assertThat(noulQuestion.type()).isEqualTo(SystemOneType.NOUL);
        assertThat(noulQuestion.criteria()).isNotNull();
        assertThat(noulQuestion.criteria().getTrueOption().getValue()).isEqualTo("Compliant");
        assertThat(noulQuestion.criteria().getFalseOption().getValue()).isEqualTo("Non-compliant");
    }

    @Test
    void testNoulQuestionCustomCriteriaWithTextContent() {
        NoulQuestion noulQuestion = SystemOneQuestion.newNoulBuilder(SystemOneContent.text("Check auth"))
                .trueOption(SystemOneContent.text("Authorized"))
                .falseOption(SystemOneContent.text("Unauthorized"))
                .build();

        assertThat(noulQuestion.type()).isEqualTo(SystemOneType.NOUL);
        assertThat(noulQuestion.criteria()).isNotNull();
        assertThat(noulQuestion.criteria().getTrueOption().getValue()).isEqualTo("Authorized");
        assertThat(noulQuestion.criteria().getFalseOption().getValue()).isEqualTo("Unauthorized");
    }

    @Test
    void testChoiceQuestion() {
        ChoiceQuestion choiceQuestion = SystemOneQuestion.newChoiceBuilder("Categorize the ticket")
                .option("billing", "Billing Inquiry")
                .option("tech", "Technical Support")
                .option("other", "Other Questions")
                .build();

        assertThat(choiceQuestion.type()).isEqualTo(SystemOneType.CHOICE);
        assertThat(choiceQuestion.instructions().getValue()).isEqualTo("Categorize the ticket");
        assertThat(choiceQuestion.criteria()).hasSize(3);
        assertThat(choiceQuestion.criteria().get("billing").getValue()).isEqualTo("Billing Inquiry");
        assertThat(choiceQuestion.criteria().get("tech").getValue()).isEqualTo("Technical Support");
        assertThat(choiceQuestion.criteria().get("other").getValue()).isEqualTo("Other Questions");
        ChoiceQuestion cast = choiceQuestion.cast();
        assertThat(cast).isSameAs(choiceQuestion);
    }

    @Test
    void testChoiceQuestionWithOptionsMap() {
        Map<String, SystemOneContent<?>> optionMap = Map.of(
                "low", SystemOneContent.text("Low Risk"),
                "high", SystemOneContent.text("High Risk")
        );
        ChoiceQuestion choiceQuestion = SystemOneQuestion.newChoiceBuilder("Risk assessment")
                .options(optionMap)
                .build();

        assertThat(choiceQuestion.criteria()).hasSize(2);
        assertThat(choiceQuestion.criteria().get("low").getValue()).isEqualTo("Low Risk");
    }

    @Test
    void testScoreQuestionWithVarargs() {
        ScoreQuestion scoreQuestion = SystemOneQuestion.newScoreBuilder("Rate satisfaction")
                .levels("Poor", "Average", "Good", "Excellent")
                .build();

        assertThat(scoreQuestion.type()).isEqualTo(SystemOneType.SCORE);
        assertThat(scoreQuestion.instructions().getValue()).isEqualTo("Rate satisfaction");
        assertThat(scoreQuestion.criteria()).hasSize(4);
        assertThat(scoreQuestion.criteria().get(0).getValue()).isEqualTo("Poor");
        assertThat(scoreQuestion.criteria().get(3).getValue()).isEqualTo("Excellent");
        ScoreQuestion cast = scoreQuestion.cast();
        assertThat(cast).isSameAs(scoreQuestion);
    }

    @Test
    void testScoreQuestionWithListAndSingles() {
        ScoreQuestion scoreQuestion = SystemOneQuestion.newScoreBuilder("Rate complexity")
                .level("Simple")
                .level(SystemOneContent.text("Medium"))
                .levels(List.of(SystemOneContent.text("Hard")))
                .build();

        assertThat(scoreQuestion.criteria()).hasSize(3);
        assertThat(scoreQuestion.criteria().get(0).getValue()).isEqualTo("Simple");
        assertThat(scoreQuestion.criteria().get(1).getValue()).isEqualTo("Medium");
        assertThat(scoreQuestion.criteria().get(2).getValue()).isEqualTo("Hard");
    }

    @Test
    void testValidationFailureOnNullInstructions() {
        assertThatThrownBy(() -> SystemOneQuestion.newNoulBuilder((SystemOneContent<?>) null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> SystemOneQuestion.newNoulBuilder(SystemOneContent.nullContent()).build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> SystemOneQuestion.newChoiceBuilder(SystemOneContent.nullContent()).build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> SystemOneQuestion.newScoreBuilder(SystemOneContent.nullContent()).build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
