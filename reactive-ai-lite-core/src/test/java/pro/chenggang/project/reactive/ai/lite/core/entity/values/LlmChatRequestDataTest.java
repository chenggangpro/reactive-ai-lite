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

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmChatRequestDataTest {


    @Test

    void testProtectedMethodsNullChecks() throws Exception {
        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder().modelNameConfigure(ctx -> "test").build();
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);
        java.util.Map<String, TokenCertification> certificationMap = Collections.emptyMap();
        
        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(certificationMap, defaultCert, providerInfo, executionInfo, false);
        
        String[] methods = {
            "loadTokenCertification", "loadModelName", "loadSystemMessage", "loadHistoricalMessage",
            "loadUserMessage", "loadMediaMessage", "loadTemperature", "loadTopP", "loadIncludeUsage",
            "loadReasoning", "loadMaxCompletionTokens", "loadToolDefinitions", "loadToolChoice", "loadToolResultMessage"
        };
        
        ExecutionContext ctx = ExecutionContext.newContext();
        for (String methodName : methods) {
            java.lang.reflect.Method method = LlmChatRequestData.LlmChatRequestDataInitializer.class.getDeclaredMethod(methodName, ChatExecutionInfo.class, ExecutionContext.class);
            method.setAccessible(true);
            
            // arg1 null
            assertThatThrownBy(() -> method.invoke(initializer, null, ctx))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
                
            // arg2 null
            assertThatThrownBy(() -> method.invoke(initializer, executionInfo, null))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void testInitializer() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .modelNameConfigure(__ -> "gpt-4")
                .defaultProfile(true)
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);

        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false
        );

        LlmChatRequestData data = initializer.initialize()
                .contextWrite(ctx -> ctx.put(ExecutionContext.class, executionContext))
                .block();
        assertThat(data).isNotNull();
        assertThat(data.getModelName()).isEqualTo("gpt-4");
        assertThat(data.getTokenCertification()).hasValue(defaultCert);
    }

    @Test
    void testInitializerWithProfileMissing() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .modelNameConfigure(__ -> "gpt-4")
                .defaultProfile(false)
                .profilePicker((ctx, profiles) -> "not-found")
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(providerInfo.profiles()).thenReturn(Set.of("default"));

        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                null,
                providerInfo,
                executionInfo,
                false
        );

        assertThatThrownBy(() -> initializer.initialize()
                        .contextWrite(ctx -> ctx.put(ExecutionContext.class, executionContext))
                        .block())
                .isInstanceOf(NoProfileFoundLlmClientException.class);
    }

    @Test
    void testInitializerFull() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .modelNameConfigure(__ ->"gpt-4")
                .defaultProfile(true)
                .systemMessageConfigure(__ ->"system-msg")
                .textMessageConfigure(__ ->"user-msg")
                .temperatureConfigure(__ ->0.8)
                .topPConfigure(__ ->0.9)
                .includeUsageConfigure(__ ->true)
                .reasoningConfigure(__ ->"thinking")
                .maxCompletionTokensConfigure(__ ->100)
                .toolChoiceConfigure(__ ->"auto")
                .structuredOutputType(String.class)
                .responseJsonSchema("{}")
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);

        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                true
        );

        LlmChatRequestData data = initializer.initialize()
                .contextWrite(ctx -> ctx.put(ExecutionContext.class, executionContext))
                .block();
        assertThat(data).isNotNull();
        assertThat(data.getModelName()).isEqualTo("gpt-4");
        assertThat(data.getTokenCertification()).hasValue(defaultCert);
        assertThat(data.getSystemMessage().getContent()).isEqualTo("system-msg");
        assertThat(data.getUserTextMessage().getContent()).isEqualTo("user-msg");
        assertThat(data.getTemperature()).hasValue(0.8);
        assertThat(data.getTopP()).hasValue(0.9);
        assertThat(data.isIncludeUsage()).isTrue();
        assertThat(data.getReasoning()).hasValue("thinking");
        assertThat(data.getMaxCompletionTokens()).hasValue(100);
        assertThat(data.getToolChoice()).hasValue("auto");
        assertThat(data.isStream()).isTrue();
        assertThat(data.getStructuredOutputType()).hasValue(String.class);
        assertThat(data.getResponseJsonSchema()).hasValue("{}");
    }

    @Test
    void testInitializerToolDefinitions() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition tool1 = mock(pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition.class);
        when(tool1.name()).thenReturn("tool1");
        pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition toolDuplicate = mock(pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition.class);
        when(toolDuplicate.name()).thenReturn("tool1");
        pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition toolNoName = mock(pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition.class);
        when(toolNoName.name()).thenReturn("");

        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .modelNameConfigure(__ ->"gpt-4")
                .defaultProfile(true)
                .toolsConfigure(__ ->List.of(tool1, toolDuplicate, toolNoName))
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);

        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false
        );

        LlmChatRequestData data = initializer.initialize()
                .contextWrite(ctx -> ctx.put(ExecutionContext.class, executionContext))
                .block();
        assertThat(data).isNotNull();
        assertThat(data.getToolDefinitions()).hasSize(1);
        assertThat(data.getToolDefinitions().get(0).name()).isEqualTo("tool1");
    }

    @Test
    void testInitializerWithProfilePicker() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .modelNameConfigure(__ ->"gpt-4")
                .defaultProfile(false)
                .profilePicker((ctx, profiles) -> "custom")
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        when(providerInfo.profiles()).thenReturn(Collections.singleton("custom"));
        TokenCertification customCert = mock(TokenCertification.class);

        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.singletonMap("custom", customCert),
                null,
                providerInfo,
                executionInfo,
                false
        );

        LlmChatRequestData data = initializer.initialize()
                .contextWrite(ctx -> ctx.put(ExecutionContext.class, executionContext))
                .block();
        assertThat(data).isNotNull();
        assertThat(data.getTokenCertification()).hasValue(customCert);
    }

    @Test
    void testInitializerWithMinimalConfig() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .modelNameConfigure(__ -> "gpt-4")
                .defaultProfile(true)
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);

        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false
        );

        LlmChatRequestData data = initializer.initialize()
                .contextWrite(ctx -> ctx.put(ExecutionContext.class, executionContext))
                .block();
        assertThat(data).isNotNull();
        assertThat(data.getModelName()).isEqualTo("gpt-4");
    }

    @Test
    void testInitializerWithNullConfigurators() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        // ExecutionInfo with null configurators
        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .modelNameConfigure(__ -> "gpt-4")
                .defaultProfile(true)
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);

        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false
        );

        LlmChatRequestData data = initializer.initialize()
                .contextWrite(ctx -> ctx.put(ExecutionContext.class, executionContext))
                .block();
        assertThat(data).isNotNull();
        assertThat(data.getTemperature()).isEmpty();
        assertThat(data.getTopP()).isEmpty();
        assertThat(data.getMaxCompletionTokens()).isEmpty();
    }

    @Test
    void testInitializerWithHistoricalAndMediaMessages() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .modelNameConfigure(__ -> "gpt-4")
                .defaultProfile(true)
                .historicalMessageConfigure(__ -> List.of(mock(pro.chenggang.project.reactive.ai.lite.core.message.Message.class)))
                .mediaMessageConfigure(__ -> mock(pro.chenggang.project.reactive.ai.lite.core.message.MediaMessage.class))
                .toolResultMessageConfigure(__ -> List.of(mock(pro.chenggang.project.reactive.ai.lite.core.message.ToolResultMessage.class)))
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);

        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false
        );

        LlmChatRequestData data = initializer.initialize()
                .contextWrite(ctx -> ctx.put(ExecutionContext.class, executionContext))
                .block();
        assertThat(data).isNotNull();
        assertThat(data.getHistoricalMessages()).hasSize(1);
        assertThat(data.getUserMediaMessage()).isPresent();
        assertThat(data.getToolResultMessages()).hasSize(1);
    }

    @Test
    void testInitializerWithDefaultSystemMessage() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .modelNameConfigure(__ -> "gpt-4")
                .defaultProfile(true)
                .systemMessageConfigure(ctx -> "default-system")
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);

        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false
        );

        LlmChatRequestData data = initializer.initialize()
                .contextWrite(ctx -> ctx.put(ExecutionContext.class, executionContext))
                .block();
        assertThat(data).isNotNull();
        assertThat(data.getSystemMessage().getContent()).isEqualTo("default-system");
    }

    @Test
    void testInitializerWithNullTools() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .modelNameConfigure(__ -> "gpt-4")
                .defaultProfile(true)
                .toolsConfigure(__ -> Collections.emptyList())
                .toolResultMessageConfigure(__ -> Collections.emptyList())
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);

        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false
        );

        LlmChatRequestData data = initializer.initialize()
                .contextWrite(ctx -> ctx.put(ExecutionContext.class, executionContext))
                .block();
        assertThat(data).isNotNull();
        assertThat(data.getToolDefinitions()).isEmpty();
        assertThat(data.getToolResultMessages()).isEmpty();
    }
    @Test
    void testInitializerExceptions() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> LlmChatRequestData.LlmChatRequestDataInitializer.of(null, null, null, null, false))
                .isInstanceOf(IllegalArgumentException.class);
        
        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder()
                .modelNameConfigure(__ -> "gpt-4")
                .defaultProfile(false)
                .build();
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        
        LlmChatRequestData.LlmChatRequestDataInitializer init1 = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(), null, providerInfo, executionInfo, false
        );
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> init1.initialize().contextWrite(ctx -> ctx.put(ExecutionContext.class, ExecutionContext.newContext())).block())
                .isInstanceOf(pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException.class);
                
        ChatExecutionInfo executionInfo2 = ChatExecutionInfo.builder()
                .modelNameConfigure(__ -> "gpt-4")
                .defaultProfile(false)
                .profilePicker((ctx, profiles) -> "custom")
                .build();
        LlmChatRequestData.LlmChatRequestDataInitializer init2 = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(), null, providerInfo, executionInfo2, false
        );
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> init2.initialize().contextWrite(ctx -> ctx.put(ExecutionContext.class, ExecutionContext.newContext())).block())
                .isInstanceOf(pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException.class);
                
        ChatExecutionInfo executionInfo3 = ChatExecutionInfo.builder()
                .modelNameConfigure(__ -> null)
                .defaultProfile(true)
                .build();
        TokenCertification defaultCert = mock(TokenCertification.class);
        LlmChatRequestData.LlmChatRequestDataInitializer init3 = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(), defaultCert, providerInfo, executionInfo3, false
        );
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> init3.initialize().contextWrite(ctx -> ctx.put(ExecutionContext.class, ExecutionContext.newContext())).block())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testInitializerOfNulls() {
        ChatExecutionInfo executionInfo = ChatExecutionInfo.builder().modelNameConfigure(ctx -> "test").build();
        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);
        java.util.Map<String, TokenCertification> certificationMap = Collections.emptyMap();
        
        assertThatThrownBy(() -> LlmChatRequestData.LlmChatRequestDataInitializer.of(null, defaultCert, providerInfo, executionInfo, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LlmChatRequestData.LlmChatRequestDataInitializer.of(certificationMap, defaultCert, null, executionInfo, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LlmChatRequestData.LlmChatRequestDataInitializer.of(certificationMap, defaultCert, providerInfo, null, false)).isInstanceOf(IllegalArgumentException.class);
    }
}
