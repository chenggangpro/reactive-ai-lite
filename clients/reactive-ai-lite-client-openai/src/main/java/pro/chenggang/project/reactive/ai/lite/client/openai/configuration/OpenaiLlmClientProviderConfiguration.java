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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.client.openai.certification.OrganizationTokenCertification;
import pro.chenggang.project.reactive.ai.lite.client.openai.properties.OpenaiClientProperties;
import pro.chenggang.project.reactive.ai.lite.client.openai.properties.OpenaiClientProperties.ChatProperties;
import pro.chenggang.project.reactive.ai.lite.client.openai.properties.OpenaiClientProperties.EmbeddingProperties;
import pro.chenggang.project.reactive.ai.lite.client.openai.provider.OpenaiLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.client.openai.provider.chat.OpenaiChatProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.client.openai.provider.embedding.OpenaiEmbeddingProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmEmbeddingProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.DefaultLlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.DefaultLlmEmbeddingProvider;

import java.util.List;
import java.util.Objects;

/**
 * Spring Boot auto-configuration that sets up the OpenAI‑backed {@link LlmChatProvider}
 * and {@link LlmEmbeddingProvider} beans for the reactive‑ai‑lite framework.
 * <p>
 * This configuration class automates the instantiation and wiring of the client side
 * abstractions required to interact with the OpenAI API. It reads configuration
 * from the environment under the {@code reactive.ai.lite.client.openai} prefix
 * (see {@link OpenaiClientProperties}), then conditionally creates provider beans
 * based on {@code reactive.ai.lite.client.openai.chat.enabled} and
 * {@code reactive.ai.lite.client.openai.embedding.enabled} properties.
 * </p>
 * <p>
 * Each provider bean is assembled from:
 * <ul>
 *   <li>a {@link WebClient.Builder} for reactive HTTP communication,</li>
 *   <li>a set of {@link TokenCertification} objects filtered by the provider's
 *       {@link Capability} (chat or embedding) and optionally enriched with
 *       OpenAI‑specific organisation/project headers,</li>
 *   <li>a dedicated delegate ({@link OpenaiChatProviderDelegate} or
 *       {@link OpenaiEmbeddingProviderDelegate}) that encapsulates the API details,</li>
 *   <li>a global {@link LlmProviderInterceptorRegistry} for intercepting requests/responses.</li>
 * </ul>
 * This design ensures a clean separation between capability‑specific logic and
 * common infrastructure.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see OpenaiClientProperties
 * @see OpenaiChatProviderDelegate
 * @see OpenaiEmbeddingProviderDelegate
 * @see LlmChatProvider
 * @see LlmEmbeddingProvider
 */
@Slf4j
@AutoConfiguration
public class OpenaiLlmClientProviderConfiguration {

    /**
     * Exposes the {@link OpenaiClientProperties} configuration bean.
     * <p>
     * This bean aggregates all OpenAI‑related client properties under the
     * {@code reactive.ai.lite.client.openai} prefix, including base URLs,
     * endpoints, certifications, and model limitations. It serves as the single
     * source of truth for configuration throughout the OpenAI provider setup,
     * enabling easy externalization and type‑safe access via Spring Boot's
     * {@code @ConfigurationProperties} mechanism.
     * </p>
     *
     * @return an instance of {@link OpenaiClientProperties} bound from the application environment
     */
    @ConfigurationProperties(OpenaiClientProperties.PREFIX)
    @Bean
    public OpenaiClientProperties openaiClientProperties() {
        return new OpenaiClientProperties();
    }

    /**
     * Creates the {@link LlmChatProvider} bean for OpenAI chat completions.
     * <p>
     * This provider is conditionally registered when the property
     * {@code reactive.ai.lite.client.openai.chat.enabled} is {@code true}
     * (default: {@code true}). The bean is built by:
     * <ol>
     *   <li>parsing the {@link OpenaiClientProperties} to extract the chat section
     *       and the list of certifications;</li>
     *   <li>filtering certifications whose {@link Capability} is {@code null} or
     *       equals {@link Capability#CHAT} – this allows certifications to be
     *       dedicated to the chat capability when needed;</li>
     *   <li>transforming each matching certification entry into a concrete
     *       {@link TokenCertification}: if both {@code organizationId} and
     *       {@code projectId} are present, an {@link OrganizationTokenCertification}
     *       (which adds the OpenAI‑specific {@code OpenAI-Organization} and
     *       {@code OpenAI-Project} headers) is used; otherwise a standard
     *       {@link BearerTokenCertification} is produced;</li>
     *   <li>constructing an {@link OpenaiChatProviderDelegate} with the chat base
     *       URL, endpoint, {@link WebClient.Builder}, default flag, and the filtered
     *       certifications;</li>
     *   <li>wrapping the delegate inside a {@link DefaultLlmChatProvider} that also
     *       receives the list of certifications and the global
     *       {@link LlmProviderInterceptorRegistry}, thereby enabling interceptor
     *       chains.</li>
     * </ol>
     * The resulting provider is ready to serve chat requests via the reactive
     * client framework, honouring the configured models and security context.
     * </p>
     *
     * @param webClientBuilder               reactive HTTP client builder provided by Spring Boot;
     *                                       a single instance is shared across all OpenAI providers
     * @param openaiClientProperties         the aggregated OpenAI configuration
     * @param llmProviderInterceptorRegistry global registry of provider interceptors
     * @return a fully configured {@link LlmChatProvider} for OpenAI chat interactions
     */
    @ConditionalOnProperty(name = "reactive.ai.lite.client.openai.chat.enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public LlmChatProvider openaiLlmChatProvider(WebClient.Builder webClientBuilder, OpenaiClientProperties openaiClientProperties, LlmProviderInterceptorRegistry llmProviderInterceptorRegistry) {
        List<TokenCertification> certifications = openaiClientProperties.getCertifications()
                .stream()
                .filter(ollamaCertification -> Objects.isNull(ollamaCertification.getCapability()) || Capability.CHAT.equals(ollamaCertification.getCapability()))
                .<TokenCertification>map(certification -> {
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
        ChatProperties chatProperties = openaiClientProperties.getChat();
        OpenaiChatProviderDelegate delegate = OpenaiChatProviderDelegate.builder()
                .name(OpenaiLlmProviderInfo.DEFAULT_NAME)
                .baseUrL(openaiClientProperties.getChatBaseUrl())
                .chatCompletionEndpoint(chatProperties.getEndpoint())
                .webClientBuilder(webClientBuilder)
                .isDefault(chatProperties.isDefault())
                .certifications(certifications)
                .supportedModels(chatProperties.getLimitedModels())
                .build();
        DefaultLlmChatProvider defaultLlmChatProvider = new DefaultLlmChatProvider(
                delegate,
                certifications,
                llmProviderInterceptorRegistry
        );
        log.info("OpenAI LLM client provider initialized successfully");
        return defaultLlmChatProvider;
    }

    /**
     * Creates the {@link LlmEmbeddingProvider} bean for OpenAI embedding generation.
     * <p>
     * This provider is conditionally registered when the property
     * {@code reactive.ai.lite.client.openai.embedding.enabled} is {@code true}
     * (disabled by default). Its construction follows the same pattern as the chat
     * provider, tailored for the embeddings capability:
     * <ol>
     *   <li>certifications are filtered for those with {@link Capability#EMBEDDING}
     *       or without a specific capability, allowing dedicated embedding
     *       credentials if configured;</li>
     *   <li>filtered certifications are converted to either
     *       {@link OrganizationTokenCertification} or {@link BearerTokenCertification}
     *       based on the presence of OpenAI organisation/project identifiers;</li>
     *   <li>an {@link OpenaiEmbeddingProviderDelegate} is built using the dedicated
     *       embeddings base URL, endpoint, and the filtered certifications;</li>
     *   <li>the delegate is wrapped by a {@link DefaultLlmEmbeddingProvider} that
     *       integrates the certifications and the interceptor registry.</li>
     * </ol>
     * This architecture keeps the chat and embedding providers completely independent,
     * allowing them to be enabled and configured separately.
     * </p>
     *
     * @param webClientBuilder               reactive HTTP client builder shared across providers
     * @param openaiClientProperties         the aggregated OpenAI configuration
     * @param llmProviderInterceptorRegistry global registry of provider interceptors
     * @return a fully configured {@link LlmEmbeddingProvider} for OpenAI embedding operations
     */
    @ConditionalOnProperty(name = "reactive.ai.lite.client.openai.embedding.enabled", havingValue = "true")
    @Bean
    public LlmEmbeddingProvider openaiLlmEmbeddingProvider(WebClient.Builder webClientBuilder,
                                                           OpenaiClientProperties openaiClientProperties,
                                                           LlmProviderInterceptorRegistry llmProviderInterceptorRegistry) {
        List<TokenCertification> certifications = openaiClientProperties.getCertifications()
                .stream()
                .filter(ollamaCertification -> Objects.isNull(ollamaCertification.getCapability()) || Capability.EMBEDDING.equals(ollamaCertification.getCapability()))
                .<TokenCertification>map(certification -> {
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
        EmbeddingProperties embeddingProperties = openaiClientProperties.getEmbedding();
        OpenaiEmbeddingProviderDelegate delegate = OpenaiEmbeddingProviderDelegate.builder()
                .name(OpenaiLlmProviderInfo.DEFAULT_NAME)
                .baseUrL(openaiClientProperties.getEmbeddingBaseUrl())
                .embeddingEndpoint(embeddingProperties.getEndpoint())
                .webClientBuilder(webClientBuilder)
                .isDefault(embeddingProperties.isDefault())
                .certifications(certifications)
                .supportedModels(embeddingProperties.getLimitedModels())
                .build();
        DefaultLlmEmbeddingProvider defaultLlmEmbeddingProvider = new DefaultLlmEmbeddingProvider(
                delegate,
                certifications,
                llmProviderInterceptorRegistry
        );
        log.info("OpenAI LLM embedding provider initialized successfully");
        return defaultLlmEmbeddingProvider;
    }
}