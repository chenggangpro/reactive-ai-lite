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
import org.springframework.util.StringUtils;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.exception.ExecutionContextLossException;
import pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SystemOneExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Base64Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestions;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Resolved data transfer object holding information needed for executing a SystemOne request.
 * <p>
 * This value object acts as a bridge between the declarative specification layer and downstream
 * provider delegates. It encapsulates the runtime {@link ExecutionContext}, target model name,
 * authentication token, input state content, question schema, and raw request customizer.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@ToString
@EqualsAndHashCode
public class LlmSystemOneRequestData {

    /**
     * Contextual metadata and state accompanying the entire request lifecycle.
     */
    @Getter
    @NonNull
    private final ExecutionContext executionContext;

    /**
     * The identifier of the model to be invoked.
     */
    @Getter
    @NonNull
    private final String modelName;

    /**
     * The authentication token used to authorize the request with the LLM provider.
     */
    private final TokenCertification tokenCertification;

    /**
     * Optional image attachments shared by all questions.
     */
    @Getter
    @NonNull
    private final List<Base64Attachment> images;

    /**
     * The input state or unstructured data payload to be evaluated by the model.
     */
    @Getter
    @NonNull
    private final SystemOneContent<?> state;

    /**
     * The predefined schema of questions evaluated in parallel against the input state.
     */
    @Getter
    @NonNull
    private final SystemOneQuestions questions;

    /**
     * Optional callback to customize the raw JSON request body before sending.
     */
    @Getter
    private final BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;

    /**
     * Constructs a new {@link LlmSystemOneRequestData} instance.
     *
     * @param executionContext              the execution context; must not be null
     * @param modelName                     the model name; must not be null
     * @param tokenCertification            optional token certification
     * @param images                        optional list of image attachments
     * @param state                         the input state content; must not be null or {@link SystemOneContent#NULL}
     * @param questions                     the questions schema; must not be null
     * @param rawRequestCustomizerConfigure optional request customizer callback
     */
    @Builder
    public LlmSystemOneRequestData(@NonNull ExecutionContext executionContext,
                                   @NonNull String modelName,
                                   TokenCertification tokenCertification,
                                   List<Base64Attachment> images,
                                   @NonNull SystemOneContent<?> state,
                                   @NonNull SystemOneQuestions questions,
                                   BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure) {
        if (state instanceof SystemOneContent.NullContent || Objects.isNull(state.getValue())) {
            throw new IllegalArgumentException("State content is required and cannot be null or SystemOneContent.NULL");
        }
        this.executionContext = executionContext;
        this.modelName = modelName;
        this.tokenCertification = tokenCertification;
        this.images = Objects.nonNull(images) ? List.copyOf(images) : List.of();
        this.state = state;
        this.questions = questions;
        this.rawRequestCustomizerConfigure = rawRequestCustomizerConfigure;
    }

    /**
     * Provides the authentication token if one has been assigned.
     *
     * @return an {@link Optional} containing the {@link TokenCertification}, or empty if none is set
     */
    public Optional<TokenCertification> getTokenCertification() {
        return Optional.ofNullable(tokenCertification);
    }

    /**
     * Helper class to initialize and construct {@link LlmSystemOneRequestData} instances.
     * <p>
     * Resolves the runtime model name, token certification, input state, questions schema,
     * and raw request customizer from the current {@link ExecutionContext} and {@link SystemOneExecutionInfo}.
     * </p>
     */
    public static class LlmSystemOneRequestDataInitializer {

        private final Map<String, TokenCertification> certificationMap;
        private final TokenCertification defaultCertification;
        private final LlmProviderInfo llmProviderInfo;
        private final SystemOneExecutionInfo executionInfo;

        private LlmSystemOneRequestDataInitializer(@NonNull Map<String, TokenCertification> certificationMap,
                                                   TokenCertification defaultCertification,
                                                   @NonNull LlmProviderInfo llmProviderInfo,
                                                   @NonNull SystemOneExecutionInfo executionInfo) {
            this.certificationMap = certificationMap;
            this.defaultCertification = defaultCertification;
            this.llmProviderInfo = llmProviderInfo;
            this.executionInfo = executionInfo;
        }

        /**
         * Creates a new instance of {@link LlmSystemOneRequestDataInitializer}.
         *
         * @param certificationMap     the map of token certifications by profile name; must not be null
         * @param defaultCertification the default token certification; may be null
         * @param llmProviderInfo      the provider descriptor; must not be null
         * @param executionInfo        the execution info for SystemOne; must not be null
         * @return a new {@link LlmSystemOneRequestDataInitializer} instance
         */
        public static LlmSystemOneRequestDataInitializer of(@NonNull Map<String, TokenCertification> certificationMap,
                                                            TokenCertification defaultCertification,
                                                            @NonNull LlmProviderInfo llmProviderInfo,
                                                            @NonNull SystemOneExecutionInfo executionInfo) {
            return new LlmSystemOneRequestDataInitializer(certificationMap, defaultCertification, llmProviderInfo, executionInfo);
        }

        /**
         * Resolves the token certification based on the profile configuration.
         *
         * @param executionInfo    the execution info
         * @param executionContext the execution context
         * @return the resolved {@link TokenCertification}, or {@code null} if none is configured
         * @throws NoProfileFoundLlmClientException if a configured profile cannot be resolved
         */
        protected TokenCertification loadTokenCertification(@NonNull SystemOneExecutionInfo executionInfo,
                                                            @NonNull ExecutionContext executionContext) {
            if (executionInfo.isDefaultProfile()) {
                return this.defaultCertification;
            }
            BiFunction<ExecutionContext, Set<String>, String> profilePicker = executionInfo.getProfilePicker();
            if (Objects.isNull(profilePicker)) {
                return null;
            }
            String pickedProfile = profilePicker.apply(executionContext, this.llmProviderInfo.profiles());
            if (Objects.isNull(pickedProfile) || !this.certificationMap.containsKey(pickedProfile)) {
                throw new NoProfileFoundLlmClientException(this.llmProviderInfo, pickedProfile);
            }
            return this.certificationMap.get(pickedProfile);
        }

        /**
         * Resolves the model name from the execution configuration.
         *
         * @param executionInfo    the execution info
         * @param executionContext the execution context
         * @return the resolved model name
         * @throws IllegalArgumentException if the resolved model name is null or blank
         */
        protected String loadModelName(@NonNull SystemOneExecutionInfo executionInfo,
                                       @NonNull ExecutionContext executionContext) {
            String modelName = executionInfo.getModelNameConfigure().apply(executionContext);
            if (!StringUtils.hasText(modelName)) {
                throw new IllegalArgumentException("Model name is required for SystemOne execution");
            }
            return modelName;
        }

        /**
         * Resolves the image attachments from the execution configuration.
         *
         * @param executionInfo    the execution info
         * @param executionContext the execution context
         * @return the resolved immutable list of {@link Base64Attachment} instances (empty list if unconfigured)
         */
        protected List<Base64Attachment> loadImages(@NonNull SystemOneExecutionInfo executionInfo,
                                                    @NonNull ExecutionContext executionContext) {
            if (Objects.isNull(executionInfo.getImagesConfigure())) {
                return List.of();
            }
            List<Base64Attachment> images = executionInfo.getImagesConfigure().apply(executionContext);
            if (Objects.isNull(images) || images.isEmpty()) {
                return List.of();
            }
            for (Base64Attachment image : images) {
                if (Objects.isNull(image) || Objects.isNull(image.base64Content()) || image.base64Content().isEmpty()) {
                    throw new IllegalArgumentException("Image attachment in images list must not be null");
                }
            }
            return List.copyOf(images);
        }

        /**
         * Resolves the input state content from the execution configuration.
         *
         * @param executionInfo    the execution info
         * @param executionContext the execution context
         * @return the resolved {@link SystemOneContent}
         * @throws IllegalArgumentException if state configuration is missing, returns null, or is {@link SystemOneContent#NULL}
         */
        protected SystemOneContent<?> loadState(@NonNull SystemOneExecutionInfo executionInfo,
                                                @NonNull ExecutionContext executionContext) {
            if (Objects.isNull(executionInfo.getStateConfigure())) {
                throw new IllegalArgumentException("State configuration is required for SystemOne execution");
            }
            SystemOneContent<?> state = executionInfo.getStateConfigure().apply(executionContext);
            if (Objects.isNull(state) || state instanceof SystemOneContent.NullContent || Objects.isNull(state.getValue())) {
                throw new IllegalArgumentException("State content is required and cannot be null or SystemOneContent.NULL");
            }
            return state;
        }

        /**
         * Resolves the questions schema from the execution configuration.
         *
         * @param executionInfo    the execution info
         * @param executionContext the execution context
         * @return the resolved {@link SystemOneQuestions}
         * @throws IllegalArgumentException if questions configuration is missing or contains no questions
         */
        protected SystemOneQuestions loadQuestions(@NonNull SystemOneExecutionInfo executionInfo,
                                                   @NonNull ExecutionContext executionContext) {
            if (Objects.isNull(executionInfo.getQuestionsConfigure())) {
                throw new IllegalArgumentException("Questions configuration is required for SystemOne execution");
            }
            SystemOneQuestions questions = executionInfo.getQuestionsConfigure().apply(executionContext);
            if (Objects.isNull(questions) || questions.getAllQuestions().isEmpty()) {
                throw new IllegalArgumentException("SystemOne execution requires at least one question");
            }
            return questions;
        }

        /**
         * Initializes and constructs the {@link LlmSystemOneRequestData} reactively from the reactive execution context.
         *
         * @return a {@link Mono} emitting the constructed {@link LlmSystemOneRequestData}
         */
        public Mono<LlmSystemOneRequestData> initialize() {
            return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(ExecutionContext.class))
                            .ofType(ExecutionContext.class)
                            .switchIfEmpty(Mono.error(new ExecutionContextLossException()))
                    )
                    .flatMap(executionContext -> Mono.fromCallable(() -> {
                        TokenCertification tokenCertification = loadTokenCertification(executionInfo, executionContext);
                        String modelName = loadModelName(executionInfo, executionContext);
                        List<Base64Attachment> images = loadImages(executionInfo, executionContext);
                        SystemOneContent<?> state = loadState(executionInfo, executionContext);
                        SystemOneQuestions questions = loadQuestions(executionInfo, executionContext);
                        return LlmSystemOneRequestData.builder()
                                .executionContext(executionContext)
                                .modelName(modelName)
                                .tokenCertification(tokenCertification)
                                .images(images)
                                .state(state)
                                .questions(questions)
                                .rawRequestCustomizerConfigure(executionInfo.getRawRequestCustomizerConfigure())
                                .build();
                    }));
        }
    }

}
