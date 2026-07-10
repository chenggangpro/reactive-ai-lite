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
import org.mockito.ArgumentCaptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmChatRequestData;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.delegate.LlmChatProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser.JsonStreamChunkSlide;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultLlmChatProviderTest {

    @Test
    void testConstructorAndInfo() {
        LlmChatProviderDelegate delegate = mock(LlmChatProviderDelegate.class);
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(delegate.providerInfo()).thenReturn(providerInfo);
        
        TokenCertification defaultCert = mock(TokenCertification.class);
        when(defaultCert.isDefault()).thenReturn(true);
        when(defaultCert.profile()).thenReturn("default");

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);

        DefaultLlmChatProvider provider = new DefaultLlmChatProvider(delegate, List.of(defaultCert), registry);

        assertThat(provider.info()).isEqualTo(providerInfo);
    }

    @Test
    void testConstructorNoDefaultCert() {
        LlmChatProviderDelegate delegate = mock(LlmChatProviderDelegate.class);
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(delegate.providerInfo()).thenReturn(providerInfo);
        
        TokenCertification notDefaultCert = mock(TokenCertification.class);
        when(notDefaultCert.isDefault()).thenReturn(false);
        when(notDefaultCert.profile()).thenReturn("not-default");

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);

        assertThatThrownBy(() -> new DefaultLlmChatProvider(delegate, List.of(notDefaultCert), registry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one default TokenCertification is required");
    }

    @Test
    void testConstructorEmptyCerts() {
        LlmChatProviderDelegate delegate = mock(LlmChatProviderDelegate.class);
        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);

        DefaultLlmChatProvider provider = new DefaultLlmChatProvider(delegate, List.of(), registry);

        assertThat(provider.defaultCertification).isNull();
    }

    @Test
    void testExecuteGeneral() {
        LlmChatProviderDelegate delegate = mock(LlmChatProviderDelegate.class);
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
        // retrieve is called directly on requestBodySpec because bodyValue returns RequestHeadersSpec
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        
        ObjectNode responseNode = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(responseNode));
        
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        when(delegate.extractGeneralResponse(any(), any())).thenReturn(Mono.just(generalResponse));

        DefaultLlmChatProvider provider = new DefaultLlmChatProvider(delegate, List.of(defaultCert), registry);

        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(c -> "model")
                .textMessageConfigure(c -> "test")
                .build();

        StepVerifier.create(provider.executeGeneral(executionInfo).contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .expectNext(generalResponse)
                .verifyComplete();
                
        verify(delegate).checkTokenCertification(any());
        
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Predicate<HttpStatusCode>> statusCaptor = ArgumentCaptor.forClass(Predicate.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<ClientResponse, Mono<? extends Throwable>>> functionCaptor = ArgumentCaptor.forClass(Function.class);
        verify(responseSpec).onStatus(statusCaptor.capture(), functionCaptor.capture());
        
        Predicate<HttpStatusCode> predicate = statusCaptor.getValue();
        assertThat(predicate.test(org.springframework.http.HttpStatus.BAD_REQUEST)).isTrue();
        assertThat(predicate.test(org.springframework.http.HttpStatus.OK)).isFalse();
    }
    
    @Test
    void testExecuteGeneralRaw() {
        LlmChatProviderDelegate delegate = mock(LlmChatProviderDelegate.class);
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
        
        ObjectNode responseNode = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        responseNode.put("key", "value");
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(responseNode));

        DefaultLlmChatProvider provider = new DefaultLlmChatProvider(delegate, List.of(defaultCert), registry);

        AtomicBoolean customizerCalled = new AtomicBoolean(false);
        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(c -> "model")
                .textMessageConfigure(c -> "test")
                .rawRequestCustomizerConfigure((ctx, node) -> {
                    customizerCalled.set(true);
                })
                .build();

        StepVerifier.create(provider.executeGeneralRaw(executionInfo).contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .expectNextMatches(rawResponse -> rawResponse.getResponseBody().get("key").asText().equals("value"))
                .verifyComplete();
                
        assertThat(customizerCalled.get()).isTrue();
    }

    @Test
    void testToResponseSpecNullBody() {
        LlmChatProviderDelegate delegate = mock(LlmChatProviderDelegate.class);
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(providerInfo.name()).thenReturn("test-provider");
        when(delegate.providerInfo()).thenReturn(providerInfo);
        
        TokenCertification defaultCert = mock(TokenCertification.class);
        when(defaultCert.isDefault()).thenReturn(true);
        when(defaultCert.profile()).thenReturn("default");

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);
        DefaultLlmChatProvider provider = new DefaultLlmChatProvider(delegate, List.of(defaultCert), registry);

        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        when(delegate.loadRequestBodySpec(any())).thenReturn(requestBodySpec);
        
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(c -> "model")
                .textMessageConfigure(c -> "test")
                .build();
                
        LlmChatRequestData data = LlmChatRequestData.LlmChatRequestDataInitializer
                .of(provider.certificationMap, provider.defaultCertification, providerInfo, executionInfo, false)
                .initialize()
                .contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext()))
                .block();

        StepVerifier.create(provider.toResponseSpec(data, null))
                .expectNext(responseSpec)
                .verifyComplete();
    }

    @Test
    void testExecuteStream() {
        LlmChatProviderDelegate delegate = mock(LlmChatProviderDelegate.class);
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
        
        when(responseSpec.bodyToFlux(String.class)).thenReturn(Flux.just("{}"));
        
        JsonStreamChunkSlide slide = JsonStreamChunkSlide.builder()
                .streamDataType(StreamDataType.TOOL_CALL)
                .dataContent(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();
        when(delegate.extractStreamChunks(any())).thenReturn(new JsonStreamChunkSlide[]{slide});
        when(delegate.mergeRawToolCallMessages(any(), anyBoolean())).thenReturn(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());

        DefaultLlmChatProvider provider = new DefaultLlmChatProvider(delegate, List.of(defaultCert), registry);

        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(c -> "model")
                .textMessageConfigure(c -> "test")
                .build();

        when(delegate.extractStreamResponseContent(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(provider.executeStream(executionInfo).contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .verifyComplete();
    }
    
    @Test
    void testExecuteStreamRaw() {
        LlmChatProviderDelegate delegate = mock(LlmChatProviderDelegate.class);
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
        
        when(responseSpec.bodyToFlux(String.class)).thenReturn(Flux.just("{}"));
        
        JsonStreamChunkSlide slide = JsonStreamChunkSlide.builder()
                .streamDataType(StreamDataType.TOOL_CALL)
                .dataContent(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode())
                .build();
        when(delegate.extractStreamChunks(any())).thenReturn(new JsonStreamChunkSlide[]{slide});
        when(delegate.mergeRawToolCallMessages(any(), anyBoolean())).thenReturn(JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());

        DefaultLlmChatProvider provider = new DefaultLlmChatProvider(delegate, List.of(defaultCert), registry);

        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(c -> "model")
                .textMessageConfigure(c -> "test")
                .build();

        StepVerifier.create(provider.executeStreamRaw(executionInfo).contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .expectNextCount(1)
                .verifyComplete();
    }
    @Test
    void testNonNullChecks() {
        LlmChatProviderDelegate delegate = mock(LlmChatProviderDelegate.class);
        TokenCertification defaultCert = mock(TokenCertification.class);
        when(defaultCert.isDefault()).thenReturn(true);
        when(defaultCert.profile()).thenReturn("default");
        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);
        
        assertThatThrownBy(() -> new DefaultLlmChatProvider(null, List.of(defaultCert), registry))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultLlmChatProvider(delegate, null, registry))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultLlmChatProvider(delegate, List.of(defaultCert), null))
                .isInstanceOf(IllegalArgumentException.class);
                
        DefaultLlmChatProvider provider = new DefaultLlmChatProvider(delegate, List.of(defaultCert), registry);
        
        assertThatThrownBy(() -> provider.executeGeneral(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.executeGeneralRaw(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.executeStream(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.executeStreamRaw(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.executeInternalRaw(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.generateRawRequestBody(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test
    void testPrivateInitializeLlmRequestDataNull() throws Exception {
        LlmChatProviderDelegate delegate = mock(LlmChatProviderDelegate.class);
        TokenCertification defaultCert = mock(TokenCertification.class);
        when(defaultCert.isDefault()).thenReturn(true);
        when(defaultCert.profile()).thenReturn("default");
        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);
        DefaultLlmChatProvider provider = new DefaultLlmChatProvider(delegate, List.of(defaultCert), registry);

        java.lang.reflect.Method method = DefaultLlmChatProvider.class.getDeclaredMethod("initializeLlmRequestData", ChatExecutionInfo.class, boolean.class);
        method.setAccessible(true);
        try {
            method.invoke(provider, null, false);
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertThat(e.getTargetException()).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
