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
package pro.chenggang.project.reactive.ai.lite.client.ollama.systemone;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;
import pro.chenggang.project.reactive.ai.lite.client.ollama.OllamaLlmClientTestApplicationTests;
import pro.chenggang.project.reactive.ai.lite.core.api.ReactiveLlmClient;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.ChoiceAnswer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.NoulAnswer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer.ScoreAnswer;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Base64Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Ollama SystemOne decision API using {@link ReactiveLlmClient}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
public class OllamaSystemOneClientTests extends OllamaLlmClientTestApplicationTests {

    @Autowired
    private ReactiveLlmClient reactiveLlmClient;

    private static final String MODEL_NAME = "clef:latest";

    @Test
    @DisplayName("Evaluate Choice question with clef:latest")
    void testSystemOneChoiceEvaluation() {
        reactiveLlmClient.systemOne()
                .model(MODEL_NAME)
                .state("Our checkout has returned 500 errors since 9am.")
                .questionsBuilder(questions -> questions
                        .question("label", SystemOneQuestion.newChoiceBuilder("Which label fits this ticket?")
                                .option("billing", "Payments and refunds")
                                .option("bug", "Software errors")
                                .option("account", "Login and access")
                                .build()
                        ))
                .general()
                .execute()
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    try {
                        log.info("SystemOne choice response:\n{}",
                                JsonRelatedUtil.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(response)
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    assertThat(response.getRawResponseBody()).isNotNull();
                    ChoiceAnswer choiceAnswer = response.getAnswer("label", ChoiceAnswer.class);
                    assertThat(choiceAnswer).isNotNull();
                    assertThat(choiceAnswer.getChoice()).isEqualTo("bug");
                    assertThat(choiceAnswer.getConfidence()).isGreaterThan(0.0);
                    assertThat(choiceAnswer.getProbabilities()).isNotEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Evaluate Noul question with image attachment with clef:latest")
    void testSystemOneWithImageEvaluation() {
        // 1x1 transparent PNG in base64
        Base64Attachment attachment = Base64Attachment.builder()
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .name("dot.png")
                .base64Content("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=")
                .build();

        reactiveLlmClient.systemOne()
                .model(MODEL_NAME)
                .image(attachment)
                .state("The user uploaded an image.")
                .questionsBuilder(questions -> questions
                        .question("is_empty", SystemOneQuestion.newNoulBuilder("Is this a blank or single-color image?")
                                .build()
                        ))
                .general()
                .execute()
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    try {
                        log.info("SystemOne image response:\n{}",
                                JsonRelatedUtil.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(response)
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    NoulAnswer noulAnswer = response.getAnswer("is_empty", NoulAnswer.class);
                    assertThat(noulAnswer).isNotNull();
                    assertThat(noulAnswer.getScale()).isGreaterThan(0.5);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Evaluate multiple questions (Noul, Choice, Score) in a single request with clef:latest")
    void testSystemOneMultipleQuestionsEvaluation() {
        reactiveLlmClient.systemOne()
                .model(MODEL_NAME)
                .state("I was charged twice. Please refund the extra payment immediately.")
                .questionsBuilder(questions -> questions
                        .question("is_urgent", SystemOneQuestion.newNoulBuilder("Is customer requesting a refund?")
                                .trueOption("The customer requests a refund")
                                .falseOption("No refund is requested")
                                .build()
                        )
                        .question("urgency", SystemOneQuestion.newScoreBuilder("How urgently does this ticket need a response?")
                                .level("Routine: no time pressure")
                                .level("Soon: a customer is inconvenienced")
                                .level("Immediate: a critical service is unavailable")
                                .build()
                        ))
                .general()
                .execute()
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    try {
                        log.info("SystemOne multi-question response:\n{}",
                                JsonRelatedUtil.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(response)
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    NoulAnswer noulAnswer = response.getAnswer("is_urgent", NoulAnswer.class);
                    assertThat(noulAnswer).isNotNull();
                    assertThat(noulAnswer.getScale()).isGreaterThan(0.5);

                    ScoreAnswer scoreAnswer = response.getAnswer("urgency", ScoreAnswer.class);
                    assertThat(scoreAnswer).isNotNull();
                    assertThat(scoreAnswer.getScore()).isGreaterThanOrEqualTo(0.0);
                    assertThat(scoreAnswer.getLegendProbabilities()).hasSize(3);
                })
                .verifyComplete();
    }
}
