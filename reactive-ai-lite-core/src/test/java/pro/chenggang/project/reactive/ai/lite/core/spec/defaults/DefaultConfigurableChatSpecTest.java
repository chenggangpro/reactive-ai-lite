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
package pro.chenggang.project.reactive.ai.lite.core.spec.defaults;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.execution.GeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.StreamExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.StructuredExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.message.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolResultMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DefaultConfigurableChatSpecTest {

    private LlmProviderRegistry registry;
    private DefaultExecutionContextSpec contextSpec;
    private DefaultProviderSpec providerSpec;
    private DefaultConfigurableChatSpec spec;

    @BeforeEach
    void setUp() {
        registry = mock(LlmProviderRegistry.class);
        contextSpec = DefaultExecutionContextSpec.of(LlmClientType.CHAT, registry);
        providerSpec = DefaultProviderSpec.of(LlmClientType.CHAT, registry, contextSpec);
        spec = new DefaultConfigurableChatSpec(LlmClientType.CHAT, registry, contextSpec, providerSpec);
    }

    @Test
    void testConfigurationMethods() {
        MediaMessage mediaMessage = mock(MediaMessage.class);
        Message historicalMessage = mock(Message.class);
        ToolDefinition tool = mock(ToolDefinition.class);
        ToolResultMessage toolResult = mock(ToolResultMessage.class);

        spec.model(ctx -> "gpt-4")
                .temperature(ctx -> 0.7)
                .topP(ctx -> 0.9)
                .includeUsage(ctx -> true)
                .reasoning(ctx -> "thinking")
                .textMessage(ctx -> "hello")
                .mediaMessage(ctx -> mediaMessage)
                .systemMessage(ctx -> "system")
                .historicalMessage(ctx -> Collections.singletonList(historicalMessage))
                .maxCompletionTokens(ctx -> 100)
                .tools(ctx -> Collections.singletonList(tool))
                .toolsResponse(ctx -> Collections.singletonList(toolResult))
                .toolChoice(ctx -> "auto")
                .distinctToolCalls(true);

        ExecutionSpec executionSpec = spec.toExecutionSpec();
        assertThat(executionSpec.getLlmClientType()).isEqualTo(LlmClientType.CHAT);
        assertThat(executionSpec.getModelNameConfigure().apply(null)).isEqualTo("gpt-4");
        assertThat(executionSpec.getTemperatureConfigure().apply(null)).isEqualTo(0.7);
        assertThat(executionSpec.getTopPConfigure().apply(null)).isEqualTo(0.9);
        assertThat(executionSpec.getIncludeUsageConfigure().apply(null)).isTrue();
        assertThat(executionSpec.getReasoningConfigure().apply(null)).isEqualTo("thinking");
        assertThat(executionSpec.getTextMessageConfigure().apply(null)).isEqualTo("hello");
        assertThat(executionSpec.getMediaMessageConfigure().apply(null)).isEqualTo(mediaMessage);
        assertThat(executionSpec.getSystemMessageConfigure().apply(null)).isEqualTo("system");
        assertThat(executionSpec.getHistoricalMessageConfigure().apply(null)).containsExactly(historicalMessage);
        assertThat(executionSpec.getMaxCompletionTokensConfigure().apply(null)).isEqualTo(100);
        assertThat(executionSpec.getToolsConfigure().apply(null)).containsExactly(tool);
        assertThat(executionSpec.getToolResultMessageConfigure().apply(null)).containsExactly(toolResult);
        assertThat(executionSpec.getToolChoiceConfigure().apply(null)).isEqualTo("auto");
        assertThat(executionSpec.isDistinctToolCalls()).isTrue();
    }

    @Test
    void testExecutionHandlers() {
        spec.model("gpt-4");

        GeneralExecution general = spec.general();
        assertThat(general).isNotNull();

        StreamExecution stream = spec.stream();
        assertThat(stream).isNotNull();

        StructuredExecution structured = spec.structured();
        assertThat(structured).isNotNull();
    }

    @Test
    void testAdditionalConfigurableMethods() {
        spec.model("gpt-4")
                .distinctToolCalls(true);
        assertThat(spec.isDistinctToolCalls()).isTrue();

        ExecutionSpec executionSpec = spec.toExecutionSpec();
        assertThat(executionSpec).isNotNull();
    }
}
