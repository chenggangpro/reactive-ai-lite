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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmChatRequestData;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StructuredResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LLmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser.JsonStreamChunkSlide;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractLlmChatProviderTest {

    @Mock
    private LLmProviderInterceptorRegistry interceptorRegistry;

    @Mock
    private TokenCertification tokenCertification;

    @Mock
    private LlmProviderInfo llmProviderInfo;

    private TestLlmChatProvider provider;

    static class TestLlmChatProvider extends AbstractLlmChatProvider {

        public TestLlmChatProvider(List<TokenCertification> certifications,
                                   Function<Map<String, TokenCertification>, LlmProviderInfo> llmProviderInfoInitializer,
                                   LLmProviderInterceptorRegistry lLmProviderInterceptorRegistry) {
            super(certifications, llmProviderInfoInitializer, lLmProviderInterceptorRegistry);
        }

        @Override
        protected RequestBodySpec loadRequestBodySpec(@NonNull LlmChatRequestData llmChatRequestData) {
            return mock(RequestBodySpec.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        }

        @Override
        protected ObjectNode initializeRequestBody(@NonNull LlmChatRequestData llmChatRequestData) {
            return JsonNodeFactory.instance.objectNode();
        }

        @Override
        protected JsonStreamChunkSlide[] extractStreamChunks(@NonNull ObjectNode rawResponseData) {
            return new JsonStreamChunkSlide[0];
        }

        @Override
        protected ObjectNode mergeRawToolCallMessages(@NonNull List<ObjectNode> rawToolCallMessages, boolean distinctToolCalls) {
            return JsonNodeFactory.instance.objectNode();
        }

        @Override
        protected Mono<GeneralResponse> extraGeneralResponse(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse) {
            return Mono.empty();
        }

        @Override
        protected <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse, @NonNull Class<R> resultType) {
            return Mono.empty();
        }

        @Override
        protected <R> Mono<StructuredResponse<R>> extractStructuredResponseContent(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawResponse rawResponse, @NonNull ParameterizedTypeReference<R> resultType) {
            return Mono.empty();
        }

        @Override
        protected Mono<StreamResponse> extractStreamResponseContent(@NonNull List<ToolDefinition> toolDefinitions, @NonNull RawStreamResponse rawStreamResponse) {
            return Mono.empty();
        }
    }

    @BeforeEach
    void setUp() {
        when(tokenCertification.profile()).thenReturn("default");
        when(tokenCertification.isDefault()).thenReturn(true);
        provider = new TestLlmChatProvider(
                Collections.singletonList(tokenCertification),
                map -> llmProviderInfo,
                interceptorRegistry
        );
    }

    @Test
    void testInfo() {
        assertThat(provider.info()).isEqualTo(llmProviderInfo);
    }

    @Test
    void testGetCertificationMap() {
        assertThat(provider.certificationMap).containsEntry("default", tokenCertification);
        assertThat(provider.defaultCertification).isEqualTo(tokenCertification);
    }

    @Test
    void testConstructorWithNoDefaultCertification() {
        when(tokenCertification.profile()).thenReturn("other");
        when(tokenCertification.isDefault()).thenReturn(false);
        
        assertThatThrownBy(() -> new TestLlmChatProvider(
                Collections.singletonList(tokenCertification),
                map -> llmProviderInfo,
                interceptorRegistry
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testExecuteGeneralRaw() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "test-model")
                .defaultProfile(true)
                .build();

        ObjectNode responseBody = JsonNodeFactory.instance.objectNode();
        when(interceptorRegistry.interceptGeneral(any(), any())).thenReturn(Mono.just(responseBody));

        StepVerifier.create(provider.executeGeneralRaw(executionInfo))
                .expectNextMatches(rawResponse -> {
                    assertThat(rawResponse.getResponseBody()).isEqualTo(responseBody);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testInitializeLlmRequestDataWithMissingCertification() {
        // Use a profile that doesn't exist
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "test-model")
                .defaultProfile(false)
                .profilePicker((ctx, profiles) -> "non-existent")
                .build();

        StepVerifier.create(provider.executeGeneralRaw(executionInfo))
                .expectError(pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException.class)
                .verify();
    }

    @Test
    void testExecuteInternalRawWithBearerToken() {
        BearerTokenCertification bearerCert = BearerTokenCertification.builder()
                .profile("bearer")
                .isDefault(false)
                .token("test-token")
                .build();
        
        TestLlmChatProvider bearerProvider = new TestLlmChatProvider(
                java.util.Arrays.asList(tokenCertification, bearerCert),
                map -> llmProviderInfo,
                interceptorRegistry
        );

        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "test-model")
                .defaultProfile(false)
                .profilePicker((ctx, profiles) -> "bearer")
                .build();

        ObjectNode responseBody = JsonNodeFactory.instance.objectNode();
        when(interceptorRegistry.interceptGeneral(any(), any())).thenReturn(Mono.just(responseBody));

        StepVerifier.create(bearerProvider.executeGeneralRaw(executionInfo))
                .expectNextMatches(rawResponse -> {
                    assertThat(rawResponse.getResponseBody()).isEqualTo(responseBody);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testConstructorWithEmptyCertifications() {
        TestLlmChatProvider emptyProvider = new TestLlmChatProvider(
                Collections.emptyList(),
                map -> llmProviderInfo,
                interceptorRegistry
        );
        assertThat(emptyProvider.defaultCertification).isNull();
    }

    @Test
    void testExecuteStreamRaw() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "test-model")
                .defaultProfile(true)
                .build();

        RawStreamResponse chunk = mock(RawStreamResponse.class);
        when(interceptorRegistry.interceptStream(any(), any())).thenReturn(reactor.core.publisher.Flux.just(chunk));

        StepVerifier.create(provider.executeStreamRaw(executionInfo))
                .expectNext(chunk)
                .verifyComplete();
    }

    @Test
    void testExecuteStructuredWithClass() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "test-model")
                .defaultProfile(true)
                .build();

        ObjectNode responseBody = JsonNodeFactory.instance.objectNode();
        when(interceptorRegistry.interceptGeneral(any(), any())).thenReturn(Mono.just(responseBody));

        StepVerifier.create(provider.executeStructured(executionInfo, String.class))
                .verifyComplete();
    }

    @Test
    void testExecuteStructuredWithParameterizedType() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "test-model")
                .defaultProfile(true)
                .build();

        ParameterizedTypeReference<List<String>> typeRef = new ParameterizedTypeReference<>() {};
        ObjectNode responseBody = JsonNodeFactory.instance.objectNode();
        when(interceptorRegistry.interceptGeneral(any(), any())).thenReturn(Mono.just(responseBody));

        StepVerifier.create(provider.executeStructured(executionInfo, typeRef))
                .verifyComplete();
    }

    @Test
    void testExecuteStructuredRawWithSchema() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "test-model")
                .defaultProfile(true)
                .build();

        ObjectNode responseBody = JsonNodeFactory.instance.objectNode();
        when(interceptorRegistry.interceptGeneral(any(), any())).thenReturn(Mono.just(responseBody));

        StepVerifier.create(provider.executeStructuredRaw(executionInfo, "{}"))
                .expectNextMatches(resp -> resp.getResponseBody().equals(responseBody))
                .verifyComplete();
    }

    @Test
    void testExecuteStream() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "test-model")
                .defaultProfile(true)
                .build();

        RawStreamResponse chunk = mock(RawStreamResponse.class);
        when(interceptorRegistry.interceptStream(any(), any())).thenReturn(reactor.core.publisher.Flux.just(chunk));

        StepVerifier.create(provider.executeStream(executionInfo))
                .verifyComplete();
    }

    @Test
    void testMergeRawToolCallMessages() {
        ObjectNode node = JsonNodeFactory.instance.objectNode().put("id", "call_1");
        List<ObjectNode> messages = List.of(node);
        ObjectNode merged = provider.mergeRawToolCallMessages(messages, true);
        assertThat(merged).isNotNull();
    }

    @Test
    void testExecuteStructuredParameterized() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "test-model")
                .defaultProfile(true)
                .build();

        ObjectNode responseBody = JsonNodeFactory.instance.objectNode();
        when(interceptorRegistry.interceptGeneral(any(), any())).thenReturn(Mono.just(responseBody));

        provider.executeStructured(executionInfo, new ParameterizedTypeReference<List<String>>() {})
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void testExecuteInternalRawWithException() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "test-model")
                .defaultProfile(true)
                .build();

        when(interceptorRegistry.interceptGeneral(any(), any())).thenReturn(Mono.error(new RuntimeException("intercept error")));

        StepVerifier.create(provider.executeGeneralRaw(executionInfo))
                .expectError(RuntimeException.class)
                .verify();
    }
}
