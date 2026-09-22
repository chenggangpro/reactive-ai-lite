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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.usage.DefaultUsage;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.ChoiceAnswer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.NoulAnswer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.ScoreAnswer;
import pro.chenggang.project.reactive.ai.lite.core.option.SystemOneType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SystemOneResponse} and {@link SystemOneAnswer} implementations.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
class SystemOneResponseTest {

    @Test
    void testSystemOneResponseBuilderAndGetters() {
        ObjectNode rawJson = JsonNodeFactory.instance.objectNode().put("status", "success");
        DefaultUsage usage = DefaultUsage.builder().promptTokens(100).completionTokens(0).totalTokens(100).build();

        NoulAnswer noulAnswer = NoulAnswer.builder().scale(0.95).build();
        SystemOneResponse response = SystemOneResponse.builder()
                .rawResponseBody(rawJson)
                .usage(usage)
                .answers(Map.of("q1", noulAnswer))
                .build();

        assertThat(response.getRawResponseBody()).isEqualTo(rawJson);
        assertThat(response.getUsage()).isEqualTo(usage);
        assertThat(response.getAnswers()).containsEntry("q1", noulAnswer);
        assertThat(response.getAnswer("q1")).isEqualTo(noulAnswer);
        assertThat(response.getAnswer("q1", NoulAnswer.class)).isEqualTo(noulAnswer);
        assertThat(response.getAnswer("non_existing")).isNull();
        assertThat(response.getAnswer("non_existing", NoulAnswer.class)).isNull();
        assertThat(response.toString()).contains("SystemOneResponse");
    }

    @Test
    void testSystemOneResponseGetAnswerTypeMismatch() {
        NoulAnswer noulAnswer = NoulAnswer.builder().scale(0.95).build();
        SystemOneResponse response = SystemOneResponse.builder()
                .answers(Map.of("q1", noulAnswer))
                .build();

        assertThatThrownBy(() -> response.getAnswer("q1", ChoiceAnswer.class))
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    void testSystemOneResponseNullAnswers() {
        SystemOneResponse response = SystemOneResponse.builder().build();

        assertThat(response.getAnswers()).isNull();
        assertThat(response.getAnswer("q1")).isNull();
        assertThat(response.getAnswer("q1", NoulAnswer.class)).isNull();
    }

    @Test
    void testNoulAnswer() {
        NoulAnswer noul = NoulAnswer.builder()
                .scale(0.875)
                .build();

        assertThat(noul.type()).isEqualTo(SystemOneType.NOUL);
        assertThat(noul.getScale()).isEqualTo(0.875);
        NoulAnswer cast = noul.cast();
        assertThat(cast).isSameAs(noul);
    }

    @Test
    void testChoiceAnswer() {
        ChoiceAnswer.Probability probA = ChoiceAnswer.Probability.builder()
                .key("tech")
                .probability(0.85)
                .build();
        ChoiceAnswer.Probability probB = ChoiceAnswer.Probability.builder()
                .key("billing")
                .probability(0.15)
                .build();

        ChoiceAnswer choice = ChoiceAnswer.builder()
                .choice("tech")
                .confidence(0.92)
                .probabilities(List.of(probA, probB))
                .build();

        assertThat(choice.type()).isEqualTo(SystemOneType.CHOICE);
        assertThat(choice.getChoice()).isEqualTo("tech");
        assertThat(choice.getConfidence()).isEqualTo(0.92);
        assertThat(choice.getProbabilities()).containsExactly(probA, probB);
        assertThat(probA.getKey()).isEqualTo("tech");
        assertThat(probA.getProbability()).isEqualTo(0.85);

        ChoiceAnswer cast = choice.cast();
        assertThat(cast).isSameAs(choice);
    }

    @Test
    void testScoreAnswer() {
        ScoreAnswer.LegendProbability legend1 = ScoreAnswer.LegendProbability.builder()
                .level("Low")
                .probability(0.1)
                .build();
        ScoreAnswer.LegendProbability legend2 = ScoreAnswer.LegendProbability.builder()
                .level("High")
                .probability(0.9)
                .build();

        ScoreAnswer score = ScoreAnswer.builder()
                .score(4.5)
                .confidence(0.96)
                .legendProbabilities(List.of(legend1, legend2))
                .build();

        assertThat(score.type()).isEqualTo(SystemOneType.SCORE);
        assertThat(score.getScore()).isEqualTo(4.5);
        assertThat(score.getConfidence()).isEqualTo(0.96);
        assertThat(score.getLegendProbabilities()).containsExactly(legend1, legend2);
        assertThat(legend1.getLevel()).isEqualTo("Low");
        assertThat(legend1.getProbability()).isEqualTo(0.1);

        ScoreAnswer cast = score.cast();
        assertThat(cast).isSameAs(score);
    }
}
