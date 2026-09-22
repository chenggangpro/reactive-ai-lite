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
package pro.chenggang.project.reactive.ai.lite.client.typesafeai.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.client.typesafeai.properties.TypeSafeAiClientProperties;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.defaults.DefaultLlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmSystemOneProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TypeSafeAiLlmClientProviderConfiguration}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
class TypeSafeAiLlmClientProviderConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TypeSafeAiLlmClientProviderConfiguration.class))
            .withUserConfiguration(TestDependencyConfiguration.class);

    @Configuration
    @EnableConfigurationProperties
    static class TestDependencyConfiguration {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }

        @Bean
        LlmProviderInterceptorRegistry llmProviderInterceptorRegistry() {
            return new DefaultLlmProviderInterceptorRegistry(java.util.List.of(), java.util.List.of());
        }
    }

    @Test
    @DisplayName("Auto-configuration creates properties and provider beans by default")
    void testDefaultConfiguration() {
        this.contextRunner
                .withPropertyValues(
                        "reactive.ai.lite.client.typesafeai.certifications[0].profile=default",
                        "reactive.ai.lite.client.typesafeai.certifications[0].token=secret-token",
                        "reactive.ai.lite.client.typesafeai.certifications[0].default=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(TypeSafeAiClientProperties.class);
                    assertThat(context).hasSingleBean(LlmSystemOneProvider.class);

                    LlmSystemOneProvider provider = context.getBean(LlmSystemOneProvider.class);
                    assertThat(provider.info().name()).isEqualTo("TypeSafeAI");
                });
    }

    @Test
    @DisplayName("Auto-configuration skips provider when system-one is disabled")
    void testDisabledConfiguration() {
        this.contextRunner
                .withPropertyValues(
                        "reactive.ai.lite.client.typesafeai.system-one.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(TypeSafeAiClientProperties.class);
                    assertThat(context).doesNotHaveBean(LlmSystemOneProvider.class);
                });
    }

    @Test
    @DisplayName("Auto-configuration binds custom properties correctly")
    void testCustomPropertiesBinding() {
        this.contextRunner
                .withPropertyValues(
                        "reactive.ai.lite.client.typesafeai.base-url=https://custom.api.typesafe.ai",
                        "reactive.ai.lite.client.typesafeai.system-one.endpoint=/v1/custom-endpoint",
                        "reactive.ai.lite.client.typesafeai.certifications[0].profile=prod",
                        "reactive.ai.lite.client.typesafeai.certifications[0].token=prod-token-123",
                        "reactive.ai.lite.client.typesafeai.certifications[0].default=true"
                )
                .run(context -> {
                    TypeSafeAiClientProperties props = context.getBean(TypeSafeAiClientProperties.class);
                    assertThat(props.getBaseUrl()).isEqualTo("https://custom.api.typesafe.ai");
                    assertThat(props.getSystemOne().getEndpoint()).isEqualTo("/v1/custom-endpoint");
                    assertThat(props.getCertifications()).hasSize(1);
                    assertThat(props.getCertifications().getFirst().getProfile()).isEqualTo("prod");
                    assertThat(props.getCertifications().getFirst().getToken()).isEqualTo("prod-token-123");

                    LlmSystemOneProvider provider = context.getBean(LlmSystemOneProvider.class);
                    assertThat(provider.info().baseUrl()).isEqualTo("https://custom.api.typesafe.ai");
                });
    }
}
