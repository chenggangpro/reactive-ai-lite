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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.chat.DeepseekChatProvider;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.chat.DeepseekLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.properties.DeepseekClientProperties;
import pro.chenggang.project.reactive.ai.lite.client.deepseek.properties.DeepseekClientProperties.ChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;

import java.util.List;

/**
 * The auto configuration for Deepseek LLM client provider.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@AutoConfiguration
@AutoConfigureAfter(WebClientAutoConfiguration.class)
public class DeepseekLlmClientProviderConfiguration {

    @ConfigurationProperties(DeepseekClientProperties.PREFIX)
    @Bean
    public DeepseekClientProperties deepseekClientProperties() {
        return new DeepseekClientProperties();
    }

    @Bean
    public LlmChatProvider llmChatProvider(WebClient.Builder webClientBuilder, DeepseekClientProperties deepseekClientProperties) {
        ChatProvider chatProvider = deepseekClientProperties.getChatProvider();
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
        return DeepseekChatProvider.builder()
                .name(DeepseekLlmProviderInfo.DEFAULT_NAME)
                .baseUrL(chatProvider.getBaseUrl())
                .chatCompletionEndpoint(chatProvider.getChatCompletionEndpoint())
                .webClientBuilder(webClientBuilder)
                .isDefault(chatProvider.isDefault())
                .certifications(certifications)
                .build();
    }
}
