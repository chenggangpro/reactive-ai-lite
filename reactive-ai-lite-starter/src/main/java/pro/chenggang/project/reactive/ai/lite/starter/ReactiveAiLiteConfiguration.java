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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import pro.chenggang.project.reactive.ai.lite.core.api.DefaultReactiveLlmClient;
import pro.chenggang.project.reactive.ai.lite.core.api.ReactiveLlmClient;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.DefaultLlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
@AutoConfiguration
@AutoConfigureAfter(WebClientAutoConfiguration.class)
public class ReactiveAiLiteConfiguration {

    @Bean
    public LlmProviderRegistry llmProviderRegistry(ObjectProvider<LlmProvider> objectProvider) {
        return new DefaultLlmProviderRegistry(objectProvider.stream().toList());
    }

    @Bean
    public ReactiveLlmClient reactiveLlmClient(LlmProviderRegistry llmProviderRegistry) {
        return new DefaultReactiveLlmClient(llmProviderRegistry);
    }
}
