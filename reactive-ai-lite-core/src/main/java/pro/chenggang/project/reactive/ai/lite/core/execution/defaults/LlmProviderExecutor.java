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
package pro.chenggang.project.reactive.ai.lite.core.execution.defaults;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.exception.ExecutionContextLossException;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Central coordinator responsible for bridging the reactive execution pipeline with dynamic
 * {@link LlmProvider} selection and execution‑specific context preparation.
 * <p>
 * The executor is constructed via a <em>builder</em> pattern (Lombok {@code @Builder}) and
 * configured with a {@link ExecutionSpec} and an {@link LlmProviderRegistry}. It simplifies
 * the process of locating the right LLM provider, constructing a concrete {@link ExecutionInfo}
 * instance, and applying the caller’s execution logic—all while automatically resolving the
 * {@link ExecutionContext} from the reactive context.
 * </p>
 * <p>
 * The generic type parameter {@code I} represents the concrete subtype of {@code ExecutionInfo}
 * that the executing logic will receive. This design allows the executor to be reused across
 * different capabilities (e.g., chat, embedding, image generation) by supplying an
 * appropriate {@code ExecutionSpec}.
 * </p>
 * <h3>Execution flow overview</h3>
 * <ol>
 *   <li>The caller invokes {@link #execute(Class, BiFunction)} or {@link #executeFlux(Class, BiFunction)}
 *       with the expected provider type and the application-specific operation.</li>
 *   <li>The current {@link ExecutionContext} is extracted from the reactive context (via
 *       {@code Mono.deferContextual} / {@code Flux.deferContextual}). If missing, an
 *       {@link ExecutionContextLossException} is emitted immediately.</li>
 *   <li>An appropriate {@link LlmProvider} instance is resolved by calling
 *       {@link #loadLlmProvider(ExecutionContext, Class)}—this step may use the registry’s
 *       default provider or apply a custom filter predicate from the spec.</li>
 *   <li>A new {@link ExecutionInfo} is materialized from the spec using the resolved context.</li>
 *   <li>Finally, the provided {@link BiFunction} (the actual LLM call) is executed,
 *       producing a {@link Mono} or {@link Flux} result.</li>
 * </ol>
 *
 * @param <I> the type of {@link ExecutionInfo} created by this executor and passed to the execution function
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LlmProviderExecutor<I extends ExecutionInfo> {

    /**
     * Central registry that provides access to all registered {@link LlmProvider}s.
     * It is used to look up the best matching provider based on capability and additional
     * filtering criteria defined in the {@link ExecutionSpec}.
     */
    @NonNull
    private final LlmProviderRegistry llmProviderRegistry;

    /**
     * Immutable specification that defines how this executor should behave:
     * <ul>
     *   <li>Which {@link Capability} the provider must support.</li>
     *   <li>Whether a default provider should be used directly.</li>
     *   <li>An optional predicate to customize provider selection using the current
     *       {@link ExecutionContext} and provider metadata.</li>
     *   <li>How to construct a concrete {@link ExecutionInfo} from a context.</li>
     * </ul>
     */
    @Getter
    @NonNull
    private final ExecutionSpec<I> executionSpec;

    /**
     * Executes a non‑streaming LLM operation and returns the result as a {@link Mono}.
     * <p>
     * Internally this method performs the full orchestration:
     * <ol>
     *   <li>Retrieves the mandatory {@link ExecutionContext} from the reactive context.</li>
     *   <li>Delegates to {@link #loadLlmProvider(ExecutionContext, Class)} to obtain a
     *       provider matching the spec’s requirements.</li>
     *   <li>Uses {@link ExecutionSpec#newExecutionInfo(ExecutionContext)} to build the
     *       execution information instance.</li>
     *   <li>Applies the caller‑supplied {@code specifiedExecution} function, which
     *       typically performs the actual LLM request and maps the response.</li>
     * </ol>
     *
     * @param providerType       the expected type of the resolved provider
     * @param specifiedExecution the function that, given the provider and execution info,
     *                           returns a {@link Mono} with the desired result
     * @param <P>                concrete type of the resolved {@link LlmProvider}
     * @param <R>                type of the result emitted by the operation
     * @return a {@link Mono} that completes with the LLM operation’s result
     */
    public <P extends LlmProvider, R> Mono<R> execute(@NonNull Class<P> providerType, @NonNull BiFunction<P, I, Mono<R>> specifiedExecution) {
        return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(ExecutionContext.class))
                        .ofType(ExecutionContext.class)
                        .switchIfEmpty(Mono.error(new ExecutionContextLossException()))
                )
                .flatMap(executionContext -> this.loadLlmProvider(executionContext, providerType)
                        .flatMap(llmProvider -> {
                            I executionInfo = executionSpec.newExecutionInfo(executionContext);
                            return specifiedExecution.apply(llmProvider, executionInfo);
                        })
                );
    }

    /**
     * Executes a streaming LLM operation and returns the results as a {@link Flux}.
     * <p>
     * The orchestration logic is identical to that of {@link #execute(Class, BiFunction)},
     * but the provided {@code specifiedExecution} function must return a {@link Flux}
     * to support streaming responses (e.g., chat completions delivered token by token).
     *
     * @param providerType       the expected type of the resolved provider
     * @param specifiedExecution the function that, given the provider and execution info,
     *                           returns a {@link Flux} of results
     * @param <P>                concrete type of the resolved {@link LlmProvider}
     * @param <R>                type of each element emitted by the stream
     * @return a {@link Flux} that emits the LLM operation’s results over time
     */
    public <P extends LlmProvider, R> Flux<R> executeFlux(@NonNull Class<P> providerType, @NonNull BiFunction<P, I, Flux<R>> specifiedExecution) {
        return Flux.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(ExecutionContext.class))
                .ofType(ExecutionContext.class)
                .switchIfEmpty(Mono.error(new ExecutionContextLossException()))
                .flatMapMany(executionContext -> this.loadLlmProvider(executionContext, providerType)
                        .flatMapMany(llmProvider -> {
                            I executionInfo = executionSpec.newExecutionInfo(executionContext);
                            return specifiedExecution.apply(llmProvider, executionInfo);
                        })
                ));
    }

    /**
     * Resolves the most suitable {@link LlmProvider} for the current execution.
     * <p>
     * The selection logic is governed by the {@link ExecutionSpec}:
     * <ul>
     *   <li>If {@link ExecutionSpec#isDefaultProvider()} is {@code true}, the registry’s
     *       default provider for the spec’s capability is used directly.</li>
     *   <li>Otherwise, an additional filtering step is performed using the spec’s
     *       {@link ExecutionSpec#getProviderFilter()} predicate. This predicate receives
     *       the current {@link ExecutionContext} and the provider’s metadata,
     *       allowing dynamic selection based on runtime conditions (e.g., model preferences,
     *       region constraints). If no predicate is configured, the first available provider
     *       for the capability is returned.</li>
     * </ul>
     *
     * @param executionContext the current runtime context propagated through the reactive chain
     * @param providerClass    the expected subtype of {@link LlmProvider}
     * @param <P>              concrete provider type
     * @return a {@link Mono} emitting the resolved provider, or an error if no matching
     *         provider can be found
     */
    public <P extends LlmProvider> Mono<P> loadLlmProvider(@NonNull ExecutionContext executionContext, @NonNull Class<P> providerClass) {
        Capability capability = executionSpec.getLlmClientType().getCapability();
        if (executionSpec.isDefaultProvider()) {
            return llmProviderRegistry.getDefaultProvider(capability)
                    .cast(providerClass);
        }
        return llmProviderRegistry.getProvider(capability, providerClass, llmProviderInfo -> {
                    if (Objects.isNull(executionSpec.getProviderFilter())) {
                        return true;
                    }
                    return executionSpec.getProviderFilter().test(executionContext, llmProviderInfo);
                }
        );
    }
}