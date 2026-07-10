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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import reactor.core.publisher.Flux;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExchangeImplPackageTest {

    @Test
    void testDefaultLlmProviderRequestExchange() {
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        ObjectNode requestBody = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        ExecutionContext context = ExecutionContext.newContext();

        DefaultLlmProviderRequestExchange exchange = DefaultLlmProviderRequestExchange.builder()
                .clientType(LlmClientType.CHAT)
                .attributes(new HashMap<>())
                .llmProviderInfo(providerInfo)
                .executionContext(context)
                .rawRequestBody(requestBody)
                .build();

        assertThat(exchange.clientType()).isEqualTo(LlmClientType.CHAT);
        assertThat(exchange.getAttributes()).isNotNull();
        assertThat(exchange.llmProviderInfo()).isEqualTo(providerInfo);
        assertThat(exchange.executionContext()).isEqualTo(context);
        assertThat(exchange.rawRequestBody()).isEqualTo(requestBody);
    }

    @Test
    void testDefaultLlmProviderGeneralResponseExchange() {
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        ObjectNode responseBody = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        ExecutionContext context = ExecutionContext.newContext();
        RuntimeException error = new RuntimeException("test error");

        DefaultLlmProviderGeneralResponseExchange exchange = DefaultLlmProviderGeneralResponseExchange.builder()
                .clientType(LlmClientType.CHAT)
                .attributes(new HashMap<>())
                .llmProviderInfo(providerInfo)
                .executionContext(context)
                .rawResponseBody(responseBody)
                .error(error)
                .build();

        assertThat(exchange.rawResponseBody()).contains(responseBody);
        assertThat(exchange.error()).contains(error);
    }

    @Test
    void testDefaultLlmProviderStreamResponseExchange() {
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        Flux<RawStreamResponse> streamResponse = Flux.empty();
        ExecutionContext context = ExecutionContext.newContext();
        RuntimeException error = new RuntimeException("test error");

        DefaultLlmProviderStreamResponseExchange exchange = DefaultLlmProviderStreamResponseExchange.builder()
                .clientType(LlmClientType.CHAT)
                .attributes(new HashMap<>())
                .llmProviderInfo(providerInfo)
                .executionContext(context)
                .rawStreamResponse(streamResponse)
                .error(error)
                .build();

        assertThat(exchange.rawStreamResponse()).isEqualTo(streamResponse);
        assertThat(exchange.error()).contains(error);
    }
}
