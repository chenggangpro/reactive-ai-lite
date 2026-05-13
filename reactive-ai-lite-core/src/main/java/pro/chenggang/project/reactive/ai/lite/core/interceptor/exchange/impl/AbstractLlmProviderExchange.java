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
package pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.impl;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderExchange;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.util.Map;

/**
 * An abstract base implementation of the {@link LlmProviderExchange} interface.
 * <p>
 * This class provides the common data structure and accessor methods for all
 * interceptor exchange types. It stores the mutable parsingAttributes map, the read-only
 * execution context view, the client type, and the provider metadata. Subclasses
 * (like request, general response, or stream response exchanges) extend this
 * to add phase-specific payload data.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@SuperBuilder
public abstract class AbstractLlmProviderExchange implements LlmProviderExchange {

    /**
     * A mutable map for interceptors to share data across the execution chain.
     */
    @NonNull
    protected final Map<String, Object> attributes;

    /**
     * The type of LLM client handling the request.
     */
    @NonNull
    protected final LlmClientType clientType;

    /**
     * Metadata about the specific LLM provider.
     */
    @NonNull
    protected final LlmProviderInfo llmProviderInfo;

    /**
     * A read-only view of the execution context.
     */
    @NonNull
    protected final ExecutionContextView executionContextView;

    /**
     * Retrieves the mutable parsingAttributes map.
     *
     * @return the parsingAttributes map
     */
    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    /**
     * Retrieves the read-only execution context view.
     *
     * @return the execution context view
     */
    @Override
    public ExecutionContextView contextView() {
        return this.executionContextView;
    }

    /**
     * Retrieves the LLM client type.
     *
     * @return the client type
     */
    @Override
    public LlmClientType clientType() {
        return this.clientType;
    }

    /**
     * Retrieves the LLM provider metadata.
     *
     * @return the provider info
     */
    @Override
    public LlmProviderInfo llmProviderInfo() {
        return this.llmProviderInfo;
    }
}
