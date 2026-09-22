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
package pro.chenggang.project.reactive.ai.lite.client.typesafeai.systemone;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pro.chenggang.project.reactive.ai.lite.client.typesafeai.TypeSafeAiLlmClientTestApplicationTests;
import pro.chenggang.project.reactive.ai.lite.core.api.ReactiveLlmClient;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.ChoiceAnswer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.NoulAnswer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.ScoreAnswer;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live API tests for the TypeSafe AI SystemOne evaluation client.
 * <p>
 * Evaluates real-world requests against {@code https://api.typesafe.ai/v1/systemone}
 * using the authentication token provided in the local environment, referencing the
 * examples from the TypeSafe AI API reference documentation.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
public class TypeSafeAiSystemOneClientTests extends TypeSafeAiLlmClientTestApplicationTests {

    @Autowired
    private ReactiveLlmClient reactiveLlmClient;

    @Autowired
    private ObjectMapper objectMapper;

    private final String model = "jev-latest";

    /**
     * Tests SystemOne general execution using the complete example from the TypeSafe AI API reference.
     * Evaluates a state containing customer support message against Noul, Choice, and Score questions.
     */
    @Test
    @DisplayName("Test SystemOne general execution with Noul, Choice, and Score questions from API reference")
    void testSystemOneGeneralExecuteWithExample() {
        reactiveLlmClient.systemOne()
                .model(model)
                .state("Help! My payouts have been failing for 3 days.")
                .questionsBuilder(questions -> questions
                        .question("is_urgent", SystemOneQuestion.newNoulBuilder("Does this convey urgency?")
                                .trueOption("Explicitly time-sensitive")
                                .falseOption("No urgency expressed")
                                .build())
                        .question("department", SystemOneQuestion.newChoiceBuilder("Which team should handle this?")
                                .option("billing", "Payments, invoicing, refunds")
                                .option("technical", "Bugs, outages, integrations")
                                .option("sales", "Pricing, upgrades, new accounts")
                                .build())
                        .question("frustration", SystemOneQuestion.newScoreBuilder("How frustrated is the customer?")
                                .level("Calm")
                                .level("Frustrated")
                                .level("Very angry")
                                .build()))
                .general()
                .execute()
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    try {
                        log.info("SystemOne general response:\n{}",
                                JsonRelatedUtil.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(response));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    assertThat(response).isNotNull();
                    assertThat(response.getAnswers()).hasSize(3);

                    // Verify Noul Answer (is_urgent)
                    NoulAnswer noulAnswer = response.getAnswer("is_urgent", NoulAnswer.class);
                    assertThat(noulAnswer).isNotNull();
                    assertThat(noulAnswer.getScale()).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
                    // Payout failure for 3 days should convey high urgency
                    assertThat(noulAnswer.getScale()).isGreaterThan(0.7);

                    // Verify Choice Answer (department)
                    ChoiceAnswer choiceAnswer = response.getAnswer("department", ChoiceAnswer.class);
                    assertThat(choiceAnswer).isNotNull();
                    assertThat(choiceAnswer.getChoice()).isEqualTo("billing");
                    assertThat(choiceAnswer.getConfidence()).isGreaterThan(0.0);
                    assertThat(choiceAnswer.getProbabilities()).isNotEmpty();

                    // Verify Score Answer (frustration)
                    ScoreAnswer scoreAnswer = response.getAnswer("frustration", ScoreAnswer.class);
                    assertThat(scoreAnswer).isNotNull();
                    assertThat(scoreAnswer.getScore()).isGreaterThanOrEqualTo(0.0);
                    assertThat(scoreAnswer.getConfidence()).isGreaterThan(0.0);
                    assertThat(scoreAnswer.getLegendProbabilities()).isNotEmpty();

                    // Verify Token Usage
                    assertThat(response.getUsage()).isNotNull();
                    assertThat(response.getUsage().getPromptTokens()).isGreaterThan(0);
                    assertThat(response.getUsage().getCompletionTokens()).isGreaterThan(0);
                    assertThat(response.getUsage().getTotalTokens()).isGreaterThan(0);
                })
                .verifyComplete();
    }

    /**
     * Tests SystemOne raw execution returning the unprocessed HTTP response body.
     */
    @Test
    @DisplayName("Test SystemOne general execution raw returning raw JSON response body")
    void testSystemOneGeneralExecuteRaw() {
        reactiveLlmClient.systemOne()
                .model(model)
                .state("Help! My payouts have been failing for 3 days.")
                .question("is_urgent", SystemOneQuestion.newNoulBuilder("Does this convey urgency?")
                        .build())
                .general()
                .executeRaw()
                .as(StepVerifier::create)
                .consumeNextWith(rawResponse -> {
                    try {
                        log.info("SystemOne raw response:\n{}",
                                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rawResponse));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    assertThat(rawResponse).isNotNull();
                    assertThat(rawResponse.getResponseBody()).isNotNull();
                    assertThat(rawResponse.getResponseBody().has("model")).isTrue();
                    assertThat(rawResponse.getResponseBody().has("answers")).isTrue();
                    assertThat(rawResponse.getResponseBody().at("/answers/is_urgent/type").asText()).isEqualTo("noul");
                    assertThat(rawResponse.getResponseBody().has("usage")).isTrue();
                })
                .verifyComplete();
    }

    /**
     * Tests a minimal SystemOne evaluation with a single boolean proposition.
     */
    @Test
    @DisplayName("Test SystemOne single Noul boolean proposition")
    void testSystemOneSingleNoulQuestion() {
        reactiveLlmClient.systemOne()
                .model(model)
                .state("System load is at 98%, memory usage at 95%, response latency spiked to 4500ms.")
                .question("is_incident", SystemOneQuestion.newNoulBuilder("Is this an active production incident?")
                        .build())
                .general()
                .execute()
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    assertThat(response).isNotNull();
                    NoulAnswer noulAnswer = response.getAnswer("is_incident", NoulAnswer.class);
                    assertThat(noulAnswer).isNotNull();
                    assertThat(noulAnswer.getScale()).isGreaterThan(0.8);
                })
                .verifyComplete();
    }
}
