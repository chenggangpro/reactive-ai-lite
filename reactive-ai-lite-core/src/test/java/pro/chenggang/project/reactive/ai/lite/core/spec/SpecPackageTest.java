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
package pro.chenggang.project.reactive.ai.lite.core.spec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.message.MediaMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolResultMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.defaults.DefaultConfigurableChatSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.defaults.DefaultConfigurableEmbeddingSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.defaults.ProviderConfigureInfo;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SpecPackageTest {

    private LlmProviderRegistry registry;
    private ProviderConfigureInfo providerConfigureInfo;

    @BeforeEach
    void setUp() {
        registry = mock(LlmProviderRegistry.class);
        providerConfigureInfo = ProviderConfigureInfo.builder()
                .defaultProvider(true)
                .defaultProfile(true)
                .build();
    }

    @Test
    void testConfigurableChatSpecDefaults() throws Exception {
        assertThatThrownBy(() -> new DefaultConfigurableChatSpec(null, registry, providerConfigureInfo)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultConfigurableChatSpec(LlmClientType.CHAT, null, providerConfigureInfo)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultConfigurableChatSpec(LlmClientType.CHAT, registry, null)).isInstanceOf(IllegalArgumentException.class);

        ConfigurableChatSpec spec = new DefaultConfigurableChatSpec(LlmClientType.CHAT, registry, providerConfigureInfo);
        ExecutionContext mockCtx = mock(ExecutionContext.class);

        spec.model("test_model");
        invokeConfigFunction(spec, "modelNameConfigure", mockCtx);
        assertThatThrownBy(() -> spec.model((String) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.model((Function<ExecutionContext, String>) null)).isInstanceOf(IllegalArgumentException.class);

        spec.temperature(0.5);
        invokeConfigFunction(spec, "temperatureConfigure", mockCtx);
        spec.temperature((Double) null);
        assertThatThrownBy(() -> spec.temperature((Function<ExecutionContext, Double>) null)).isInstanceOf(IllegalArgumentException.class);

        spec.topP(0.9);
        invokeConfigFunction(spec, "topPConfigure", mockCtx);
        spec.topP((Double) null);
        assertThatThrownBy(() -> spec.topP((Function<ExecutionContext, Double>) null)).isInstanceOf(IllegalArgumentException.class);

        spec.includeUsage();
        invokeConfigFunction(spec, "includeUsageConfigure", mockCtx);
        assertThatThrownBy(() -> spec.includeUsage((Function<ExecutionContext, Boolean>) null)).isInstanceOf(IllegalArgumentException.class);

        spec.reasoning("reason");
        invokeConfigFunction(spec, "reasoningConfigure", mockCtx);
        spec.reasoning((String) null);
        assertThatThrownBy(() -> spec.reasoning((Function<ExecutionContext, String>) null)).isInstanceOf(IllegalArgumentException.class);

        spec.systemMessage("sys_msg");
        invokeConfigFunction(spec, "systemMessageConfigure", mockCtx);
        spec.systemMessage((String) null);
        assertThatThrownBy(() -> spec.systemMessage((Function<ExecutionContext, String>) null)).isInstanceOf(IllegalArgumentException.class);

        List<Message> history = List.of(DefaultTextMessage.builder().role(Role.USER.getValue()).content("hist").build());
        spec.historicalMessage(history);
        invokeConfigFunction(spec, "historicalMessageConfigure", mockCtx);
        spec.historicalMessage((List<Message>) null);
        assertThatThrownBy(() -> spec.historicalMessage((Function<ExecutionContext, List<Message>>) null)).isInstanceOf(IllegalArgumentException.class);

        spec.textMessage("usr_msg");
        invokeConfigFunction(spec, "textMessageConfigure", mockCtx);
        assertThatThrownBy(() -> spec.textMessage((String) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.textMessage((Function<ExecutionContext, String>) null)).isInstanceOf(IllegalArgumentException.class);

        List<Attachment> attachments = List.of(mock(Attachment.class));
        spec.mediaMessage("media_msg", attachments);
        invokeConfigFunction(spec, "mediaMessageConfigure", mockCtx);
        assertThatThrownBy(() -> spec.mediaMessage((String) null, attachments)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.mediaMessage("media_msg", (List<Attachment>) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.mediaMessage((Function<ExecutionContext, MediaMessage>) null)).isInstanceOf(IllegalArgumentException.class);

        spec.maxCompletionTokens(100);
        invokeConfigFunction(spec, "maxCompletionTokensConfigure", mockCtx);
        assertThatThrownBy(() -> spec.maxCompletionTokens((Integer) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.maxCompletionTokens((Function<ExecutionContext, Integer>) null)).isInstanceOf(IllegalArgumentException.class);

        List<ToolDefinition> tools = List.of(mock(ToolDefinition.class));
        spec.tools(tools);
        invokeConfigFunction(spec, "toolsConfigure", mockCtx);
        assertThatThrownBy(() -> spec.tools((Collection<ToolDefinition>) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.tools((Function<ExecutionContext, Collection<ToolDefinition>>) null)).isInstanceOf(IllegalArgumentException.class);
        
        spec.distinctToolCalls(true);

        spec.toolChoice("auto");
        invokeConfigFunction(spec, "toolChoiceConfigure", mockCtx);
        assertThatThrownBy(() -> spec.toolChoice((String) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.toolChoice((Function<ExecutionContext, String>) null)).isInstanceOf(IllegalArgumentException.class);

        List<ToolResultMessage> toolResponses = List.of(ToolResultMessage.newToolResultMessage("id").content("msg").build());
        spec.toolsResponse(toolResponses);
        invokeConfigFunction(spec, "toolsResultMessageConfigure", mockCtx);
        assertThatThrownBy(() -> spec.toolsResponse((Collection<ToolResultMessage>) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.toolsResponse((Function<ExecutionContext, Collection<ToolResultMessage>>) null)).isInstanceOf(IllegalArgumentException.class);

        Consumer<ObjectNode> customizer = node -> {};
        spec.rawRequestCustomizer(customizer);
        invokeConfigFunction(spec, "rawRequestCustomizerConfigure", mockCtx);
        assertThatThrownBy(() -> spec.rawRequestCustomizer((Consumer<ObjectNode>) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.rawRequestCustomizer((BiConsumer<ExecutionContext, ObjectNode>) null)).isInstanceOf(IllegalArgumentException.class);

        invokeMethod(spec, "toChatExecutionSpec");

        assertThat(spec.general()).isNotNull();
        assertThat(spec.stream()).isNotNull();
        assertThat(spec.structured()).isNotNull();
    }

    @Test
    void testConfigurableEmbeddingSpecDefaults() throws Exception {
        assertThatThrownBy(() -> new DefaultConfigurableEmbeddingSpec(null, registry, providerConfigureInfo)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultConfigurableEmbeddingSpec(LlmClientType.EMBEDDING, null, providerConfigureInfo)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultConfigurableEmbeddingSpec(LlmClientType.EMBEDDING, registry, null)).isInstanceOf(IllegalArgumentException.class);

        ConfigurableEmbeddingSpec spec = new DefaultConfigurableEmbeddingSpec(LlmClientType.EMBEDDING, registry, providerConfigureInfo);
        ExecutionContext mockCtx = mock(ExecutionContext.class);

        spec.model("emb_model");
        invokeConfigFunction(spec, "modelNameConfigure", mockCtx);
        assertThatThrownBy(() -> spec.model((String) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.model((Function<ExecutionContext, String>) null)).isInstanceOf(IllegalArgumentException.class);

        spec.inputText(List.of("input1"));
        invokeConfigFunction(spec, "inputTextConfigure", mockCtx);
        assertThatThrownBy(() -> spec.inputText((List<String>) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.inputText((Function<ExecutionContext, List<String>>) null)).isInstanceOf(IllegalArgumentException.class);

        spec.dimensions(256);
        invokeConfigFunction(spec, "dimensionsConfigure", mockCtx);
        spec.dimensions((Integer) null);
        assertThatThrownBy(() -> spec.dimensions((Function<ExecutionContext, Integer>) null)).isInstanceOf(IllegalArgumentException.class);

        Consumer<ObjectNode> customizer = node -> {};
        spec.rawRequestCustomizer(customizer);
        invokeConfigFunction(spec, "rawRequestCustomizerConfigure", mockCtx);
        assertThatThrownBy(() -> spec.rawRequestCustomizer((Consumer<ObjectNode>) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.rawRequestCustomizer((BiConsumer<ExecutionContext, ObjectNode>) null)).isInstanceOf(IllegalArgumentException.class);

        invokeMethod(spec, "toEmbeddingExecutionSpec");

        assertThat(spec.general()).isNotNull();
    }

    private void invokeConfigFunction(Object obj, String fieldName, ExecutionContext ctx) throws Exception {
        System.out.println("Invoking: " + fieldName);
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Object function = field.get(obj);
        if (function instanceof java.util.function.Function) {
            Object unused = ((java.util.function.Function) function).apply(ctx);
        } else if (function instanceof BiConsumer) {
            ((BiConsumer) function).accept(ctx, mock(ObjectNode.class));
        }
    }

    private void invokeMethod(Object obj, String methodName) throws Exception {
        System.out.println("Invoking method: " + methodName);
        java.lang.reflect.Method method = obj.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(obj);
    }
}
