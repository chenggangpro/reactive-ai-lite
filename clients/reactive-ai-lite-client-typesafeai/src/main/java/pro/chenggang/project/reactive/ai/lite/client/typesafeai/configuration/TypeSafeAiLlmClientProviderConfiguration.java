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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.client.typesafeai.properties.TypeSafeAiClientProperties;
import pro.chenggang.project.reactive.ai.lite.client.typesafeai.properties.TypeSafeAiClientProperties.SystemOneProperties;
import pro.chenggang.project.reactive.ai.lite.client.typesafeai.provider.TypeSafeAiLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.client.typesafeai.provider.systemone.TypeSafeAiSystemOneProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmSystemOneProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.DefaultLlmSystemOneProvider;

import java.util.List;
import java.util.Objects;

/**
 * Spring Boot auto-configuration for the TypeSafe AI SystemOne client.
 * <p>
 * Binds {@link TypeSafeAiClientProperties} and conditionally instantiates the
 * {@link LlmSystemOneProvider} backed by {@link TypeSafeAiSystemOneProviderDelegate}.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
public class TypeSafeAiLlmClientProviderConfiguration {

    /**
     * Creates and registers the {@link TypeSafeAiClientProperties} configuration bean.
     *
     * @return a new properties holder bound to {@value TypeSafeAiClientProperties#PREFIX}
     */
    @ConfigurationProperties(TypeSafeAiClientProperties.PREFIX)
    @Bean
    public TypeSafeAiClientProperties typeSafeAiClientProperties() {
        return new TypeSafeAiClientProperties();
    }

    /**
     * Conditionally registers an {@link LlmSystemOneProvider} bean for TypeSafe AI
     * when {@code reactive.ai.lite.client.typesafeai.system-one.enabled} is true or absent.
     *
     * @param webClientBuilder               the reactive web client builder
     * @param properties                     the TypeSafe AI client properties
     * @param llmProviderInterceptorRegistry the registry of LLM provider interceptors
     * @return a configured {@link DefaultLlmSystemOneProvider} instance
     */
    @ConditionalOnProperty(name = "reactive.ai.lite.client.typesafeai.system-one.enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public LlmSystemOneProvider typeSafeAiLlmSystemOneProvider(WebClient.Builder webClientBuilder,
                                                              TypeSafeAiClientProperties properties,
                                                              LlmProviderInterceptorRegistry llmProviderInterceptorRegistry) {
        List<TokenCertification> certifications = properties.getCertifications()
                .stream()
                .filter(cert -> Objects.isNull(cert.getCapability()) || Capability.SYSTEM_ONE.equals(cert.getCapability()))
                .<TokenCertification>map(cert -> BearerTokenCertification.builder()
                        .profile(cert.getProfile())
                        .token(cert.getToken())
                        .isDefault(cert.isDefault())
                        .build()
                )
                .toList();
        SystemOneProperties systemOneProperties = properties.getSystemOne();
        TypeSafeAiSystemOneProviderDelegate delegate = TypeSafeAiSystemOneProviderDelegate.builder()
                .name(TypeSafeAiLlmProviderInfo.DEFAULT_NAME)
                .baseUrl(properties.getSystemOneBaseUrl())
                .systemOneEndpoint(systemOneProperties.getEndpoint())
                .webClientBuilder(webClientBuilder)
                .isDefault(systemOneProperties.isDefault())
                .certifications(certifications)
                .supportedModels(systemOneProperties.getLimitedModels())
                .build();
        DefaultLlmSystemOneProvider provider = new DefaultLlmSystemOneProvider(
                delegate,
                certifications,
                llmProviderInterceptorRegistry
        );
        log.info("TypeSafe AI SystemOne provider initialized successfully");
        return provider;
    }
}
