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
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
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
    void testInitializer() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "gpt-4")
                .defaultProfile(true)
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);
        
        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false,
                null,
                null
        );

        LlmChatRequestData data = initializer.initialize();
        assertThat(data.getModelName()).isEqualTo("gpt-4");
        assertThat(data.getTokenCertification()).hasValue(defaultCert);
    }

    @Test
    void testInitializerWithProfileMissing() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "gpt-4")
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
                false,
                null,
                null
        );

        assertThatThrownBy(initializer::initialize)
                .isInstanceOf(NoProfileFoundLlmClientException.class);
    }

    @Test
    void testInitializerFull() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "gpt-4")
                .defaultProfile(true)
                .systemMessageConfigure(ctx -> "system-msg")
                .textMessageConfigure(ctx -> "user-msg")
                .temperatureConfigure(ctx -> 0.8)
                .topPConfigure(ctx -> 0.9)
                .includeUsageConfigure(ctx -> true)
                .reasoningConfigure(ctx -> "thinking")
                .maxCompletionTokensConfigure(ctx -> 100)
                .toolChoiceConfigure(ctx -> "auto")
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);
        
        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                true,
                String.class,
                "{}"
        );

        LlmChatRequestData data = initializer.initialize();
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

        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "gpt-4")
                .defaultProfile(true)
                .toolsConfigure(ctx -> List.of(tool1, toolDuplicate, toolNoName))
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);
        
        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false,
                null,
                null
        );

        LlmChatRequestData data = initializer.initialize();
        assertThat(data.getToolDefinitions()).hasSize(1);
        assertThat(data.getToolDefinitions().get(0).name()).isEqualTo("tool1");
    }

    @Test
    void testInitializerWithProfilePicker() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "gpt-4")
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
                false,
                null,
                null
        );

        LlmChatRequestData data = initializer.initialize();
        assertThat(data.getTokenCertification()).hasValue(customCert);
    }

    @Test
    void testInitializerWithMinimalConfig() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "gpt-4")
                .defaultProfile(true)
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);
        
        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false,
                null,
                null
        );

        LlmChatRequestData data = initializer.initialize();
        assertThat(data.getModelName()).isEqualTo("gpt-4");
        assertThat(data.getSystemMessage().getContent()).isEmpty();
        assertThat(data.getUserTextMessage().getContent()).isEmpty();
        assertThat(data.getHistoricalMessages()).isEmpty();
        assertThat(data.getUserMediaMessage()).isEmpty();
        assertThat(data.getToolResultMessages()).isEmpty();
    }

    @Test
    void testInitializerWithNullConfigurators() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        // ExecutionInfo with null configurators
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "gpt-4")
                .defaultProfile(true)
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);
        
        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false,
                null,
                null
        );

        LlmChatRequestData data = initializer.initialize();
        assertThat(data.getTemperature()).isEmpty();
        assertThat(data.getTopP()).isEmpty();
        assertThat(data.getMaxCompletionTokens()).isEmpty();
    }

    @Test
    void testInitializerWithHistoricalAndMediaMessages() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "gpt-4")
                .defaultProfile(true)
                .historicalMessageConfigure(ctx -> List.of(mock(pro.chenggang.project.reactive.ai.lite.core.message.Message.class)))
                .mediaMessageConfigure(ctx -> mock(pro.chenggang.project.reactive.ai.lite.core.message.MediaMessage.class))
                .toolResultMessageConfigure(ctx -> List.of(mock(pro.chenggang.project.reactive.ai.lite.core.message.ToolResultMessage.class)))
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);
        
        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false,
                null,
                null
        );

        LlmChatRequestData data = initializer.initialize();
        assertThat(data.getHistoricalMessages()).hasSize(1);
        assertThat(data.getUserMediaMessage()).isPresent();
        assertThat(data.getToolResultMessages()).hasSize(1);
    }

    @Test
    void testInitializerWithDefaultSystemMessage() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "gpt-4")
                .defaultProfile(true)
                .defaultSystemMessageConfigure(ctx -> "default-system")
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);
        
        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false,
                null,
                null
        );

        LlmChatRequestData data = initializer.initialize();
        assertThat(data.getSystemMessage().getContent()).isEqualTo("default-system");
    }

    @Test
    void testInitializerWithNullTools() {
        ExecutionContext executionContext = ExecutionContext.newContext();
        ExecutionInfo executionInfo = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(ctx -> "gpt-4")
                .defaultProfile(true)
                .toolsConfigure(ctx -> null)
                .toolResultMessageConfigure(ctx -> null)
                .build();

        LlmProviderInfo providerInfo = mock(LlmProviderInfo.class);
        TokenCertification defaultCert = mock(TokenCertification.class);
        
        LlmChatRequestData.LlmChatRequestDataInitializer initializer = LlmChatRequestData.LlmChatRequestDataInitializer.of(
                Collections.emptyMap(),
                defaultCert,
                providerInfo,
                executionInfo,
                false,
                null,
                null
        );

        LlmChatRequestData data = initializer.initialize();
        assertThat(data.getToolDefinitions()).isEmpty();
        assertThat(data.getToolResultMessages()).isEmpty();
    }
}
