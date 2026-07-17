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
import pro.chenggang.project.reactive.ai.lite.core.execution.SpeechExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.LlmProviderExecutor;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechRawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SpeechExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SpeechExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmSpeechProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link SpeechExecution}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public class DefaultSpeechExecution implements SpeechExecution {

    private final LlmProviderExecutor<SpeechExecutionInfo> llmProviderExecutor;

    private DefaultSpeechExecution(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull SpeechExecutionSpec executionSpec) {
        this.llmProviderExecutor = LlmProviderExecutor.<SpeechExecutionInfo>builder()
                .llmProviderRegistry(llmProviderRegistry)
                .executionSpec(executionSpec)
                .build();
    }

    /**
     * Creates a new instance of DefaultSpeechExecution.
     *
     * @param llmProviderRegistry the LLM provider registry
     * @param executionSpec       the speech execution spec
     * @return a new DefaultSpeechExecution instance
     */
    public static SpeechExecution of(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull SpeechExecutionSpec executionSpec) {
        return new DefaultSpeechExecution(llmProviderRegistry, executionSpec);
    }

    /**
     * Executes the speech request and returns the processed response.
     *
     * @return a Mono emitting the SpeechResponse
     */
    @Override
    public Mono<SpeechResponse> execute() {
        return llmProviderExecutor.execute(LlmSpeechProvider.class, LlmSpeechProvider::executeSpeech)
                .contextWrite(context -> {
                    ExecutionSpec<SpeechExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Executes the speech request and returns the raw un-processed response.
     *
     * @return a Mono emitting the SpeechRawResponse
     */
    @Override
    public Mono<SpeechRawResponse> executeRaw() {
        return llmProviderExecutor.execute(LlmSpeechProvider.class, LlmSpeechProvider::executeSpeechRaw)
                .contextWrite(context -> {
                    ExecutionSpec<SpeechExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

}
