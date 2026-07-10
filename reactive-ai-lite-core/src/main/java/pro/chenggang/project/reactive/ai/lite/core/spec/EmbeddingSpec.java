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

import pro.chenggang.project.reactive.ai.lite.core.execution.EmbeddingExecution;

/**
 * A specification interface for embedding operations, acting as the entry point to a fluent, reactive DSL for
 * generating text embeddings.
 * <p>
 * This interface abstracts the configuration and execution of embedding requests from the concrete implementation
 * details. By returning an {@link EmbeddingExecution} via the {@link #general()} method, it allows clients to
 * build and customize a default (general) embedding request in a consistent manner. Implementations may provide
 * different execution strategies, but this spec ensures a uniform starting point for embedding operations.
 * </p>
 * <p>
 * Typically used in combination with higher-level reactive components to incorporate AI‑powered embedding
 * generation into a broader data processing pipeline.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see EmbeddingExecution
 */
public interface EmbeddingSpec {

    /**
     * Returns an {@link EmbeddingExecution} instance that represents the general (default) execution strategy
     * for embedding requests.
     * <p>
     * The returned execution object serves as a builder for configuring parameters such as the input text,
     * model options, and result handling. Once configured, it can be used to trigger the embedding generation
     * in a reactive fashion (e.g., returning a {@code Mono<String>} or similar). This method is the primary
     * dispatch point for all basic embedding operations.
     * </p>
     *
     * @return a non‑null {@code EmbeddingExecution} for further request customization and execution
     */
    EmbeddingExecution general();
}