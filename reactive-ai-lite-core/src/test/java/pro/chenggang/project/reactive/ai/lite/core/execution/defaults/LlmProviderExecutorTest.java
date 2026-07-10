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
package pro.chenggang.project.reactive.ai.lite.core.execution.defaults;

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.exception.ExecutionContextLossException;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.function.BiPredicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmProviderExecutorTest {

    @Test
    void testExecuteWithNulls() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ExecutionSpec spec = mock(ExecutionSpec.class);
        LlmProviderExecutor executor = LlmProviderExecutor.builder()
                .llmProviderRegistry(registry)
                .executionSpec(spec)
                .build();
                
        assertThatThrownBy(() -> executor.execute(null, (p, i) -> Mono.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> executor.execute(LlmProvider.class, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> executor.executeFlux(null, (p, i) -> Flux.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> executor.executeFlux(LlmProvider.class, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> executor.loadLlmProvider(null, LlmProvider.class))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> executor.loadLlmProvider(mock(ExecutionContext.class), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    void testExecuteContextLoss() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ExecutionSpec spec = mock(ExecutionSpec.class);
        LlmProviderExecutor executor = LlmProviderExecutor.builder()
                .llmProviderRegistry(registry)
                .executionSpec(spec)
                .build();
                
        StepVerifier.create(executor.execute(LlmProvider.class, (p, i) -> Mono.just("result")))
                .expectError(ExecutionContextLossException.class)
                .verify();
                
        StepVerifier.create(executor.executeFlux(LlmProvider.class, (p, i) -> Flux.just("result")))
                .expectError(ExecutionContextLossException.class)
                .verify();
    }
    
    @Test
    void testExecuteDefaultProvider() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ExecutionSpec spec = mock(ExecutionSpec.class);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        ExecutionInfo info = mock(ExecutionInfo.class);
        when(spec.newExecutionInfo(any())).thenReturn(info);
        
        LlmProvider provider = mock(LlmProvider.class);
        when(registry.getDefaultProvider(Capability.CHAT)).thenReturn((Mono) Mono.just(provider));
        
        LlmProviderExecutor executor = LlmProviderExecutor.builder()
                .llmProviderRegistry(registry)
                .executionSpec(spec)
                .build();
                
        StepVerifier.create(executor.execute(LlmProvider.class, (p, i) -> Mono.just("result"))
                        .contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .expectNext("result")
                .verifyComplete();
                
        StepVerifier.create(executor.executeFlux(LlmProvider.class, (p, i) -> Flux.just("result"))
                        .contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .expectNext("result")
                .verifyComplete();
    }
    
    @Test
    void testExecuteCustomProvider() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ExecutionSpec spec = mock(ExecutionSpec.class);
        when(spec.isDefaultProvider()).thenReturn(false);
        when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        
        BiPredicate<ExecutionContext, LlmProviderInfo> filter = (ctx, i) -> true;
        when(spec.getProviderFilter()).thenReturn(filter);
        ExecutionInfo info = mock(ExecutionInfo.class);
        when(spec.newExecutionInfo(any())).thenReturn(info);
        
        LlmProvider provider = mock(LlmProvider.class);
        when(registry.getProvider(eq(Capability.CHAT), eq(LlmProvider.class), any())).thenReturn((Mono) Mono.just(provider));
        
        LlmProviderExecutor executor = LlmProviderExecutor.builder()
                .llmProviderRegistry(registry)
                .executionSpec(spec)
                .build();
                
        StepVerifier.create(executor.execute(LlmProvider.class, (p, i) -> Mono.just("result"))
                        .contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .expectNext("result")
                .verifyComplete();
    }
}
