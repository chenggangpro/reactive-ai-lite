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

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionBeforeInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderRequestExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LLmProviderExecutionBeforeInterceptorChainTest {

    @Test
    void testChainExecution() {
        LlmProviderExecutionBeforeInterceptor interceptor = mock(LlmProviderExecutionBeforeInterceptor.class);
        when(interceptor.interceptBefore(any(), any())).thenReturn(Mono.empty());

        LlmProviderExecutionBeforeInterceptorChain chain = new LlmProviderExecutionBeforeInterceptorChain(
                Collections.singletonList(interceptor)
        );

        LlmProviderRequestExchange exchange = mock(LlmProviderRequestExchange.class);
        StepVerifier.create(chain.next(exchange))
                .verifyComplete();

        verify(interceptor).interceptBefore(any(), any());
    }
}
