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
package pro.chenggang.project.reactive.ai.lite.core.entity.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmEmbeddingRequestData.LlmEmbeddingRequestDataInitializer;
import pro.chenggang.project.reactive.ai.lite.core.exception.ExecutionContextLossException;
import pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.EmbeddingExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class LlmEmbeddingRequestDataTest {

    private Map<String, TokenCertification> certificationMap;
    private TokenCertification defaultCertification;
    private LlmProviderInfo providerInfo;

    @BeforeEach
    void setUp() {
        defaultCertification = mock(TokenCertification.class);
        TokenCertification profileCert = mock(TokenCertification.class);
        certificationMap = Map.of("test_profile", profileCert);

        providerInfo = new LlmProviderInfo() {

            @Override
            public String name() {
                return "test_provider";
            }

            @Override
            public String baseUrl() {
                return "http://localhost";
            }

            @Override
            public String endpoint() {
                return "/embedding";
            }

            @Override
            public Set<String> profiles() {
                return Set.of("test_profile");
            }
        };
    }

    @Test
    void testInitializerOfNulls() {
        EmbeddingExecutionInfo executionInfo = EmbeddingExecutionInfo.builder().modelNameConfigure(ctx -> "test").build();
        assertThatThrownBy(() -> LlmEmbeddingRequestDataInitializer.of(null, defaultCertification, providerInfo, executionInfo)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LlmEmbeddingRequestDataInitializer.of(certificationMap, defaultCertification, null, executionInfo)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LlmEmbeddingRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testInitializeWithoutContext() {
        EmbeddingExecutionInfo executionInfo = EmbeddingExecutionInfo.builder()
                .modelNameConfigure(ctx -> "test")
                .build();
        LlmEmbeddingRequestDataInitializer initializer = LlmEmbeddingRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, executionInfo);

        StepVerifier.create(initializer.initialize())
                .expectError(ExecutionContextLossException.class)
                .verify();
    }

    @Test
    void testInitializeWithDefaultProfile() {
        EmbeddingExecutionInfo executionInfo = EmbeddingExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "embedding_model")
                .inputTextConfigure(ctx -> List.of("input1", "input2"))
                .dimensionsConfigure(ctx -> 1536)
                .build();

        LlmEmbeddingRequestDataInitializer initializer = LlmEmbeddingRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, executionInfo);

        ExecutionContext context = ExecutionContext.newContext();
        StepVerifier.create(initializer.initialize().contextWrite(Context.of(ExecutionContext.class, context)))
                .assertNext(data -> {
                    assertThat(data.getTokenCertification()).contains(defaultCertification);
                    assertThat(data.getModelName()).isEqualTo("embedding_model");
                    assertThat(data.getInput()).containsExactly("input1", "input2");
                    assertThat(data.getDimensions()).isEqualTo(1536);
                })
                .verifyComplete();
    }

    @Test
    void testInitializeWithProfilePicker() {
        EmbeddingExecutionInfo executionInfo = EmbeddingExecutionInfo.builder()
                .defaultProfile(false)
                .profilePicker((ctx, profiles) -> "test_profile")
                .modelNameConfigure(ctx -> "embedding_model")
                .build();

        LlmEmbeddingRequestDataInitializer initializer = LlmEmbeddingRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, executionInfo);

        ExecutionContext context = ExecutionContext.newContext();
        StepVerifier.create(initializer.initialize().contextWrite(Context.of(ExecutionContext.class, context)))
                .assertNext(data -> {
                    assertThat(data.getTokenCertification()).contains(certificationMap.get("test_profile"));
                    assertThat(data.getInput()).isEmpty();
                    assertThat(data.getDimensions()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void testInitializeWithInvalidProfile() {
        EmbeddingExecutionInfo executionInfo = EmbeddingExecutionInfo.builder()
                .defaultProfile(false)
                .profilePicker((ctx, profiles) -> "invalid_profile")
                .modelNameConfigure(ctx -> "embedding_model")
                .build();

        LlmEmbeddingRequestDataInitializer initializer = LlmEmbeddingRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, executionInfo);

        ExecutionContext context = ExecutionContext.newContext();
        StepVerifier.create(initializer.initialize().contextWrite(Context.of(ExecutionContext.class, context)))
                .expectError(NoProfileFoundLlmClientException.class)
                .verify();
    }

    @Test
    void testLlmEmbeddingRequestDataGetters() {
        TokenCertification profileCert = mock(TokenCertification.class);
        BiConsumer<ExecutionContext, ObjectNode> customizer = (ctx, node) -> {};
        
        LlmEmbeddingRequestData data = LlmEmbeddingRequestData.builder()
                .executionContext(ExecutionContext.newContext())
                .tokenCertification(profileCert)
                .modelName("test_model")
                .input(List.of("text1"))
                .dimensions(128)
                .rawRequestCustomizerConfigure(customizer)
                .build();

        assertThat(data.getTokenCertification()).contains(profileCert);
        assertThat(data.getModelName()).isEqualTo("test_model");
        assertThat(data.getInput()).containsExactly("text1");
        assertThat(data.getDimensions()).isEqualTo(128);
        assertThat(data.getRawRequestCustomizerConfigure()).isEqualTo(customizer);
    }

    @Test
    void testInitializeWithNullOptionalFields() {
        EmbeddingExecutionInfo executionInfo = EmbeddingExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test_model")
                .inputTextConfigure(null)
                .dimensionsConfigure(null)
                .build();

        LlmEmbeddingRequestDataInitializer initializer = LlmEmbeddingRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, executionInfo);

        ExecutionContext context = ExecutionContext.newContext();
        StepVerifier.create(initializer.initialize().contextWrite(Context.of(ExecutionContext.class, context)))
                .assertNext(data -> {
                    assertThat(data.getInput()).isEmpty();
                    assertThat(data.getDimensions()).isNull();
                    assertThat(data.getTokenCertification()).contains(defaultCertification);
                })
                .verifyComplete();
    }

    @Test
    void testInitializeWithNoProfileSelected() {
        EmbeddingExecutionInfo executionInfo = EmbeddingExecutionInfo.builder()
                .defaultProfile(false)
                .profilePicker(null)
                .modelNameConfigure(ctx -> "test_model")
                .inputTextConfigure(ctx -> List.of("test"))
                .build();

        LlmEmbeddingRequestDataInitializer initializer = LlmEmbeddingRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, executionInfo);

        ExecutionContext context = ExecutionContext.newContext();
        StepVerifier.create(initializer.initialize().contextWrite(Context.of(ExecutionContext.class, context)))
                .assertNext(data -> {
                    assertThat(data.getTokenCertification()).isEmpty();
                })
                .verifyComplete();
    }
}
