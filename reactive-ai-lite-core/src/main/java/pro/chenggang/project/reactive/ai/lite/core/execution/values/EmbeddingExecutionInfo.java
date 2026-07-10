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
package pro.chenggang.project.reactive.ai.lite.core.execution.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Immutable configuration container holding dynamic value providers for building LLM embedding requests.
 * <p>
 * Each function field is called during request construction with the current {@link ExecutionContext},
 * enabling runtime decisions about model selection, input texts, dimensions, and raw payload customisation.
 * <p>
 * The container is created via its builder and is typically assembled inside an {@code EmbeddingRequestConfigurer}
 * or directly in an agent workflow. Because all fields are final, the instance is thread‑safe and can be reused
 * across many execution cycles.
 * <p>
 * Implements {@link ExecutionInfo} to bridge the request‑configuration layer with higher‑level orchestration APIs.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ExecutionInfo
 * @see ExecutionContext
 */
@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EmbeddingExecutionInfo implements ExecutionInfo {

    /**
     * Flag indicating whether a default embedding profile should be used when no explicit profile is resolved.
     * <p>
     * When {@code true}, the downstream engine will ignore any profile selection logic and apply a pre‑configured
     * default. When {@code false}, the {@link #profilePicker} function <strong>must</strong> be able to determine
     * a profile name from the current execution context and the available profile set.
     */
    private final boolean defaultProfile;

    /**
     * Resolves the embedding profile name from the context and the set of all available profiles.
     * <p>
     * This {@link BiFunction} receives the current {@link ExecutionContext} and a {@link Set} of profile names
     * that the system knows about. It must return a non‑{@code null} profile name that will be used to load the
     * corresponding configuration (endpoint, API key, dimension limits, etc.). Implementations typically examine
     * context metadata, e.g., “tenant=” or “model-quality=” to decide which profile to pick.
     *
     * @see #defaultProfile
     */
    private final BiFunction<ExecutionContext, Set<String>, String> profilePicker;

    /**
     * Supplies the embedding model name for the current execution.
     * <p>
     * This function is called after the profile is resolved and is expected to return the identifier of the model
     * (e.g., {@code "text-embedding-ada-002"}). It can be as simple as a constant or may inspect the
     * {@link ExecutionContext} to support dynamic model selection based on parameters like requested quality or
     * cost limit.
     * <p>
     * The field is marked as {@code @NonNull} because a model name is mandatory for every embedding request.
     */
    @NonNull
    private final Function<ExecutionContext, String> modelNameConfigure;

    /**
     * Post‑processor that can modify the raw JSON request body before it is sent to the LLM provider.
     * <p>
     * This {@link BiConsumer} receives the current {@link ExecutionContext} and a mutable {@link ObjectNode}
     * representing the fully constructed embedding request payload. Implementations may append, remove, or
     * adjust any fields, e.g., inserting provider‑specific metadata or optimising the request for a particular
     * model version.
     */
    private final BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;

    /**
     * Dynamically provides the list of input texts to be embedded.
     * <p>
     * A {@link Function} that accepts the {@link ExecutionContext} and returns a {@link List} of
     * {@link String}s representing the texts to embed. This allows the input to be sourced from context
     * variables, aggregated from conversation history, or generated on the fly based on the current state.
     * The returned list may contain one or multiple texts; the downstream embedding provider must support
     * batching if more than one is supplied.
     */
    private final Function<ExecutionContext, List<String>> inputTextConfigure;

    /**
     * Dynamically determines the desired embedding dimension (vector size).
     * <p>
     * A {@link Function} that inspects the {@link ExecutionContext} and returns an {@link Integer} specifying
     * the number of dimensions for the output vectors. This enables the system to request lower‑dimensional
     * embeddings for speed‑sensitive or memory‑constrained scenarios, or higher‑dimensional vectors for better
     * semantic quality. The actual allowed range depends on the chosen model and provider.
     */
    private final Function<ExecutionContext, Integer> dimensionsConfigure;

}