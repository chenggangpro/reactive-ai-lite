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
package pro.chenggang.project.reactive.ai.lite.core.api.defaults;

import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.api.ClientRequest;
import pro.chenggang.project.reactive.ai.lite.core.api.ReactiveLlmClient;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;

/**
 * Default implementation of {@link ReactiveLlmClient}.
 * <p>
 * This class serves as the primary, out-of-the-box client for reactive AI Lite. It acts as a
 * lightweight factory for {@link ClientRequest} instances, delegating all actual LLM provider
 * interactions to the underlying {@link LlmProviderRegistry}. The registry is injected via
 * constructor (Lombok {@code @RequiredArgsConstructor}) and is used to resolve the correct provider
 * when a request is about to be executed.
 * </p>
 * <p>
 * Typically, this default client is automatically wired into the application context when no custom
 * {@code ReactiveLlmClient} bean is defined. It simplifies the initial setup by providing a
 * consistent entry point for building LLM request pipelines.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@RequiredArgsConstructor
public class DefaultReactiveLlmClient implements ReactiveLlmClient {

    /**
     * The central registry that maintains all configured LLM provider instances.
     * <p>
     * This registry is consulted whenever a {@link ClientRequest} needs to obtain a provider
     * to execute the actual LLM call. It is typically populated by auto-configuration scanning
     * for {@code LlmProvider} beans during application startup.
     * </p>
     */
    private final LlmProviderRegistry llmProviderRegistry;

    /**
     * Creates a new default {@link ClientRequest} that will use the {@link LlmProviderRegistry}
     * to resolve the appropriate LLM provider at execution time.
     * <p>
     * The returned request object is a {@link DefaultClientRequest} that carries a reference to
     * the registry, enabling dynamic provider selection based on the request's configuration
     * (e.g., model name, provider name).
     * </p>
     *
     * @return a new {@code ClientRequest} instance ready for further setup and execution
     */
    @Override
    public ClientRequest newRequest() {
        return new DefaultClientRequest(this.llmProviderRegistry);
    }
}