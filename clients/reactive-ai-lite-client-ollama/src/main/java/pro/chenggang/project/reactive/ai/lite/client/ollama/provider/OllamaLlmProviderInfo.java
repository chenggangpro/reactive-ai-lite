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
package pro.chenggang.project.reactive.ai.lite.client.ollama.provider;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.AbstractLlmProviderInfo;

/**
 * Concrete implementation of {@link AbstractLlmProviderInfo} that encapsulates the identity
 * and configuration metadata for the <a href="https://ollama.com/">Ollama</a> language model provider.
 * <p>
 * This class is built using the {@link SuperBuilder} annotation, which inherits the builder pattern
 * from the superclass and generates a fluent API for constructing instances. It enforces that the
 * {@code name} field is always provided (marked with {@link NonNull}), ensuring each instance is
 * valid after construction. The class is designed to be used as part of the reactive AI lite client’s
 * provider resolution mechanism, where multiple LLM providers can coexist and be distinguished
 * by their unique name.
 * </p>
 * <p>
 * The typical use case is to create a builder and set the {@code name} to a custom identifier (often
 * {@link #DEFAULT_NAME}) before passing the provider info to the framework’s configuration.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see AbstractLlmProviderInfo
 */
@SuperBuilder
public class OllamaLlmProviderInfo extends AbstractLlmProviderInfo {

    /**
     * The default name constant for the Ollama provider.
     * <p>
     * This value is pre‑defined as {@code "Ollama"} to serve as a sensible default when no
     * explicit name is specified. It allows developers using the builder to omit a custom
     * name and still have a recognizable provider identifier that matches typical deployment
     * configurations.
     * </p>
     */
    public static final String DEFAULT_NAME = "Ollama";

    /**
     * The unique name that identifies this Ollama provider within the reactive AI lite ecosystem.
     * <p>
     * This field is mandatory and must not be {@code null} (enforced by {@link NonNull}).
     * It is set through the builder and then returned by the {@link #name()} method. The name
     * is used internally to route language model requests to the correct provider and to
     * distinguish between different Ollama instances or configurations.
     * </p>
     */
    @NonNull
    private final String name;

    /**
     * Retrieves the unique provider name.
     * <p>
     * This override satisfies the contract defined in {@link pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo#name()},
     * returning the value that was set during construction. The name is intended to be an invariant
     * property of the provider instance, and the framework relies on it to correctly select the
     * corresponding LLM back‑end.
     * </p>
     *
     * @return the provider name as configured, guaranteed not to be {@code null}
     */
    @Override
    public String name() {
        return this.name;
    }

}