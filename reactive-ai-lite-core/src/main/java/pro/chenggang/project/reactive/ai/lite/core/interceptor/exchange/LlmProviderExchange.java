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
package pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange;

import pro.chenggang.project.reactive.ai.lite.core.entity.AttributesAbility;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

/**
 * The base contract for data exchanged between interceptors during an LLM request lifecycle.
 * <p>
 * This interface defines the core context available to all interceptors, regardless of
 * whether they are operating on the request before execution, or the response after
 * execution. It provides access to the shared parsingAttributes, the execution context view,
 * the client type, and metadata about the provider handling the request.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmProviderExchange extends AttributesAbility {

    /**
     * Retrieves the read-only view of the execution context.
     * <p>
     * This context view contains parsingAttributes and state configured before the request
     * began processing, useful for tracing and correlation.
     * </p>
     *
     * @return the {@link ExecutionContextView}
     */
    ExecutionContextView contextView();

    /**
     * Retrieves the type of LLM client executing the request (e.g., CHAT, IMAGE).
     *
     * @return the {@link LlmClientType}
     */
    LlmClientType clientType();

    /**
     * Retrieves the metadata of the LLM provider handling the request.
     *
     * @return the {@link LlmProviderInfo}
     */
    LlmProviderInfo llmProviderInfo();

}