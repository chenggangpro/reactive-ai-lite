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
package pro.chenggang.project.reactive.ai.lite.starter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import pro.chenggang.project.reactive.ai.lite.core.api.ReactiveLlmClient;
import pro.chenggang.project.reactive.ai.lite.core.api.defaults.DefaultReactiveLlmClient;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionAfterInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionBeforeInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.defaults.DefaultLlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.logging.LlmProviderExecutionLoggingInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.DefaultLlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.starter.properties.ReactiveAiClientProperties;

/**
 * Spring Boot auto-configuration for the Reactive AI Lite client.
 * <p>
 * This class assembles the core beans required to bootstrap a reactive LLM client:
 * <ul>
 *   <li>Configuration properties ({@link ReactiveAiClientProperties}) bound to the
 *       {@code reactive.ai.lite} prefix.</li>
 *   <li>A logging interceptor that conditionally records provider execution details
 *       based on the {@code enable-logging} property.</li>
 *   <li>An interceptor registry that collects all available before- and after-execution
 *       interceptors. A custom registry can be supplied by marking it as
 *       {@link ConditionalOnMissingBean}.</li>
 *   <li>A provider registry that discovers all {@link LlmProvider} implementations.</li>
 *   <li>The default {@link ReactiveLlmClient} which delegates to the registered providers.</li>
 * </ul>
 * All beans are defined as non-conditional (except for the interceptor registry) to ensure
 * the framework is fully functional out-of-the-box while allowing overrides where necessary.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
@AutoConfiguration
public class ReactiveAiLiteConfiguration {

    /**
     * Creates the configuration properties holder for the reactive AI client.
     * <p>
     * Properties are bound from the {@code reactive.ai.lite} prefix. This bean
     * centralizes all tunable parameters (e.g., logging, timeouts, provider
     * selection) and is injected into other beans that need configuration awareness.
     *
     * @return the {@link ReactiveAiClientProperties} instance, never {@code null}
     */
    @ConfigurationProperties(prefix = ReactiveAiClientProperties.PREFIX)
    @Bean
    public ReactiveAiClientProperties reactiveAiClientProperties() {
        return new ReactiveAiClientProperties();
    }

    /**
     * Creates a logging interceptor that conditionally logs LLM provider execution details.
     * <p>
     * The decision to log is delegated to the {@link ReactiveAiClientProperties#isEnableLogging()}
     * method, allowing runtime control (e.g., via external configuration) without restarting
     * the application. This interceptor is typically registered automatically through the
     * {@link LlmProviderInterceptorRegistry} to provide visibility into provider calls.
     *
     * @param reactiveAiClientProperties the client configuration properties,
     *                                   used to obtain the logging flag
     * @return a new {@link LlmProviderExecutionLoggingInterceptor} instance
     */
    @Bean
    public LlmProviderExecutionLoggingInterceptor llmProviderExecutionLoggingInterceptor(ReactiveAiClientProperties reactiveAiClientProperties) {
        LlmProviderExecutionLoggingInterceptor loggingInterceptor = new LlmProviderExecutionLoggingInterceptor(reactiveAiClientProperties::isEnableLogging);
        log.info("Load LLM provider execution logging interceptor successfully");
        return loggingInterceptor;
    }

    /**
     * Creates the default {@link LlmProviderInterceptorRegistry} that aggregates
     * all available before- and after-execution interceptors.
     * <p>
     * This bean is marked with {@link ConditionalOnMissingBean} so that users can
     * provide a custom registry implementation if needed. The registry is built by
     * collecting all {@link LlmProviderExecutionBeforeInterceptor} and
     * {@link LlmProviderExecutionAfterInterceptor} beans from the application context,
     * enabling a pluggable interception chain around every provider call.
     *
     * @param beforeInterceptorObjectProvider a lazy provider for before-execution interceptors
     * @param afterInterceptorObjectProvider  a lazy provider for after-execution interceptors
     * @return a fully initialized {@link DefaultLlmProviderInterceptorRegistry}
     */
    @ConditionalOnMissingBean
    @Bean
    public LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry(ObjectProvider<LlmProviderExecutionBeforeInterceptor> beforeInterceptorObjectProvider,
                                                                         ObjectProvider<LlmProviderExecutionAfterInterceptor> afterInterceptorObjectProvider) {
        return new DefaultLlmProviderInterceptorRegistry(beforeInterceptorObjectProvider.stream().toList(), afterInterceptorObjectProvider.stream().toList());
    }

    /**
     * Creates the default {@link LlmProviderRegistry} that discovers all
     * {@link LlmProvider} beans in the context.
     * <p>
     * The registry is the central lookup mechanism for the client to find available
     * LLM providers (e.g., OpenAI, Ollama, custom implementations). By using
     * {@link ObjectProvider}, the registry automatically picks up any providers
     * added through auto-configuration or user beans.
     *
     * @param objectProvider a provider that can supply zero or more {@link LlmProvider} instances
     * @return a new {@link DefaultLlmProviderRegistry} containing all discovered providers
     */
    @Bean
    public LlmProviderRegistry llmProviderRegistry(ObjectProvider<LlmProvider> objectProvider) {
        return new DefaultLlmProviderRegistry(objectProvider.stream().toList());
    }

    /**
     * Creates the primary reactive LLM client backed by the provider registry.
     * <p>
     * The {@link DefaultReactiveLlmClient} is the main entry point for applications.
     * It selects an appropriate provider based on the caller's request and delegates
     * the LLM interaction. By injecting the {@link LlmProviderRegistry} here, the client
     * gains access to all configured providers without being aware of their instantiation.
     *
     * @param llmProviderRegistry the registry containing all available LLM providers
     * @return a fully operational {@link ReactiveLlmClient}
     */
    @Bean
    public ReactiveLlmClient reactiveLlmClient(LlmProviderRegistry llmProviderRegistry) {
        DefaultReactiveLlmClient reactiveLlmClient = new DefaultReactiveLlmClient(llmProviderRegistry);
        log.info("Load reactive lite LLM client successfully");
        return reactiveLlmClient;
    }
}