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
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmSystemOneRequestData.LlmSystemOneRequestDataInitializer;
import pro.chenggang.project.reactive.ai.lite.core.exception.ExecutionContextLossException;
import pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SystemOneExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestions;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link LlmSystemOneRequestData} and {@link LlmSystemOneRequestDataInitializer}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
class LlmSystemOneRequestDataTest {

    private Map<String, TokenCertification> certificationMap;
    private TokenCertification defaultCertification;
    private LlmProviderInfo providerInfo;
    private SystemOneQuestions testQuestions;

    @BeforeEach
    void setUp() {
        defaultCertification = mock(TokenCertification.class);
        TokenCertification profileCert = mock(TokenCertification.class);
        certificationMap = Map.of("test_profile", profileCert);

        testQuestions = SystemOneQuestions.of("q1", SystemOneQuestion.newNoulBuilder("Is input valid?").build());

        providerInfo = new LlmProviderInfo() {
            @Override
            public String name() {
                return "test_systemone_provider";
            }

            @Override
            public String baseUrl() {
                return "https://api.test.com";
            }

            @Override
            public String endpoint() {
                return "/v1/evaluate";
            }

            @Override
            public Set<String> profiles() {
                return Set.of("test_profile");
            }
        };
    }

    @Test
    void testBuilderAndGetters() {
        ExecutionContext ctx = ExecutionContext.newContext();
        TokenCertification token = mock(TokenCertification.class);
        BiConsumer<ExecutionContext, ObjectNode> customizer = (c, node) -> {};
        SystemOneContent<?> state = SystemOneContent.text("sample payload");

        LlmSystemOneRequestData requestData = LlmSystemOneRequestData.builder()
                .executionContext(ctx)
                .modelName("test-model")
                .tokenCertification(token)
                .state(state)
                .questions(testQuestions)
                .rawRequestCustomizerConfigure(customizer)
                .build();

        assertThat(requestData.getExecutionContext()).isEqualTo(ctx);
        assertThat(requestData.getModelName()).isEqualTo("test-model");
        assertThat(requestData.getTokenCertification()).contains(token);
        assertThat(requestData.getState()).isEqualTo(state);
        assertThat(requestData.getQuestions()).isEqualTo(testQuestions);
        assertThat(requestData.getRawRequestCustomizerConfigure()).isSameAs(customizer);
        assertThat(requestData.toString()).contains("test-model");
    }

    @Test
    void testNonNullValidation() {
        ExecutionContext ctx = ExecutionContext.newContext();

        assertThatThrownBy(() -> LlmSystemOneRequestData.builder().modelName("test").build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> LlmSystemOneRequestData.builder().executionContext(ctx).build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> LlmSystemOneRequestData.builder()
                .executionContext(ctx)
                .modelName("test")
                .questions(testQuestions)
                .build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> LlmSystemOneRequestData.builder()
                .executionContext(ctx)
                .modelName("test")
                .state(SystemOneContent.NULL)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testInitializerOfNullChecks() {
        SystemOneExecutionInfo execInfo = SystemOneExecutionInfo.builder()
                .modelNameConfigure(ctx -> "test")
                .questionsConfigure(ctx -> testQuestions)
                .build();

        assertThatThrownBy(() -> LlmSystemOneRequestDataInitializer.of(null, defaultCertification, providerInfo, execInfo))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LlmSystemOneRequestDataInitializer.of(certificationMap, defaultCertification, null, execInfo))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LlmSystemOneRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testInitializeWithoutContext() {
        SystemOneExecutionInfo execInfo = SystemOneExecutionInfo.builder()
                .modelNameConfigure(ctx -> "test")
                .questionsConfigure(ctx -> testQuestions)
                .build();
        LlmSystemOneRequestDataInitializer initializer = LlmSystemOneRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, execInfo);

        StepVerifier.create(initializer.initialize())
                .expectError(ExecutionContextLossException.class)
                .verify();
    }

    @Test
    void testInitializeWithDefaultProfile() {
        SystemOneContent<?> state = SystemOneContent.text("user text");
        SystemOneExecutionInfo execInfo = SystemOneExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "jev-1")
                .stateConfigure(ctx -> state)
                .questionsConfigure(ctx -> testQuestions)
                .build();

        LlmSystemOneRequestDataInitializer initializer = LlmSystemOneRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, execInfo);

        ExecutionContext context = ExecutionContext.newContext();
        StepVerifier.create(initializer.initialize().contextWrite(Context.of(ExecutionContext.class, context)))
                .assertNext(data -> {
                    assertThat(data.getTokenCertification()).contains(defaultCertification);
                    assertThat(data.getModelName()).isEqualTo("jev-1");
                    assertThat(data.getState()).isEqualTo(state);
                    assertThat(data.getQuestions()).isEqualTo(testQuestions);
                })
                .verifyComplete();
    }

    @Test
    void testInitializeWithDefaultStateWhenUnconfigured() {
        SystemOneExecutionInfo execInfo = SystemOneExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "jev-1")
                .stateConfigure(null)
                .questionsConfigure(ctx -> testQuestions)
                .build();

        LlmSystemOneRequestDataInitializer initializer = LlmSystemOneRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, execInfo);

        ExecutionContext context = ExecutionContext.newContext();
        StepVerifier.create(initializer.initialize().contextWrite(Context.of(ExecutionContext.class, context)))
                .assertNext(data -> {
                    assertThat(data.getState()).isEqualTo(SystemOneContent.NULL);
                })
                .verifyComplete();
    }

    @Test
    void testInitializeWithProfilePicker() {
        SystemOneExecutionInfo execInfo = SystemOneExecutionInfo.builder()
                .defaultProfile(false)
                .profilePicker((ctx, profiles) -> "test_profile")
                .modelNameConfigure(ctx -> "jev-1")
                .questionsConfigure(ctx -> testQuestions)
                .build();

        LlmSystemOneRequestDataInitializer initializer = LlmSystemOneRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, execInfo);

        ExecutionContext context = ExecutionContext.newContext();
        StepVerifier.create(initializer.initialize().contextWrite(Context.of(ExecutionContext.class, context)))
                .assertNext(data -> {
                    assertThat(data.getTokenCertification()).contains(certificationMap.get("test_profile"));
                })
                .verifyComplete();
    }

    @Test
    void testInitializeWithInvalidProfile() {
        SystemOneExecutionInfo execInfo = SystemOneExecutionInfo.builder()
                .defaultProfile(false)
                .profilePicker((ctx, profiles) -> "non_existent_profile")
                .modelNameConfigure(ctx -> "jev-1")
                .questionsConfigure(ctx -> testQuestions)
                .build();

        LlmSystemOneRequestDataInitializer initializer = LlmSystemOneRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, execInfo);

        ExecutionContext context = ExecutionContext.newContext();
        StepVerifier.create(initializer.initialize().contextWrite(Context.of(ExecutionContext.class, context)))
                .expectError(NoProfileFoundLlmClientException.class)
                .verify();
    }

    @Test
    void testInitializeWithoutProfileSelected() {
        SystemOneExecutionInfo execInfo = SystemOneExecutionInfo.builder()
                .defaultProfile(false)
                .profilePicker(null)
                .modelNameConfigure(ctx -> "jev-1")
                .questionsConfigure(ctx -> testQuestions)
                .build();

        LlmSystemOneRequestDataInitializer initializer = LlmSystemOneRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, execInfo);

        ExecutionContext context = ExecutionContext.newContext();
        StepVerifier.create(initializer.initialize().contextWrite(Context.of(ExecutionContext.class, context)))
                .assertNext(data -> {
                    assertThat(data.getTokenCertification()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void testInitializeMissingModelName() {
        SystemOneExecutionInfo execInfo = SystemOneExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "")
                .questionsConfigure(ctx -> testQuestions)
                .build();

        LlmSystemOneRequestDataInitializer initializer = LlmSystemOneRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, execInfo);

        ExecutionContext context = ExecutionContext.newContext();
        StepVerifier.create(initializer.initialize().contextWrite(Context.of(ExecutionContext.class, context)))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testInitializeMissingQuestions() {
        SystemOneExecutionInfo execInfo = SystemOneExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "jev-1")
                .questionsConfigure(null)
                .build();

        LlmSystemOneRequestDataInitializer initializer = LlmSystemOneRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, execInfo);

        ExecutionContext context = ExecutionContext.newContext();
        StepVerifier.create(initializer.initialize().contextWrite(Context.of(ExecutionContext.class, context)))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testInitializeEmptyQuestions() {
        SystemOneExecutionInfo execInfo = SystemOneExecutionInfo.builder()
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "jev-1")
                .questionsConfigure(ctx -> SystemOneQuestions.newBuilder().build())
                .build();

        LlmSystemOneRequestDataInitializer initializer = LlmSystemOneRequestDataInitializer.of(certificationMap, defaultCertification, providerInfo, execInfo);

        ExecutionContext context = ExecutionContext.newContext();
        StepVerifier.create(initializer.initialize().contextWrite(Context.of(ExecutionContext.class, context)))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
