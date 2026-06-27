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
package pro.chenggang.project.reactive.ai.lite.client.openai.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.client.openai.certification.OrganizationTokenCertification;
import pro.chenggang.project.reactive.ai.lite.client.openai.chat.OpenaiChatProvider;
import pro.chenggang.project.reactive.ai.lite.client.openai.chat.OpenaiLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.client.openai.properties.OpenaiClientProperties;
import pro.chenggang.project.reactive.ai.lite.client.openai.properties.OpenaiClientProperties.ChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;

import java.util.List;

/**
 * The auto configuration for OpenAI LLM client provider.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(WebClientAutoConfiguration.class)
public class OpenaiLlmClientProviderConfiguration {

    @ConfigurationProperties(OpenaiClientProperties.PREFIX)
    @Bean
    public OpenaiClientProperties openaiClientProperties() {
        return new OpenaiClientProperties();
    }

    @Bean
    public LlmChatProvider llmChatProvider(WebClient.Builder webClientBuilder, OpenaiClientProperties openaiClientProperties, LlmProviderInterceptorRegistry llmProviderInterceptorRegistry) {
        ChatProvider chatProvider = openaiClientProperties.getChatProvider();
        List<TokenCertification> certifications = chatProvider.getCertifications()
                .stream()
                .map(certification -> {
                    if (StringUtils.hasText(certification.getOrganizationId()) && StringUtils.hasText(certification.getProjectId())) {
                        return OrganizationTokenCertification.builder()
                                .profile(certification.getProfile())
                                .token(certification.getToken())
                                .organizationId(certification.getOrganizationId())
                                .projectId(certification.getProjectId())
                                .isDefault(certification.isDefault())
                                .build();
                    }
                    return BearerTokenCertification.builder()
                            .profile(certification.getProfile())
                            .token(certification.getToken())
                            .isDefault(certification.isDefault())
                            .build();
                })
                .toList();
        OpenaiChatProvider openaiChatProvider = OpenaiChatProvider.builder()
                .name(OpenaiLlmProviderInfo.DEFAULT_NAME)
                .baseUrL(chatProvider.getBaseUrl())
                .chatCompletionEndpoint(chatProvider.getChatCompletionEndpoint())
                .webClientBuilder(webClientBuilder)
                .isDefault(chatProvider.isDefault())
                .certifications(certifications)
                .supportedModels(chatProvider.getLimitedModels())
                .lLmProviderInterceptorRegistry(llmProviderInterceptorRegistry)
                .build();
        log.info("OpenAI LLM client provider initialized successfully");
        return openaiChatProvider;
    }
}
