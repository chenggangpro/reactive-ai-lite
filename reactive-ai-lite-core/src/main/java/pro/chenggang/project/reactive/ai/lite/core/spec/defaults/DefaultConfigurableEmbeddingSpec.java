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
import pro.chenggang.project.reactive.ai.lite.core.execution.EmbeddingExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.embedding.EmbeddingGeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.EmbeddingExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableEmbeddingSpec;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Default implementation of {@link ConfigurableEmbeddingSpec} that consolidates configuration
 * for embedding model calls. This class captures user-provided functions for dynamic resolution
 * of model parameters such as model name, input text, dimensions, and raw request customization.
 * It builds on the provider configuration defined earlier (via {@link ProviderConfigureInfo}) and
 * uses the {@link LlmProviderRegistry} to locate the appropriate LLM provider at execution time.
 * <p>
 * Instances are created by the framework through fluent API builders. Once all configurations
 * are set, the {@link #general()} method produces a {@link EmbeddingExecution} that can execute
 * embedding requests with the specified settings.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ConfigurableEmbeddingSpec
 * @see EmbeddingExecution
 */
@Getter(AccessLevel.PROTECTED)
public class DefaultConfigurableEmbeddingSpec implements ConfigurableEmbeddingSpec {

    /**
     * The type of LLM client (e.g., OpenAI, Azure) to be used for embedding operations.
     * Used to filter available provider implementations.
     */
    @NonNull
    private final LlmClientType llmClientType;

    /**
     * The registry containing available LLM provider implementations, enabling dynamic
     * provider selection at runtime.
     */
    @NonNull
    private final LlmProviderRegistry llmProviderRegistry;

    /**
     * Container holding the provider-specific configuration (parent attributes,
     * context configuration, default provider/profile flags, filters, etc.)
     * set in the previous specification stage.
     */
    @NonNull
    private final ProviderConfigureInfo providerConfigureInfo;

    /**
     * Function that resolves the embedding model name from the execution context.
     * May be {@code null} if not configured.
     */
    private Function<ExecutionContext, String> modelNameConfigure;

    /**
     * Function that extracts the list of input texts to embed from the execution context.
     * May be {@code null} if not configured.
     */
    private Function<ExecutionContext, List<String>> inputTextConfigure;

    /**
     * Function that determines the output vector dimensions based on the execution context.
     * May be {@code null} if not configured.
     */
    private Function<ExecutionContext, Integer> dimensionsConfigure;

    /**
     * Consumer that allows customizing the raw JSON request payload before it is sent
     * to the LLM provider. May be {@code null} if not configured.
     */
    private BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;

    /**
     * Constructs a new {@link DefaultConfigurableEmbeddingSpec} with required provider-related
     * dependencies. The constructor receives the client type, provider registry, and the preceding
     * provider specification so that this spec can build upon the already chosen provider settings.
     *
     * @param llmClientType        the LLM client type, not null
     * @param llmProviderRegistry  the registry for locating provider implementations, not null
     * @param providerConfigureInfo the provider configuration details from the previous spec stage, not null
     */
    public DefaultConfigurableEmbeddingSpec(@NonNull LlmClientType llmClientType,
                                        @NonNull LlmProviderRegistry llmProviderRegistry,
                                        @NonNull ProviderConfigureInfo providerConfigureInfo) {
        this.llmClientType = llmClientType;
        this.llmProviderRegistry = llmProviderRegistry;
        this.providerConfigureInfo = providerConfigureInfo;
    }

    /**
     * Configures the embedding model name resolver. The provided function will be invoked at execution
     * time with the current {@link ExecutionContext} to dynamically determine which model to use.
     *
     * @param modelNameConfigure a function mapping execution context to model name, must not be null
     * @return this spec instance for method chaining
     */
    @Override
    public ConfigurableEmbeddingSpec model(@NonNull Function<ExecutionContext, String> modelNameConfigure) {
        this.modelNameConfigure = modelNameConfigure;
        return this;
    }

    /**
     * Configures how the input texts to be embedded are extracted from the execution context.
     * The function receives the context and returns a list of strings, each to be embedded individually.
     *
     * @param inputTextConfigure a function extracting input texts from context, must not be null
     * @return this spec instance for method chaining
     */
    @Override
    public ConfigurableEmbeddingSpec inputText(@NonNull Function<ExecutionContext, List<String>> inputTextConfigure) {
        this.inputTextConfigure = inputTextConfigure;
        return this;
    }

    /**
     * Sets the desired dimensionality (vector size) for the embedding output. The function is called
     * with the execution context to allow dynamic dimension selection per request.
     *
     * @param dimensionsConfigure a function returning the number of dimensions, must not be null
     * @return this spec instance for method chaining
     */
    @Override
    public ConfigurableEmbeddingSpec dimensions(@NonNull Function<ExecutionContext, Integer> dimensionsConfigure) {
        this.dimensionsConfigure = dimensionsConfigure;
        return this;
    }

    /**
     * Registers a customizer for the raw JSON request payload before it is sent to the LLM provider.
     * The provided consumer receives the {@link ExecutionContext} and an {@link ObjectNode}
     * representing the request body, allowing arbitrary modifications (e.g., adding additional fields,
     * overriding defaults). This is useful for advanced use cases not covered by standard configuration.
     *
     * @param rawRequestCustomizerConfigure a consumer to modify the raw JSON request, must not be null
     * @return this spec instance for method chaining
     */
    @Override
    public ConfigurableEmbeddingSpec rawRequestCustomizer(@NonNull BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure) {
        this.rawRequestCustomizerConfigure = rawRequestCustomizerConfigure;
        return this;
    }

    /**
     * Creates an {@link EmbeddingExecution} instance that can execute embedding requests using
     * the consolidated specification. This method builds the final {@link EmbeddingExecutionSpec}
     * and delegates to {@link EmbeddingGeneralExecution#of} for construction.
     *
     * @return a new embedding execution handler ready to run
     */
    @Override
    public EmbeddingExecution general() {
        return EmbeddingGeneralExecution.of(this.llmProviderRegistry, this.toEmbeddingExecutionSpec());
    }

    /**
     * Assembles all configured parameters into a single {@link EmbeddingExecutionSpec} object.
     * This includes provider configuration, model name resolver, input text resolver, dimensions,
     * and raw request customizer. Only non-null values are set to avoid overriding defaults in the builder.
     * <p>
     * The resulting spec is typically used to create the final execution instance.
     *
     * @return the fully populated execution specification
     */
    protected EmbeddingExecutionSpec toEmbeddingExecutionSpec() {
        var builder = EmbeddingExecutionSpec.builder();
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
                .dimensionsConfigure(this.dimensionsConfigure)
                .build();
    }
}