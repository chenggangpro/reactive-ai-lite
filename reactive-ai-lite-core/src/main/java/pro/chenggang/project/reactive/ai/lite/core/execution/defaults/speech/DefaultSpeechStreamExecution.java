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
package pro.chenggang.project.reactive.ai.lite.core.execution.defaults.speech;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.SpeechStreamExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.LlmProviderExecutor;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechRawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SpeechExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SpeechExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmSpeechProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Flux;

/**
 * Default implementation of {@link DefaultSpeechStreamExecution}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public class DefaultSpeechStreamExecution implements SpeechStreamExecution {

    private final LlmProviderExecutor<SpeechExecutionInfo> llmProviderExecutor;

    private DefaultSpeechStreamExecution(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull SpeechExecutionSpec executionSpec) {
        this.llmProviderExecutor = LlmProviderExecutor.<SpeechExecutionInfo>builder()
                .llmProviderRegistry(llmProviderRegistry)
                .executionSpec(executionSpec)
                .build();
    }

    public static DefaultSpeechStreamExecution of(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull SpeechExecutionSpec executionSpec) {
        return new DefaultSpeechStreamExecution(llmProviderRegistry, executionSpec);
    }

    @Override
    public Flux<SpeechStreamResponse> execute() {
        return llmProviderExecutor.executeFlux(LlmSpeechProvider.class, LlmSpeechProvider::executeSpeechStream)
                .contextWrite(context -> {
                    ExecutionSpec<SpeechExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    @Override
    public Flux<SpeechRawResponse> executeRaw() {
        return llmProviderExecutor.executeFlux(LlmSpeechProvider.class, LlmSpeechProvider::executeSpeechStreamRaw)
                .contextWrite(context -> {
                    ExecutionSpec<SpeechExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

}
