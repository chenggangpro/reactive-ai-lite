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
 * @author Cheng Gang
 * @version 0.1.0
 */
@SuperBuilder
public abstract class AbstractLlmProviderExchange implements LlmProviderExchange {

    @NonNull
    protected final Map<String, Object> attributes;

    @NonNull
    protected final LlmClientType clientType;

    @NonNull
    protected final LlmProviderInfo llmProviderInfo;

    @NonNull
    protected final ExecutionContextView executionContextView;

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public ExecutionContextView contextView() {
        return this.executionContextView;
    }

    @Override
    public LlmClientType clientType() {
        return this.clientType;
    }

    @Override
    public LlmProviderInfo llmProviderInfo() {
        return this.llmProviderInfo;
    }
}
