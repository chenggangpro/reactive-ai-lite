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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.core.publisher.Flux;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LlmProviderExchangeTest {

    @Test
    void testDefaultLlmProviderRequestExchange() {
        LlmClientType clientType = LlmClientType.CHAT;
        LlmProviderInfo info = mock(LlmProviderInfo.class);
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        ObjectNode requestBody = JsonNodeFactory.instance.objectNode();
        
        DefaultLlmProviderRequestExchange exchange = DefaultLlmProviderRequestExchange.builder()
                .attributes(new HashMap<>())
                .clientType(clientType)
                .llmProviderInfo(info)
                .executionContextView(contextView)
                .rawRequestBody(requestBody)
                .build();
        
        assertThat(exchange.clientType()).isEqualTo(clientType);
        assertThat(exchange.llmProviderInfo()).isEqualTo(info);
        assertThat(exchange.contextView()).isEqualTo(contextView);
        assertThat(exchange.rawRequestBody()).isEqualTo(requestBody);
    }

    @Test
    void testDefaultLlmProviderGeneralResponseExchange() {
        LlmClientType clientType = LlmClientType.CHAT;
        LlmProviderInfo info = mock(LlmProviderInfo.class);
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        ObjectNode responseBody = JsonNodeFactory.instance.objectNode();
        Throwable error = new RuntimeException("err");
        
        DefaultLlmProviderGeneralResponseExchange exchange = DefaultLlmProviderGeneralResponseExchange.builder()
                .attributes(new HashMap<>())
                .clientType(clientType)
                .llmProviderInfo(info)
                .executionContextView(contextView)
                .rawResponseBody(responseBody)
                .error(error)
                .build();
        
        assertThat(exchange.rawResponseBody()).contains(responseBody);
        assertThat(exchange.error()).contains(error);
    }

    @Test
    void testDefaultLlmProviderStreamResponseExchange() {
        LlmClientType clientType = LlmClientType.CHAT;
        LlmProviderInfo info = mock(LlmProviderInfo.class);
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<RawStreamResponse> stream = Flux.empty();
        
        DefaultLlmProviderStreamResponseExchange exchange = DefaultLlmProviderStreamResponseExchange.builder()
                .attributes(new HashMap<>())
                .clientType(clientType)
                .llmProviderInfo(info)
                .executionContextView(contextView)
                .rawStreamResponse(stream)
                .build();
        
        assertThat(exchange.rawStreamResponse()).isEqualTo(stream);
    }
}
