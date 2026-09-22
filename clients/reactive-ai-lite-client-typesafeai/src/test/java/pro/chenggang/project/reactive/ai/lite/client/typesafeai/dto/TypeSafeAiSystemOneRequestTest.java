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
package pro.chenggang.project.reactive.ai.lite.client.typesafeai.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TypeSafeAiSystemOneRequest} and {@link TypeSafeAiQuestion}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
class TypeSafeAiSystemOneRequestTest {

    @Test
    @DisplayName("Serialize request with Noul question without criteria")
    void testSerializeNoulQuestionWithoutCriteria() throws Exception {
        TypeSafeAiQuestion question = TypeSafeAiQuestion.builder()
                .type("noul")
                .instructions("Is this spam?")
                .build();

        TypeSafeAiSystemOneRequest request = TypeSafeAiSystemOneRequest.builder()
                .model("jev-1")
                .state("Subject: Free prize!\nBody: Click here.")
                .questions(Map.of("is_spam", question))
                .build();

        String json = JsonRelatedUtil.OBJECT_MAPPER.writeValueAsString(request);
        JsonNode node = JsonRelatedUtil.OBJECT_MAPPER.readTree(json);

        assertThat(node.get("model").asText()).isEqualTo("jev-1");
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

        TypeSafeAiQuestion question = TypeSafeAiQuestion.builder()
                .type("noul")
                .instructions("Evaluate phishing risk")
                .criteria(criteria)
                .build();

        TypeSafeAiSystemOneRequest request = TypeSafeAiSystemOneRequest.builder()
                .model("jev-1")
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

        TypeSafeAiQuestion question = TypeSafeAiQuestion.builder()
                .type("choice")
                .instructions("Classify the ticket category")
                .criteria(choices)
                .build();

        TypeSafeAiSystemOneRequest request = TypeSafeAiSystemOneRequest.builder()
                .model("jev-1")
                .state(Map.of("message", "My screen is broken"))
                .questions(Map.of("ticket_class", question))
                .build();

        String json = JsonRelatedUtil.OBJECT_MAPPER.writeValueAsString(request);
        JsonNode node = JsonRelatedUtil.OBJECT_MAPPER.readTree(json);

        assertThat(node.get("state").isObject()).isTrue();
        assertThat(node.get("state").get("message").asText()).isEqualTo("My screen is broken");

        JsonNode qNode = node.get("questions").get("ticket_class");
        assertThat(qNode.get("type").asText()).isEqualTo("choice");
        assertThat(qNode.get("criteria").get("refund").asText()).isEqualTo("Customer requests money back");
        assertThat(qNode.get("criteria").get("tech_support").asText()).isEqualTo("Customer has technical problem");
    }

    @Test
    @DisplayName("Serialize request with Score question")
    void testSerializeScoreQuestion() throws Exception {
        List<String> levels = List.of(
                "Level 1: Novice with no prior experience",
                "Level 2: Intermediate with some experience",
                "Level 3: Expert with extensive leadership experience"
        );

        TypeSafeAiQuestion question = TypeSafeAiQuestion.builder()
                .type("score")
                .instructions("Evaluate applicant seniority")
                .criteria(levels)
                .build();

        TypeSafeAiSystemOneRequest request = TypeSafeAiSystemOneRequest.builder()
                .model("jev-1")
                .state(List.of("Resume: 5 years Java development"))
                .questions(Map.of("seniority_level", question))
                .build();

        String json = JsonRelatedUtil.OBJECT_MAPPER.writeValueAsString(request);
        JsonNode node = JsonRelatedUtil.OBJECT_MAPPER.readTree(json);

        assertThat(node.get("state").isArray()).isTrue();
        JsonNode qNode = node.get("questions").get("seniority_level");
        assertThat(qNode.get("type").asText()).isEqualTo("score");
        assertThat(qNode.get("criteria").isArray()).isTrue();
        assertThat(qNode.get("criteria").size()).isEqualTo(3);
        assertThat(qNode.get("criteria").get(0).asText()).isEqualTo("Level 1: Novice with no prior experience");
    }

    @Test
    @DisplayName("Verify equals, hashCode, toString for DTOs")
    void testEqualsAndHashCode() {
        TypeSafeAiQuestion q1 = TypeSafeAiQuestion.builder()
                .type("noul")
                .instructions("test")
                .criteria("crit")
                .build();
        TypeSafeAiQuestion q2 = TypeSafeAiQuestion.builder()
                .type("noul")
                .instructions("test")
                .criteria("crit")
                .build();

        assertThat(q1).isEqualTo(q2);
        assertThat(q1.hashCode()).isEqualTo(q2.hashCode());
        assertThat(q1.toString()).contains("noul", "test");

        TypeSafeAiSystemOneRequest r1 = TypeSafeAiSystemOneRequest.builder()
                .model("m1")
                .state("state1")
                .questions(Map.of("q", q1))
                .build();
        TypeSafeAiSystemOneRequest r2 = TypeSafeAiSystemOneRequest.builder()
                .model("m1")
                .state("state1")
                .questions(Map.of("q", q2))
                .build();

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        assertThat(r1.toString()).contains("m1", "state1");
    }
}
