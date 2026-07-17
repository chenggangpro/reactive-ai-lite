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
import pro.chenggang.project.reactive.ai.lite.core.execution.SpeechExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.SpeechStreamExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.speech.DefaultSpeechExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.speech.DefaultSpeechStreamExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SpeechExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableSpeechSpec;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Default implementation of {@link ConfigurableSpeechSpec}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter(AccessLevel.PROTECTED)
public class DefaultConfigurableSpeechSpec implements ConfigurableSpeechSpec {

    @NonNull
    private final LlmClientType llmClientType;

    @NonNull
    private final LlmProviderRegistry llmProviderRegistry;

    @NonNull
    private final ProviderConfigureInfo providerConfigureInfo;

    private Function<ExecutionContext, String> modelNameConfigure;
    private Function<ExecutionContext, String> inputTextConfigure;
    private Function<ExecutionContext, String> voiceConfigure;
    private Function<ExecutionContext, Double> speedConfigure;
    private Function<ExecutionContext, String> responseFormatConfigure;
    private BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;

    /**
     * Constructs a new DefaultConfigurableSpeechSpec.
     *
     * @param llmClientType         the LLM client type
     * @param llmProviderRegistry   the provider registry
     * @param providerConfigureInfo the provider configure info
     */
    public DefaultConfigurableSpeechSpec(@NonNull LlmClientType llmClientType,
                                         @NonNull LlmProviderRegistry llmProviderRegistry,
                                         @NonNull ProviderConfigureInfo providerConfigureInfo) {
        this.llmClientType = llmClientType;
        this.llmProviderRegistry = llmProviderRegistry;
        this.providerConfigureInfo = providerConfigureInfo;
    }

    @Override
    public ConfigurableSpeechSpec model(@NonNull Function<ExecutionContext, String> modelNameConfigure) {
        this.modelNameConfigure = modelNameConfigure;
        return this;
    }

    @Override
    public ConfigurableSpeechSpec inputText(@NonNull Function<ExecutionContext, String> inputTextConfigure) {
        this.inputTextConfigure = inputTextConfigure;
        return this;
    }

    @Override
    public ConfigurableSpeechSpec voice(@NonNull Function<ExecutionContext, String> voiceConfigure) {
        this.voiceConfigure = voiceConfigure;
        return this;
    }

    @Override
    public ConfigurableSpeechSpec speed(@NonNull Function<ExecutionContext, Double> speedConfigure) {
        this.speedConfigure = speedConfigure;
        return this;
    }

    @Override
    public ConfigurableSpeechSpec responseFormat(@NonNull Function<ExecutionContext, String> responseFormatConfigure) {
        this.responseFormatConfigure = responseFormatConfigure;
        return this;
    }

    @Override
    public ConfigurableSpeechSpec rawRequestCustomizer(@NonNull BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure) {
        this.rawRequestCustomizerConfigure = rawRequestCustomizerConfigure;
        return this;
    }

    /**
     * Initiates a general speech execution using this specification.
     *
     * @return a {@link SpeechExecution} instance
     */
    @Override
    public SpeechExecution general() {
        return DefaultSpeechExecution.of(this.llmProviderRegistry, this.toSpeechExecutionSpec());
    }

    /**
     * Initiates a streaming speech execution using this specification.
     *
     * @return a {@link SpeechStreamExecution} instance
     */
    @Override
    public SpeechStreamExecution stream() {
        return DefaultSpeechStreamExecution.of(this.llmProviderRegistry, this.toSpeechExecutionSpec());
    }

    /**
     * Converts this configurable spec into a structured {@link SpeechExecutionSpec}.
     *
     * @return the resulting SpeechExecutionSpec
     */
    protected SpeechExecutionSpec toSpeechExecutionSpec() {
        var builder = SpeechExecutionSpec.builder();
        if (Objects.nonNull(this.rawRequestCustomizerConfigure)) {
            builder.rawRequestCustomizerConfigure(this.rawRequestCustomizerConfigure);
        }
        return builder.llmClientType(llmClientType)
                .parentAttributes(providerConfigureInfo.getParentAttributes())
                .contextConfigure(providerConfigureInfo.getContextConfigure())
                .defaultProvider(providerConfigureInfo.isDefaultProvider())
                .providerFilter(providerConfigureInfo.getProviderFilter())
                .defaultProfile(providerConfigureInfo.isDefaultProfile())
                .profilePicker(providerConfigureInfo.getProfilePicker())
                .modelNameConfigure(this.modelNameConfigure)
                .inputTextConfigure(this.inputTextConfigure)
                .voiceConfigure(this.voiceConfigure)
                .speedConfigure(this.speedConfigure)
                .responseFormatConfigure(this.responseFormatConfigure)
                .build();
    }
}
