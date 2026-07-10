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
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.api.ClientRequest.ContextMerger;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Abstract base for the execution specification that captures all configuration needed to
 * instantiate an {@link ExecutionInfo} for an LLM request.
 * <p>
 * This class serves as the immutable blueprint constructed by the fluent builder (typically from
 * {@link pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableChatSpec}). It holds:
 * <ul>
 *   <li><b>Static choices</b> – whether to use default provider/profile, and the concrete
 *       {@link LlmClientType}.</li>
 *   <li><b>Dynamic selection logic</b> – functions that, at execution time, inspect the
 *       {@link ExecutionContext} and available providers/profiles to decide which provider,
 *       profile, and model to use.</li>
 *   <li><b>Context customization</b> – a {@link ContextMerger} that enriches the execution
 *       context with attributes and settings before the request is built.</li>
 *   <li><b>Request customization</b> – a consumer that can modify the raw JSON payload
 *       right before it is sent to the LLM provider.</li>
 * </ul>
 * </p>
 * <p>
 * When {@link #newExecutionInfo(ExecutionContext)} is invoked, these static and dynamic
 * configuration elements are combined with the given runtime context to produce a concrete
 * {@link ExecutionInfo} subtype. This design decouples the specification of “what to execute”
 * from the actual execution mechanics, enabling lazy resolution of providers and profiles,
 * context‑aware model selection, and full auditability of the execution parameters.
 * </p>
 *
 * @param <I> the specific type of {@link ExecutionInfo} this spec creates (e.g., a chat‑oriented
 *            or embedding‑oriented execution info)
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@SuperBuilder
public abstract class ExecutionSpec<I extends ExecutionInfo> {

    /**
     * The mandatory type of LLM client required for the request (e.g., {@code CHAT}, {@code EMBEDDING}).
     * <p>
     * This field defines the core capability expected from the selected provider. During execution
     * the system uses this to filter available providers to only those that support the given
     * client type, ensuring the request is routed to a compatible service.
     * </p>
     */
    @NonNull
    private final LlmClientType llmClientType;

    /**
     * Flag indicating whether the built‑in default provider should be used without any
     * dynamic selection.
     * <p>
     * When {@code true}, the execution logic bypasses the {@link #providerFilter} and
     * directly selects the default provider for the given {@link #llmClientType}. This
     * is typically set when the user does not specify a particular provider and trusts
     * the framework's default.
     * </p>
     */
    private final boolean defaultProvider;

    /**
     * Flag indicating whether the default profile of the selected provider should be used.
     * <p>
     * When {@code true}, the execution logic skips the {@link #profilePicker} function and
     * simply adopts the provider's default profile. This simplifies configuration for
     * users who do not need fine‑grained profile selection.
     * </p>
     */
    private final boolean defaultProfile;

    /**
     * Attributes inherited from a parent {@link ExecutionContext}, if any.
     * <p>
     * These key‑value pairs are merged into the execution context before any custom
     * configuration is applied. They allow coarse‑grained settings (like user‑level
     * defaults or session attributes) to propagate into each LLM request without
     * re‑specifying them every time.
     * </p>
     */
    private final Map<String, Object> parentAttributes;

    /**
     * A consumer that performs additional, user‑defined configuration on the
     * {@link ExecutionContext} right after the {@code parentAttributes} have been merged.
     * <p>
     * This functional interface receives a mutable copy of the context builder and can
     * set headers, parameters, or any other request‑scoped data. It is the primary hook
     * for contextual customization in the fluent API. If not set, no extra configuration
     * is applied.
     * </p>
     */
    private final ContextMerger contextConfigure;

    /**
     * A dynamic predicate that selects an LLM provider from the available set.
     * <p>
     * At execution time, the system retrieves all compatible providers (those supporting
     * {@link #llmClientType}) and feeds each one – along with the current
     * {@link ExecutionContext} – into this predicate. The first provider for which the
     * predicate returns {@code true} is chosen. This enables context‑aware routing,
     * such as selecting a provider based on user tier, region, or request complexity.
     * </p>
     * <p>
     * If this filter is {@code null} and {@link #defaultProvider} is {@code false}, an
     * exception is thrown because no selection logic has been provided.
     * </p>
     */
    private final BiPredicate<ExecutionContext, LlmProviderInfo> providerFilter;

    /**
     * A function that selects a profile name for the chosen provider.
     * <p>
     * Once a provider has been determined, the system calls this function with the current
     * {@link ExecutionContext} and the set of profile names that the provider exposes.
     * The returned string identifies the profile to use. This allows dynamic profile
     * switching based on context – for example, using a “fast” profile for simple queries
     * and a “detailed” profile for complex ones.
     * </p>
     * <p>
     * If this picker is {@code null} and {@link #defaultProfile} is {@code false}, an
     * exception will be raised during execution.
     * </p>
     */
    private final BiFunction<ExecutionContext, Set<String>, String> profilePicker;

    /**
     * A required function that determines the concrete model name for the request.
     * <p>
     * After the provider and profile have been resolved, this function is called with
     * the execution context to obtain the specific model identifier (e.g., {@code "gpt-4o"}).
     * This design allows model selection to be fully context‑driven, potentially varying
     * per request without any static configuration.
     * </p>
     */
    @NonNull
    private final Function<ExecutionContext, String> modelNameConfigure;

    /**
     * A consumer that can adjust the raw JSON request body right before it is sent
     * to the LLM provider.
     * <p>
     * Both the current {@link ExecutionContext} and the root {@link ObjectNode} of the
     * JSON payload are passed in, allowing arbitrary low‑level modifications (e.g.,
     * adding custom fields, overriding parameters that are not exposed through higher‑level
     * APIs). By default this is a no‑op consumer, meaning no customization is applied
     * unless explicitly configured.
     * </p>
     */
    @lombok.Builder.Default
    private final BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure = ((executionContext, jsonNodes) -> {});

    /**
     * Creates a concrete {@link ExecutionInfo} by binding this specification's static and
     * dynamic configuration to the provided runtime {@link ExecutionContext}.
     * <p>
     * Implementations of this method are expected to:
     * <ol>
     *   <li>Merge {@link #parentAttributes} into the context.</li>
     *   <li>Apply the {@link #contextConfigure} merger, if present.</li>
     *   <li>Use {@link #providerFilter}, {@link #profilePicker}, and
     *       {@link #modelNameConfigure} (respecting the {@code default} flags) to
     *       resolve the final provider, profile, and model.</li>
     *   <li>Construct a subtype of {@link ExecutionInfo} that encapsulates all resolved
     *       parameters along with the remainder of the specification (e.g., the
     *       {@link #rawRequestCustomizerConfigure} consumer).</li>
     * </ol>
     * The resulting {@link ExecutionInfo} is a fully‑realized, immutable descriptor ready
     * to be passed to the LLM client for invocation.
     * </p>
     *
     * @param executionContext the runtime execution context that carries request‑scoped
     *                         attributes and serves as input to all dynamic selection functions;
     *                         must not be {@code null}
     * @return a new {@link ExecutionInfo} instance configured according to this specification
     *         and the given context
     */
    public abstract I newExecutionInfo(@NonNull ExecutionContext executionContext);
}