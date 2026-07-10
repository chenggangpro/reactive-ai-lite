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
package pro.chenggang.project.reactive.ai.lite.core.spec.defaults;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import pro.chenggang.project.reactive.ai.lite.core.api.ClientRequest.ContextMerger;
import pro.chenggang.project.reactive.ai.lite.core.api.defaults.DefaultClientRequest;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Internal data object that holds configuration options used by the default request specification
 * {@link DefaultClientRequest}. It aggregates all provider‑, context‑, and profile‑selection related
 * settings in a single place, allowing the builder to construct a fully specified request with
 * defaults and custom behavior.
 * <p>
 * The instance is created exclusively via its Lombok builder, ensuring that all combinations of
 * optional and required properties are set in a clear, fluent manner. The class is purposely kept
 * package‑private to prevent external dependency on its structure; clients interact only through the
 * public {@code DefaultClientRequest} API.
 * <p>
 * Key responsibilities:
 * <ul>
 *   <li>Carry parent‑level attributes that are merged into the execution context.
 *   <li>Provide a strategy for merging additional context from the client request.</li>
 *   <li>Indicate whether the specification acts as a default provider or uses a default profile.
 *   <li>Define predicates and pickers that route a request to the appropriate provider and profile
 *       based on runtime conditions.</li>
 * </ul>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProviderConfigureInfo {

    /**
     * A map of additional attributes inherited from a parent specification or global configuration.
     * These attributes are merged into the {@link ExecutionContext} before a request is processed,
     * allowing cross‑cutting concerns (e.g., tracing IDs, environment tags) to be propagated
     * without modifying each individual request.
     */
    private Map<String, Object> parentAttributes;

    /**
     * A custom merger that allows the client request to contribute context information beyond
     * what the provider adds automatically. The merger is applied after default context population
     * and can be used to inject session‑specific, request‑specific, or caller‑managed state into
     * the execution environment. If {@code null}, no extra context is merged.
     */
    private ContextMerger contextConfigure;

    /**
     * Flag indicating whether this configuration represents a <em>default provider</em> selection.
     * When {@code true}, the provider filter is ignored and the default provider is chosen. This
     * simplifies common cases where the caller does not require dynamic provider routing.
     * <p>
     * Defaults to {@code false}.
     */
    @Builder.Default
    private boolean defaultProvider = false;

    /**
     * Flag indicating whether a default profile should be applied. When {@code true}, the
     * {@link #profilePicker} is bypassed and the system’s default profile (often defined by the
     * provider or a global setting) is used. This avoids the need to configure a profile picker
     * when only a single profile is relevant.
     * <p>
     * Defaults to {@code false}.
     */
    @Builder.Default
    private boolean defaultProfile = false;

    /**
     * A predicate evaluated against the current {@link ExecutionContext} and {@link LlmProviderInfo}
     * to dynamically decide whether a specific provider should be used. If it returns {@code true},
     * the provider is selected; otherwise, the next candidate is tested. This enables runtime
     * routing based on user, role, request parameters, or any other contextual data.
     * <p>
     * Set to {@code null} when provider selection is static (i.e., when {@link #defaultProvider}
     * is {@code true}) or when the first matching provider should be used without custom logic.
     */
    private BiPredicate<ExecutionContext, LlmProviderInfo> providerFilter;

    /**
     * A function that determines which profile identifier to use given the current
     * {@link ExecutionContext} and the set of available profile identifiers. It allows the
     * request to select a specific configuration profile (e.g., “production”, “staging”,
     * “fast‑inference”) based on runtime conditions. If {@code null} and {@link #defaultProfile}
     * is {@code false}, the default profile is used.
     */
    private BiFunction<ExecutionContext, Set<String>, String> profilePicker;
}