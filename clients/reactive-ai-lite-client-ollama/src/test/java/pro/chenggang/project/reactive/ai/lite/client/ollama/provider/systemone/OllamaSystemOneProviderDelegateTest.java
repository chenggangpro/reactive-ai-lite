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
package pro.chenggang.project.reactive.ai.lite.client.ollama.provider.systemone;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.client.ollama.provider.OllamaLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.UriTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmSystemOneRequestData;
import pro.chenggang.project.reactive.ai.lite.core.exception.ResponseMessageExtractFailedException;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Base64Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.ChoiceQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.NoulQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.ScoreQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestions;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OllamaSystemOneProviderDelegate}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
class OllamaSystemOneProviderDelegateTest {

    private OllamaSystemOneProviderDelegate delegate;

    @BeforeEach
    void setUp() {
        TokenCertification cert = BearerTokenCertification.builder()
                .profile("default")
                .token("test-token")
                .isDefault(true)
                .build();

        delegate = OllamaSystemOneProviderDelegate.builder()
                .webClientBuilder(WebClient.builder())
                .baseUrl("http://localhost:11434")
                .systemOneEndpoint("/v1/systemone")
                .isDefault(true)
                .name(OllamaLlmProviderInfo.DEFAULT_NAME)
                .supportedModels(Set.of("clef:latest", "nimble"))
                .certifications(List.of(cert))
                .build();
    }

    @Test
    @DisplayName("Provider info and toString metadata check")
    void testProviderInfo() {
        assertThat(delegate.providerInfo()).isNotNull();
        assertThat(delegate.providerInfo().name()).isEqualTo(OllamaLlmProviderInfo.DEFAULT_NAME);
        assertThat(delegate.providerInfo().baseUrl()).isEqualTo("http://localhost:11434");
        assertThat(delegate.providerInfo().endpoint()).isEqualTo("/v1/systemone");
        assertThat(delegate.providerInfo().supportModel("clef:latest")).isTrue();
        assertThat(delegate.providerInfo().supportModel("nimble")).isTrue();
        assertThat(delegate.providerInfo().supportModel("unknown")).isFalse();
        assertThat(delegate.providerInfo().isDefault()).isTrue();
        assertThat(delegate.toString()).contains("OllamaSystemOneProviderDelegate");
    }

    @Test
    @DisplayName("checkTokenCertification is no-op for local Ollama without tokens")
    void testCheckTokenCertificationNoOp() {
        NoulQuestion noulQuestion = SystemOneQuestion.newNoulBuilder("Is input valid?").build();
        SystemOneQuestions questions = SystemOneQuestions.of("q1", noulQuestion);

        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("clef:latest")
                .state(SystemOneContent.text("text"))
                .questions(questions)
                .build();

        assertThatCode(() -> delegate.checkTokenCertification(requestData))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("loadRequestBodySpec configures request spec without token")
    void testLoadRequestBodySpecWithoutToken() {
        NoulQuestion noulQuestion = SystemOneQuestion.newNoulBuilder("Is input valid?").build();
        SystemOneQuestions questions = SystemOneQuestions.of("q1", noulQuestion);

        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("clef:latest")
                .state(SystemOneContent.text("text"))
                .questions(questions)
                .build();

        WebClient.RequestBodySpec spec = delegate.loadRequestBodySpec(requestData);
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("loadRequestBodySpec applies Bearer and Uri token certifications")
    void testLoadRequestBodySpecWithTokens() {
        NoulQuestion noulQuestion = SystemOneQuestion.newNoulBuilder("Is input valid?").build();
        SystemOneQuestions questions = SystemOneQuestions.of("q1", noulQuestion);

        TokenCertification bearerCert = BearerTokenCertification.builder()
                .profile("bearer-profile")
                .token("bearer-token-123")
                .build();

        LlmSystemOneRequestData bearerData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("clef:latest")
                .tokenCertification(bearerCert)
                .state(SystemOneContent.text("text"))
                .questions(questions)
                .build();

        assertThat(delegate.loadRequestBodySpec(bearerData)).isNotNull();

        TokenCertification uriCert = UriTokenCertification.builder()
                .profile("uri-profile")
                .name("api_key")
                .token("secret-key")
                .isDefault(false)
                .build();

        LlmSystemOneRequestData uriData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("clef:latest")
                .tokenCertification(uriCert)
                .state(SystemOneContent.text("text"))
                .questions(questions)
                .build();

        assertThat(delegate.loadRequestBodySpec(uriData)).isNotNull();
    }

    @Test
    @DisplayName("Initialize request body with text state and Noul question with criteria")
    void testInitializeRequestBodyNoulQuestion() {
        NoulQuestion noulQuestion = SystemOneQuestion.newNoulBuilder("Is customer happy?")
                .trueOption(SystemOneContent.text("Customer expresses joy"))
                .falseOption(SystemOneContent.text("Customer expresses dissatisfaction"))
                .build();

        SystemOneQuestions questions = SystemOneQuestions.of("is_happy", noulQuestion);

        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("clef:latest")
                .state(SystemOneContent.text("I love your product!"))
                .questions(questions)
                .build();

        ObjectNode body = delegate.initializeRequestBody(requestData);

        assertThat(body.get("model").asText()).isEqualTo("clef:latest");
        assertThat(body.get("state").asText()).isEqualTo("I love your product!");
        assertThat(body.has("questions")).isTrue();

        ObjectNode qNode = (ObjectNode) body.get("questions").get("is_happy");
        assertThat(qNode.get("type").asText()).isEqualTo("noul");
        assertThat(qNode.get("instructions").asText()).isEqualTo("Is customer happy?");
        assertThat(qNode.get("criteria").get("true").asText()).isEqualTo("Customer expresses joy");
        assertThat(qNode.get("criteria").get("false").asText()).isEqualTo("Customer expresses dissatisfaction");
    }

    @Test
    @DisplayName("Initialize request body with Choice question")
    void testInitializeRequestBodyChoiceQuestion() {
        ChoiceQuestion choiceQuestion = SystemOneQuestion.newChoiceBuilder("Classify ticket")
                .option("billing", "Payment issue")
                .option("technical", "Bug or crash")
                .build();

        SystemOneQuestions questions = SystemOneQuestions.of("category", choiceQuestion);

        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("clef:latest")
                .state(SystemOneContent.text("The payment gateway failed."))
                .questions(questions)
                .build();

        ObjectNode body = delegate.initializeRequestBody(requestData);

        ObjectNode qNode = (ObjectNode) body.get("questions").get("category");
        assertThat(qNode.get("type").asText()).isEqualTo("choice");
        assertThat(qNode.get("criteria").get("billing").asText()).isEqualTo("Payment issue");
        assertThat(qNode.get("criteria").get("technical").asText()).isEqualTo("Bug or crash");
    }

    @Test
    @DisplayName("Initialize request body with Score question")
    void testInitializeRequestBodyScoreQuestion() {
        ScoreQuestion scoreQuestion = SystemOneQuestion.newScoreBuilder("Rate satisfaction")
                .level("Terrible")
                .level("Neutral")
                .level("Excellent")
                .build();

        SystemOneQuestions questions = SystemOneQuestions.of("satisfaction", scoreQuestion);

        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("clef:latest")
                .state(SystemOneContent.text("Great support!"))
                .questions(questions)
                .build();

        ObjectNode body = delegate.initializeRequestBody(requestData);

        ObjectNode qNode = (ObjectNode) body.get("questions").get("satisfaction");
        assertThat(qNode.get("type").asText()).isEqualTo("score");
        assertThat(qNode.get("criteria").isArray()).isTrue();
        assertThat(qNode.get("criteria").get(0).asText()).isEqualTo("Terrible");
        assertThat(qNode.get("criteria").get(1).asText()).isEqualTo("Neutral");
        assertThat(qNode.get("criteria").get(2).asText()).isEqualTo("Excellent");
    }

    @Test
    @DisplayName("Initialize request body serializes valid images")
    void testInitializeRequestBodyWithImages() {
        NoulQuestion noulQuestion = SystemOneQuestion.newNoulBuilder("Check image content").build();
        SystemOneQuestions questions = SystemOneQuestions.of("is_valid", noulQuestion);

        Base64Attachment pngImage = Base64Attachment.builder()
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .name("test.png")
                .base64Content("aGVsbG8=")
                .build();

        Base64Attachment jpegImage = Base64Attachment.builder()
                .mimeType(MimeTypeUtils.IMAGE_JPEG)
                .name("test.jpg")
                .base64Content("d29ybGQ=")
                .build();

        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("clef:latest")
                .state(SystemOneContent.text("image context"))
                .images(List.of(pngImage, jpegImage))
                .questions(questions)
                .build();

        ObjectNode body = delegate.initializeRequestBody(requestData);

        assertThat(body.has("images")).isTrue();
        assertThat(body.get("images").isArray()).isTrue();
        assertThat(body.get("images").size()).isEqualTo(2);
        assertThat(body.get("images").get(0).asText()).isEqualTo("aGVsbG8=");
        assertThat(body.get("images").get(1).asText()).isEqualTo("d29ybGQ=");
    }

    @Test
    @DisplayName("Initialize request body rejects unsupported image MIME types")
    void testInitializeRequestBodyWithInvalidImages() {
        NoulQuestion noulQuestion = SystemOneQuestion.newNoulBuilder("Check image content").build();
        SystemOneQuestions questions = SystemOneQuestions.of("is_valid", noulQuestion);

        Base64Attachment gifImage = Base64Attachment.builder()
                .mimeType(MimeTypeUtils.IMAGE_GIF)
                .name("test.gif")
                .base64Content("Z2lm")
                .build();

        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("clef:latest")
                .state(SystemOneContent.text("gif context"))
                .images(List.of(gifImage))
                .questions(questions)
                .build();

        assertThatThrownBy(() -> delegate.initializeRequestBody(requestData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported image MIME type");
    }

    @Test
    @DisplayName("Extract general response successfully parses noul, choice, score answers and usage")
    void testExtractGeneralResponseSuccess() throws Exception {
        String jsonResponse = """
                {
                  "model": "clef:latest",
                  "answers": {
                    "is_urgent": {
                      "type": "noul",
                      "noul": 0.95
                    },
                    "department": {
                      "type": "choice",
                      "choice": "billing",
                      "confidence": 0.88,
                      "probabilities": {
                        "technical": 0.08,
                        "billing": 0.92,
                        "sales": 0.0
                      }
                    },
                    "frustration": {
                      "type": "score",
                      "score": 1.05,
                      "confidence": 0.92,
                      "legend": {
                        "0": "Calm",
                        "1": "Frustrated",
                        "2": "Very angry"
                      },
                      "probabilities": {
                        "0": 0.0,
                        "1": 0.95,
                        "2": 0.05
                      }
                    }
                  },
                  "usage": {
                    "input_tokens": 426,
                    "output_tokens": 73
                  }
                }
                """;

        ObjectNode responseBodyNode = (ObjectNode) JsonRelatedUtil.OBJECT_MAPPER.readTree(jsonResponse);
        ExecutionContext ctx = ExecutionContext.newContext();

        RawResponse rawResponse = org.mockito.Mockito.mock(RawResponse.class);
        org.mockito.Mockito.when(rawResponse.getExecutionContext()).thenReturn(ctx);
        org.mockito.Mockito.when(rawResponse.getResponseBody()).thenReturn(responseBodyNode);

        StepVerifier.create(delegate.extractGeneralResponse(rawResponse))
                .assertNext(response -> {
                    assertThat(response.getExecutionContext()).isEqualTo(ctx);
                    assertThat(response.getRawResponseBody()).isEqualTo(responseBodyNode);

                    assertThat(response.getUsage()).isNotNull();
                    assertThat(response.getUsage().getPromptTokens()).isEqualTo(426);
                    assertThat(response.getUsage().getCompletionTokens()).isEqualTo(73);

                    Map<String, SystemOneAnswer> answers = response.getAnswers();
                    assertThat(answers).hasSize(3);

                    // Noul
                    assertThat(answers.get("is_urgent")).isInstanceOf(SystemOneAnswer.NoulAnswer.class);
                    SystemOneAnswer.NoulAnswer noulAnswer = (SystemOneAnswer.NoulAnswer) answers.get("is_urgent");
                    assertThat(noulAnswer.getScale()).isEqualTo(0.95);

                    // Choice
                    assertThat(answers.get("department")).isInstanceOf(SystemOneAnswer.ChoiceAnswer.class);
                    SystemOneAnswer.ChoiceAnswer choiceAnswer = (SystemOneAnswer.ChoiceAnswer) answers.get("department");
                    assertThat(choiceAnswer.getChoice()).isEqualTo("billing");
                    assertThat(choiceAnswer.getConfidence()).isEqualTo(0.88);
                    assertThat(choiceAnswer.getProbabilities()).hasSize(3);

                    // Score
                    assertThat(answers.get("frustration")).isInstanceOf(SystemOneAnswer.ScoreAnswer.class);
                    SystemOneAnswer.ScoreAnswer scoreAnswer = (SystemOneAnswer.ScoreAnswer) answers.get("frustration");
                    assertThat(scoreAnswer.getScore()).isEqualTo(1.05);
                    assertThat(scoreAnswer.getConfidence()).isEqualTo(0.92);
                    assertThat(scoreAnswer.getLegendProbabilities()).hasSize(3);
                    assertThat(scoreAnswer.getLegendProbabilities().get(0).getLevel()).isEqualTo("Calm");
                    assertThat(scoreAnswer.getLegendProbabilities().get(0).getProbability()).isEqualTo(0.0);
                    assertThat(scoreAnswer.getLegendProbabilities().get(1).getLevel()).isEqualTo("Frustrated");
                    assertThat(scoreAnswer.getLegendProbabilities().get(1).getProbability()).isEqualTo(0.95);
                    assertThat(scoreAnswer.getLegendProbabilities().get(2).getLevel()).isEqualTo("Very angry");
                    assertThat(scoreAnswer.getLegendProbabilities().get(2).getProbability()).isEqualTo(0.05);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Extract general response fails when answers node is missing")
    void testExtractGeneralResponseMissingAnswers() throws Exception {
        String jsonResponse = """
                {
                  "model": "clef:latest",
                  "usage": {
                    "input_tokens": 10,
                    "output_tokens": 5
                  }
                }
                """;

        ObjectNode responseBodyNode = (ObjectNode) JsonRelatedUtil.OBJECT_MAPPER.readTree(jsonResponse);
        RawResponse rawResponse = org.mockito.Mockito.mock(RawResponse.class);
        org.mockito.Mockito.when(rawResponse.getExecutionContext()).thenReturn(ExecutionContext.newContext());
        org.mockito.Mockito.when(rawResponse.getResponseBody()).thenReturn(responseBodyNode);

        StepVerifier.create(delegate.extractGeneralResponse(rawResponse))
                .expectError(ResponseMessageExtractFailedException.class)
                .verify();
    }
}
