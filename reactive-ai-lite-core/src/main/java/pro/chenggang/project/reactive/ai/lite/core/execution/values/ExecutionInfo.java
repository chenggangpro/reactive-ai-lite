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
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Immutable container capturing all dynamic configuration functions required to assemble
 * an LLM request at execution time.
 * <p>
 * Instead of hardcoding request parameters, the provider uses this interface's functions
 * to resolve values from the current {@link ExecutionContext} (typically sourced from the
 * Reactor context). This enables per‑request decisions such as profile selection, model
 * name assignment, or raw payload customization without altering the provider's core
 * logic.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface ExecutionInfo {

    /**
     * Communicates whether the provider should fall back to the configured default
     * profile instead of applying a dynamic choice.
     * <p>
     * When {@code true}, the execution skips the profile‑picker function entirely,
     * reducing overhead in scenarios where profile selection is static or predetermined
     * by the environment.
     * </p>
     *
     * @return {@code true} if the default profile is to be used, {@code false} if
     *         dynamic profile selection must occur
     */
    boolean isDefaultProfile();

    /**
     * Supplies a function that chooses a profile name from the set of available profiles,
     * driven by the runtime {@link ExecutionContext}.
     * <p>
     * This decouples profile routing from the provider, enabling logic such as:
     * <ul>
     *   <li>selecting a premium profile for authenticated users</li>
     *   <li>falling back to a free tier profile when credits are exhausted</li>
     *   <li>using a region‑specific endpoint based on user locality</li>
     * </ul>
     * The function receives the context and the complete set of known profile names,
     * and must return exactly one valid name.
     * </p>
     *
     * @return a bi‑function mapping {@code (ExecutionContext, Set<String>)} to the
     *         chosen profile name; not invoked when {@link #isDefaultProfile()} returns
     *         {@code true}
     */
    BiFunction<ExecutionContext, Set<String>, String> getProfilePicker();

    /**
     * Provides a function that determines the concrete model name to use for the
     * current request, based on the execution context.
     * <p>
     * Typical use‑cases include:
     * <ul>
     *   <li>selecting a lighter, faster model for low‑latency responses</li>
     *   <li>routing to a fine‑tuned variant when the user provides specific instructions</li>
     *   <li>applying a default model name when no explicit choice is made</li>
     * </ul>
     * The function is invoked by the provider during request assembly and its result is
     * directly embedded into the outgoing payload.
     * </p>
     *
     * @return a function that accepts the current {@link ExecutionContext} and returns
     *         the desired model name
     */
    Function<ExecutionContext, String> getModelNameConfigure();

    /**
     * Offers a consumer that can inject arbitrary modifications into the raw JSON
     * request payload just before it is dispatched.
     * <p>
     * This hook is intended for advanced scenarios that cannot be expressed through
     * standard configuration fields. For example:
     * <ul>
     *   <li>attaching custom provider‑specific parameters (e.g., {@code top_k})</li>
     *   <li>applying request‑level metadata (tenant id, trace id) from the context</li>
     *   <li>performing last‑minute validation or transformation of the payload</li>
     * </ul>
     * The consumer receives both the current context and a mutable {@link ObjectNode}
     * representing the JSON body, allowing it to read from the context and write into
     * the node.
     * </p>
     *
     * @return a bi‑consumer accepting {@code (ExecutionContext, ObjectNode)} to
     *         customize the raw request
     */
    BiConsumer<ExecutionContext, ObjectNode> getRawRequestCustomizerConfigure();

}