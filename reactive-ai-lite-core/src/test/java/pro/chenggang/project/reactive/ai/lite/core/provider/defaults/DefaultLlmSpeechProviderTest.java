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
package pro.chenggang.project.reactive.ai.lite.core.provider.defaults;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SpeechExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.delegate.LlmSpeechProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the DefaultLlmSpeechProvider.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
class DefaultLlmSpeechProviderTest {

    /**
     * Tests the constructor and getter methods.
     */
    @Test
    void testConstructorAndGetters() {
        LlmSpeechProviderDelegate delegate = mock(LlmSpeechProviderDelegate.class);
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(delegate.providerInfo()).thenReturn(providerInfo);

        TokenCertification defaultCert = mock(TokenCertification.class);
        when(defaultCert.isDefault()).thenReturn(true);
        when(defaultCert.profile()).thenReturn("default");

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);

        DefaultLlmSpeechProvider provider = new DefaultLlmSpeechProvider(delegate, List.of(defaultCert), registry);

        assertThat(provider.capability()).isEqualTo(Capability.SPEECH);
        assertThat(provider.info()).isEqualTo(providerInfo);
    }

    /**
     * Tests constructor behavior when no default certification is provided.
     */
    @Test
    void testConstructorNoDefaultCert() {
        LlmSpeechProviderDelegate delegate = mock(LlmSpeechProviderDelegate.class);
        TokenCertification notDefaultCert = mock(TokenCertification.class);
        when(notDefaultCert.isDefault()).thenReturn(false);
        when(notDefaultCert.profile()).thenReturn("not-default");

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);

        assertThatThrownBy(() -> new DefaultLlmSpeechProvider(delegate, List.of(notDefaultCert), registry))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Tests standard speech execution logic.
     */
    @Test
    void testExecuteSpeech() {
        LlmSpeechProviderDelegate delegate = mock(LlmSpeechProviderDelegate.class);
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(providerInfo.name()).thenReturn("test-provider");
        when(delegate.providerInfo()).thenReturn(providerInfo);
        
        TokenCertification defaultCert = mock(TokenCertification.class);
        when(defaultCert.isDefault()).thenReturn(true);
        when(defaultCert.profile()).thenReturn("default");

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);
        when(registry.interceptGeneral(any(), any())).thenAnswer(inv -> inv.getArgument(1));

        ObjectNode rawRequest = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        when(delegate.initializeRequestBody(any())).thenReturn(rawRequest);

        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        when(delegate.loadRequestBodySpec(any())).thenReturn(requestBodySpec);
        
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        when(requestBodySpec.bodyValue(any())).thenReturn(headersSpec);
        
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        
        DataBuffer mockBuffer = DefaultDataBufferFactory.sharedInstance.wrap("audio-binary-data".getBytes());
        when(delegate.extractGeneralResponse(any(), any())).thenReturn(Mono.just(mockBuffer));

        DefaultLlmSpeechProvider provider = new DefaultLlmSpeechProvider(delegate, List.of(defaultCert), registry);

        SpeechExecutionInfo executionInfo = SpeechExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(c -> "model")
                .inputTextConfigure(c -> "hello")
                .voiceConfigure(c -> "nova")
                .build();

        StepVerifier.create(provider.executeSpeech(executionInfo).contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .consumeNextWith(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getAudioData()).isNotNull();
                    byte[] audioBytes = response.getAudioData();
                    assertThat(new String(audioBytes)).isEqualTo("audio-binary-data");
                })
                .verifyComplete();
    }

    /**
     * Tests streaming speech execution logic.
     */
    @Test
    void testExecuteSpeechStream() {
        LlmSpeechProviderDelegate delegate = mock(LlmSpeechProviderDelegate.class);
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(providerInfo.name()).thenReturn("test-provider");
        when(delegate.providerInfo()).thenReturn(providerInfo);
        
        TokenCertification defaultCert = mock(TokenCertification.class);
        when(defaultCert.isDefault()).thenReturn(true);
        when(defaultCert.profile()).thenReturn("default");

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);
        when(registry.interceptStream(any(), any())).thenAnswer(inv -> inv.getArgument(1));

        ObjectNode rawRequest = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        when(delegate.initializeRequestBody(any())).thenReturn(rawRequest);

        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        when(delegate.loadRequestBodySpec(any())).thenReturn(requestBodySpec);
        
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        when(requestBodySpec.bodyValue(any())).thenReturn(headersSpec);
        
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        
        DataBuffer chunk1 = DefaultDataBufferFactory.sharedInstance.wrap("chunk1".getBytes());
        DataBuffer chunk2 = DefaultDataBufferFactory.sharedInstance.wrap("chunk2".getBytes());
        when(delegate.extractStreamResponse(any(), any())).thenReturn(Flux.just(chunk1, chunk2));

        DefaultLlmSpeechProvider provider = new DefaultLlmSpeechProvider(delegate, List.of(defaultCert), registry);

        SpeechExecutionInfo executionInfo = SpeechExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(c -> "model")
                .inputTextConfigure(c -> "hello")
                .voiceConfigure(c -> "nova")
                .build();

        StepVerifier.create(provider.executeSpeechStream(executionInfo).contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .consumeNextWith(response -> {
                    assertThat(response).isNotNull();
                    byte[] bytes = response.getChunk();
                    assertThat(new String(bytes)).isEqualTo("chunk1");
                })
                .consumeNextWith(response -> {
                    assertThat(response).isNotNull();
                    byte[] bytes = response.getChunk();
                    assertThat(new String(bytes)).isEqualTo("chunk2");
                })
                .verifyComplete();
    }
}
