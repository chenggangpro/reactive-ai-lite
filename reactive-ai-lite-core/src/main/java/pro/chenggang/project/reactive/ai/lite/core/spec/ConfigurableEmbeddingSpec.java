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

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An extension of {@link EmbeddingSpec} that provides a fluent API for configuring
 * embedding request parameters. This interface supports both static values and dynamic
 * resolution via {@link ExecutionContext}, enabling the same spec instance to adapt to
 * different contexts at request time.
 * <p>
 * The functional configuration methods accept lambdas that receive the current
 * {@link ExecutionContext}, allowing model name, input text, dimensions, and raw request
 * customizations to be derived from contextual data. Default convenience methods provide
 * shorthand for common static cases, promoting readability and reducing boilerplate.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public interface ConfigurableEmbeddingSpec extends EmbeddingSpec {

    /**
     * Dynamically configures the model name to be used for the embedding request.
     * The provided function is evaluated at execution time with the current
     * {@link ExecutionContext}, enabling model selection based on context
     * attributes (e.g., user preference, request origin, A/B testing flags).
     *
     * @param modelNameConfigure a function that accepts the {@link ExecutionContext}
     *                            and returns the model name to be used
     * @return this {@link ConfigurableEmbeddingSpec} instance for method chaining
     */
    ConfigurableEmbeddingSpec model(@NonNull Function<ExecutionContext, String> modelNameConfigure);

    /**
     * Sets a static model name for the embedding request.
     * This is a convenience method that delegates to {@link #model(Function)}
     * with a constant function ignoring the context.
     *
     * @param modelName the static model name; must not be null
     * @return this {@link ConfigurableEmbeddingSpec} instance for method chaining
     */
    default ConfigurableEmbeddingSpec model(@NonNull String modelName) {
        return model(contextView -> modelName);
    }

    /**
     * Dynamically configures the input text for the embedding request.
     * The function is called with the current {@link ExecutionContext} to
     * produce the list of strings to be embedded. This allows the input to
     * depend on contextual data such as conversation history, user input, or
     * other runtime variables.
     *
     * @param inputTextConfigure a function that receives the {@link ExecutionContext}
     *                            and returns a {@link List} of strings to embed
     * @return this {@link ConfigurableEmbeddingSpec} instance for method chaining
     */
    ConfigurableEmbeddingSpec inputText(@NonNull Function<ExecutionContext, List<String>> inputTextConfigure);

    /**
     * Convenience method to specify a single input string.
     * Wraps the given text into a singleton list and delegates to
     * {@link #inputText(Function)} with a constant function ignoring the context.
     *
     * @param inputText the input string to embed; must not be null
     * @return this {@link ConfigurableEmbeddingSpec} instance for method chaining
     */
    default ConfigurableEmbeddingSpec inputText(@NonNull String inputText) {
        return inputText(contextView -> List.of(inputText));
    }

    /**
     * Convenience method to specify a static list of input texts.
     * Delegates to {@link #inputText(Function)} with a constant function
     * that returns the given list, ignoring the context.
     *
     * @param inputText a list of strings to embed; must not be null
     * @return this {@link ConfigurableEmbeddingSpec} instance for method chaining
     */
    default ConfigurableEmbeddingSpec inputText(@NonNull List<String> inputText) {
        return inputText(contextView -> inputText);
    }

    /**
     * Dynamically configures the dimensions (embedding vector size) for the request.
     * The function is evaluated at request time with the {@link ExecutionContext},
     * allowing dimension selection to depend on context (e.g., model variant, quality
     * requirements, storage constraints).
     *
     * @param dimensionsConfigure a function that accepts the {@link ExecutionContext}
     *                             and returns the desired dimension count
     * @return this {@link ConfigurableEmbeddingSpec} instance for method chaining
     */
    ConfigurableEmbeddingSpec dimensions(@NonNull Function<ExecutionContext, Integer> dimensionsConfigure);

    /**
     * Sets static dimensions for the embedding request.
     * If the provided dimension is non-null, it delegates to
     * {@link #dimensions(Function)} with a constant function; if null,
     * the spec remains unchanged (allowing optional dimensions to be
     * omitted, relying on model defaults).
     *
     * @param dimensions the desired dimension count; may be null to indicate no override
     * @return this {@link ConfigurableEmbeddingSpec} instance for method chaining
     */
    default ConfigurableEmbeddingSpec dimensions(Integer dimensions) {
        if (Objects.nonNull(dimensions)) {
            return dimensions(contextView -> dimensions);
        }
        return this;
    }

    /**
     * Registers a raw request customizer that has access to the current
     * {@link ExecutionContext} and the JSON object representing the request.
     * This allows advanced modifications to the request payload that are not
     * covered by the standard spec methods, such as adding provider-specific
     * parameters or transforming the input based on dynamic context information.
     *
     * @param rawRequestCustomizerConfigure a {@link BiConsumer} that receives the
     *                                       {@link ExecutionContext} and the mutable
     *                                       {@link ObjectNode} of the request
     * @return this {@link ConfigurableEmbeddingSpec} instance for method chaining
     */
    ConfigurableEmbeddingSpec rawRequestCustomizer(@NonNull BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure);

    /**
     * Registers a raw request customizer that only needs the request node
     * (without context). This convenience method wraps the consumer into a
     * {@link BiConsumer} that ignores the context, delegating to
     * {@link #rawRequestCustomizer(BiConsumer)}.
     *
     * @param rawRequestCustomizerConfigure a {@link Consumer} that accepts the
     *                                       mutable {@link ObjectNode} of the request
     * @return this {@link ConfigurableEmbeddingSpec} instance for method chaining
     */
    default ConfigurableEmbeddingSpec rawRequestCustomizer(@NonNull Consumer<ObjectNode> rawRequestCustomizerConfigure) {
        return rawRequestCustomizer((contextView, jsonNode) -> rawRequestCustomizerConfigure.accept(jsonNode));
    }
}