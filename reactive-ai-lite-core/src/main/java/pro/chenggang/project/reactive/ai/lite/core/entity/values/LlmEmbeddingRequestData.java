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
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.EmbeddingExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Resolved data transfer object that gathers all necessary information for an embedding request to a specific LLM provider.
 * <p>
 * This immutable value object acts as a bridge between the declarative {@link EmbeddingExecutionInfo}
 * and the concrete provider delegate. It carries the execution context, model name, authentication token,
 * input texts, desired output dimensions, and an optional request body customizer.
 * </p>
 * <p>
 * The class is designed to be built via {@link #builder()} and is typically produced by
 * {@link LlmEmbeddingRequestDataInitializer#initialize()}, which resolves dynamic values
 * (like model name and token) using the current {@link ExecutionContext} and configured rules.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@ToString
@EqualsAndHashCode
@Builder
public class LlmEmbeddingRequestData {

    /**
     * The contextual metadata and state that accompanies the entire request lifecycle.
     * It may contain session information, correlation IDs, or other environment details
     * needed for logging, monitoring, or conditional processing.
     */
    @NonNull
    private final ExecutionContext executionContext;

    /**
     * The identifier of the embedding model to be invoked, as resolved by the
     * {@code modelNameConfigure} function in {@link EmbeddingExecutionInfo}.
     * This is provider-dependent and will be sent as part of the API request.
     */
    @NonNull
    private final String modelName;

    /**
     * The authentication token used to authorize the embedding request with the LLM provider.
     * Can be {@code null} if no authentication is required or if the provider uses alternative
     * mechanisms. The token is typically selected based on the profile configuration.
     */
    private final TokenCertification tokenCertification;

    /**
     * The list of textual inputs to be converted into vector embeddings.
     * Must not be {@code null} but can be empty. Each element represents a separate
     * input or document whose vector representation will be computed by the model.
     */
    @NonNull
    private final List<String> input;

    /**
     * The number of dimensions the resulting embeddings should have, if the model supports
     * such a configuration. A {@code null} value means the model's default dimensionality
     * will be used.
     */
    private final Integer dimensions;

    /**
     * An optional callback that allows modification of the raw JSON request body
     * right before it is serialized and dispatched to the LLM provider.
     * This enables fine-grained control over the request structure, such as adding
     * provider-specific parameters or overriding standard fields.
     */
    private final BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;

    /**
     * Provides the authentication token if one has been assigned to this data object.
     * The token is retrieved from the profile-based selection logic during initialization.
     *
     * @return an {@link Optional} containing the {@link TokenCertification}, or empty if none is set
     */
    public Optional<TokenCertification> getTokenCertification() {
        return Optional.ofNullable(tokenCertification);
    }

    /**
     * Returns the customizer function that can intercept and modify the raw JSON payload.
     * This function is invoked just before the HTTP call, receiving the current {@link ExecutionContext}
     * and the mutable {@link ObjectNode} representing the request body.
     *
     * @return the raw request customizer, may be {@code null} if no customization is needed
     */
    public BiConsumer<ExecutionContext, ObjectNode> getRawRequestCustomizerConfigure() {
        return rawRequestCustomizerConfigure;
    }

    /**
     * Helper class that constructs a {@link LlmEmbeddingRequestData} instance by resolving
     * dynamic values (model name, input, dimensions, token) from the {@link EmbeddingExecutionInfo}
     * and the current {@link ExecutionContext}, using the supplied {@link LlmProviderInfo}
     * and a map of profile-specific certifications.
     * <p>
     * The initialization process is reactive and lazy: it defers evaluation until subscription,
     * ensuring that the {@link ExecutionContext} is available from the subscriber context.
     * Profile selection logic decides which token to use based on whether a default profile
     * or a custom profile picker is defined.
     * </p>
     */
    public static class LlmEmbeddingRequestDataInitializer {

        /**
         * A map from profile name (matching the provider's defined profiles) to the corresponding
         * authentication certification. Used when a specific profile is picked.
         */
        private final Map<String, TokenCertification> certificationMap;

        /**
         * The default token certification to fall back on when the execution info indicates
         * that the default profile should be used.
         */
        private final TokenCertification defaultCertification;

        /**
         * Metadata about the LLM provider, including its available profiles, which drives the
         * profile-picking logic.
         */
        private final LlmProviderInfo llmProviderInfo;

        /**
         * The execution info that carries the declarative instructions (model resolver, input supplier,
         * dimension resolver, profile selector, and request customizer) to be turned into concrete values.
         */
        private final EmbeddingExecutionInfo executionInfo;

        /**
         * Private constructor to enforce creation through the static factory method {@link #of}.
         *
         * @param certificationMap  profiles mapped to their token certifications
         * @param defaultCertification the fallback certification, may be {@code null}
         * @param llmProviderInfo   the provider descriptor, must not be {@code null}
         * @param executionInfo     the embedding execution configuration, must not be {@code null}
         */
        private LlmEmbeddingRequestDataInitializer(@NonNull Map<String, TokenCertification> certificationMap,
                                                   TokenCertification defaultCertification,
                                                   @NonNull LlmProviderInfo llmProviderInfo,
                                                   @NonNull EmbeddingExecutionInfo executionInfo) {
            this.certificationMap = certificationMap;
            this.defaultCertification = defaultCertification;
            this.llmProviderInfo = llmProviderInfo;
            this.executionInfo = executionInfo;
        }

        /**
         * Static factory method to create an initializer instance.
         *
         * @param certificationMap  must not be {@code null}, maps profile name to token certification
         * @param defaultCertification optional default certification
         * @param llmProviderInfo   must not be {@code null}, provides provider metadata
         * @param executionInfo     must not be {@code null}, defines how to resolve request fields
         * @return a new initializer configured with the given dependencies
         */
        public static LlmEmbeddingRequestDataInitializer of(@NonNull Map<String, TokenCertification> certificationMap,
                                                            TokenCertification defaultCertification,
                                                            @NonNull LlmProviderInfo llmProviderInfo,
                                                            @NonNull EmbeddingExecutionInfo executionInfo) {
            return new LlmEmbeddingRequestDataInitializer(certificationMap, defaultCertification, llmProviderInfo, executionInfo);
        }

        /**
         * Initiates the reactive construction of a {@link LlmEmbeddingRequestData} using the
         * contextual {@link ExecutionContext} obtained from the subscriber's context.
         * <p>
         * The process resolves the model name via {@code modelNameConfigure}, extracts the input list,
         * determines the output vector dimensions (if configured), and selects the appropriate
         * {@link TokenCertification} based on the profile rules. If a profile is selected that does not
         * exist in the certification map, a {@link NoProfileFoundLlmClientException} is thrown.
         * </p>
         * <p>
         * This method defers its work until subscription to ensure the execution context is available
         * at runtime, enabling dynamic resolution of all field values.
         * </p>
         *
         * @return a {@link Mono} that, upon subscription, emits the fully resolved {@link LlmEmbeddingRequestData}
         */
        public Mono<LlmEmbeddingRequestData> initialize() {
            return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(ExecutionContext.class))
                            .ofType(ExecutionContext.class)
                            .switchIfEmpty(Mono.error(new pro.chenggang.project.reactive.ai.lite.core.exception.ExecutionContextLossException()))
                    )
                    .flatMap(executionContext -> {
                        return Mono.fromCallable(() -> {
                            String modelName = this.executionInfo.getModelNameConfigure().apply(executionContext);
                            List<String> input = this.executionInfo.getInputTextConfigure() != null ? this.executionInfo.getInputTextConfigure().apply(executionContext) : List.of();
                            Integer dimensions = this.executionInfo.getDimensionsConfigure() != null ? this.executionInfo.getDimensionsConfigure().apply(executionContext) : null;
                            TokenCertification tokenCertification = null;
                            if (this.executionInfo.isDefaultProfile()) {
                                tokenCertification = this.defaultCertification;
                            } else if (this.executionInfo.getProfilePicker() != null) {
                                String profile = this.executionInfo.getProfilePicker().apply(executionContext, this.llmProviderInfo.profiles());
                                tokenCertification = this.certificationMap.get(profile);
                                if (tokenCertification == null) {
                                    throw new NoProfileFoundLlmClientException(this.llmProviderInfo, profile);
                                }
                            }
                            return LlmEmbeddingRequestData.builder()
                                    .executionContext(executionContext)
                                    .modelName(modelName)
                                    .tokenCertification(tokenCertification)
                                    .input(input)
                                    .dimensions(dimensions)
                                    .rawRequestCustomizerConfigure(this.executionInfo.getRawRequestCustomizerConfigure())
                                    .build();
                        });
                    });
        }
    }
}