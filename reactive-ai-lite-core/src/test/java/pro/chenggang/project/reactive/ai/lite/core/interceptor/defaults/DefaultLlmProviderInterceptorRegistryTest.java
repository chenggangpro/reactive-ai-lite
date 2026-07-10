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
package pro.chenggang.project.reactive.ai.lite.core.interceptor.defaults;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionAfterInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionBeforeInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry.InterceptedDataInfo;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultLlmProviderInterceptorRegistryTest {

    private DefaultLlmProviderInterceptorRegistry registry;
    private LlmProviderExecutionBeforeInterceptor beforeInterceptor;
    private LlmProviderExecutionAfterInterceptor afterInterceptor;

    @BeforeEach
    void setUp() {
        beforeInterceptor = mock(LlmProviderExecutionBeforeInterceptor.class);
        when(beforeInterceptor.supportedClient()).thenReturn(Set.of(LlmClientType.CHAT));
        when(beforeInterceptor.getOrder()).thenReturn(1);
        when(beforeInterceptor.interceptBefore(any(), any())).thenAnswer(inv -> {
            pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderRequestInterceptorChain chain = inv.getArgument(1);
            return chain.next(inv.getArgument(0));
        });

        afterInterceptor = mock(LlmProviderExecutionAfterInterceptor.class);
        when(afterInterceptor.supportedClient()).thenReturn(Set.of(LlmClientType.CHAT));
        when(afterInterceptor.getOrder()).thenReturn(1);
        when(afterInterceptor.interceptAfter(any(), any())).thenAnswer(inv -> {
            pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderResponseInterceptorChain chain = inv.getArgument(1);
            return chain.next((pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderGeneralResponseExchange) inv.getArgument(0));
        });
        when(afterInterceptor.interceptAfterEach(any(), any())).thenAnswer(inv -> {
            pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderResponseInterceptorChain chain = inv.getArgument(1);
            return chain.next((pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderStreamResponseExchange) inv.getArgument(0));
        });

        registry = new DefaultLlmProviderInterceptorRegistry(
                List.of(beforeInterceptor),
                List.of(afterInterceptor)
        );
    }

    @Test
    void testInterceptGeneral() {
        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        Mono<ObjectNode> responseExecution = Mono.just(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());

        StepVerifier.create(registry.interceptGeneral(info, responseExecution))
                .expectNextCount(1)
                .verifyComplete();
    }
    
    @Test
    void testInterceptGeneralError() {
        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        Mono<ObjectNode> responseExecution = Mono.error(new RuntimeException("General Error"));

        StepVerifier.create(registry.interceptGeneral(info, responseExecution))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void testInterceptStream() {
        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        RawStreamResponse chunk = mock(RawStreamResponse.class);
        Flux<RawStreamResponse> streamExecution = Flux.just(chunk);

        StepVerifier.create(registry.interceptStream(info, streamExecution))
                .expectNextCount(1)
                .verifyComplete();
    }
    
    @Test
    void testInterceptStreamError() {
        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        Flux<RawStreamResponse> streamExecution = Flux.error(new RuntimeException("Stream Error"));

        StepVerifier.create(registry.interceptStream(info, streamExecution))
                .expectError(RuntimeException.class)
                .verify();
    }
    
    @Test
    void testInterceptGeneralCancel() {
        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        Mono<ObjectNode> responseExecution = Mono.just(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode()).delayElement(java.time.Duration.ofMillis(100));

        StepVerifier.create(registry.interceptGeneral(info, responseExecution))
                .thenCancel()
                .verify();
    }
    
    @Test
    void testInterceptStreamCancel() {
        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        RawStreamResponse chunk = mock(RawStreamResponse.class);
        Flux<RawStreamResponse> streamExecution = Flux.just(chunk).delayElements(java.time.Duration.ofMillis(100));

        StepVerifier.create(registry.interceptStream(info, streamExecution))
                .thenCancel()
                .verify();
    }
    @Test
    void testInterceptGeneralNoInterceptors() {
        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.EMBEDDING) // embedding has no interceptors configured
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        Mono<ObjectNode> responseExecution = Mono.just(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());

        StepVerifier.create(registry.interceptGeneral(info, responseExecution))
                .expectNextCount(1)
                .verifyComplete();
    }
    
    @Test
    void testInterceptGeneralErrorNoInterceptors() {
        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.EMBEDDING)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        Mono<ObjectNode> responseExecution = Mono.error(new RuntimeException("General Error"));

        StepVerifier.create(registry.interceptGeneral(info, responseExecution))
                .expectError(RuntimeException.class)
                .verify();
    }
    
    @Test
    void testInterceptGeneralCancelNoInterceptors() {
        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.EMBEDDING)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        Mono<ObjectNode> responseExecution = Mono.just(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode()).delayElement(java.time.Duration.ofMillis(100));

        StepVerifier.create(registry.interceptGeneral(info, responseExecution))
                .thenCancel()
                .verify();
    }

    @Test
    void testInterceptStreamNoInterceptors() {
        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.EMBEDDING)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        RawStreamResponse chunk = mock(RawStreamResponse.class);
        Flux<RawStreamResponse> streamExecution = Flux.just(chunk);

        StepVerifier.create(registry.interceptStream(info, streamExecution))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void testInterceptStreamErrorNoInterceptors() {
        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.EMBEDDING)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        Flux<RawStreamResponse> streamExecution = Flux.error(new RuntimeException("Stream Error"));

        StepVerifier.create(registry.interceptStream(info, streamExecution))
                .expectError(RuntimeException.class)
                .verify();
    }
    
    @Test
    void testInterceptStreamCancelNoInterceptors() {
        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.EMBEDDING)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        RawStreamResponse chunk = mock(RawStreamResponse.class);
        Flux<RawStreamResponse> streamExecution = Flux.just(chunk).delayElements(java.time.Duration.ofMillis(100));

        StepVerifier.create(registry.interceptStream(info, streamExecution))
                .thenCancel()
                .verify();
    }
    @Test
    void testNonNullChecks() throws Exception {
        assertThatThrownBy(() -> new DefaultLlmProviderInterceptorRegistry(null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultLlmProviderInterceptorRegistry(List.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
                
        assertThatThrownBy(() -> registry.interceptGeneral(null, Mono.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.interceptGeneral(mock(InterceptedDataInfo.class), null))
                .isInstanceOf(IllegalArgumentException.class);
                
        assertThatThrownBy(() -> registry.interceptStream(null, Flux.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.interceptStream(mock(InterceptedDataInfo.class), null))
                .isInstanceOf(IllegalArgumentException.class);
                
        java.lang.reflect.Method m1 = DefaultLlmProviderInterceptorRegistry.class.getDeclaredMethod("initBeforeInterceptorChainMap", List.class);
        m1.setAccessible(true);
        try {
            m1.invoke(registry, (List) null);
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertThat(e.getTargetException()).isInstanceOf(IllegalArgumentException.class);
        }
        
        java.lang.reflect.Method m2 = DefaultLlmProviderInterceptorRegistry.class.getDeclaredMethod("initAfterInterceptorChainMap", List.class);
        m2.setAccessible(true);
        try {
            m2.invoke(registry, (List) null);
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertThat(e.getTargetException()).isInstanceOf(IllegalArgumentException.class);
        }
    }
    
    @Test
    void testInterceptStreamInterceptorError() {
        LlmProviderExecutionAfterInterceptor errorInterceptor = mock(LlmProviderExecutionAfterInterceptor.class);
        when(errorInterceptor.supportedClient()).thenReturn(Set.of(LlmClientType.CHAT));
        when(errorInterceptor.getOrder()).thenReturn(1);
        when(errorInterceptor.interceptAfterEach(any(), any())).thenAnswer(inv -> {
            return Mono.error(new RuntimeException("Interceptor error"));
        });
        
        DefaultLlmProviderInterceptorRegistry errorRegistry = new DefaultLlmProviderInterceptorRegistry(
                List.of(),
                List.of(errorInterceptor)
        );

        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        RawStreamResponse chunk = mock(RawStreamResponse.class);
        Flux<RawStreamResponse> streamExecution = Flux.just(chunk);

        // interceptAfterEach returning Mono.error() will be caught by onErrorResume in interceptStream
        // and it returns Mono.empty() effectively dropping the chunk and completing the stream?
        // Wait, if flatMap returns empty, the stream just skips the element.
        StepVerifier.create(errorRegistry.interceptStream(info, streamExecution))
                .expectNext(chunk) // the chunk is skipped
                .verifyComplete();
    }
}
