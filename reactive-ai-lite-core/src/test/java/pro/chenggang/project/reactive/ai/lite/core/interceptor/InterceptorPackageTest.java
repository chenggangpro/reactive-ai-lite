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
package pro.chenggang.project.reactive.ai.lite.core.interceptor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InterceptorPackageTest {

    @Test
    void testInterceptedDataInfo() {
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        ObjectNode requestBody = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        ExecutionContext context = ExecutionContext.newContext();

        LlmProviderInterceptorRegistry.InterceptedDataInfo dataInfo = LlmProviderInterceptorRegistry.InterceptedDataInfo.builder()
                .clientType(LlmClientType.CHAT)
                .llmProviderInfo(providerInfo)
                .executionContext(context)
                .rawRequestBody(requestBody)
                .build();

        assertThat(dataInfo.getClientType()).isEqualTo(LlmClientType.CHAT);
        assertThat(dataInfo.getLlmProviderInfo()).isEqualTo(providerInfo);
        assertThat(dataInfo.getExecutionContext()).isEqualTo(context);
        assertThat(dataInfo.getRawRequestBody()).isEqualTo(requestBody);

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);
        Mono<ObjectNode> execution = Mono.just(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());
        when(registry.interceptGeneral(any(), ArgumentMatchers.<Mono<ObjectNode>>any())).thenReturn(execution);

        StepVerifier.create(dataInfo.interceptGeneral(registry, execution))
                .expectNextCount(1)
                .verifyComplete();

        Flux<RawStreamResponse> streamExecution = Flux.empty();
        when(registry.interceptStream(any(), ArgumentMatchers.<Flux<RawStreamResponse>>any())).thenReturn(streamExecution);

        StepVerifier.create(dataInfo.interceptStream(registry, streamExecution))
                .verifyComplete();
    }
}
