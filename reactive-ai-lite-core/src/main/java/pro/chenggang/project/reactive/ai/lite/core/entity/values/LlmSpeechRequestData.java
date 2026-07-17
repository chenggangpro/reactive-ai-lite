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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.exception.ExecutionContextLossException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Encapsulates the core data required to construct a speech generation request.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LlmSpeechRequestData {

    /**
     * The execution context associated with this request.
     */
    @Getter
    @NonNull
    private final ExecutionContext executionContext;

    /**
     * The token certification to use for this request.
     */
    private final TokenCertification tokenCertification;

    /**
     * The name of the speech model to use.
     */
    @Getter
    private final String modelName;

    /**
     * The text input to convert to speech.
     */
    @Getter
    private final String input;

    /**
     * The voice to use for speech generation.
     */
    @Getter
    private final String voice;

    /**
     * The speed of the generated speech.
     */
    @Getter
    private final Double speed;

    /**
     * The audio format of the response (e.g., mp3, pcm).
     */
    @Getter
    private final String responseFormat;

    /**
     * A consumer to customize the raw JSON request body before sending.
     */
    @Getter
    private final BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;

    /**
     * Retrieves the token certification wrapped in an Optional.
     *
     * @return an Optional containing the token certification, or empty if none is set
     */
    public Optional<TokenCertification> getTokenCertification() {
        return Optional.ofNullable(tokenCertification);
    }

    /**
     * Helper class to initialize and construct {@link LlmSpeechRequestData} instances.
     */
    public static class LlmSpeechRequestDataInitializer {

        private final Map<String, TokenCertification> certificationMap;
        private final TokenCertification defaultCertification;
        private final pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo providerInfo;
        private final pro.chenggang.project.reactive.ai.lite.core.execution.values.SpeechExecutionInfo executionInfo;

        private LlmSpeechRequestDataInitializer(Map<String, TokenCertification> certificationMap,
                                                TokenCertification defaultCertification,
                                                pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo providerInfo,
                                                pro.chenggang.project.reactive.ai.lite.core.execution.values.SpeechExecutionInfo executionInfo) {
            this.certificationMap = certificationMap;
            this.defaultCertification = defaultCertification;
            this.providerInfo = providerInfo;
            this.executionInfo = executionInfo;
        }

        /**
         * Creates a new instance of LlmSpeechRequestDataInitializer.
         *
         * @param certificationMap     the map of token certifications by profile name
         * @param defaultCertification the default token certification
         * @param providerInfo         the provider info
         * @param executionInfo        the execution info for speech
         * @return a new LlmSpeechRequestDataInitializer instance
         */
        public static LlmSpeechRequestDataInitializer of(Map<String, TokenCertification> certificationMap,
                                                         TokenCertification defaultCertification,
                                                         pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo providerInfo,
                                                         pro.chenggang.project.reactive.ai.lite.core.execution.values.SpeechExecutionInfo executionInfo) {
            return new LlmSpeechRequestDataInitializer(certificationMap, defaultCertification, providerInfo, executionInfo);
        }

        /**
         * Loads the appropriate token certification based on the execution info and context.
         *
         * @param executionInfo    the execution info
         * @param executionContext the execution context
         * @return the resolved TokenCertification
         * @throws pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException if no matching profile is found
         */
        protected TokenCertification loadTokenCertification(@NonNull pro.chenggang.project.reactive.ai.lite.core.execution.values.SpeechExecutionInfo executionInfo,
                                                            @NonNull ExecutionContext executionContext) {
            if (executionInfo.isDefaultProfile()) {
                return this.defaultCertification;
            }
            java.util.function.BiFunction<ExecutionContext, java.util.Set<String>, String> profilePicker = executionInfo.getProfilePicker();
            if (Objects.isNull(profilePicker)) {
                throw new pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException(this.providerInfo);
            }
            String pickedProfile = profilePicker.apply(executionContext, this.providerInfo.profiles());
            if (Objects.isNull(pickedProfile) || !this.certificationMap.containsKey(pickedProfile)) {
                throw new pro.chenggang.project.reactive.ai.lite.core.exception.NoProfileFoundLlmClientException(this.providerInfo, pickedProfile);
            }
            return certificationMap.get(pickedProfile);
        }

        /**
         * Initializes and builds the LlmSpeechRequestData.
         *
         * @return a Mono emitting the fully constructed LlmSpeechRequestData
         */
        public Mono<LlmSpeechRequestData> initialize() {
            return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(ExecutionContext.class))
                            .ofType(ExecutionContext.class)
                            .switchIfEmpty(Mono.error(new ExecutionContextLossException()))
                    )
                    .flatMap(executionContext -> {
                        return Mono.fromCallable(() -> {
                            TokenCertification tokenCertification = loadTokenCertification(executionInfo, executionContext);
                            String model = executionInfo.getModelNameConfigure().apply(executionContext);
                            if (model == null || model.isBlank()) {
                                throw new IllegalArgumentException("Model name is required for Speech execution");
                            }
                            String input = null;
                            if (Objects.nonNull(executionInfo.getInputTextConfigure())) {
                                input = executionInfo.getInputTextConfigure().apply(executionContext);
                            }
                            String voice = null;
                            if (Objects.nonNull(executionInfo.getVoiceConfigure())) {
                                voice = executionInfo.getVoiceConfigure().apply(executionContext);
                            }
                            Double speed = null;
                            if (Objects.nonNull(executionInfo.getSpeedConfigure())) {
                                speed = executionInfo.getSpeedConfigure().apply(executionContext);
                            }
                            String responseFormat = null;
                            if (Objects.nonNull(executionInfo.getResponseFormatConfigure())) {
                                responseFormat = executionInfo.getResponseFormatConfigure().apply(executionContext);
                            }
                            return LlmSpeechRequestData.builder()
                                    .executionContext(executionContext)
                                    .tokenCertification(tokenCertification)
                                    .modelName(model)
                                    .input(input)
                                    .voice(voice)
                                    .speed(speed)
                                    .responseFormat(responseFormat)
                                    .rawRequestCustomizerConfigure(executionInfo.getRawRequestCustomizerConfigure())
                                    .build();
                        });
                    });
        }
    }
}
