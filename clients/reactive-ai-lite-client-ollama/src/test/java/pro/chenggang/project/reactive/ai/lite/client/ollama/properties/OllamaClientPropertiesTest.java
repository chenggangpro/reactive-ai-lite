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
package pro.chenggang.project.reactive.ai.lite.client.ollama.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.client.ollama.properties.OllamaClientProperties.OllamaCertification;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OllamaClientProperties}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
class OllamaClientPropertiesTest {

    @Test
    @DisplayName("Default properties are initialized properly")
    void testDefaultProperties() throws Exception {
        OllamaClientProperties properties = new OllamaClientProperties();
        assertThat(properties.getBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.getCertifications()).isEmpty();
        assertThat(properties.getChat()).isNotNull();
        assertThat(properties.getChat().isEnabled()).isTrue();
        assertThat(properties.getChat().getEndpoint()).isEqualTo("/api/chat");
        assertThat(properties.getEmbedding()).isNotNull();
        assertThat(properties.getEmbedding().isEnabled()).isFalse();
        assertThat(properties.getEmbedding().getEndpoint()).isEqualTo("/api/embed");
        assertThat(properties.getSystemOne()).isNotNull();
        assertThat(properties.getSystemOne().isEnabled()).isFalse();
        assertThat(properties.getSystemOne().getEndpoint()).isEqualTo("/v1/systemone");
        assertThat(properties.getSystemOne().isDefault()).isTrue();

        assertThatCode(properties::afterPropertiesSet).doesNotThrowAnyException();
        assertThat(properties.getChatBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.getEmbeddingBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.getSystemOneBaseUrl()).isEqualTo("http://localhost:11434");
    }

    @Test
    @DisplayName("Single certification is automatically marked as default")
    void testSingleCertificationAutoDefault() throws Exception {
        OllamaClientProperties properties = new OllamaClientProperties();
        OllamaCertification cert = new OllamaCertification();
        cert.setProfile("my-profile");
        cert.setToken("my-token");
        cert.setDefault(false);
        cert.setCapability(Capability.SYSTEM_ONE);

        List<OllamaCertification> certs = new ArrayList<>();
        certs.add(cert);
        properties.setCertifications(certs);

        properties.afterPropertiesSet();

        assertThat(cert.isDefault()).isTrue();
        assertThat(cert.getCapability()).isEqualTo(Capability.SYSTEM_ONE);
    }

    @Test
    @DisplayName("Multiple certifications with exactly one default succeeds")
    void testMultipleCertificationsValid() throws Exception {
        OllamaClientProperties properties = new OllamaClientProperties();

        OllamaCertification c1 = new OllamaCertification();
        c1.setProfile("profile-1");
        c1.setToken("token-1");
        c1.setDefault(true);

        OllamaCertification c2 = new OllamaCertification();
        c2.setProfile("profile-2");
        c2.setToken("token-2");
        c2.setDefault(false);

        properties.setCertifications(List.of(c1, c2));
        assertThatCode(properties::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Multiple certifications with no default fails validation")
    void testMultipleCertificationsNoDefaultFails() {
        OllamaClientProperties properties = new OllamaClientProperties();

        OllamaCertification c1 = new OllamaCertification();
        c1.setProfile("profile-1");
        c1.setToken("token-1");
        c1.setDefault(false);

        OllamaCertification c2 = new OllamaCertification();
        c2.setProfile("profile-2");
        c2.setToken("token-2");
        c2.setDefault(false);

        properties.setCertifications(List.of(c1, c2));
        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one default Ollama certification is required");
    }

    @Test
    @DisplayName("Multiple certifications with more than one default fails validation")
    void testMultipleCertificationsMultipleDefaultsFails() {
        OllamaClientProperties properties = new OllamaClientProperties();

        OllamaCertification c1 = new OllamaCertification();
        c1.setProfile("profile-1");
        c1.setToken("token-1");
        c1.setDefault(true);

        OllamaCertification c2 = new OllamaCertification();
        c2.setProfile("profile-2");
        c2.setToken("token-2");
        c2.setDefault(true);

        properties.setCertifications(List.of(c1, c2));
        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only one default Ollama certification is allowed");
    }

    @Test
    @DisplayName("Duplicate certification profiles fail validation")
    void testDuplicateProfilesFail() {
        OllamaClientProperties properties = new OllamaClientProperties();

        OllamaCertification c1 = new OllamaCertification();
        c1.setProfile("same-profile");
        c1.setToken("token-1");
        c1.setDefault(true);

        OllamaCertification c2 = new OllamaCertification();
        c2.setProfile("same-profile");
        c2.setToken("token-2");
        c2.setDefault(false);

        properties.setCertifications(List.of(c1, c2));
        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("All Ollama certification profiles must be unique");
    }

    @Test
    @DisplayName("Certification with blank token fails validation")
    void testBlankTokenFails() {
        OllamaClientProperties properties = new OllamaClientProperties();

        OllamaCertification c1 = new OllamaCertification();
        c1.setProfile("profile-1");
        c1.setToken("   ");
        c1.setDefault(true);

        properties.setCertifications(List.of(c1));
        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The token of Ollama certification is required");
    }

    @Test
    @DisplayName("Certification with blank profile fails validation")
    void testBlankProfileFails() {
        OllamaClientProperties properties = new OllamaClientProperties();

        OllamaCertification c1 = new OllamaCertification();
        c1.setProfile("");
        c1.setToken("valid-token");
        c1.setDefault(true);

        properties.setCertifications(List.of(c1));
        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The profile of Ollama certification is required");
    }

    @Test
    @DisplayName("Blank baseUrl fails validation")
    void testBlankBaseUrlFails() {
        OllamaClientProperties properties = new OllamaClientProperties();
        properties.setBaseUrl("");

        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The base-url of Ollama API is required");
    }

    @Test
    @DisplayName("Chat, embedding, and system-one custom base-url overrides are respected")
    void testBaseUrlOverrides() {
        OllamaClientProperties properties = new OllamaClientProperties();
        properties.setBaseUrl("http://localhost:11434");
        properties.getChat().setBaseUrl("http://custom-chat:11434");
        properties.getEmbedding().setBaseUrl("http://custom-embed:11434");
        properties.getSystemOne().setBaseUrl("http://custom-systemone:11434");

        assertThat(properties.getChatBaseUrl()).isEqualTo("http://custom-chat:11434");
        assertThat(properties.getEmbeddingBaseUrl()).isEqualTo("http://custom-embed:11434");
        assertThat(properties.getSystemOneBaseUrl()).isEqualTo("http://custom-systemone:11434");

        properties.setChat(null);
        properties.setEmbedding(null);
        properties.setSystemOne(null);
        assertThat(properties.getChatBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.getEmbeddingBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.getSystemOneBaseUrl()).isEqualTo("http://localhost:11434");
    }

    @Test
    @DisplayName("SystemOne empty endpoint fails validation when enabled")
    void testSystemOneEmptyEndpointFailsWhenEnabled() {
        OllamaClientProperties properties = new OllamaClientProperties();
        properties.getSystemOne().setEnabled(true);
        properties.getSystemOne().setEndpoint("");

        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The endpoint of Ollama SystemOne API is required");
    }

    @Test
    @DisplayName("SystemOne empty endpoint passes validation when disabled")
    void testSystemOneEmptyEndpointIgnoredWhenDisabled() {
        OllamaClientProperties properties = new OllamaClientProperties();
        properties.getSystemOne().setEnabled(false);
        properties.getSystemOne().setEndpoint("");

        assertThatCode(properties::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Chat empty endpoint fails validation when enabled")
    void testChatEmptyEndpointFailsWhenEnabled() {
        OllamaClientProperties properties = new OllamaClientProperties();
        properties.getChat().setEnabled(true);
        properties.getChat().setEndpoint("");

        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The endpoint of Ollama chat API is required");
    }

    @Test
    @DisplayName("Embedding empty endpoint fails validation when enabled")
    void testEmbeddingEmptyEndpointFailsWhenEnabled() {
        OllamaClientProperties properties = new OllamaClientProperties();
        properties.getEmbedding().setEnabled(true);
        properties.getEmbedding().setEndpoint("");

        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The endpoint of Ollama embedding API is required");
    }

    @Test
    @DisplayName("SystemOne limited models and flags configuration")
    void testSystemOneLimitedModels() {
        OllamaClientProperties properties = new OllamaClientProperties();
        properties.getSystemOne().setLimitedModels(Set.of("clef:latest", "nimble"));
        properties.getSystemOne().setDefault(true);

        assertThat(properties.getSystemOne().getLimitedModels()).containsExactlyInAnyOrder("clef:latest", "nimble");
        assertThat(properties.getSystemOne().isDefault()).isTrue();
    }
}
