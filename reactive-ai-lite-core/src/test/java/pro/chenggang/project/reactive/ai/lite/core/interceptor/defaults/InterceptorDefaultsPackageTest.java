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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InterceptorDefaultsPackageTest {

    @Test
    void testDefaultLlmProviderInterceptorRegistryGeneral() {
        LlmProviderExecutionBeforeInterceptor before = mock(LlmProviderExecutionBeforeInterceptor.class);
        when(before.supportedClient()).thenReturn(Set.of(LlmClientType.CHAT));
        when(before.getOrder()).thenReturn(1);
        when(before.interceptBefore(any(), any())).thenReturn(Mono.empty());

        LlmProviderExecutionAfterInterceptor after = mock(LlmProviderExecutionAfterInterceptor.class);
        when(after.supportedClient()).thenReturn(Set.of(LlmClientType.CHAT));
        when(after.getOrder()).thenReturn(1);
        when(after.interceptAfter(any(), any())).thenReturn(Mono.empty());
        when(after.interceptAfterEach(any(), any())).thenReturn(Mono.empty());

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(
                List.of(before),
                List.of(after)
        );

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        ObjectNode requestBody = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        ExecutionContext context = ExecutionContext.newContext();

        InterceptedDataInfo dataInfo = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(providerInfo)
                .executionContext(context)
                .rawRequestBody(requestBody)
                .build();

        Mono<ObjectNode> execution = Mono.just(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());

        StepVerifier.create(registry.interceptGeneral(dataInfo, execution))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void testDefaultLlmProviderInterceptorRegistryStream() {
        LlmProviderExecutionBeforeInterceptor before = mock(LlmProviderExecutionBeforeInterceptor.class);
        when(before.supportedClient()).thenReturn(Set.of(LlmClientType.CHAT));
        when(before.getOrder()).thenReturn(1);
        when(before.interceptBefore(any(), any())).thenReturn(Mono.empty());

        LlmProviderExecutionAfterInterceptor after = mock(LlmProviderExecutionAfterInterceptor.class);
        when(after.supportedClient()).thenReturn(Set.of(LlmClientType.CHAT));
        when(after.getOrder()).thenReturn(1);
        when(after.interceptAfter(any(), any())).thenReturn(Mono.empty());
        when(after.interceptAfterEach(any(), any())).thenReturn(Mono.empty());

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(
                List.of(before),
                List.of(after)
        );

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        ObjectNode requestBody = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        ExecutionContext context = ExecutionContext.newContext();

        InterceptedDataInfo dataInfo = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(providerInfo)
                .executionContext(context)
                .rawRequestBody(requestBody)
                .build();

        Flux<RawStreamResponse> execution = Flux.just(mock(RawStreamResponse.class));

        StepVerifier.create(registry.interceptStream(dataInfo, execution))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void testDefaultLlmProviderInterceptorRegistryGeneralError() {
        LlmProviderExecutionBeforeInterceptor before = mock(LlmProviderExecutionBeforeInterceptor.class);
        when(before.supportedClient()).thenReturn(Set.of(LlmClientType.CHAT));
        when(before.interceptBefore(any(), any())).thenReturn(Mono.empty());

        LlmProviderExecutionAfterInterceptor after = mock(LlmProviderExecutionAfterInterceptor.class);
        when(after.supportedClient()).thenReturn(Set.of(LlmClientType.CHAT));
        when(after.interceptAfter(any(), any())).thenReturn(Mono.empty());

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(List.of(before), List.of(after));

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        InterceptedDataInfo dataInfo = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(providerInfo)
                .executionContext(ExecutionContext.newContext())
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        Mono<ObjectNode> execution = Mono.error(new RuntimeException("Test Exception"));

        StepVerifier.create(registry.interceptGeneral(dataInfo, execution))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void testDefaultLlmProviderInterceptorRegistryStreamError() {
        LlmProviderExecutionBeforeInterceptor before = mock(LlmProviderExecutionBeforeInterceptor.class);
        when(before.supportedClient()).thenReturn(Set.of(LlmClientType.CHAT));
        when(before.interceptBefore(any(), any())).thenReturn(Mono.empty());

        LlmProviderExecutionAfterInterceptor after = mock(LlmProviderExecutionAfterInterceptor.class);
        when(after.supportedClient()).thenReturn(Set.of(LlmClientType.CHAT));
        when(after.interceptAfter(any(), any())).thenReturn(Mono.empty());
        when(after.interceptAfterEach(any(), any())).thenReturn(Mono.empty());

        DefaultLlmProviderInterceptorRegistry registry = new DefaultLlmProviderInterceptorRegistry(List.of(before), List.of(after));

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        InterceptedDataInfo dataInfo = InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(providerInfo)
                .executionContext(ExecutionContext.newContext())
                .rawRequestBody(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();

        Flux<RawStreamResponse> execution = Flux.error(new RuntimeException("Stream Exception"));

        StepVerifier.create(registry.interceptStream(dataInfo, execution))
                .expectError(RuntimeException.class)
                .verify();
    }
}
