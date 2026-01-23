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
package pro.chenggang.project.reactive.ai.lite.core.spec.defaults;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.TraceId;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.ExecutionContextSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.ProviderSpec;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultExecutionContextSpec implements ExecutionContextSpec {

    private final LlmClientType llmClientType;
    private final LlmProviderRegistry llmProviderRegistry;
    @Getter(AccessLevel.PROTECTED)
    private TraceId parentTraceId;
    @Getter(AccessLevel.PROTECTED)
    private Map<String, Object> parentAttributes;
    @Getter(AccessLevel.PROTECTED)
    private Supplier<String> traceIdGenerator;
    @Getter(AccessLevel.PROTECTED)
    private Consumer<ExecutionContext> contextConfigure;

    public static DefaultExecutionContextSpec of(@NonNull LlmClientType llmClientType, @NonNull LlmProviderRegistry llmProviderRegistry) {
        return new DefaultExecutionContextSpec(llmClientType, llmProviderRegistry);
    }

    @Override
    public ExecutionContextSpec parentTraceId(@NonNull TraceId parentTraceId) {
        this.parentTraceId = parentTraceId;
        return this;
    }

    @Override
    public ExecutionContextSpec parentAttributes(@NonNull Map<String, Object> parentAttributes) {
        this.parentAttributes = parentAttributes;
        return this;
    }

    @Override
    public ExecutionContextSpec traceIdGenerator(@NonNull Supplier<String> traceIdGenerator) {
        this.traceIdGenerator = traceIdGenerator;
        return this;
    }

    @Override
    public ExecutionContextSpec contextConfigure(@NonNull Consumer<ExecutionContext> contextConfigure) {
        this.contextConfigure = contextConfigure;
        return this;
    }

    @Override
    public ProviderSpec providerSpec() {
        return DefaultProviderSpec.of(llmClientType, llmProviderRegistry, this);
    }
}
