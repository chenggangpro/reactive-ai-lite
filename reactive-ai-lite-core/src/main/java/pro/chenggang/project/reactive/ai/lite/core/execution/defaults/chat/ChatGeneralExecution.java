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
package pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.execution.GeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.LlmProviderExecutor;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Mono;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
public class ChatGeneralExecution implements GeneralExecution {

    private final LlmProviderExecutor llmProviderExecutor;

    private ChatGeneralExecution(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ExecutionSpec executionSpec) {
        this.llmProviderExecutor = LlmProviderExecutor.builder()
                .llmProviderRegistry(llmProviderRegistry)
                .executionSpec(executionSpec)
                .build();
    }

    public static GeneralExecution of(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ExecutionSpec executionSpec) {
        return new ChatGeneralExecution(llmProviderRegistry, executionSpec);
    }

    @Override
    public ExecutionSpec executionSpec() {
        return this.llmProviderExecutor.getExecutionSpec();
    }

    @Override
    public Mono<GeneralResponse> execute() {
        return llmProviderExecutor.executeChat(LlmChatProvider::executeGeneral);
    }

    @Override
    public Mono<RawResponse> executeRaw() {
        return llmProviderExecutor.executeChat(LlmChatProvider::executeGeneralRaw);
    }

}
