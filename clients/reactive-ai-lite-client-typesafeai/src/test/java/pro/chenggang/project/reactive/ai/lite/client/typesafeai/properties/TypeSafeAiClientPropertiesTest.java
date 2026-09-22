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
package pro.chenggang.project.reactive.ai.lite.client.typesafeai.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.client.typesafeai.properties.TypeSafeAiClientProperties.TypeSafeAiCertification;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TypeSafeAiClientProperties}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
class TypeSafeAiClientPropertiesTest {

    @Test
    @DisplayName("Default properties are initialized properly")
    void testDefaultProperties() throws Exception {
        TypeSafeAiClientProperties properties = new TypeSafeAiClientProperties();
        assertThat(properties.getBaseUrl()).isEqualTo("https://api.typesafe.ai");
        assertThat(properties.getCertifications()).isEmpty();
        assertThat(properties.getSystemOne()).isNotNull();
        assertThat(properties.getSystemOne().isEnabled()).isTrue();
        assertThat(properties.getSystemOne().getEndpoint()).isEqualTo("/v1/systemone");
        assertThat(properties.getSystemOne().isDefault()).isTrue();

        assertThatCode(properties::afterPropertiesSet).doesNotThrowAnyException();
        assertThat(properties.getSystemOneBaseUrl()).isEqualTo("https://api.typesafe.ai");
    }

    @Test
    @DisplayName("Single certification is automatically marked as default")
    void testSingleCertificationAutoDefault() throws Exception {
        TypeSafeAiClientProperties properties = new TypeSafeAiClientProperties();
        TypeSafeAiCertification cert = new TypeSafeAiCertification();
        cert.setProfile("my-profile");
        cert.setToken("my-token");
        cert.setDefault(false);
        cert.setCapability(Capability.SYSTEM_ONE);

        List<TypeSafeAiCertification> certs = new ArrayList<>();
        certs.add(cert);
        properties.setCertifications(certs);

        properties.afterPropertiesSet();

        assertThat(cert.isDefault()).isTrue();
        assertThat(cert.getCapability()).isEqualTo(Capability.SYSTEM_ONE);
    }

    @Test
    @DisplayName("Multiple certifications with exactly one default succeeds")
    void testMultipleCertificationsValid() throws Exception {
        TypeSafeAiClientProperties properties = new TypeSafeAiClientProperties();

        TypeSafeAiCertification c1 = new TypeSafeAiCertification();
        c1.setProfile("profile-1");
        c1.setToken("token-1");
        c1.setDefault(true);

        TypeSafeAiCertification c2 = new TypeSafeAiCertification();
        c2.setProfile("profile-2");
        c2.setToken("token-2");
        c2.setDefault(false);

        properties.setCertifications(List.of(c1, c2));
        assertThatCode(properties::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Multiple certifications with no default fails validation")
    void testMultipleCertificationsNoDefaultFails() {
        TypeSafeAiClientProperties properties = new TypeSafeAiClientProperties();

        TypeSafeAiCertification c1 = new TypeSafeAiCertification();
        c1.setProfile("profile-1");
        c1.setToken("token-1");
        c1.setDefault(false);

        TypeSafeAiCertification c2 = new TypeSafeAiCertification();
        c2.setProfile("profile-2");
        c2.setToken("token-2");
        c2.setDefault(false);

        properties.setCertifications(List.of(c1, c2));
        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one default TypeSafe AI certification is required");
    }

    @Test
    @DisplayName("Multiple certifications with more than one default fails validation")
    void testMultipleCertificationsMultipleDefaultsFails() {
        TypeSafeAiClientProperties properties = new TypeSafeAiClientProperties();

        TypeSafeAiCertification c1 = new TypeSafeAiCertification();
        c1.setProfile("profile-1");
        c1.setToken("token-1");
        c1.setDefault(true);

        TypeSafeAiCertification c2 = new TypeSafeAiCertification();
        c2.setProfile("profile-2");
        c2.setToken("token-2");
        c2.setDefault(true);

        properties.setCertifications(List.of(c1, c2));
        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only one default TypeSafe AI certification is allowed");
    }

    @Test
    @DisplayName("Duplicate certification profiles fail validation")
    void testDuplicateProfilesFail() {
        TypeSafeAiClientProperties properties = new TypeSafeAiClientProperties();

        TypeSafeAiCertification c1 = new TypeSafeAiCertification();
        c1.setProfile("same-profile");
        c1.setToken("token-1");
        c1.setDefault(true);

        TypeSafeAiCertification c2 = new TypeSafeAiCertification();
        c2.setProfile("same-profile");
        c2.setToken("token-2");
        c2.setDefault(false);

        properties.setCertifications(List.of(c1, c2));
        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("All TypeSafe AI certification profiles must be unique");
    }

    @Test
    @DisplayName("Certification with blank token fails validation")
    void testBlankTokenFails() {
        TypeSafeAiClientProperties properties = new TypeSafeAiClientProperties();

        TypeSafeAiCertification c1 = new TypeSafeAiCertification();
        c1.setProfile("profile-1");
        c1.setToken("   ");
        c1.setDefault(true);

        properties.setCertifications(List.of(c1));
        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The token of TypeSafe AI certification is required");
    }

    @Test
    @DisplayName("Certification with blank profile fails validation")
    void testBlankProfileFails() {
        TypeSafeAiClientProperties properties = new TypeSafeAiClientProperties();

        TypeSafeAiCertification c1 = new TypeSafeAiCertification();
        c1.setProfile("");
        c1.setToken("valid-token");
        c1.setDefault(true);

        properties.setCertifications(List.of(c1));
        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The profile of TypeSafe AI certification is required");
    }

    @Test
    @DisplayName("Blank baseUrl fails validation")
    void testBlankBaseUrlFails() {
        TypeSafeAiClientProperties properties = new TypeSafeAiClientProperties();
        properties.setBaseUrl("");

        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The base-url of TypeSafe AI API is required");
    }

    @Test
    @DisplayName("SystemOne custom base-url override is respected")
    void testSystemOneBaseUrlOverride() {
        TypeSafeAiClientProperties properties = new TypeSafeAiClientProperties();
        properties.setBaseUrl("https://api.typesafe.ai");
        properties.getSystemOne().setBaseUrl("https://custom-systemone.typesafe.ai");

        assertThat(properties.getSystemOneBaseUrl()).isEqualTo("https://custom-systemone.typesafe.ai");

        properties.setSystemOne(null);
        assertThat(properties.getSystemOneBaseUrl()).isEqualTo("https://api.typesafe.ai");
    }

    @Test
    @DisplayName("SystemOne empty endpoint fails validation when enabled")
    void testEmptyEndpointFailsWhenEnabled() {
        TypeSafeAiClientProperties properties = new TypeSafeAiClientProperties();
        properties.getSystemOne().setEndpoint("");

        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The endpoint of TypeSafe AI SystemOne API is required");
    }

    @Test
    @DisplayName("SystemOne empty endpoint passes validation when disabled")
    void testEmptyEndpointIgnoredWhenDisabled() {
        TypeSafeAiClientProperties properties = new TypeSafeAiClientProperties();
        properties.getSystemOne().setEnabled(false);
        properties.getSystemOne().setEndpoint("");

        assertThatCode(properties::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SystemOne limited models configuration")
    void testSystemOneLimitedModels() {
        TypeSafeAiClientProperties properties = new TypeSafeAiClientProperties();
        properties.getSystemOne().setLimitedModels(Set.of("jev-1", "jev-mini"));

        assertThat(properties.getSystemOne().getLimitedModels()).containsExactlyInAnyOrder("jev-1", "jev-mini");
    }
}
