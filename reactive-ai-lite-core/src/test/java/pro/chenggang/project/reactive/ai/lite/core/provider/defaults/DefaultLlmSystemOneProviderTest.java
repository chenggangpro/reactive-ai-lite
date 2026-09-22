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
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SystemOneExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestions;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.delegate.LlmSystemOneProviderDelegate;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLlmSystemOneProvider} and {@link LlmSystemOneProviderDelegate}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
class DefaultLlmSystemOneProviderTest {

    @Test
    void testConstructorAndGetters() {
        LlmSystemOneProviderDelegate delegate = mock(LlmSystemOneProviderDelegate.class);
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(delegate.providerInfo()).thenReturn(providerInfo);

        TokenCertification defaultCert = mock(TokenCertification.class);
        when(defaultCert.isDefault()).thenReturn(true);
        when(defaultCert.profile()).thenReturn("default");

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);

        DefaultLlmSystemOneProvider provider = new DefaultLlmSystemOneProvider(delegate, List.of(defaultCert), registry);

        assertThat(provider.capability()).isEqualTo(Capability.SYSTEM_ONE);
        assertThat(provider.info()).isEqualTo(providerInfo);
    }

    @Test
    void testConstructorNoDefaultCert() {
        LlmSystemOneProviderDelegate delegate = mock(LlmSystemOneProviderDelegate.class);
        TokenCertification notDefaultCert = mock(TokenCertification.class);
        when(notDefaultCert.isDefault()).thenReturn(false);
        when(notDefaultCert.profile()).thenReturn("not-default");

        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);

        assertThatThrownBy(() -> new DefaultLlmSystemOneProvider(delegate, List.of(notDefaultCert), registry))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testConstructorEmptyCerts() {
        LlmSystemOneProviderDelegate delegate = mock(LlmSystemOneProviderDelegate.class);
        LlmProviderInterceptorRegistry registry = mock(LlmProviderInterceptorRegistry.class);

        DefaultLlmSystemOneProvider provider = new DefaultLlmSystemOneProvider(delegate, List.of(), registry);
        assertThat(provider.defaultCertification).isNull();
    }

    @Test
    void testExecuteSystemOne() {
        LlmSystemOneProviderDelegate delegate = mock(LlmSystemOneProviderDelegate.class);
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

        SystemOneResponse systemOneResponse = mock(SystemOneResponse.class);
        SystemOneQuestions questions = SystemOneQuestions.of("q1", SystemOneQuestion.newNoulBuilder("test").build());
        when(delegate.extractGeneralResponse(any(RawResponse.class))).thenReturn(Mono.just(systemOneResponse));

        DefaultLlmSystemOneProvider provider = new DefaultLlmSystemOneProvider(delegate, List.of(defaultCert), registry);

        SystemOneExecutionInfo executionInfo = SystemOneExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(c -> "jev-1")
                .stateConfigure(c -> SystemOneContent.text("sample payload"))
                .questionsConfigure(c -> questions)
                .build();

        StepVerifier.create(provider.executeSystemOne(executionInfo).contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .expectNext(systemOneResponse)
                .verifyComplete();

        verify(delegate).extractGeneralResponse(any(RawResponse.class));
    }

    @Test
    void testExecuteSystemOneRaw() {
        LlmSystemOneProviderDelegate delegate = mock(LlmSystemOneProviderDelegate.class);
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

        ObjectNode rawResponseNode = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode().put("status", "ok");
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(rawResponseNode));

        DefaultLlmSystemOneProvider provider = new DefaultLlmSystemOneProvider(delegate, List.of(defaultCert), registry);

        SystemOneQuestions questions = SystemOneQuestions.of("q1", SystemOneQuestion.newNoulBuilder("test").build());
        SystemOneExecutionInfo executionInfo = SystemOneExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(c -> "jev-1")
                .stateConfigure(c -> SystemOneContent.text("sample payload"))
                .questionsConfigure(c -> questions)
                .build();

        StepVerifier.create(provider.executeSystemOneRaw(executionInfo).contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .assertNext(rawResponse -> {
                    assertThat(rawResponse.getResponseBody()).isEqualTo(rawResponseNode);
                    assertThat(rawResponse.getExecutionContext()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void testExecuteWithRawRequestCustomizer() {
        LlmSystemOneProviderDelegate delegate = mock(LlmSystemOneProviderDelegate.class);
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

        DefaultLlmSystemOneProvider provider = new DefaultLlmSystemOneProvider(delegate, List.of(defaultCert), registry);

        AtomicBoolean customizerCalled = new AtomicBoolean(false);
        SystemOneQuestions questions = SystemOneQuestions.of("q1", SystemOneQuestion.newNoulBuilder("test").build());
        SystemOneExecutionInfo executionInfo = SystemOneExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(c -> "jev-1")
                .stateConfigure(c -> SystemOneContent.text("sample payload"))
                .questionsConfigure(c -> questions)
                .rawRequestCustomizerConfigure((ctx, node) -> {
                    customizerCalled.set(true);
                    node.put("custom_field", "custom_value");
                })
                .build();

        StepVerifier.create(provider.executeSystemOneRaw(executionInfo).contextWrite(Context.of(ExecutionContext.class, ExecutionContext.newContext())))
                .assertNext(rawResponse -> {
                    assertThat(customizerCalled).isTrue();
                    assertThat(rawRequest.has("custom_field")).isTrue();
                    assertThat(rawRequest.get("custom_field").asText()).isEqualTo("custom_value");
                })
                .verifyComplete();
    }

}
