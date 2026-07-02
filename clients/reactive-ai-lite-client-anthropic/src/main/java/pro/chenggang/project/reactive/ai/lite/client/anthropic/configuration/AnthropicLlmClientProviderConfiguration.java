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
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.chat.AnthropicChatProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.chat.AnthropicLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.properties.AnthropicClientProperties;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.properties.AnthropicClientProperties.ChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.DefaultLlmChatProvider;

import java.util.List;

/**
 * The auto configuration for Anthropic LLM client provider.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(WebClientAutoConfiguration.class)
public class AnthropicLlmClientProviderConfiguration {

    @ConfigurationProperties(AnthropicClientProperties.PREFIX)
    @Bean
    public AnthropicClientProperties AnthropicClientProperties() {
        return new AnthropicClientProperties();
    }

    @Bean
    public LlmChatProvider anthropicLlmChatProvider(WebClient.Builder webClientBuilder, AnthropicClientProperties AnthropicClientProperties, LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry) {
        ChatProvider chatProvider = AnthropicClientProperties.getChatProvider();
        List<TokenCertification> certifications = chatProvider.getCertifications()
                .stream()
                .<TokenCertification>map(certification -> {
                    return BearerTokenCertification.builder()
                            .profile(certification.getProfile())
                            .token(certification.getToken())
                            .isDefault(certification.isDefault())
                            .build();
                })
                .toList();
        AnthropicChatProviderDelegate delegate = AnthropicChatProviderDelegate.builder()
                .name(AnthropicLlmProviderInfo.DEFAULT_NAME)
                .baseUrL(chatProvider.getBaseUrl())
                .chatCompletionEndpoint(chatProvider.getChatCompletionEndpoint())
                .webClientBuilder(webClientBuilder)
                .isDefault(chatProvider.isDefault())
                .certifications(certifications)
                .apiVersion(chatProvider.getAnthropicVersion())
                .supportedModels(chatProvider.getLimitedModels())
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
