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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.SystemOneExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.systemone.SystemOneGeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SystemOneExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestions;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableSystemOneSpec;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Default implementation of {@link ConfigurableSystemOneSpec}.
 * <p>
 * Consolidates configuration for SystemOne calls, capturing provider configuration,
 * dynamic model name resolution, and raw payload customization.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 * @see ConfigurableSystemOneSpec
 * @see SystemOneExecution
 */
@Getter(AccessLevel.PROTECTED)
public class DefaultConfigurableSystemOneSpec implements ConfigurableSystemOneSpec {

    /**
     * The LLM client type associated with SystemOne.
     */
    @NonNull
    private final LlmClientType llmClientType;

    /**
     * The registry for resolving provider implementations.
     */
    @NonNull
    private final LlmProviderRegistry llmProviderRegistry;

    /**
     * Container holding inherited provider settings from previous builder steps.
     */
    @NonNull
    private final ProviderConfigureInfo providerConfigureInfo;

    /**
     * Function resolving the model name from the execution context.
     */
    private Function<ExecutionContext, String> modelNameConfigure;

    /**
     * Function resolving the input state content from the execution context.
     */
    private Function<ExecutionContext, ? extends SystemOneContent<?>> stateConfigure;

    /**
     * Function resolving the questions schema from the execution context.
     */
    private Function<ExecutionContext, SystemOneQuestions> questionsConfigure;

    /**
     * Consumer for modifying the raw JSON request payload before transmission.
     */
    private BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;

    /**
     * Constructs a new {@link DefaultConfigurableSystemOneSpec} with required dependencies.
     *
     * @param llmClientType         the LLM client type; must not be null
     * @param llmProviderRegistry   the provider registry; must not be null
     * @param providerConfigureInfo the provider configuration snapshot; must not be null
     */
    public DefaultConfigurableSystemOneSpec(@NonNull LlmClientType llmClientType,
                                            @NonNull LlmProviderRegistry llmProviderRegistry,
                                            @NonNull ProviderConfigureInfo providerConfigureInfo) {
        this.llmClientType = llmClientType;
        this.llmProviderRegistry = llmProviderRegistry;
        this.providerConfigureInfo = providerConfigureInfo;
    }

    /**
     * Configures the dynamic model name resolver.
     *
     * @param modelNameConfigure the function mapping execution context to model name; must not be null
     * @return this spec instance for method chaining
     */
    @Override
    public ConfigurableSystemOneSpec model(@NonNull Function<ExecutionContext, String> modelNameConfigure) {
        this.modelNameConfigure = modelNameConfigure;
        return this;
    }

    /**
     * Configures the dynamic input state content resolver.
     *
     * @param <T>           the type of SystemOneContent
     * @param stateConfigure the function mapping execution context to state content; must not be null
     * @return this spec instance for method chaining
     */
    @Override
    public <T extends SystemOneContent<?>> ConfigurableSystemOneSpec state(@NonNull Function<ExecutionContext, T> stateConfigure) {
        this.stateConfigure = stateConfigure;
        return this;
    }

    /**
     * Configures the dynamic questions schema resolver.
     *
     * @param questionsConfigure the function mapping execution context to questions; must not be null
     * @return this spec instance for method chaining
     */
    @Override
    public ConfigurableSystemOneSpec questions(@NonNull Function<ExecutionContext, SystemOneQuestions> questionsConfigure) {
        this.questionsConfigure = questionsConfigure;
        return this;
    }

    /**
     * Registers a customizer for the raw JSON request payload.
     *
     * @param rawRequestCustomizerConfigure the customizer consumer; must not be null
     * @return this spec instance for method chaining
     */
    @Override
    public ConfigurableSystemOneSpec rawRequestCustomizer(@NonNull BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure) {
        this.rawRequestCustomizerConfigure = rawRequestCustomizerConfigure;
        return this;
    }

    /**
     * Initiates a general SystemOne execution using this specification.
     *
     * @return a {@link SystemOneExecution} instance ready to execute
     */
    @Override
    public SystemOneExecution general() {
        return SystemOneGeneralExecution.of(this.llmProviderRegistry, this.toSystemOneExecutionSpec());
    }

    /**
     * Converts this configurable specification into a structured {@link SystemOneExecutionSpec}.
     *
     * @return the populated {@link SystemOneExecutionSpec}
     */
    protected SystemOneExecutionSpec toSystemOneExecutionSpec() {
        var builder = SystemOneExecutionSpec.builder();
        if (Objects.nonNull(this.rawRequestCustomizerConfigure)) {
            builder.rawRequestCustomizerConfigure(this.rawRequestCustomizerConfigure);
        }
        if (Objects.nonNull(this.stateConfigure)) {
            builder.stateConfigure(this.stateConfigure);
        }
        if (Objects.nonNull(this.questionsConfigure)) {
            builder.questionsConfigure(this.questionsConfigure);
        }
        return builder.llmClientType(llmClientType)
                .parentAttributes(providerConfigureInfo.getParentAttributes())
                .contextConfigure(providerConfigureInfo.getContextConfigure())
                .defaultProvider(providerConfigureInfo.isDefaultProvider())
                .providerFilter(providerConfigureInfo.getProviderFilter())
                .defaultProfile(providerConfigureInfo.isDefaultProfile())
                .profilePicker(providerConfigureInfo.getProfilePicker())
                .modelNameConfigure(this.modelNameConfigure)
                .build();
    }
}
