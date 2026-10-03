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
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Base64Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestions;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An extension of {@link SystemOneSpec} that provides a fluent configuration API for SystemOne requests.
 * <p>
 * Supports both static values and dynamic resolution via {@link ExecutionContext}, enabling runtime adaptation
 * to context parameters (e.g. tenant, model routing flags).
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see SystemOneSpec
 * @see ExecutionContext
 * @since 0.1.0
 */
public interface ConfigurableSystemOneSpec extends SystemOneSpec {

    /**
     * Dynamically configures the model name to be used for the SystemOne request.
     * The function is evaluated at execution time with the current {@link ExecutionContext}.
     *
     * @param modelNameConfigure a function mapping {@link ExecutionContext} to the model name; must not be null
     * @return this spec instance for method chaining
     */
    ConfigurableSystemOneSpec model(@NonNull Function<ExecutionContext, String> modelNameConfigure);

    /**
     * Sets a static model name for the SystemOne request.
     * Delegates to {@link #model(Function)} with a constant function.
     *
     * @param modelName the static model name; must not be null
     * @return this spec instance for method chaining
     */
    default ConfigurableSystemOneSpec model(@NonNull String modelName) {
        return model(contextView -> modelName);
    }

    /**
     * Dynamically configures the image attachments to be evaluated by the SystemOne model.
     * The function is evaluated at execution time with the current {@link ExecutionContext}.
     *
     * @param imagesConfigure a function mapping {@link ExecutionContext} to the list of {@link Base64Attachment} images; must not be null
     * @return this spec instance for method chaining
     */
    ConfigurableSystemOneSpec images(@NonNull Function<ExecutionContext, List<Base64Attachment>> imagesConfigure);

    /**
     * Sets static image attachments for the SystemOne request.
     *
     * @param images the list of {@link Base64Attachment} images; must not be null
     * @return this spec instance for method chaining
     */
    default ConfigurableSystemOneSpec images(@NonNull List<Base64Attachment> images) {
        return images(contextView -> images);
    }

    /**
     * Sets static image attachments for the SystemOne request using varargs.
     *
     * @param images the {@link Base64Attachment} images to attach
     * @return this spec instance for method chaining
     */
    default ConfigurableSystemOneSpec images(@NonNull Base64Attachment... images) {
        return images(contextView -> List.of(images));
    }

    /**
     * Convenience method to attach a single image to the SystemOne request.
     *
     * @param image the {@link Base64Attachment} image; must not be null
     * @return this spec instance for method chaining
     */
    default ConfigurableSystemOneSpec image(@NonNull Base64Attachment image) {
        return images(List.of(image));
    }

    /**
     * Dynamically configures the input state content to be evaluated by the SystemOne model.
     * The function is evaluated at execution time with the current {@link ExecutionContext}.
     *
     * @param <T>            the type of SystemOneContent
     * @param stateConfigure a function mapping {@link ExecutionContext} to the state content; must not be null
     * @return this spec instance for method chaining
     */
    <T extends SystemOneContent<?>> ConfigurableSystemOneSpec state(@NonNull Function<ExecutionContext, T> stateConfigure);

    /**
     * Sets static input state content for the SystemOne request.
     *
     * @param state the state content to evaluate; must not be null
     * @return this spec instance for method chaining
     */
    default ConfigurableSystemOneSpec state(@NonNull SystemOneContent<?> state) {
        if (state instanceof SystemOneContent.NullContent || Objects.isNull(state.getValue())) {
            throw new IllegalArgumentException("State content is required and cannot be SystemOneContent.NULL");
        }
        return state(contextView -> state);
    }

    /**
     * Convenience method to configure plain text input state.
     *
     * @param text the text to evaluate; must not be null
     * @return this spec instance for method chaining
     */
    default ConfigurableSystemOneSpec state(@NonNull String text) {
        return state(SystemOneContent.text(text));
    }

    /**
     * Convenience method to configure structured key-value map input state.
     *
     * @param objectMap the key-value map to evaluate; must not be null
     * @return this spec instance for method chaining
     */
    default ConfigurableSystemOneSpec state(@NonNull Map<String, Object> objectMap) {
        return state(SystemOneContent.object(objectMap));
    }

    /**
     * Convenience method to configure array or list input state.
     *
     * @param initialValues the array elements to evaluate
     * @return this spec instance for method chaining
     */
    default ConfigurableSystemOneSpec state(Object... initialValues) {
        return state(SystemOneContent.array(initialValues));
    }

    /**
     * Dynamically configures the questions schema to be evaluated by the SystemOne model.
     * The function is evaluated at execution time with the current {@link ExecutionContext}.
     *
     * @param questionsConfigure a function mapping {@link ExecutionContext} to the {@link SystemOneQuestions}; must not be null
     * @return this spec instance for method chaining
     */
    ConfigurableSystemOneSpec questions(@NonNull Function<ExecutionContext, SystemOneQuestions> questionsConfigure);

    /**
     * Sets static questions to be evaluated by the SystemOne model.
     *
     * @param questions the questions schema; must not be null
     * @return this spec instance for method chaining
     */
    default ConfigurableSystemOneSpec questions(@NonNull SystemOneQuestions questions) {
        return questions(contextView -> questions);
    }

    /**
     * Configures questions using a fluent builder consumer.
     *
     * @param questionsConsumer a consumer that configures a {@link SystemOneQuestions.Builder}; must not be null
     * @return this spec instance for method chaining
     */
    default ConfigurableSystemOneSpec questionsBuilder(@NonNull Consumer<SystemOneQuestions.Builder> questionsConsumer) {
        SystemOneQuestions.Builder builder = SystemOneQuestions.newBuilder();
        questionsConsumer.accept(builder);
        return questions(builder.build());
    }

    /**
     * Convenience method to configure a single question with its identifier.
     *
     * @param key      the unique question identifier; must not be null
     * @param question the question definition; must not be null
     * @return this spec instance for method chaining
     */
    default ConfigurableSystemOneSpec question(@NonNull String key, @NonNull SystemOneQuestion question) {
        return questions(SystemOneQuestions.of(key, question));
    }

    /**
     * Convenience method to configure questions from a map of question definitions.
     *
     * @param questionMap map of question identifiers to question definitions; must not be null
     * @return this spec instance for method chaining
     */
    default ConfigurableSystemOneSpec questions(@NonNull Map<String, SystemOneQuestion> questionMap) {
        return questions(SystemOneQuestions.of(questionMap));
    }

    /**
     * Registers a raw request customizer that has access to the current {@link ExecutionContext}
     * and the mutable {@link ObjectNode} of the request.
     *
     * @param rawRequestCustomizerConfigure a {@link BiConsumer} modifying the request body; must not be null
     * @return this spec instance for method chaining
     */
    ConfigurableSystemOneSpec rawRequestCustomizer(@NonNull BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure);

    /**
     * Registers a raw request customizer that modifies the mutable {@link ObjectNode} without context.
     *
     * @param rawRequestCustomizerConfigure a {@link Consumer} modifying the request body; must not be null
     * @return this spec instance for method chaining
     */
    default ConfigurableSystemOneSpec rawRequestCustomizer(@NonNull Consumer<ObjectNode> rawRequestCustomizerConfigure) {
        return rawRequestCustomizer((contextView, jsonNode) -> rawRequestCustomizerConfigure.accept(jsonNode));
    }
}
