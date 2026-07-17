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
import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An extension of {@link SpeechSpec} that provides a fluent API for configuring
 * speech request parameters.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface ConfigurableSpeechSpec extends SpeechSpec {

    /**
     * Dynamically configures the model name to be used for the speech request.
     *
     * @param modelNameConfigure a function that accepts the {@link ExecutionContext}
     *                            and returns the model name to be used
     * @return this {@link ConfigurableSpeechSpec} instance for method chaining
     */
    ConfigurableSpeechSpec model(@NonNull Function<ExecutionContext, String> modelNameConfigure);

    /**
     * Sets a static model name for the speech request.
     *
     * @param modelName the static model name; must not be null
     * @return this {@link ConfigurableSpeechSpec} instance for method chaining
     */
    default ConfigurableSpeechSpec model(@NonNull String modelName) {
        return model(contextView -> modelName);
    }

    /**
     * Dynamically configures the text to be synthesized into speech.
     *
     * @param inputTextConfigure a function that receives the {@link ExecutionContext}
     *                            and returns a string to synthesize
     * @return this {@link ConfigurableSpeechSpec} instance for method chaining
     */
    ConfigurableSpeechSpec inputText(@NonNull Function<ExecutionContext, String> inputTextConfigure);

    /**
     * Convenience method to specify a single input string.
     *
     * @param inputText the input string to synthesize; must not be null
     * @return this {@link ConfigurableSpeechSpec} instance for method chaining
     */
    default ConfigurableSpeechSpec inputText(@NonNull String inputText) {
        return inputText(contextView -> inputText);
    }

    /**
     * Dynamically configures the voice to use for the speech request.
     *
     * @param voiceConfigure a function that accepts the {@link ExecutionContext}
     *                        and returns the voice name (e.g. alloy)
     * @return this {@link ConfigurableSpeechSpec} instance for method chaining
     */
    ConfigurableSpeechSpec voice(@NonNull Function<ExecutionContext, String> voiceConfigure);

    /**
     * Sets a static voice name for the speech request.
     *
     * @param voice the static voice name; must not be null
     * @return this {@link ConfigurableSpeechSpec} instance for method chaining
     */
    default ConfigurableSpeechSpec voice(@NonNull String voice) {
        return voice(contextView -> voice);
    }

    /**
     * Dynamically configures the speed of the generated audio.
     *
     * @param speedConfigure a function that accepts the {@link ExecutionContext}
     *                        and returns the speed (e.g. 1.0)
     * @return this {@link ConfigurableSpeechSpec} instance for method chaining
     */
    ConfigurableSpeechSpec speed(@NonNull Function<ExecutionContext, Double> speedConfigure);

    /**
     * Sets a static speed for the generated audio.
     *
     * @param speed the static speed; may be null to indicate no override
     * @return this {@link ConfigurableSpeechSpec} instance for method chaining
     */
    default ConfigurableSpeechSpec speed(Double speed) {
        if (Objects.nonNull(speed)) {
            return speed(contextView -> speed);
        }
        return this;
    }

    /**
     * Dynamically configures the response format for the speech request.
     *
     * @param responseFormatConfigure a function that accepts the {@link ExecutionContext}
     *                                 and returns the response format (e.g. mp3)
     * @return this {@link ConfigurableSpeechSpec} instance for method chaining
     */
    ConfigurableSpeechSpec responseFormat(@NonNull Function<ExecutionContext, String> responseFormatConfigure);

    /**
     * Sets a static response format for the speech request.
     *
     * @param responseFormat the static response format; may be null to indicate no override
     * @return this {@link ConfigurableSpeechSpec} instance for method chaining
     */
    default ConfigurableSpeechSpec responseFormat(String responseFormat) {
        if (Objects.nonNull(responseFormat)) {
            return responseFormat(contextView -> responseFormat);
        }
        return this;
    }

    /**
     * Registers a raw request customizer.
     *
     * @param rawRequestCustomizerConfigure a {@link BiConsumer}
     * @return this {@link ConfigurableSpeechSpec} instance for method chaining
     */
    ConfigurableSpeechSpec rawRequestCustomizer(@NonNull BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure);

    /**
     * Registers a raw request customizer.
     *
     * @param rawRequestCustomizerConfigure a {@link Consumer}
     * @return this {@link ConfigurableSpeechSpec} instance for method chaining
     */
    default ConfigurableSpeechSpec rawRequestCustomizer(@NonNull Consumer<ObjectNode> rawRequestCustomizerConfigure) {
        return rawRequestCustomizer((contextView, jsonNode) -> rawRequestCustomizerConfigure.accept(jsonNode));
    }
}
