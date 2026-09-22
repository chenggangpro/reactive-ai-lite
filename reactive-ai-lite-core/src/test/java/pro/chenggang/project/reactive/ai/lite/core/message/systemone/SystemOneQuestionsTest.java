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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SystemOneQuestions} container.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
class SystemOneQuestionsTest {

    @Test
    void testSingleQuestionFactory() {
        SystemOneQuestion question = SystemOneQuestion.newNoulBuilder("Is input present?").build();
        SystemOneQuestions questions = SystemOneQuestions.of("is_present", question);

        assertThat(questions.getAllQuestions()).hasSize(1);
        assertThat(questions.getAllQuestions()).containsKey("is_present");
        assertThat(questions.getAllQuestions().get("is_present")).isEqualTo(question);
    }

    @Test
    void testMapFactory() {
        SystemOneQuestion q1 = SystemOneQuestion.newNoulBuilder("Is urgent?").build();
        SystemOneQuestion q2 = SystemOneQuestion.newScoreBuilder("Severity")
                .levels("Low", "High")
                .build();

        SystemOneQuestions questions = SystemOneQuestions.of(Map.of("urgent", q1, "severity", q2));

        assertThat(questions.getAllQuestions()).hasSize(2);
        assertThat(questions.getAllQuestions()).containsEntry("urgent", q1);
        assertThat(questions.getAllQuestions()).containsEntry("severity", q2);
    }

    @Test
    void testBuilder() {
        SystemOneQuestion q1 = SystemOneQuestion.newNoulBuilder("Is urgent?").build();
        SystemOneQuestion q2 = SystemOneQuestion.newChoiceBuilder("Type")
                .option("a", "Type A")
                .option("b", "Type B")
                .build();

        SystemOneQuestions questions = SystemOneQuestions.newBuilder()
                .question("q1", q1)
                .questions(Map.of("q2", q2))
                .build();

        assertThat(questions.getAllQuestions()).hasSize(2);
        assertThat(questions.getAllQuestions().get("q1")).isEqualTo(q1);
        assertThat(questions.getAllQuestions().get("q2")).isEqualTo(q2);
    }

    @Test
    void testEmptyQuestionsThrows() {
        assertThatThrownBy(() -> SystemOneQuestions.newBuilder().build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one question");
    }

    @Test
    void testNullChecks() {
        SystemOneQuestion q = SystemOneQuestion.newNoulBuilder("Test").build();

        assertThatThrownBy(() -> SystemOneQuestions.of(null, q))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SystemOneQuestions.of("key", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SystemOneQuestions.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
