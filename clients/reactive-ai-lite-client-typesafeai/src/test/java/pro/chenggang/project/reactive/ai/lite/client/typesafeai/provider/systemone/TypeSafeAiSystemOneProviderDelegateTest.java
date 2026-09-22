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
package pro.chenggang.project.reactive.ai.lite.client.typesafeai.provider.systemone;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmSystemOneRequestData;
import pro.chenggang.project.reactive.ai.lite.core.exception.ResponseMessageExtractFailedException;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.ChoiceQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.NoulQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.ScoreQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestions;
import pro.chenggang.project.reactive.ai.lite.core.option.SystemOneType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TypeSafeAiSystemOneProviderDelegate}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
class TypeSafeAiSystemOneProviderDelegateTest {

    private TypeSafeAiSystemOneProviderDelegate delegate;
    private WebClient.Builder webClientBuilder;

    @BeforeEach
    void setUp() {
        webClientBuilder = WebClient.builder();
        delegate = TypeSafeAiSystemOneProviderDelegate.builder()
                .webClientBuilder(webClientBuilder)
                .baseUrl("https://api.typesafe.ai")
                .systemOneEndpoint("/v1/systemone")
                .isDefault(true)
                .name("TypeSafeAI")
                .certifications(List.of())
                .build();
    }

    @Test
    @DisplayName("Provider info returns configured properties")
    void testProviderInfo() {
        LlmProviderInfo info = delegate.providerInfo();
        assertThat(info).isNotNull();
        assertThat(info.name()).isEqualTo("TypeSafeAI");
        assertThat(info.baseUrl()).isEqualTo("https://api.typesafe.ai");
    }

    @Test
    @DisplayName("ToString includes delegate properties")
    void testToString() {
        String str = delegate.toString();
        assertThat(str).contains("TypeSafeAiSystemOneProviderDelegate", "https://api.typesafe.ai", "/v1/systemone");
    }

    @Test
    @DisplayName("Load request body spec builds POST request with authorization header")
    void testLoadRequestBodySpecWithToken() {
        TokenCertification tokenCert = mock(TokenCertification.class);
        when(tokenCert.token()).thenReturn("test-secret-token");

        NoulQuestion question = SystemOneQuestion.newNoulBuilder("check").build();
        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("jev-1")
                .tokenCertification(tokenCert)
                .state(SystemOneContent.text("sample"))
                .questions(SystemOneQuestions.of("q1", question))
                .build();

        WebClient.RequestBodySpec spec = delegate.loadRequestBodySpec(requestData);
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("Load request body spec without token throws IllegalStateException")
    void testLoadRequestBodySpecWithoutToken() {
        NoulQuestion question = SystemOneQuestion.newNoulBuilder("check").build();
        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("jev-1")
                .state(SystemOneContent.text("sample"))
                .questions(SystemOneQuestions.of("q1", question))
                .build();

        assertThatThrownBy(() -> delegate.loadRequestBodySpec(requestData))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least one token certification is required");
    }

    @Test
    @DisplayName("Initialize request body converts Noul question with criteria correctly")
    void testInitializeRequestBodyNoulWithCriteria() {
        NoulQuestion noulQuestion = SystemOneQuestion.newNoulBuilder("Evaluate if valid")
                .trueOption("Valid content")
                .falseOption("Invalid content")
                .build();

        SystemOneQuestions questions = SystemOneQuestions.of("is_valid", noulQuestion);
        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("jev-1")
                .state(SystemOneContent.text("Input text data"))
                .questions(questions)
                .build();

        ObjectNode body = delegate.initializeRequestBody(requestData);

        assertThat(body.get("model").asText()).isEqualTo("jev-1");
        assertThat(body.get("state").asText()).isEqualTo("Input text data");
        assertThat(body.has("questions")).isTrue();

        ObjectNode qNode = (ObjectNode) body.get("questions").get("is_valid");
        assertThat(qNode.get("type").asText()).isEqualTo("noul");
        assertThat(qNode.get("instructions").asText()).isEqualTo("Evaluate if valid");
        assertThat(qNode.get("criteria").get("true").asText()).isEqualTo("Valid content");
        assertThat(qNode.get("criteria").get("false").asText()).isEqualTo("Invalid content");
    }

    @Test
    @DisplayName("Initialize request body converts Noul question without criteria correctly")
    void testInitializeRequestBodyNoulWithoutCriteria() {
        NoulQuestion noulQuestion = SystemOneQuestion.newNoulBuilder("Evaluate if valid")
                .build();

        SystemOneQuestions questions = SystemOneQuestions.of("is_valid", noulQuestion);
        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("jev-1")
                .state(SystemOneContent.text("Input text data"))
                .questions(questions)
                .build();

        ObjectNode body = delegate.initializeRequestBody(requestData);

        ObjectNode qNode = (ObjectNode) body.get("questions").get("is_valid");
        assertThat(qNode.get("type").asText()).isEqualTo("noul");
        assertThat(qNode.has("criteria")).isFalse();
    }

    @Test
    @DisplayName("Initialize request body converts Choice question correctly")
    void testInitializeRequestBodyChoice() {
        ChoiceQuestion choiceQuestion = SystemOneQuestion.newChoiceBuilder(SystemOneContent.object(Map.of("prompt", "Choose priority")))
                .option("low", "Low urgency")
                .option("high", "High urgency")
                .build();

        SystemOneQuestions questions = SystemOneQuestions.of("priority", choiceQuestion);
        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("jev-1")
                .state(SystemOneContent.object(Map.of("issue", "bug")))
                .questions(questions)
                .build();

        ObjectNode body = delegate.initializeRequestBody(requestData);

        assertThat(body.get("state").get("issue").asText()).isEqualTo("bug");

        ObjectNode qNode = (ObjectNode) body.get("questions").get("priority");
        assertThat(qNode.get("type").asText()).isEqualTo("choice");
        assertThat(qNode.get("instructions").get("prompt").asText()).isEqualTo("Choose priority");
        assertThat(qNode.get("criteria").get("low").asText()).isEqualTo("Low urgency");
        assertThat(qNode.get("criteria").get("high").asText()).isEqualTo("High urgency");
    }

    @Test
    @DisplayName("Initialize request body converts Choice question with simple String options")
    void testInitializeRequestBodyChoiceSimpleOptions() {
        ChoiceQuestion choiceQuestion = SystemOneQuestion.newChoiceBuilder("Pick an animal")
                .option("dog", "dog")
                .option("cat", "cat")
                .option("bird", "bird")
                .build();

        SystemOneQuestions questions = SystemOneQuestions.of("animal", choiceQuestion);
        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("jev-1")
                .state(SystemOneContent.array("element1", "element2"))
                .questions(questions)
                .build();

        ObjectNode body = delegate.initializeRequestBody(requestData);

        assertThat(body.get("state").isArray()).isTrue();

        ObjectNode qNode = (ObjectNode) body.get("questions").get("animal");
        assertThat(qNode.get("type").asText()).isEqualTo("choice");
        assertThat(qNode.get("criteria").get("dog").asText()).isEqualTo("dog");
        assertThat(qNode.get("criteria").get("cat").asText()).isEqualTo("cat");
        assertThat(qNode.get("criteria").get("bird").asText()).isEqualTo("bird");
    }

    @Test
    @DisplayName("Initialize request body converts Score question correctly")
    void testInitializeRequestBodyScore() {
        ScoreQuestion scoreQuestion = SystemOneQuestion.newScoreBuilder("Score satisfaction")
                .levels("Poor", "Average", "Excellent")
                .build();

        SystemOneQuestions questions = SystemOneQuestions.of("satisfaction", scoreQuestion);
        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .modelName("jev-1")
                .state(SystemOneContent.text("review"))
                .questions(questions)
                .build();

        ObjectNode body = delegate.initializeRequestBody(requestData);

        ObjectNode qNode = (ObjectNode) body.get("questions").get("satisfaction");
        assertThat(qNode.get("type").asText()).isEqualTo("score");
        assertThat(qNode.get("criteria").isArray()).isTrue();
        assertThat(qNode.get("criteria").get(0).asText()).isEqualTo("Poor");
        assertThat(qNode.get("criteria").get(1).asText()).isEqualTo("Average");
        assertThat(qNode.get("criteria").get(2).asText()).isEqualTo("Excellent");
    }

    @Test
    @DisplayName("Extract general response successfully parses noul, choice, score answers and usage")
    void testExtractGeneralResponseSuccess() {
        ObjectNode responseJson = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();

        // Usage
        ObjectNode usageNode = responseJson.putObject("usage");
        usageNode.put("input_tokens", 120);
        usageNode.put("output_tokens", 30);
        usageNode.put("total_tokens", 150);

        // Answers
        ObjectNode answersNode = responseJson.putObject("answers");

        // Noul answer
        ObjectNode noulNode = answersNode.putObject("is_spam");
        noulNode.put("type", "noul");
        noulNode.put("noul", 0.95);

        // Choice answer
        ObjectNode choiceNode = answersNode.putObject("ticket_category");
        choiceNode.put("type", "choice");
        choiceNode.put("choice", "billing");
        choiceNode.put("confidence", 0.88);
        ObjectNode choiceProbs = choiceNode.putObject("probabilities");
        choiceProbs.put("billing", 0.88);
        choiceProbs.put("technical", 0.12);

        // Score answer
        ObjectNode scoreNode = answersNode.putObject("satisfaction_score");
        scoreNode.put("type", "score");
        scoreNode.put("score", 2.8);
        scoreNode.put("confidence", 0.91);
        ObjectNode scoreLegend = scoreNode.putObject("legend");
        scoreLegend.put("0", "Poor");
        scoreLegend.put("1", "Average");
        scoreLegend.put("2", "Good");
        ObjectNode scoreProbs = scoreNode.putObject("probabilities");
        scoreProbs.put("0", 0.05);
        scoreProbs.put("1", 0.15);
        scoreProbs.put("2", 0.80);

        RawResponse rawResponse = mock(RawResponse.class);
        ExecutionContext executionContext = ExecutionContext.newContext();
        when(rawResponse.getExecutionContext()).thenReturn(executionContext);
        when(rawResponse.getResponseBody()).thenReturn(responseJson);

        StepVerifier.create(delegate.extractGeneralResponse(rawResponse))
                .assertNext(response -> {
                    assertThat(response.getExecutionContext()).isEqualTo(executionContext);
                    assertThat(response.getRawResponseBody()).isEqualTo(responseJson);

                    // Verify usage
                    assertThat(response.getUsage()).isNotNull();
                    assertThat(response.getUsage().getPromptTokens()).isEqualTo(120);
                    assertThat(response.getUsage().getCompletionTokens()).isEqualTo(30);
                    assertThat(response.getUsage().getTotalTokens()).isEqualTo(150);
                    assertThat(response.getUsage().getRawUsage()).isEqualTo(usageNode);

                    // Verify Noul
                    SystemOneAnswer noulAnswer = response.getAnswer("is_spam");
                    assertThat(noulAnswer).isNotNull();
                    assertThat(noulAnswer.type()).isEqualTo(SystemOneType.NOUL);
                    SystemOneAnswer.NoulAnswer typedNoul = response.getAnswer("is_spam", SystemOneAnswer.NoulAnswer.class);
                    assertThat(typedNoul.getScale()).isEqualTo(0.95);

                    // Verify Choice
                    SystemOneAnswer.ChoiceAnswer typedChoice = response.getAnswer("ticket_category", SystemOneAnswer.ChoiceAnswer.class);
                    assertThat(typedChoice).isNotNull();
                    assertThat(typedChoice.type()).isEqualTo(SystemOneType.CHOICE);
                    assertThat(typedChoice.getChoice()).isEqualTo("billing");
                    assertThat(typedChoice.getConfidence()).isEqualTo(0.88);
                    assertThat(typedChoice.getProbabilities()).hasSize(2);
                    assertThat(typedChoice.getProbabilities().get(0).getKey()).isEqualTo("billing");
                    assertThat(typedChoice.getProbabilities().get(0).getProbability()).isEqualTo(0.88);

                    // Verify Score
                    SystemOneAnswer.ScoreAnswer typedScore = response.getAnswer("satisfaction_score", SystemOneAnswer.ScoreAnswer.class);
                    assertThat(typedScore).isNotNull();
                    assertThat(typedScore.type()).isEqualTo(SystemOneType.SCORE);
                    assertThat(typedScore.getScore()).isEqualTo(2.8);
                    assertThat(typedScore.getConfidence()).isEqualTo(0.91);
                    assertThat(typedScore.getLegendProbabilities()).hasSize(3);
                    assertThat(typedScore.getLegendProbabilities().get(0).getLevel()).isEqualTo("Poor");
                    assertThat(typedScore.getLegendProbabilities().get(0).getProbability()).isEqualTo(0.05);
                    assertThat(typedScore.getLegendProbabilities().get(2).getLevel()).isEqualTo("Good");
                    assertThat(typedScore.getLegendProbabilities().get(2).getProbability()).isEqualTo(0.80);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Extract general response completes empty when responseBody is null")
    void testExtractGeneralResponseNullBody() {
        RawResponse rawResponse = mock(RawResponse.class);
        when(rawResponse.getResponseBody()).thenReturn(null);

        StepVerifier.create(delegate.extractGeneralResponse(rawResponse))
                .verifyComplete();
    }

    @Test
    @DisplayName("Extract general response throws ResponseMessageExtractFailedException when answers node is missing")
    void testExtractGeneralResponseMissingAnswers() {
        ObjectNode responseJson = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        responseJson.put("error", "some error");

        RawResponse rawResponse = mock(RawResponse.class);
        when(rawResponse.getExecutionContext()).thenReturn(ExecutionContext.newContext());
        when(rawResponse.getResponseBody()).thenReturn(responseJson);

        StepVerifier.create(delegate.extractGeneralResponse(rawResponse))
                .expectError(ResponseMessageExtractFailedException.class)
                .verify();
    }

    @Test
    @DisplayName("Test token usage extraction helper methods")
    void testTokenUsageExtractors() {
        ObjectNode usageNode = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        usageNode.put("input_tokens", 50);
        usageNode.put("output_tokens", 25);
        usageNode.put("total_tokens", 85);

        assertThat(delegate.extractPromptTokenUsage(usageNode)).isEqualTo(50);
        assertThat(delegate.extractCompletionTokenUsage(usageNode)).isEqualTo(25);
        assertThat(delegate.extractOtherTokenUsage(usageNode)).isEqualTo(10);

        ObjectNode altUsageNode = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        altUsageNode.put("prompt_tokens", 30);
        altUsageNode.put("completion_tokens", 15);

        assertThat(delegate.extractPromptTokenUsage(altUsageNode)).isEqualTo(30);
        assertThat(delegate.extractCompletionTokenUsage(altUsageNode)).isEqualTo(15);
        assertThat(delegate.extractOtherTokenUsage(altUsageNode)).isEqualTo(0);

        ObjectNode emptyUsageNode = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        assertThat(delegate.extractPromptTokenUsage(emptyUsageNode)).isEqualTo(0);
        assertThat(delegate.extractCompletionTokenUsage(emptyUsageNode)).isEqualTo(0);
        assertThat(delegate.extractOtherTokenUsage(emptyUsageNode)).isEqualTo(0);
    }

    @Test
    @DisplayName("Load request body spec throws IllegalStateException when token certification is missing")
    void testLoadRequestBodySpecMissingCertification() {
        LlmSystemOneRequestData requestData = mock(LlmSystemOneRequestData.class);
        when(requestData.getTokenCertification()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> delegate.loadRequestBodySpec(requestData))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least one token certification is required");
    }

    @Test
    @DisplayName("Extract general response handles alternate usage keys and noul scale key")
    void testExtractGeneralResponseAlternateKeys() {
        ObjectNode responseJson = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();

        ObjectNode usageNode = responseJson.putObject("usage");
        usageNode.put("prompt_tokens", 80);
        usageNode.put("completion_tokens", 20);

        ObjectNode answersNode = responseJson.putObject("answers");
        ObjectNode noulNode = answersNode.putObject("q_scale");
        noulNode.put("type", "noul");
        noulNode.put("scale", 0.75);

        RawResponse rawResponse = mock(RawResponse.class);
        when(rawResponse.getExecutionContext()).thenReturn(ExecutionContext.newContext());
        when(rawResponse.getResponseBody()).thenReturn(responseJson);

        StepVerifier.create(delegate.extractGeneralResponse(rawResponse))
                .assertNext(response -> {
                    assertThat(response.getUsage()).isNotNull();
                    assertThat(response.getUsage().getPromptTokens()).isEqualTo(80);
                    assertThat(response.getUsage().getCompletionTokens()).isEqualTo(20);
                    assertThat(response.getUsage().getTotalTokens()).isEqualTo(100);

                    SystemOneAnswer.NoulAnswer noul = response.getAnswer("q_scale", SystemOneAnswer.NoulAnswer.class);
                    assertThat(noul.getScale()).isEqualTo(0.75);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Extract general response gracefully handles unknown types and non-object nodes")
    void testExtractGeneralResponseUnknownAndMalformedNodes() {
        ObjectNode responseJson = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        ObjectNode answersNode = responseJson.putObject("answers");

        // non-object node
        answersNode.put("malformed", "not-an-object");

        // unknown type
        ObjectNode unknownNode = answersNode.putObject("unknown_q");
        unknownNode.put("type", "unsupported_future_type");

        // valid noul
        ObjectNode noulNode = answersNode.putObject("valid_q");
        noulNode.put("type", "noul");
        noulNode.put("noul", 0.5);

        RawResponse rawResponse = mock(RawResponse.class);
        when(rawResponse.getExecutionContext()).thenReturn(ExecutionContext.newContext());
        when(rawResponse.getResponseBody()).thenReturn(responseJson);

        StepVerifier.create(delegate.extractGeneralResponse(rawResponse))
                .assertNext(response -> {
                    assertThat(response.getAnswers()).hasSize(1);
                    assertThat(response.getAnswer("valid_q")).isNotNull();
                    assertThat(response.getAnswer("malformed")).isNull();
                    assertThat(response.getAnswer("unknown_q")).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Extract general response handles absent usage node")
    void testExtractGeneralResponseWithoutUsage() {
        ObjectNode responseJson = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        ObjectNode answersNode = responseJson.putObject("answers");
        ObjectNode noulNode = answersNode.putObject("check");
        noulNode.put("type", "noul");
        noulNode.put("noul", 0.85);

        RawResponse rawResponse = mock(RawResponse.class);
        when(rawResponse.getExecutionContext()).thenReturn(ExecutionContext.newContext());
        when(rawResponse.getResponseBody()).thenReturn(responseJson);

        StepVerifier.create(delegate.extractGeneralResponse(rawResponse))
                .assertNext(response -> {
                    assertThat(response.getUsage()).isNull();
                    assertThat(response.getAnswer("check")).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Extract general response ensures score legend probabilities index matches index key and level matches legend")
    void testExtractGeneralResponseScoreLegendOrder() {
        ObjectNode responseJson = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        ObjectNode answersNode = responseJson.putObject("answers");
        ObjectNode scoreNode = answersNode.putObject("sentiment");
        scoreNode.put("type", "score");
        scoreNode.put("score", 1.5);
        scoreNode.put("confidence", 0.95);

        // Put legend and probabilities with keys
        ObjectNode legendNode = scoreNode.putObject("legend");
        legendNode.put("2", "Positive");
        legendNode.put("0", "Negative");
        legendNode.put("1", "Neutral");

        ObjectNode probsNode = scoreNode.putObject("probabilities");
        probsNode.put("2", 0.60);
        probsNode.put("0", 0.10);
        probsNode.put("1", 0.30);

        RawResponse rawResponse = mock(RawResponse.class);
        when(rawResponse.getExecutionContext()).thenReturn(ExecutionContext.newContext());
        when(rawResponse.getResponseBody()).thenReturn(responseJson);

        StepVerifier.create(delegate.extractGeneralResponse(rawResponse))
                .assertNext(response -> {
                    SystemOneAnswer.ScoreAnswer scoreAnswer = response.getAnswer("sentiment", SystemOneAnswer.ScoreAnswer.class);
                    assertThat(scoreAnswer).isNotNull();
                    assertThat(scoreAnswer.getScore()).isEqualTo(1.5);
                    assertThat(scoreAnswer.getConfidence()).isEqualTo(0.95);
                    assertThat(scoreAnswer.getLegendProbabilities()).hasSize(3);

                    // Index 0 in list must correspond to index key "0" and level "Negative"
                    assertThat(scoreAnswer.getLegendProbabilities().get(0).getLevel()).isEqualTo("Negative");
                    assertThat(scoreAnswer.getLegendProbabilities().get(0).getProbability()).isEqualTo(0.10);

                    // Index 1 in list must correspond to index key "1" and level "Neutral"
                    assertThat(scoreAnswer.getLegendProbabilities().get(1).getLevel()).isEqualTo("Neutral");
                    assertThat(scoreAnswer.getLegendProbabilities().get(1).getProbability()).isEqualTo(0.30);

                    // Index 2 in list must correspond to index key "2" and level "Positive"
                    assertThat(scoreAnswer.getLegendProbabilities().get(2).getLevel()).isEqualTo("Positive");
                    assertThat(scoreAnswer.getLegendProbabilities().get(2).getProbability()).isEqualTo(0.60);
                })
                .verifyComplete();
    }
}
