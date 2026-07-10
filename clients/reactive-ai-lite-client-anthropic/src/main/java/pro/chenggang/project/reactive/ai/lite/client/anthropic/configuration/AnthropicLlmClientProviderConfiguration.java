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
package pro.chenggang.project.reactive.ai.lite.client.anthropic.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.properties.AnthropicClientProperties;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.properties.AnthropicClientProperties.ChatProperties;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.provider.AnthropicLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.provider.chat.AnthropicChatProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.DefaultLlmChatProvider;

import java.util.List;

/**
 * Spring Boot auto-configuration for the Anthropic LLM client provider.
 * <p>
 * This configuration is activated automatically and runs after the {@link WebClientAutoConfiguration}
 * to ensure that a reactive {@link WebClient.Builder} is available. It defines two beans:
 * <ul>
 *     <li>{@link AnthropicClientProperties} – bound to the configuration prefix
 *         {@value AnthropicClientProperties#PREFIX}, providing all necessary client and chat settings.</li>
 *     <li>{@link LlmChatProvider} – conditionally created (default: enabled) and pre‑configured
 *         with the Anthropic API version, base URL, certifications, supported models,
 *         and an interceptor registry for request/response processing.</li>
 * </ul>
 * The resulting chat provider delegates to {@link AnthropicChatProviderDelegate} and is wrapped by
 * {@link DefaultLlmChatProvider}, which integrates with the framework’s interceptor mechanism.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see AnthropicClientProperties
 * @see AnthropicChatProviderDelegate
 * @see DefaultLlmChatProvider
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter({WebClientAutoConfiguration.class})
public class AnthropicLlmClientProviderConfiguration {

    /**
     * Exposes the Anthropic client properties as a Spring bean, populated from the
     * {@value AnthropicClientProperties#PREFIX} configuration prefix.
     * <p>
     * These properties include API keys, certifications, base URLs, model restrictions,
     * and the Anthropic API version. They are later consumed when building the
     * {@link AnthropicChatProviderDelegate}.
     * </p>
     *
     * @return a new {@link AnthropicClientProperties} instance bound to the external configuration
     */
    @ConfigurationProperties(AnthropicClientProperties.PREFIX)
    @Bean
    public AnthropicClientProperties AnthropicClientProperties() {
        return new AnthropicClientProperties();
    }

    /**
     * Creates a fully configured {@link LlmChatProvider} for Anthropic, conditionally enabled
     * by the property {@code reactive.ai.lite.client.anthropic.chat.enabled} (default: {@code true}).
     * <p>
     * The construction process:
     * <ol>
     *     <li>Convert the raw certification properties into a list of {@link TokenCertification}
     *         (using {@link BearerTokenCertification}) while preserving profile and default flag.</li>
     *     <li>Build an {@link AnthropicChatProviderDelegate} with:
     *         <ul>
     *             <li>the static provider name {@link AnthropicLlmProviderInfo#DEFAULT_NAME}</li>
     *             <li>the base URL and chat endpoint from the properties</li>
     *             <li>the supplied {@link WebClient.Builder} for reactive HTTP calls</li>
     *             <li>the default flag from the chat properties</li>
     *             <li>the list of certifications</li>
     *             <li>the Anthropic API version (e.g., {@code 2023-06-01})</li>
     *             <li>the limited model set (if configured) to restrict allowed model names</li>
     *         </ul>
     *     </li>
     *     <li>Wrap the delegate together with the certifications and the
     *         {@link LlmProviderInterceptorRegistry} inside a {@link DefaultLlmChatProvider},
     *         which handles interceptor execution and default certification selection.</li>
     * </ol>
     * </p>
     *
     * @param webClientBuilder               the reactive web client builder provided by Spring Boot
     * @param anthropicClientProperties      the Anthropic-specific client configuration
     * @param lLmProviderInterceptorRegistry the global registry of LLM provider interceptors
     * @return a ready-to-use {@link LlmChatProvider} instance
     */
    @ConditionalOnProperty(name = "reactive.ai.lite.client.anthropic.chat.enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public LlmChatProvider anthropicLlmChatProvider(WebClient.Builder webClientBuilder,
                                                    AnthropicClientProperties anthropicClientProperties,
                                                    LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry) {
        List<TokenCertification> certifications = anthropicClientProperties.getCertifications()
                .stream()
                .<TokenCertification>map(certification -> {
                    return BearerTokenCertification.builder()
                            .profile(certification.getProfile())
                            .token(certification.getToken())
                            .isDefault(certification.isDefault())
                            .build();
                })
                .toList();
        ChatProperties chatProperties = anthropicClientProperties.getChat();
        AnthropicChatProviderDelegate delegate = AnthropicChatProviderDelegate.builder()
                .name(AnthropicLlmProviderInfo.DEFAULT_NAME)
                .baseUrL(anthropicClientProperties.getChatBaseUrl())
                .chatCompletionEndpoint(chatProperties.getEndpoint())
                .webClientBuilder(webClientBuilder)
                .isDefault(chatProperties.isDefault())
                .certifications(certifications)
                .apiVersion(anthropicClientProperties.getAnthropicVersion())
                .supportedModels(chatProperties.getLimitedModels())
                .build();
        DefaultLlmChatProvider defaultLlmChatProvider = new DefaultLlmChatProvider(
                delegate,
                certifications,
                lLmProviderInterceptorRegistry
        );
        log.info("Anthropic LLM client provider initialized successfully");
        return defaultLlmChatProvider;
    }
}