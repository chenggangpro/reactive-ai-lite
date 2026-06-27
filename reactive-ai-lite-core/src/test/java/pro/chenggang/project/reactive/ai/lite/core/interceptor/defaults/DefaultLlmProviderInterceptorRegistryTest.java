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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionAfterInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionBeforeInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry.InterceptedDataInfo;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultLlmProviderInterceptorRegistryTest {

    @Test
    void testRegistryInitialization() {
        LlmProviderExecutionBeforeInterceptor before = mock(LlmProviderExecutionBeforeInterceptor.class);
        when(before.supportedClient()).thenReturn(Collections.singleton(LlmClientType.CHAT));
        when(before.getOrder()).thenReturn(0);

        LlmProviderExecutionAfterInterceptor after = mock(LlmProviderExecutionAfterInterceptor.class);
        when(after.supportedClient()).thenReturn(Collections.singleton(LlmClientType.CHAT));
        when(after.getOrder()).thenReturn(0);

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(
                Collections.singletonList(before),
                Collections.singletonList(after)
        );

        assertThat(registry).isNotNull();
    }

    @Test
    void testInterceptGeneral() {
        LlmProviderExecutionBeforeInterceptor before = mock(LlmProviderExecutionBeforeInterceptor.class);
        when(before.supportedClient()).thenReturn(Collections.singleton(LlmClientType.CHAT));
        when(before.getOrder()).thenReturn(0);
        when(before.interceptBefore(any(), any())).thenReturn(Mono.empty());

        LlmProviderExecutionAfterInterceptor after = mock(LlmProviderExecutionAfterInterceptor.class);
        when(after.supportedClient()).thenReturn(Collections.singleton(LlmClientType.CHAT));
        when(after.getOrder()).thenReturn(0);
        when(after.interceptAfter(any(), any())).thenReturn(Mono.empty());

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(
                Collections.singletonList(before),
                Collections.singletonList(after)
        );

        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonNodeFactory.instance.objectNode())
                .build();

        ObjectNode response = JsonNodeFactory.instance.objectNode();
        Mono<ObjectNode> execution = Mono.just(response);

        StepVerifier.create(registry.interceptGeneral(info, execution))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testInterceptStream() {
        LlmProviderExecutionBeforeInterceptor before = mock(LlmProviderExecutionBeforeInterceptor.class);
        when(before.supportedClient()).thenReturn(Collections.singleton(LlmClientType.CHAT));
        when(before.getOrder()).thenReturn(0);
        when(before.interceptBefore(any(),
                any()
        )).thenAnswer(inv -> ((pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderRequestInterceptorChain) inv.getArgument(1)).next(inv.getArgument(0)));

        LlmProviderExecutionAfterInterceptor after = mock(LlmProviderExecutionAfterInterceptor.class);
        when(after.supportedClient()).thenReturn(Collections.singleton(LlmClientType.CHAT));
        when(after.getOrder()).thenReturn(0);
        when(after.interceptAfterEach(any(),
                any()
        )).thenAnswer(inv -> ((pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderResponseInterceptorChain) inv.getArgument(1)).next((pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderStreamResponseExchange) inv.getArgument(
                0)));

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(
                Collections.singletonList(before),
                Collections.singletonList(after)
        );

        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonNodeFactory.instance.objectNode())
                .build();

        RawStreamResponse chunk = mock(RawStreamResponse.class);
        Flux<RawStreamResponse> execution = Flux.just(chunk);

        StepVerifier.create(registry.interceptStream(info, execution))
                .expectNext(chunk)
                .verifyComplete();
    }

    @Test
    void testInterceptGeneralWithNoMatchingInterceptors() {
        LlmProviderExecutionBeforeInterceptor before = mock(LlmProviderExecutionBeforeInterceptor.class);
        when(before.supportedClient()).thenReturn(Collections.singleton(LlmClientType.AUDIO));
        when(before.getOrder()).thenReturn(0);

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(
                Collections.singletonList(before),
                Collections.emptyList()
        );

        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonNodeFactory.instance.objectNode())
                .build();

        ObjectNode response = JsonNodeFactory.instance.objectNode();
        Mono<ObjectNode> execution = Mono.just(response);

        StepVerifier.create(registry.interceptGeneral(info, execution))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testInterceptStreamWithNoMatchingInterceptors() {
        LlmProviderExecutionAfterInterceptor after = mock(LlmProviderExecutionAfterInterceptor.class);
        when(after.supportedClient()).thenReturn(Collections.singleton(LlmClientType.AUDIO));
        when(after.getOrder()).thenReturn(0);

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(
                Collections.emptyList(),
                Collections.singletonList(after)
        );

        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonNodeFactory.instance.objectNode())
                .build();

        RawStreamResponse chunk = mock(RawStreamResponse.class);
        Flux<RawStreamResponse> execution = Flux.just(chunk);

        StepVerifier.create(registry.interceptStream(info, execution))
                .expectNext(chunk)
                .verifyComplete();
    }

    @Test
    void testInterceptGeneralWithBeforeInterceptorsOnly() {
        LlmProviderExecutionBeforeInterceptor before = mock(LlmProviderExecutionBeforeInterceptor.class);
        when(before.supportedClient()).thenReturn(Collections.singleton(LlmClientType.CHAT));
        when(before.getOrder()).thenReturn(0);
        when(before.interceptBefore(any(),
                any()
        )).thenAnswer(inv -> ((pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderRequestInterceptorChain) inv.getArgument(1)).next(inv.getArgument(0)));

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(
                Collections.singletonList(before),
                Collections.emptyList()
        );

        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonNodeFactory.instance.objectNode())
                .build();

        ObjectNode response = JsonNodeFactory.instance.objectNode();
        Mono<ObjectNode> execution = Mono.just(response);

        StepVerifier.create(registry.interceptGeneral(info, execution))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testInterceptGeneralWithAfterInterceptorsOnly() {
        LlmProviderExecutionAfterInterceptor after = mock(LlmProviderExecutionAfterInterceptor.class);
        when(after.supportedClient()).thenReturn(Collections.singleton(LlmClientType.CHAT));
        when(after.getOrder()).thenReturn(0);
        when(after.interceptAfter(any(),
                any()
        )).thenAnswer(inv -> ((pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderResponseInterceptorChain) inv.getArgument(1)).next((pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderGeneralResponseExchange) inv.getArgument(
                0)));

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(
                Collections.emptyList(),
                Collections.singletonList(after)
        );

        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonNodeFactory.instance.objectNode())
                .build();

        ObjectNode response = JsonNodeFactory.instance.objectNode();
        Mono<ObjectNode> execution = Mono.just(response);

        StepVerifier.create(registry.interceptGeneral(info, execution))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testInterceptGeneralWithMultipleInterceptors() {
        LlmProviderExecutionBeforeInterceptor before1 = mock(LlmProviderExecutionBeforeInterceptor.class);
        when(before1.supportedClient()).thenReturn(Collections.singleton(LlmClientType.CHAT));
        when(before1.getOrder()).thenReturn(1);
        when(before1.interceptBefore(any(),
                any()
        )).thenAnswer(inv -> ((pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderRequestInterceptorChain) inv.getArgument(1)).next(inv.getArgument(0)));

        LlmProviderExecutionBeforeInterceptor before2 = mock(LlmProviderExecutionBeforeInterceptor.class);
        when(before2.supportedClient()).thenReturn(Collections.singleton(LlmClientType.CHAT));
        when(before2.getOrder()).thenReturn(2);
        when(before2.interceptBefore(any(),
                any()
        )).thenAnswer(inv -> ((pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderRequestInterceptorChain) inv.getArgument(1)).next(inv.getArgument(0)));

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(
                java.util.Arrays.asList(before1, before2),
                Collections.emptyList()
        );

        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonNodeFactory.instance.objectNode())
                .build();

        ObjectNode response = JsonNodeFactory.instance.objectNode();
        Mono<ObjectNode> execution = Mono.just(response);

        StepVerifier.create(registry.interceptGeneral(info, execution))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testInterceptGeneralWithError() {
        LlmProviderExecutionAfterInterceptor after = mock(LlmProviderExecutionAfterInterceptor.class);
        when(after.supportedClient()).thenReturn(Collections.singleton(LlmClientType.CHAT));
        when(after.getOrder()).thenReturn(0);
        when(after.interceptAfter(any(),
                any()
        )).thenAnswer(inv -> ((pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderResponseInterceptorChain) inv.getArgument(1)).next((pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderGeneralResponseExchange) inv.getArgument(
                0)));

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(
                Collections.emptyList(),
                Collections.singletonList(after)
        );

        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonNodeFactory.instance.objectNode())
                .build();

        Mono<ObjectNode> execution = Mono.error(new RuntimeException("execution error"));

        StepVerifier.create(registry.interceptGeneral(info, execution))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void testInterceptStreamWithError() {
        LlmProviderExecutionAfterInterceptor after = mock(LlmProviderExecutionAfterInterceptor.class);
        when(after.supportedClient()).thenReturn(Collections.singleton(LlmClientType.CHAT));
        when(after.getOrder()).thenReturn(0);
        when(after.interceptAfterEach(any(),
                any()
        )).thenAnswer(inv -> ((pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderResponseInterceptorChain) inv.getArgument(1)).next((pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderStreamResponseExchange) inv.getArgument(
                0)));

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(
                Collections.emptyList(),
                Collections.singletonList(after)
        );

        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonNodeFactory.instance.objectNode())
                .build();

        Flux<RawStreamResponse> execution = Flux.error(new RuntimeException("stream error"));

        StepVerifier.create(registry.interceptStream(info, execution))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void testInterceptGeneralWithCancel() {
        LlmProviderExecutionAfterInterceptor after = mock(LlmProviderExecutionAfterInterceptor.class);
        when(after.supportedClient()).thenReturn(Collections.singleton(LlmClientType.CHAT));
        when(after.getOrder()).thenReturn(0);
        when(after.interceptAfter(any(),
                any()
        )).thenAnswer(inv -> ((pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderResponseInterceptorChain) inv.getArgument(1)).next((pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderGeneralResponseExchange) inv.getArgument(
                0)));

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(
                Collections.emptyList(),
                Collections.singletonList(after)
        );

        InterceptedDataInfo info = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(mock(LlmProviderInfo.class))
                .executionContext(mock(ExecutionContext.class))
                .rawRequestBody(JsonNodeFactory.instance.objectNode())
                .build();

        Mono<ObjectNode> execution = Mono.never();

        StepVerifier.create(registry.interceptGeneral(info, execution))
                .thenCancel()
                .verify();
    }
}
