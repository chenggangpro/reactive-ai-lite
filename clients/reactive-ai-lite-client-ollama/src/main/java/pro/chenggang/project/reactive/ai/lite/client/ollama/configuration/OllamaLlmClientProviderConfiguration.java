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
package pro.chenggang.project.reactive.ai.lite.client.ollama.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.client.ollama.properties.OllamaClientProperties;
import pro.chenggang.project.reactive.ai.lite.client.ollama.properties.OllamaClientProperties.ChatProperties;
import pro.chenggang.project.reactive.ai.lite.client.ollama.properties.OllamaClientProperties.EmbeddingProperties;
import pro.chenggang.project.reactive.ai.lite.client.ollama.provider.OllamaLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.client.ollama.provider.chat.OllamaChatProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.client.ollama.provider.embedding.OllamaEmbeddingProviderDelegate;
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
 * Auto-configuration class responsible for creating the core beans required to
 * interact with Ollama's chat and embedding LLM services in a reactive AI‑lite
 * client environment.
 * <p>
 * This configuration is activated automatically and conditionally creates
 * {@link LlmChatProvider} and {@link LlmEmbeddingProvider} beans depending on
 * the application properties. It uses {@link WebClient.Builder} for HTTP
 * communication, loads Ollama‑specific configuration through
 * {@link OllamaClientProperties}, and integrates with the central
 * {@link LlmProviderInterceptorRegistry} for request/response interception.
 * <p>
 * The resulting providers are wrapped in standard (default) implementations
 * that handle parameter resolution, interceptor chaining, and response
 * processing, while delegating the actual low‑level HTTP calls to
 * Ollama‑aware delegates.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
@AutoConfiguration
public class OllamaLlmClientProviderConfiguration {

    /**
     * Creates a Spring-managed {@link OllamaClientProperties} bean bound to the
     * prefix {@code reactive.ai.lite.client.ollama}.
     * <p>
     * Without this explicit bean definition, Spring Boot auto‑configuration
     * would not be able to inject the configuration values into the provider
     * beans. The returned object is automatically populated from the
     * application's property sources.
     *
     * @return a freshly instantiated but still unbound properties holder that
     *         Spring will subsequently bind to the external configuration
     */
    @ConfigurationProperties(OllamaClientProperties.PREFIX)
    @Bean
    public OllamaClientProperties ollamaClientProperties() {
        return new OllamaClientProperties();
    }

    /**
     * Conditionally creates an {@link LlmChatProvider} bean for Ollama’s chat
     * models.
     * <p>
     * The bean is registered only when the property
     * {@code reactive.ai.lite.client.ollama.chat.enabled} is {@code true} or
     * <em>missing</em> (i.e. {@code matchIfMissing = true}). It assembles the
     * necessary components:
     * <ul>
     *   <li>Extracts and converts the token‑based certifications from the
     *       {@link OllamaClientProperties#getCertifications()} list, filtering
     *       those that either have no explicit capability or explicitly allow
     *       {@link Capability#CHAT}.</li>
     *   <li>Uses the chat‑specific base URL and endpoint from
     *       {@link ChatProperties} to build an
     *       {@link OllamaChatProviderDelegate}.</li>
     *   <li>Wraps the delegate and certifications into a
     *       {@link DefaultLlmChatProvider}, which integrates with the
     *       {@link LlmProviderInterceptorRegistry}.</li>
     * </ul>
     *
     * @param webClientBuilder              the reactive web client builder,
     *                                      provided by Spring Boot auto‑configuration
     * @param ollamaClientProperties        the Ollama client configuration
     *                                      properties, fully populated from the
     *                                      application context
     * @param lLmProviderInterceptorRegistry the central registry for LLM provider
     *                                      interceptors
     * @return a fully configured {@link DefaultLlmChatProvider} backed by
     *         Ollama’s HTTP API
     */
    @ConditionalOnProperty(name = "reactive.ai.lite.client.ollama.chat.enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public LlmChatProvider ollamaLlmChatProvider(WebClient.Builder webClientBuilder, OllamaClientProperties ollamaClientProperties, LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry) {
        List<TokenCertification> certifications = ollamaClientProperties.getCertifications()
                .stream()
                .filter(ollamaCertification -> Objects.isNull(ollamaCertification.getCapability()) || Capability.CHAT.equals(ollamaCertification.getCapability()))
                .<TokenCertification>map(certification -> {
                    return BearerTokenCertification.builder()
                            .profile(certification.getProfile())
                            .token(certification.getToken())
                            .isDefault(certification.isDefault())
                            .build();
                })
                .toList();
        ChatProperties chatProperties = ollamaClientProperties.getChat();
        OllamaChatProviderDelegate delegate = OllamaChatProviderDelegate.builder()
                .name(OllamaLlmProviderInfo.DEFAULT_NAME)
                .baseUrL(ollamaClientProperties().getChatBaseUrl())
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
        log.info("Ollama LLM client provider initialized successfully");
        return defaultLlmChatProvider;
    }

    /**
     * Conditionally creates an {@link LlmEmbeddingProvider} bean for Ollama’s
     * embedding models.
     * <p>
     * Unlike the chat provider, this bean requires the property
     * {@code reactive.ai.lite.client.ollama.embedding.enabled} to be
     * explicitly set to {@code true}; it is <em>not</em> activated by default
     * ({@code matchIfMissing = false}). The construction mirrors that of the
     * chat provider:
     * <ul>
     *   <li>Certifications are filtered for {@link Capability#EMBEDDING} (or
     *       no explicit capability) and converted to
     *       {@link BearerTokenCertification} instances.</li>
     *   <li>An {@link OllamaEmbeddingProviderDelegate} is built using the
     *       embedding base URL and endpoint from
     *       {@link EmbeddingProperties}.</li>
     *   <li>The delegate is wrapped in a
     *       {@link DefaultLlmEmbeddingProvider} that enriches it with
     *       interceptor support.</li>
     * </ul>
     *
     * @param webClientBuilder              the reactive web client builder
     * @param ollamaClientProperties        the Ollama client configuration
     *                                      properties
     * @param lLmProviderInterceptorRegistry the central interceptor registry
     * @return a configured {@link DefaultLlmEmbeddingProvider} for Ollama
     *         embeddings
     */
    @ConditionalOnProperty(name = "reactive.ai.lite.client.ollama.embedding.enabled", havingValue = "true")
    @Bean
    public LlmEmbeddingProvider ollamaLlmEmbeddingProvider(WebClient.Builder webClientBuilder,
                                                           OllamaClientProperties ollamaClientProperties,
                                                           LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry) {
        List<TokenCertification> certifications = ollamaClientProperties.getCertifications()
                .stream()
                .filter(ollamaCertification -> Objects.isNull(ollamaCertification.getCapability()) || Capability.EMBEDDING.equals(ollamaCertification.getCapability()))
                .<TokenCertification>map(certification -> {
                    return BearerTokenCertification.builder()
                            .profile(certification.getProfile())
                            .token(certification.getToken())
                            .isDefault(certification.isDefault())
                            .build();
                })
                .toList();
        EmbeddingProperties embeddingProperties = ollamaClientProperties.getEmbedding();
        OllamaEmbeddingProviderDelegate delegate = OllamaEmbeddingProviderDelegate.builder()
                .name(OllamaLlmProviderInfo.DEFAULT_NAME)
                .baseUrL(ollamaClientProperties.getEmbeddingBaseUrl())
                .embeddingEndpoint(embeddingProperties.getEndpoint())
                .webClientBuilder(webClientBuilder)
                .isDefault(embeddingProperties.isDefault())
                .supportedModels(embeddingProperties.getLimitedModels())
                .certifications(certifications)
                .build();
        DefaultLlmEmbeddingProvider defaultLlmEmbeddingProvider = new DefaultLlmEmbeddingProvider(
                delegate,
                certifications,
                lLmProviderInterceptorRegistry
        );
        log.info("Ollama LLM embedding provider initialized successfully");
        return defaultLlmEmbeddingProvider;
    }
}