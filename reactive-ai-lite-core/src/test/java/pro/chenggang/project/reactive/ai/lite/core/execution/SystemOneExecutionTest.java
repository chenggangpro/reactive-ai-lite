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
package pro.chenggang.project.reactive.ai.lite.core.execution;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.usage.DefaultUsage;
import pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawResponseConverter;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.systemone.SystemOneGeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SystemOneExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SystemOneExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmSystemOneProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SystemOne execution implementations and contracts.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
class SystemOneExecutionTest {

    private LlmProviderRegistry registry;
    private LlmSystemOneProvider provider;

    @BeforeEach
    void setUp() {
        registry = mock(LlmProviderRegistry.class);
        provider = mock(LlmSystemOneProvider.class);
    }

    @Test
    void testSystemOneGeneralExecutionNullArguments() {
        SystemOneExecutionSpec spec = SystemOneExecutionSpec.builder()
                .llmClientType(LlmClientType.SYSTEM_ONE)
                .defaultProvider(true)
                .modelNameConfigure(ctx -> "test-model")
                .build();

        assertThatThrownBy(() -> SystemOneGeneralExecution.of(null, spec))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SystemOneGeneralExecution.of(registry, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testExecuteDefaultProvider() {
        SystemOneExecutionSpec spec = SystemOneExecutionSpec.builder()
                .llmClientType(LlmClientType.SYSTEM_ONE)
                .defaultProvider(true)
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test-model")
                .parentAttributes(Map.of("session", "123"))
                .build();

        when(registry.getDefaultProvider(Capability.SYSTEM_ONE))
                .thenReturn((Mono) Mono.just(provider));

        SystemOneResponse response = SystemOneResponse.builder()
                .usage(DefaultUsage.builder().promptTokens(10).completionTokens(20).totalTokens(30).build())
                .rawResponseBody(JsonNodeFactory.instance.objectNode().put("result", "fast"))
                .build();

        when(provider.executeSystemOne(any(SystemOneExecutionInfo.class))).thenReturn(Mono.just(response));

        SystemOneExecution execution = SystemOneGeneralExecution.of(registry, spec);

        StepVerifier.create(execution.execute())
                .assertNext(res -> {
                    assertThat(res).isNotNull();
                    assertThat(res.getUsage().getTotalTokens()).isEqualTo(30);
                    assertThat(res.getRawResponseBody().get("result").asText()).isEqualTo("fast");
                })
                .verifyComplete();
    }

    @Test
    void testExecuteRawAndConverter() {
        SystemOneExecutionSpec spec = SystemOneExecutionSpec.builder()
                .llmClientType(LlmClientType.SYSTEM_ONE)
                .defaultProvider(true)
                .modelNameConfigure(ctx -> "test-model")
                .build();

        when(registry.getDefaultProvider(Capability.SYSTEM_ONE))
                .thenReturn((Mono) Mono.just(provider));

        ObjectNode node = JsonNodeFactory.instance.objectNode().put("status", "ok");
        RawResponse rawResponse = RawResponse.builder()
                .responseBody(node)
                .build();

        when(provider.executeSystemOneRaw(any(SystemOneExecutionInfo.class))).thenReturn(Mono.just(rawResponse));

        SystemOneExecution execution = SystemOneGeneralExecution.of(registry, spec);

        StepVerifier.create(execution.executeRaw())
                .expectNext(rawResponse)
                .verifyComplete();

        RawResponseConverter<String> converter = raw -> raw.getResponseBody().get("status").asText();

        StepVerifier.create(execution.execute(converter))
                .expectNext("ok")
                .verifyComplete();
    }

    @Test
    void testExecuteWithCustomProviderFilterAndProfilePicker() {
        SystemOneExecutionSpec spec = SystemOneExecutionSpec.builder()
                .llmClientType(LlmClientType.SYSTEM_ONE)
                .defaultProvider(false)
                .providerFilter((ctx, info) -> "system-one-fast".equals(info.name()))
                .defaultProfile(false)
                .profilePicker((ctx, profiles) -> "fast-profile")
                .modelNameConfigure(ctx -> "custom-model")
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(providerInfo.name()).thenReturn("system-one-fast");
        when(providerInfo.profiles()).thenReturn(Set.of("fast-profile"));
        when(provider.info()).thenReturn(providerInfo);

        when(registry.getProvider(eq(Capability.SYSTEM_ONE), eq(LlmSystemOneProvider.class), any()))
                .thenReturn(Mono.just(provider));

        SystemOneResponse response = SystemOneResponse.builder().build();
        when(provider.executeSystemOne(any(SystemOneExecutionInfo.class))).thenReturn(Mono.just(response));

        SystemOneExecution execution = SystemOneGeneralExecution.of(registry, spec);

        StepVerifier.create(execution.execute())
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testSystemOneExecutionInterfaceDefaultMethod() {
        SystemOneExecution execution = new SystemOneExecution() {
            @Override
            public Mono<SystemOneResponse> execute() {
                return Mono.empty();
            }

            @Override
            public Mono<RawResponse> executeRaw() {
                RawResponse raw = mock(RawResponse.class);
                return Mono.just(raw);
            }
        };

        StepVerifier.create(execution.execute(raw -> "converted-output"))
                .expectNext("converted-output")
                .verifyComplete();
    }
}
