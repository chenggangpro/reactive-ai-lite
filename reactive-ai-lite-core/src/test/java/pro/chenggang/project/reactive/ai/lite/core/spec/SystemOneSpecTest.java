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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.SystemOneExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SystemOneExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SystemOneExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestions;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.defaults.DefaultConfigurableSystemOneSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.defaults.ProviderConfigureInfo;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ConfigurableSystemOneSpec} and {@link DefaultConfigurableSystemOneSpec}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
class SystemOneSpecTest {

    private LlmProviderRegistry registry;
    private ProviderConfigureInfo providerConfigureInfo;

    @BeforeEach
    void setUp() {
        registry = mock(LlmProviderRegistry.class);
        providerConfigureInfo = ProviderConfigureInfo.builder()
                .defaultProvider(true)
                .defaultProfile(true)
                .parentAttributes(Map.of("k", "v"))
                .build();
    }

    @Test
    void testConstructorNullChecks() {
        assertThatThrownBy(() -> new DefaultConfigurableSystemOneSpec(null, registry, providerConfigureInfo))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultConfigurableSystemOneSpec(LlmClientType.SYSTEM_ONE, null, providerConfigureInfo))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultConfigurableSystemOneSpec(LlmClientType.SYSTEM_ONE, registry, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testModelConfiguration() {
        DefaultConfigurableSystemOneSpec spec = new DefaultConfigurableSystemOneSpec(
                LlmClientType.SYSTEM_ONE,
                registry,
                providerConfigureInfo
        );
        ExecutionContext ctx = ExecutionContext.newContext();

        // Null checks
        assertThatThrownBy(() -> spec.model((String) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.model((Function<ExecutionContext, String>) null))
                .isInstanceOf(IllegalArgumentException.class);

        // Static model
        spec.model("system-one-model-v1");
        SystemOneExecutionSpec execSpec = invokeToSystemOneExecutionSpec(spec);
        assertThat(execSpec.getModelNameConfigure().apply(ctx)).isEqualTo("system-one-model-v1");

        // Dynamic model function
        spec.model(c -> "dynamic-model");
        execSpec = invokeToSystemOneExecutionSpec(spec);
        assertThat(execSpec.getModelNameConfigure().apply(ctx)).isEqualTo("dynamic-model");
    }

    @Test
    void testRawRequestCustomizer() {
        DefaultConfigurableSystemOneSpec spec = new DefaultConfigurableSystemOneSpec(
                LlmClientType.SYSTEM_ONE,
                registry,
                providerConfigureInfo
        );
        ExecutionContext ctx = ExecutionContext.newContext();
        ObjectNode node = JsonNodeFactory.instance.objectNode();

        // Null checks
        assertThatThrownBy(() -> spec.rawRequestCustomizer((Consumer<ObjectNode>) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.rawRequestCustomizer((BiConsumer<ExecutionContext, ObjectNode>) null))
                .isInstanceOf(IllegalArgumentException.class);

        spec.model("test-model");

        // Consumer customization
        AtomicBoolean invokedConsumer = new AtomicBoolean(false);
        spec.rawRequestCustomizer(n -> invokedConsumer.set(true));
        SystemOneExecutionSpec execSpec = invokeToSystemOneExecutionSpec(spec);
        execSpec.getRawRequestCustomizerConfigure().accept(ctx, node);
        assertThat(invokedConsumer.get()).isTrue();

        // BiConsumer customization
        AtomicBoolean invokedBiConsumer = new AtomicBoolean(false);
        spec.rawRequestCustomizer((c, n) -> invokedBiConsumer.set(true));
        execSpec = invokeToSystemOneExecutionSpec(spec);
        execSpec.getRawRequestCustomizerConfigure().accept(ctx, node);
        assertThat(invokedBiConsumer.get()).isTrue();
    }

    @Test
    void testGeneralExecutionCreation() {
        DefaultConfigurableSystemOneSpec spec = new DefaultConfigurableSystemOneSpec(
                LlmClientType.SYSTEM_ONE,
                registry,
                providerConfigureInfo
        );
        spec.model("system-one-model");

        SystemOneExecution execution = spec.general();
        assertThat(execution).isNotNull();
    }

    @Test
    void testExecutionSpecToExecutionInfo() {
        DefaultConfigurableSystemOneSpec spec = new DefaultConfigurableSystemOneSpec(
                LlmClientType.SYSTEM_ONE,
                registry,
                providerConfigureInfo
        );
        spec.model("system-one-model");

        SystemOneExecutionSpec execSpec = invokeToSystemOneExecutionSpec(spec);
        assertThat(execSpec.getLlmClientType()).isEqualTo(LlmClientType.SYSTEM_ONE);
        assertThat(execSpec.isDefaultProvider()).isTrue();
        assertThat(execSpec.isDefaultProfile()).isTrue();
        assertThat(execSpec.getParentAttributes()).containsEntry("k", "v");

        ExecutionContext ctx = ExecutionContext.newContext();
        SystemOneExecutionInfo info = execSpec.newExecutionInfo(ctx);
        assertThat(info).isNotNull();
        assertThat(info.isDefaultProfile()).isTrue();
        assertThat(info.getModelNameConfigure().apply(ctx)).isEqualTo("system-one-model");
    }

    @Test
    void testStateConfiguration() {
        DefaultConfigurableSystemOneSpec spec = new DefaultConfigurableSystemOneSpec(
                LlmClientType.SYSTEM_ONE,
                registry,
                providerConfigureInfo
        );
        spec.model("system-one-model");
        ExecutionContext ctx = ExecutionContext.newContext();

        // Null checks
        assertThatThrownBy(() -> spec.state((Function<ExecutionContext, SystemOneContent<?>>) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.state((SystemOneContent<?>) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.state((String) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.state((Map<String, Object>) null))
                .isInstanceOf(IllegalArgumentException.class);

        // String state
        spec.state("plain text input");
        SystemOneExecutionSpec execSpec = invokeToSystemOneExecutionSpec(spec);
        assertThat(execSpec.getStateConfigure().apply(ctx).getValue()).isEqualTo("plain text input");

        // Map state
        spec.state(Map.of("user_id", "u123"));
        execSpec = invokeToSystemOneExecutionSpec(spec);
        Map<?, ?> mapValue = (Map<?, ?>) execSpec.getStateConfigure().apply(ctx).getValue();
        assertThat(mapValue.get("user_id")).isEqualTo("u123");

        // Array state
        spec.state("a", "b", "c");
        execSpec = invokeToSystemOneExecutionSpec(spec);
        java.util.List<?> listValue = (java.util.List<?>) execSpec.getStateConfigure().apply(ctx).getValue();
        assertThat(listValue).hasSize(3);
        assertThat(listValue.get(0)).isEqualTo("a");
        assertThat(listValue.get(1)).isEqualTo("b");
        assertThat(listValue.get(2)).isEqualTo("c");

        // Empty state
        spec.emptyState();
        execSpec = invokeToSystemOneExecutionSpec(spec);
        assertThat(execSpec.getStateConfigure().apply(ctx).getValue()).isNull();

        // Dynamic state function
        spec.state(c -> SystemOneContent.text("dynamic payload"));
        execSpec = invokeToSystemOneExecutionSpec(spec);
        assertThat(execSpec.getStateConfigure().apply(ctx).getValue()).isEqualTo("dynamic payload");
    }

    @Test
    void testQuestionsConfiguration() {
        DefaultConfigurableSystemOneSpec spec = new DefaultConfigurableSystemOneSpec(
                LlmClientType.SYSTEM_ONE,
                registry,
                providerConfigureInfo
        );
        spec.model("system-one-model");
        ExecutionContext ctx = ExecutionContext.newContext();
        SystemOneQuestion q1 = SystemOneQuestion.newNoulBuilder("Is legit?").build();
        SystemOneQuestion q2 = SystemOneQuestion.newChoiceBuilder("Category")
                .option("a", "Alpha")
                .option("b", "Beta")
                .build();

        // Null checks
        assertThatThrownBy(() -> spec.questions((Function<ExecutionContext, SystemOneQuestions>) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.questions((SystemOneQuestions) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.questionsBuilder((Consumer<SystemOneQuestions.Builder>) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.question(null, q1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.question("q1", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spec.questions((Map<String, SystemOneQuestion>) null))
                .isInstanceOf(IllegalArgumentException.class);

        // Single question
        spec.question("is_legit", q1);
        SystemOneExecutionSpec execSpec = invokeToSystemOneExecutionSpec(spec);
        assertThat(execSpec.getQuestionsConfigure().apply(ctx).getAllQuestions()).containsKey("is_legit");

        // Questions builder consumer
        spec.questionsBuilder(b -> b.question("q1", q1).question("q2", q2));
        execSpec = invokeToSystemOneExecutionSpec(spec);
        assertThat(execSpec.getQuestionsConfigure().apply(ctx).getAllQuestions()).hasSize(2);

        // Questions Map
        spec.questions(Map.of("q1", q1, "q2", q2));
        execSpec = invokeToSystemOneExecutionSpec(spec);
        assertThat(execSpec.getQuestionsConfigure().apply(ctx).getAllQuestions()).hasSize(2);

        // Dynamic questions function
        spec.questions(c -> SystemOneQuestions.of("dyn", q1));
        execSpec = invokeToSystemOneExecutionSpec(spec);
        assertThat(execSpec.getQuestionsConfigure().apply(ctx).getAllQuestions()).containsKey("dyn");
    }

    private SystemOneExecutionSpec invokeToSystemOneExecutionSpec(DefaultConfigurableSystemOneSpec spec) {
        try {
            java.lang.reflect.Method method = DefaultConfigurableSystemOneSpec.class.getDeclaredMethod("toSystemOneExecutionSpec");
            method.setAccessible(true);
            return (SystemOneExecutionSpec) method.invoke(spec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
