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
package pro.chenggang.project.reactive.ai.lite.client.ollama.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OllamaSystemOneRequest} and {@link OllamaSystemOneQuestion}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
class OllamaSystemOneRequestTest {

    @Test
    @DisplayName("Serialize request with Noul question without criteria")
    void testSerializeNoulQuestionWithoutCriteria() throws Exception {
        OllamaSystemOneQuestion question = OllamaSystemOneQuestion.builder()
                .type("noul")
                .instructions("Is this spam?")
                .build();

        OllamaSystemOneRequest request = OllamaSystemOneRequest.builder()
                .model("clef:latest")
                .state("Subject: Free prize!\nBody: Click here.")
                .questions(Map.of("is_spam", question))
                .build();

        String json = JsonRelatedUtil.OBJECT_MAPPER.writeValueAsString(request);
        JsonNode node = JsonRelatedUtil.OBJECT_MAPPER.readTree(json);

        assertThat(node.get("model").asText()).isEqualTo("clef:latest");
        assertThat(node.get("state").asText()).isEqualTo("Subject: Free prize!\nBody: Click here.");
        assertThat(node.get("questions").has("is_spam")).isTrue();

        JsonNode qNode = node.get("questions").get("is_spam");
        assertThat(qNode.get("type").asText()).isEqualTo("noul");
        assertThat(qNode.get("instructions").asText()).isEqualTo("Is this spam?");
        assertThat(qNode.has("criteria")).isFalse();
    }

    @Test
    @DisplayName("Serialize request with Noul question with criteria")
    void testSerializeNoulQuestionWithCriteria() throws Exception {
        Map<String, String> criteria = Map.of(
                "true", "The text clearly attempts phishing or scams",
                "false", "The text appears legitimate"
        );

        OllamaSystemOneQuestion question = OllamaSystemOneQuestion.builder()
                .type("noul")
                .instructions("Evaluate phishing risk")
                .criteria(criteria)
                .build();

        OllamaSystemOneRequest request = OllamaSystemOneRequest.builder()
                .model("clef:latest")
                .state("Sample text")
                .questions(Map.of("phishing_check", question))
                .build();

        String json = JsonRelatedUtil.OBJECT_MAPPER.writeValueAsString(request);
        JsonNode node = JsonRelatedUtil.OBJECT_MAPPER.readTree(json);

        JsonNode criteriaNode = node.get("questions").get("phishing_check").get("criteria");
        assertThat(criteriaNode.isObject()).isTrue();
        assertThat(criteriaNode.get("true").asText()).isEqualTo("The text clearly attempts phishing or scams");
        assertThat(criteriaNode.get("false").asText()).isEqualTo("The text appears legitimate");
    }

    @Test
    @DisplayName("Serialize request with Choice question")
    void testSerializeChoiceQuestion() throws Exception {
        Map<String, String> choices = new LinkedHashMap<>();
        choices.put("refund", "Customer requests money back");
        choices.put("tech_support", "Customer has technical problem");
        choices.put("general", "General inquiry");

        OllamaSystemOneQuestion question = OllamaSystemOneQuestion.builder()
                .type("choice")
                .instructions("Classify the ticket category")
                .criteria(choices)
                .build();

        OllamaSystemOneRequest request = OllamaSystemOneRequest.builder()
                .model("clef:latest")
                .state("Can I get my payment back?")
                .questions(Map.of("category", question))
                .build();

        String json = JsonRelatedUtil.OBJECT_MAPPER.writeValueAsString(request);
        JsonNode node = JsonRelatedUtil.OBJECT_MAPPER.readTree(json);

        JsonNode qNode = node.get("questions").get("category");
        assertThat(qNode.get("type").asText()).isEqualTo("choice");
        JsonNode criteriaNode = qNode.get("criteria");
        assertThat(criteriaNode.isObject()).isTrue();
        assertThat(criteriaNode.get("refund").asText()).isEqualTo("Customer requests money back");
        assertThat(criteriaNode.get("tech_support").asText()).isEqualTo("Customer has technical problem");
        assertThat(criteriaNode.get("general").asText()).isEqualTo("General inquiry");
    }

    @Test
    @DisplayName("Serialize request with Score question")
    void testSerializeScoreQuestion() throws Exception {
        List<String> levels = List.of("Poor", "Fair", "Good", "Excellent");

        OllamaSystemOneQuestion question = OllamaSystemOneQuestion.builder()
                .type("score")
                .instructions("Rate the quality of the response")
                .criteria(levels)
                .build();

        OllamaSystemOneRequest request = OllamaSystemOneRequest.builder()
                .model("clef:latest")
                .state("Detailed and helpful answer.")
                .questions(Map.of("quality", question))
                .build();

        String json = JsonRelatedUtil.OBJECT_MAPPER.writeValueAsString(request);
        JsonNode node = JsonRelatedUtil.OBJECT_MAPPER.readTree(json);

        JsonNode qNode = node.get("questions").get("quality");
        assertThat(qNode.get("type").asText()).isEqualTo("score");
        JsonNode criteriaNode = qNode.get("criteria");
        assertThat(criteriaNode.isArray()).isTrue();
        assertThat(criteriaNode.size()).isEqualTo(4);
        assertThat(criteriaNode.get(0).asText()).isEqualTo("Poor");
        assertThat(criteriaNode.get(3).asText()).isEqualTo("Excellent");
    }

    @Test
    @DisplayName("Serialize request with structured JSON Object state")
    void testSerializeStructuredStateMap() throws Exception {
        Map<String, Object> stateMap = Map.of(
                "action", "refund",
                "amount", 100.5,
                "user_id", "u-12345"
        );

        OllamaSystemOneQuestion question = OllamaSystemOneQuestion.builder()
                .type("noul")
                .instructions("Is the amount above threshold?")
                .build();

        OllamaSystemOneRequest request = OllamaSystemOneRequest.builder()
                .model("clef:latest")
                .state(stateMap)
                .questions(Map.of("is_high_value", question))
                .build();

        String json = JsonRelatedUtil.OBJECT_MAPPER.writeValueAsString(request);
        JsonNode node = JsonRelatedUtil.OBJECT_MAPPER.readTree(json);

        JsonNode stateNode = node.get("state");
        assertThat(stateNode.isObject()).isTrue();
        assertThat(stateNode.get("action").asText()).isEqualTo("refund");
        assertThat(stateNode.get("amount").asDouble()).isEqualTo(100.5);
        assertThat(stateNode.get("user_id").asText()).isEqualTo("u-12345");
    }

    @Test
    @DisplayName("Serialize request with structured List state")
    void testSerializeStructuredStateList() throws Exception {
        List<String> logEntries = List.of(
                "INFO: User login success",
                "WARN: High CPU utilization",
                "ERROR: Database connection timeout"
        );

        OllamaSystemOneQuestion question = OllamaSystemOneQuestion.builder()
                .type("noul")
                .instructions("Does this log contain critical errors?")
                .build();

        OllamaSystemOneRequest request = OllamaSystemOneRequest.builder()
                .model("clef:latest")
                .state(logEntries)
                .questions(Map.of("has_error", question))
                .build();

        String json = JsonRelatedUtil.OBJECT_MAPPER.writeValueAsString(request);
        JsonNode node = JsonRelatedUtil.OBJECT_MAPPER.readTree(json);

        JsonNode stateNode = node.get("state");
        assertThat(stateNode.isArray()).isTrue();
        assertThat(stateNode.size()).isEqualTo(3);
        assertThat(stateNode.get(2).asText()).isEqualTo("ERROR: Database connection timeout");
    }

    @Test
    @DisplayName("Serialize request with images and keep_alive")
    void testSerializeWithImagesAndKeepAlive() throws Exception {
        OllamaSystemOneQuestion question = OllamaSystemOneQuestion.builder()
                .type("noul")
                .instructions("Does this image contain text?")
                .build();

        OllamaSystemOneRequest request = OllamaSystemOneRequest.builder()
                .model("clef:latest")
                .state("User image inspection")
                .images(List.of("aGVsbG8=", "d29ybGQ="))
                .questions(Map.of("has_text", question))
                .keepAlive("5m")
                .build();

        String json = JsonRelatedUtil.OBJECT_MAPPER.writeValueAsString(request);
        JsonNode node = JsonRelatedUtil.OBJECT_MAPPER.readTree(json);

        assertThat(node.has("images")).isTrue();
        assertThat(node.get("images").isArray()).isTrue();
        assertThat(node.get("images").get(0).asText()).isEqualTo("aGVsbG8=");
        assertThat(node.get("images").get(1).asText()).isEqualTo("d29ybGQ=");
        assertThat(node.get("keep_alive").asText()).isEqualTo("5m");
    }

    @Test
    @DisplayName("Deserialize OllamaSystemOneRequest from JSON")
    void testDeserialize() throws Exception {
        String json = """
                {
                  "model": "clef:latest",
                  "state": "The user is asking for assistance.",
                  "images": ["aGVsbG8="],
                  "questions": {
                    "q1": {
                      "type": "noul",
                      "instructions": "Is polite?"
                    }
                  },
                  "keep_alive": "10m"
                }
                """;

        OllamaSystemOneRequest request = JsonRelatedUtil.OBJECT_MAPPER.readValue(json, OllamaSystemOneRequest.class);
        assertThat(request.getModel()).isEqualTo("clef:latest");
        assertThat(request.getState()).isEqualTo("The user is asking for assistance.");
        assertThat(request.getImages()).containsExactly("aGVsbG8=");
        assertThat(request.getKeepAlive()).isEqualTo("10m");
        assertThat(request.getQuestions()).containsKey("q1");
        assertThat(request.getQuestions().get("q1").getType()).isEqualTo("noul");
        assertThat(request.getQuestions().get("q1").getInstructions()).isEqualTo("Is polite?");
    }
}
