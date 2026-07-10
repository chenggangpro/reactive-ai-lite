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
package pro.chenggang.project.reactive.ai.lite.client.deepseek.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.properties.DeepseekClientProperties;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.properties.DeepseekClientProperties.ChatProperties;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.provider.DeepseekLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.provider.chat.DeepseekChatProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.DefaultLlmChatProvider;

import java.util.List;

/**
 * Spring auto-configuration for the Deepseek LLM provider client.
 * <p>
 * This configuration class automatically registers beans required to interact with
 * the Deepseek chat API, including:
 * <ul>
 *   <li>{@link DeepseekClientProperties} to externalize client configuration
 *       under the {@value DeepseekClientProperties#PREFIX} property prefix.</li>
 *   <li>{@link LlmChatProvider} instance (via {@link DefaultLlmChatProvider}) that
 *       delegates to the Deepseek chat completion endpoint, using bearer token
 *       certifications and optional provider interceptors.</li>
 * </ul>
 * The chat provider bean is conditionally created only when the property
 * {@code reactive.ai.lite.client.deepseek.chat.enabled} is {@code true} (default).
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
@AutoConfiguration
public class DeepseekLlmClientProviderConfiguration {

    /**
     * Creates a {@link DeepseekClientProperties} bean backed by configuration properties
     * prefixed with {@value DeepseekClientProperties#PREFIX}.
     * <p>
     * This binds all properties under the prefix (e.g., certifications, chat settings)
     * from Spring's environment to allow customization of the Deepseek client's behavior
     * without code changes.
     *
     * @return a new properties holder initialized from external configuration
     */
    @ConfigurationProperties(DeepseekClientProperties.PREFIX)
    @Bean
    public DeepseekClientProperties deepseekClientProperties() {
        return new DeepseekClientProperties();
    }

    /**
     * Constructs the Deepseek {@link LlmChatProvider} bean.
     * <p>
     * This provider is built by:
     * <ol>
     *   <li>Converting each certification entry from the properties into a
     *       {@link BearerTokenCertification}.</li>
     *   <li>Creating a {@link DeepseekChatProviderDelegate} configured with the
     *       base URL, chat completion endpoint, WebClient builder, and
     *       supported models.</li>
     *   <li>Wrapping the delegate inside a {@link DefaultLlmChatProvider},
     *       which also attaches the certifications and the global
     *       {@link LlmProviderInterceptorRegistry} for request/response
     *       interception.</li>
     * </ol>
     * The resulting provider can be injected anywhere an {@link LlmChatProvider} is
     * required and will handle chat requests to the Deepseek API.
     *
     * @param webClientBuilder                Spring WebClient.Builder for HTTP communication
     * @param deepseekClientProperties        the Deepseek-specific configuration properties
     * @param lLmProviderInterceptorRegistry  registry of interceptors common across all LLM providers
     * @return a fully configured {@link DefaultLlmChatProvider} instance
     */
    @ConditionalOnProperty(name = "reactive.ai.lite.client.deepseek.chat.enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public LlmChatProvider deepseekLlmChatProvider(WebClient.Builder webClientBuilder,
                                                   DeepseekClientProperties deepseekClientProperties,
                                                   LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry) {
        List<TokenCertification> certifications = deepseekClientProperties.getCertifications()
                .stream()
                .<TokenCertification>map(certification -> {
                    return BearerTokenCertification.builder()
                            .profile(certification.getProfile())
                            .token(certification.getToken())
                            .isDefault(certification.isDefault())
                            .build();
                })
                .toList();
        ChatProperties chatProperties = deepseekClientProperties.getChat();
        DeepseekChatProviderDelegate delegate = DeepseekChatProviderDelegate.builder()
                .name(DeepseekLlmProviderInfo.DEFAULT_NAME)
                .baseUrL(deepseekClientProperties.getChatBaseUrl())
                .chatCompletionEndpoint(chatProperties.getEndpoint())
                .webClientBuilder(webClientBuilder)
                .isDefault(chatProperties.isDefault())
                .certifications(certifications)
                .supportedModels(chatProperties.getLimitedModels())
                .build();
        DefaultLlmChatProvider defaultLlmChatProvider = new DefaultLlmChatProvider(
                delegate,
                certifications,
                lLmProviderInterceptorRegistry
        );
        log.info("Deepseek LLM client provider initialized successfully");
        return defaultLlmChatProvider;
    }
}