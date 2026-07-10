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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.EmbeddingResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.EmbeddingExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.delegate.LlmEmbeddingProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultLlmEmbeddingProviderTest {

    @Test
    void testConstructorAndGetters() {
        LlmEmbeddingProviderDelegate delegate = mock(LlmEmbeddingProviderDelegate.class);
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(delegate.providerInfo()).thenReturn(providerInfo);
        
        TokenCertification defaultCert = mock(TokenCertification.class);
        when(defaultCert.isDefault()).thenReturn(true);
        when(defaultCert.profile()).thenReturn("default");

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);

        DefaultLlmEmbeddingProvider provider = new DefaultLlmEmbeddingProvider(delegate, List.of(defaultCert), registry);
        
        assertThat(provider.capability()).isEqualTo(Capability.EMBEDDING);
        assertThat(provider.info()).isEqualTo(providerInfo);
    }

    @Test
    void testConstructorNoDefaultCert() {
        LlmEmbeddingProviderDelegate delegate = mock(LlmEmbeddingProviderDelegate.class);
        TokenCertification notDefaultCert = mock(TokenCertification.class);
        when(notDefaultCert.isDefault()).thenReturn(false);
        when(notDefaultCert.profile()).thenReturn("not-default");

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);

        assertThatThrownBy(() -> new DefaultLlmEmbeddingProvider(delegate, List.of(notDefaultCert), registry))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testConstructorEmptyCerts() {
        LlmEmbeddingProviderDelegate delegate = mock(LlmEmbeddingProviderDelegate.class);
        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);

        DefaultLlmEmbeddingProvider provider = new DefaultLlmEmbeddingProvider(delegate, List.of(), registry);
        assertThat(provider.defaultCertification).isNull();
    }

    @Test
    void testExecuteEmbedding() {
        LlmEmbeddingProviderDelegate delegate = mock(LlmEmbeddingProviderDelegate.class);
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
        
        ObjectNode rawResponse = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(rawResponse));
        
        EmbeddingResponse embeddingResponse = mock(EmbeddingResponse.class);
        when(delegate.extractGeneralResponse(any())).thenReturn(Mono.just(embeddingResponse));

        DefaultLlmEmbeddingProvider provider = new DefaultLlmEmbeddingProvider(delegate, List.of(defaultCert), registry);

        EmbeddingExecutionInfo executionInfo = EmbeddingExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(c -> "model")
                .inputTextConfigure(c -> List.of("test"))
                .build();

        StepVerifier.create(provider.executeEmbedding(executionInfo).contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .expectNext(embeddingResponse)
                .verifyComplete();
    }

    @Test
    void testExecuteEmbeddingRaw() {
        LlmEmbeddingProviderDelegate delegate = mock(LlmEmbeddingProviderDelegate.class);
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
        
        ObjectNode rawResponseNode = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(rawResponseNode));
        
        DefaultLlmEmbeddingProvider provider = new DefaultLlmEmbeddingProvider(delegate, List.of(defaultCert), registry);

        EmbeddingExecutionInfo executionInfo = EmbeddingExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(c -> "model")
                .inputTextConfigure(c -> List.of("test"))
                .build();

        StepVerifier.create(provider.executeEmbeddingRaw(executionInfo).contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .consumeNextWith(rawResponse -> {
                    assertThat(rawResponse).isNotNull();
                    assertThat(rawResponse.getResponseBody()).isEqualTo(rawResponseNode);
                })
                .verifyComplete();
    }

    @Test
    void testToResponseSpecNullBody() {
        LlmEmbeddingProviderDelegate delegate = mock(LlmEmbeddingProviderDelegate.class);
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(providerInfo.name()).thenReturn("test-provider");
        when(delegate.providerInfo()).thenReturn(providerInfo);
        
        TokenCertification defaultCert = mock(TokenCertification.class);
        when(defaultCert.isDefault()).thenReturn(true);
        when(defaultCert.profile()).thenReturn("default");

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);

        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        when(delegate.loadRequestBodySpec(any())).thenReturn(requestBodySpec);
        
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        DefaultLlmEmbeddingProvider provider = new DefaultLlmEmbeddingProvider(delegate, List.of(defaultCert), registry);

        pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmEmbeddingRequestData requestData = mock(pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmEmbeddingRequestData.class);
        
        StepVerifier.create(provider.toResponseSpec(requestData, null))
                .expectNext(responseSpec)
                .verifyComplete();
    }

    @Test
    void testGenerateRawRequestBodyCustomizer() {
        LlmEmbeddingProviderDelegate delegate = mock(LlmEmbeddingProviderDelegate.class);
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(providerInfo.name()).thenReturn("test-provider");
        when(delegate.providerInfo()).thenReturn(providerInfo);
        
        TokenCertification defaultCert = mock(TokenCertification.class);
        when(defaultCert.isDefault()).thenReturn(true);
        when(defaultCert.profile()).thenReturn("default");

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);

        DefaultLlmEmbeddingProvider provider = new DefaultLlmEmbeddingProvider(delegate, List.of(defaultCert), registry);

        pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmEmbeddingRequestData requestData = mock(pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmEmbeddingRequestData.class);
        ObjectNode rawRequest = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        when(delegate.initializeRequestBody(any())).thenReturn(rawRequest);

        java.util.function.BiConsumer<ExecutionContext, ObjectNode> customizer = (ctx, body) -> body.put("custom", "value");
        when(requestData.getRawRequestCustomizerConfigure()).thenReturn(customizer);

        StepVerifier.create(provider.generateRawRequestBody(requestData))
                .consumeNextWith(body -> {
                    assertThat(body.get("custom").asText()).isEqualTo("value");
                })
                .verifyComplete();
    }
    @Test
    void testNonNullChecks() throws Exception {
        LlmEmbeddingProviderDelegate delegate = mock(LlmEmbeddingProviderDelegate.class);
        TokenCertification defaultCert = mock(TokenCertification.class);
        when(defaultCert.isDefault()).thenReturn(true);
        when(defaultCert.profile()).thenReturn("default");
        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);
        
        assertThatThrownBy(() -> new DefaultLlmEmbeddingProvider(null, List.of(defaultCert), registry))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultLlmEmbeddingProvider(delegate, null, registry))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultLlmEmbeddingProvider(delegate, List.of(defaultCert), null))
                .isInstanceOf(IllegalArgumentException.class);
                
        DefaultLlmEmbeddingProvider provider = new DefaultLlmEmbeddingProvider(delegate, List.of(defaultCert), registry);
        
        assertThatThrownBy(() -> provider.executeEmbedding(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.executeEmbeddingRaw(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.executeInternalRaw(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.generateRawRequestBody(null))
                .isInstanceOf(IllegalArgumentException.class);
                
        java.lang.reflect.Method method = DefaultLlmEmbeddingProvider.class.getDeclaredMethod("initializeLlmRequestData", EmbeddingExecutionInfo.class);
        method.setAccessible(true);
        try {
            method.invoke(provider, (EmbeddingExecutionInfo) null);
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertThat(e.getTargetException()).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
