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
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderRequestInterceptorChain;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderResponseInterceptorChain;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderGeneralResponseExchange;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderRequestExchange;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderStreamResponseExchange;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoggingInterceptorTest {

    @Test
    void testLlmProviderExecutionLoggingInterceptor() {
        LlmProviderExecutionLoggingInterceptor interceptor = new LlmProviderExecutionLoggingInterceptor(() -> true);
        
        assertThat(interceptor.getOrder()).isEqualTo(Integer.MIN_VALUE);
        assertThat(interceptor.supportedClient()).contains(LlmClientType.CHAT);

        LlmProviderRequestExchange requestExchange = mock(LlmProviderRequestExchange.class);
        when(requestExchange.getAttributes()).thenReturn(new HashMap<>());
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(providerInfo.baseUrl()).thenReturn("http://localhost");
        when(providerInfo.endpoint()).thenReturn("/test");
        when(requestExchange.llmProviderInfo()).thenReturn(providerInfo);
        when(requestExchange.rawRequestBody()).thenReturn(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());
        when(requestExchange.clientType()).thenReturn(LlmClientType.CHAT);
        
        LlmProviderRequestInterceptorChain reqChain = mock(LlmProviderRequestInterceptorChain.class);
        when(reqChain.next(any(LlmProviderRequestExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(interceptor.interceptBefore(requestExchange, reqChain))
                .verifyComplete();
                
        // Test already existing instant attribute
        Map<String, Object> existingAttrs = new HashMap<>();
        existingAttrs.put(LlmProviderExecutionLoggingInterceptor.EXECUTION_INSTANT_ATTR_KEY, Instant.now());
        when(requestExchange.getAttributes()).thenReturn(existingAttrs);
        StepVerifier.create(interceptor.interceptBefore(requestExchange, reqChain))
                .verifyComplete();

        LlmProviderGeneralResponseExchange responseExchange = mock(LlmProviderGeneralResponseExchange.class);
        Map<String, Object> attrs = new HashMap<>();
        when(responseExchange.getAttributes()).thenReturn(attrs);
        when(responseExchange.getAttributeOrDefault(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(responseExchange.error()).thenReturn(Optional.of(new RuntimeException("Test Exception")));
        when(responseExchange.rawResponseBody()).thenReturn(Optional.of(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode()));
        
        LlmProviderResponseInterceptorChain resChain = mock(LlmProviderResponseInterceptorChain.class);
        when(resChain.next(any(LlmProviderGeneralResponseExchange.class))).thenReturn(Mono.empty());
        when(resChain.next(any(LlmProviderStreamResponseExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(interceptor.interceptAfter(responseExchange, resChain))
                .verifyComplete();
                
        // test already logged
        StepVerifier.create(interceptor.interceptAfter(responseExchange, resChain))
                .verifyComplete();

        assertThat(attrs.get(LlmProviderExecutionLoggingInterceptor.ALREADY_LOGGED_ATTR_KEY)).isEqualTo(true);

        // Stream
        LlmProviderStreamResponseExchange streamExchange = mock(LlmProviderStreamResponseExchange.class);
        Map<String, Object> streamAttrs = new HashMap<>();
        when(streamExchange.getAttributes()).thenReturn(streamAttrs);
        when(streamExchange.getAttributeOrDefault(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(streamExchange.error()).thenReturn(Optional.empty());
        
        RawStreamResponse chunk = mock(RawStreamResponse.class);
        when(chunk.getDataContent()).thenReturn(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());
        when(streamExchange.rawStreamResponse()).thenReturn(Flux.just(chunk));

        StepVerifier.create(interceptor.interceptAfterEach(streamExchange, resChain))
                .verifyComplete();
                
        // test stream already logged
        StepVerifier.create(interceptor.interceptAfterEach(streamExchange, resChain))
                .verifyComplete();
                
        // test stream error
        when(streamExchange.error()).thenReturn(Optional.of(new RuntimeException("Stream Error")));
        when(streamExchange.getAttributes()).thenReturn(new HashMap<>());
        StepVerifier.create(interceptor.interceptAfterEach(streamExchange, resChain))
                .verifyComplete();
                
        // Test Disabled Logging
        LlmProviderExecutionLoggingInterceptor disabledInterceptor = new LlmProviderExecutionLoggingInterceptor(() -> false);
        StepVerifier.create(disabledInterceptor.interceptBefore(requestExchange, reqChain)).verifyComplete();
        StepVerifier.create(disabledInterceptor.interceptAfter(responseExchange, resChain)).verifyComplete();
        StepVerifier.create(disabledInterceptor.interceptAfterEach(streamExchange, resChain)).verifyComplete();
    }
}
