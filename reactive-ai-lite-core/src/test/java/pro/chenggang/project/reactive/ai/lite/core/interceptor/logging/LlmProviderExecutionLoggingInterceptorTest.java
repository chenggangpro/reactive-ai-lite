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
package pro.chenggang.project.reactive.ai.lite.core.interceptor.logging;

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderRequestInterceptorChain;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderResponseInterceptorChain;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderGeneralResponseExchange;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderRequestExchange;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderStreamResponseExchange;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmProviderExecutionLoggingInterceptorTest {

    @Test
    void testInterceptBefore() {
        LlmProviderExecutionLoggingInterceptor interceptor = new LlmProviderExecutionLoggingInterceptor(() -> true);
        LlmProviderRequestExchange exchange = mock(LlmProviderRequestExchange.class);
        LlmProviderRequestInterceptorChain chain = mock(LlmProviderRequestInterceptorChain.class);
        LlmProviderInfo info = mock(LlmProviderInfo.class);

        when(exchange.llmProviderInfo()).thenReturn(info);
        when(exchange.getAttributes()).thenReturn(new HashMap<>());
        when(exchange.rawRequestBody()).thenReturn(null);
        when(chain.next(any())).thenReturn(Mono.empty());

        StepVerifier.create(interceptor.interceptBefore(exchange, chain))
                .verifyComplete();
    }

    @Test
    void testInterceptAfterEach() {
        LlmProviderExecutionLoggingInterceptor interceptor = new LlmProviderExecutionLoggingInterceptor(() -> true);
        LlmProviderStreamResponseExchange exchange = mock(LlmProviderStreamResponseExchange.class);
        LlmProviderResponseInterceptorChain chain = mock(LlmProviderResponseInterceptorChain.class);

        when(exchange.rawStreamResponse()).thenReturn(reactor.core.publisher.Flux.empty());
        when(chain.next(any(LlmProviderStreamResponseExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(interceptor.interceptAfterEach(exchange, chain))
                .verifyComplete();
    }

    @Test
    void testInterceptBeforeDisabled() {
        LlmProviderExecutionLoggingInterceptor interceptor = new LlmProviderExecutionLoggingInterceptor(() -> false);
        LlmProviderRequestExchange exchange = mock(LlmProviderRequestExchange.class);
        LlmProviderRequestInterceptorChain chain = mock(LlmProviderRequestInterceptorChain.class);

        when(chain.next(any())).thenReturn(Mono.empty());

        StepVerifier.create(interceptor.interceptBefore(exchange, chain))
                .verifyComplete();
    }

    @Test
    void testInterceptAfterWithError() {
        LlmProviderExecutionLoggingInterceptor interceptor = new LlmProviderExecutionLoggingInterceptor(() -> true);
        LlmProviderGeneralResponseExchange exchange = mock(LlmProviderGeneralResponseExchange.class);
        LlmProviderResponseInterceptorChain chain = mock(LlmProviderResponseInterceptorChain.class);

        when(exchange.getAttributes()).thenReturn(new HashMap<>());
        when(exchange.getAttributeOrDefault(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(exchange.rawResponseBody()).thenReturn(Optional.empty());
        when(exchange.error()).thenReturn(Optional.of(new RuntimeException("test")));
        when(chain.next(any(LlmProviderGeneralResponseExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(interceptor.interceptAfter(exchange, chain))
                .verifyComplete();
    }
}
