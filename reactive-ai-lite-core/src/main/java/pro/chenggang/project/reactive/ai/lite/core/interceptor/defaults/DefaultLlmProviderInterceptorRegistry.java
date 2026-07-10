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
package pro.chenggang.project.reactive.ai.lite.core.interceptor.defaults;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionAfterInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionBeforeInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderRequestExchange;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.impl.DefaultLlmProviderGeneralResponseExchange;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.impl.DefaultLlmProviderRequestExchange;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.impl.DefaultLlmProviderStreamResponseExchange;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderRequestExchange.RAW_RESPONSE_BODY_ATTRIBUTE_KEY;

/**
 * The default implementation of the {@link LlmProviderInterceptorRegistry}.
 * <p>
 * This class orchestrates the execution of interceptor chains around the core LLM request/response flow.
 * It groups the provided {@link LlmProviderExecutionBeforeInterceptor} and {@link LlmProviderExecutionAfterInterceptor}
 * instances by their supported {@link LlmClientType} and sorts them by order. This pre‑computation avoids sorting
 * on every request and ensures consistent interceptor execution order.
 * </p>
 * <p>
 * For each intercepted execution (both general and streaming), the registry leverages Reactor’s
 * {@link Mono#usingWhen} (or {@link Flux#usingWhen}) to manage a per‑request attribute map. This map is
 * passed through the interceptor exchanges, allowing interceptors to share state for the duration of the request.
 * The resource is cleaned up after the final response, on error, or on cancellation.
 * </p>
 * <p>
 * The before chain is invoked first, and if it completes without error the actual LLM request is performed.
 * After the response is received (or an error occurs), the after chain is invoked. This symmetrical design
 * guarantees that cross‑cutting concerns (logging, metrics, headers) are applied consistently.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProviderInterceptorRegistry
 */
@Slf4j
public class DefaultLlmProviderInterceptorRegistry implements LlmProviderInterceptorRegistry {

    /**
     * Pre‑computed map of client type → sorted {@link LlmProviderExecutionBeforeInterceptor} chain.
     * The chain is built at construction time, grouping interceptors by supported clients and ordering
     * them by {@link LlmProviderExecutionBeforeInterceptor#getOrder()}. This avoids per‑request overhead.
     */
    private final Map<LlmClientType, LlmProviderExecutionBeforeInterceptorChain> beforeInterceptorChainMap;

    /**
     * Pre‑computed map of client type → sorted {@link LlmProviderExecutionAfterInterceptor} chain.
     * Similar to {@link #beforeInterceptorChainMap} but for “after” interceptors.
     */
    private final Map<LlmClientType, LlmProviderExecutionAfterInterceptorChain> afterInterceptorChainMap;

    /**
     * Constructs a new {@link DefaultLlmProviderInterceptorRegistry} and immediately initializes
     * the before‑ and after‑interceptor chains based on the provided interceptor lists.
     * <p>
     * Each interceptor declares the {@link LlmClientType}(s) it supports via
     * {@link LlmProviderExecutionBeforeInterceptor#supportedClient()} /
     * {@link LlmProviderExecutionAfterInterceptor#supportedClient()}. Interceptors are grouped by client type
     * and internally sorted by their natural ordering (defined by 
     * {@link LlmProviderExecutionBeforeInterceptor#getOrder()} / 
     * {@link LlmProviderExecutionAfterInterceptor#getOrder()}). The sorted list is then wrapped into a
     * chain object that can invoke interceptors sequentially.
     * </p>
     *
     * @param beforeInterceptors the list of before interceptors (must not be {@code null})
     * @param afterInterceptors  the list of after interceptors (must not be {@code null})
     */
    public DefaultLlmProviderInterceptorRegistry(
            @NonNull List<LlmProviderExecutionBeforeInterceptor> beforeInterceptors,
            @NonNull List<LlmProviderExecutionAfterInterceptor> afterInterceptors) {
        this.beforeInterceptorChainMap = initBeforeInterceptorChainMap(beforeInterceptors);
        this.afterInterceptorChainMap = initAfterInterceptorChainMap(afterInterceptors);
    }

    /**
     * Applies interceptors around a general (non‑streaming) request execution.
     * <p>
     * The sequence is:
     * <ol>
     *   <li>Retrieve the current {@link Context} (merged with any provided via {@code contextWrite})</li>
     *   <li>Use {@link Mono#usingWhen(Publisher, Function, Function, BiFunction, Function)} to create a resource tuple containing the original
     *   {@link InterceptedDataInfo} and a fresh {@link ConcurrentHashMap} for interceptor attributes.</li>
     *   <li>Look up the pre‑built before‑chain for the client type; if present, build a
     *   {@link DefaultLlmProviderRequestExchange} and execute the chain. Failure here prevents the
     *   actual request.</li>
     *   <li>If the before‑chain succeeds, execute the core {@code generalExecution} logic.</li>
     *   <li>When the execution emits a response, store the raw {@link ObjectNode} data in the attribute map
     *   under the key {@link LlmProviderRequestExchange#RAW_RESPONSE_BODY_ATTRIBUTE_KEY}
     *   so that the after‑chain can access it.</li>
     *   <li>On success, on error, or on cancellation, look up the after‑chain, build the appropriate
     *   {@link DefaultLlmProviderGeneralResponseExchange} and invoke it. The attribute map is cleared
     *   as part of the resource cleanup.</li>
     *   <li>The resulting {@link Mono} is written with the original context, ensuring the context flows
     *   through the entire chain.</li>
     * </ol>
     * </p>
     * <p>
     * This design guarantees that the before and after interceptors are always called exactly once,
     * regardless of success or failure, and that the attribute map is available to all interceptors
     * within the same logical request.
     * </p>
     *
     * @param interceptedDataInfo the immutable request metadata (client type, provider info, etc.)
     * @param generalExecution    the core LLM request logic that produces a {@link Mono} of
     *                            {@link ObjectNode} (the raw response body)
     * @return a {@link Mono} that emits the raw response body after all interceptors have been applied
     */
    @Override
    public Mono<ObjectNode> interceptGeneral(
            @NonNull InterceptedDataInfo interceptedDataInfo,
            @NonNull Mono<ObjectNode> generalExecution) {
        return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView)
                .defaultIfEmpty(Context.empty())
                .flatMap(context -> {
                    return Mono.usingWhen(
                                    initializeResourceData(interceptedDataInfo),
                                    resourceData -> {
                                        InterceptedDataInfo resourceDataInfo = resourceData.getT1();
                                        return Mono.justOrEmpty(this.beforeInterceptorChainMap.get(resourceDataInfo.getClientType()))
                                                .flatMap(chain -> {
                                                    return Mono.fromCallable(() -> {
                                                                return DefaultLlmProviderRequestExchange.builder()
                                                                        .clientType(resourceDataInfo.getClientType())
                                                                        .attributes(resourceData.getT2())
                                                                        .llmProviderInfo(resourceDataInfo.getLlmProviderInfo())
                                                                        .executionContext(resourceDataInfo.getExecutionContext())
                                                                        .rawRequestBody(resourceDataInfo.getRawRequestBody())
                                                                        .build();
                                                            })
                                                            .flatMap(chain::next);
                                                })
                                                .then(generalExecution)
                                                .doOnNext(rawResponseData -> resourceData.getT2().put(RAW_RESPONSE_BODY_ATTRIBUTE_KEY, rawResponseData));
                                    },
                                    resourceData -> {
                                        InterceptedDataInfo resourceDataInfo = resourceData.getT1();
                                        return Mono.justOrEmpty(this.afterInterceptorChainMap.get(resourceDataInfo.getClientType()))
                                                .flatMap(afterChain -> {
                                                    return Mono.fromCallable(() -> DefaultLlmProviderGeneralResponseExchange.builder()
                                                                    .clientType(resourceDataInfo.getClientType())
                                                                    .attributes(resourceData.getT2())
                                                                    .llmProviderInfo(resourceDataInfo.getLlmProviderInfo())
                                                                    .executionContext(resourceDataInfo.getExecutionContext())
                                                                    .rawResponseBody((ObjectNode) resourceData.getT2().get(RAW_RESPONSE_BODY_ATTRIBUTE_KEY))
                                                                    .build())
                                                            .flatMap(afterChain::next);
                                                })
                                                .then(Mono.defer(() -> Mono.fromRunnable(resourceData.getT2()::clear)));
                                    },
                                    (resourceData, err) -> {
                                        InterceptedDataInfo resourceDataInfo = resourceData.getT1();
                                        return Mono.justOrEmpty(this.afterInterceptorChainMap.get(resourceDataInfo.getClientType()))
                                                .flatMap(afterChain -> {
                                                    return Mono.fromCallable(() -> DefaultLlmProviderGeneralResponseExchange.builder()
                                                                    .clientType(resourceDataInfo.getClientType())
                                                                    .attributes(resourceData.getT2())
                                                                    .llmProviderInfo(resourceDataInfo.getLlmProviderInfo())
                                                                    .executionContext(resourceDataInfo.getExecutionContext())
                                                                    .rawResponseBody((ObjectNode) resourceData.getT2().get(RAW_RESPONSE_BODY_ATTRIBUTE_KEY))
                                                                    .error(err)
                                                                    .build())
                                                            .flatMap(afterChain::next);
                                                })
                                                .then(Mono.defer(() -> Mono.fromRunnable(resourceData.getT2()::clear)));
                                    },
                                    resourceData -> {
                                        InterceptedDataInfo resourceDataInfo = resourceData.getT1();
                                        return Mono.justOrEmpty(this.afterInterceptorChainMap.get(resourceDataInfo.getClientType()))
                                                .flatMap(afterChain -> {
                                                    return Mono.fromCallable(() -> DefaultLlmProviderGeneralResponseExchange.builder()
                                                                    .clientType(resourceDataInfo.getClientType())
                                                                    .attributes(resourceData.getT2())
                                                                    .llmProviderInfo(resourceDataInfo.getLlmProviderInfo())
                                                                    .executionContext(resourceDataInfo.getExecutionContext())
                                                                    .build())
                                                            .flatMap(afterChain::next);
                                                })
                                                .then(Mono.defer(() -> Mono.fromRunnable(resourceData.getT2()::clear)));
                                    }
                            )
                            .contextWrite(context);
                })
        );
    }

    /**
     * Applies interceptors around a streaming request execution.
     * <p>
     * The sequence is similar to {@link #interceptGeneral(InterceptedDataInfo, Mono)} but adapted for a
     * {@link Flux} of {@link RawStreamResponse}:
     * <ol>
     *   <li>Retrieve the Reactor {@link Context}.</li>
     *   <li>Use {@link Flux#usingWhen} to create the resource tuple.</li>
     *   <li>Execute the before‑chain. If it fails, the stream is not started.</li>
     *   <li>Once the before‑chain completes, the actual stream execution is initiated. The stream is
     *   published via {@link Flux#publish(Function)} so that both the downstream consumer and the after‑interceptor
     *   can observe the same stream without interfering.</li>
     *   <li>While the stream is active, the after‑chain is triggered as a separate {@link Mono} that
     *   creates a {@link DefaultLlmProviderStreamResponseExchange} containing the shared stream. This
     *   ensures the after‑interceptor can process the stream as it arrives. The after‑chain’s output
     *   (typically the same stream or an enriched one) is merged with the original stream, so the
     *   interceptor’s modifications are visible to the caller.</li>
     *   <li>On success, error, or cancellation, the after‑chain for that event is invoked (if applicable)
     *   and the attribute map is cleared.</li>
     * </ol>
     * </p>
     * <p>
     * The merging strategy uses {@link Flux#merge(Mono, Flux)} to combine the after‑interceptor’s
     * response with the original stream, giving the interceptor the opportunity to transform or augment
     * the stream. If the after‑interceptor emits an error, it is caught and logged to avoid interrupting
     * the main stream.
     * </p>
     *
     * @param interceptedDataInfo the immutable request metadata
     * @param streamExecution     the core LLM request logic that produces a {@link Flux} of
     *                            {@link RawStreamResponse}
     * @return a {@link Flux} that emits the raw stream responses after all interceptors have been applied
     */
    @Override
    public Flux<RawStreamResponse> interceptStream(
            @NonNull LlmProviderInterceptorRegistry.InterceptedDataInfo interceptedDataInfo,
            @NonNull Flux<RawStreamResponse> streamExecution) {
        return Flux.deferContextual(contextView -> Mono.justOrEmpty(contextView)
                .defaultIfEmpty(Context.empty())
                .flatMapMany(context -> {
                    return Flux.usingWhen(
                                    initializeResourceData(interceptedDataInfo),
                                    resourceData -> {
                                        InterceptedDataInfo resourceDataInfo = resourceData.getT1();
                                        return Mono.justOrEmpty(this.beforeInterceptorChainMap.get(resourceDataInfo.getClientType()))
                                                .flatMap(chain -> {
                                                    return Mono.fromCallable(() -> {
                                                                return DefaultLlmProviderRequestExchange.builder()
                                                                        .clientType(resourceDataInfo.getClientType())
                                                                        .attributes(resourceData.getT2())
                                                                        .llmProviderInfo(resourceDataInfo.getLlmProviderInfo())
                                                                        .executionContext(resourceDataInfo.getExecutionContext())
                                                                        .rawRequestBody(resourceDataInfo.getRawRequestBody())
                                                                        .build();
                                                            })
                                                            .flatMap(chain::next);
                                                })
                                                .thenMany(Flux.defer(() -> {
                                                    return streamExecution.publish(rawStreamResponseFlux -> {
                                                        Mono<RawStreamResponse> interceptorExecution = Mono.justOrEmpty(this.afterInterceptorChainMap.get(resourceDataInfo.getClientType()))
                                                                .flatMap(afterChain -> {
                                                                    return Mono.fromCallable(() -> DefaultLlmProviderStreamResponseExchange.builder()
                                                                                    .clientType(resourceDataInfo.getClientType())
                                                                                    .attributes(resourceData.getT2())
                                                                                    .llmProviderInfo(resourceDataInfo.getLlmProviderInfo())
                                                                                    .executionContext(resourceDataInfo.getExecutionContext())
                                                                                    .rawStreamResponse(rawStreamResponseFlux)
                                                                                    .build())
                                                                            .flatMap(afterChain::next);
                                                                })
                                                                .cast(RawStreamResponse.class)
                                                                .onErrorResume(err -> {
                                                                    log.error("Error occurred while intercepting stream response", err);
                                                                    return Mono.empty();
                                                                });
                                                        return Flux.merge(interceptorExecution, rawStreamResponseFlux);
                                                    });
                                                }));
                                    },
                                    resourceData -> Mono.defer(() -> Mono.fromRunnable(resourceData.getT2()::clear)),
                                    (resourceData, err) -> {
                                        InterceptedDataInfo resourceDataInfo = resourceData.getT1();
                                        return Mono.justOrEmpty(this.afterInterceptorChainMap.get(resourceDataInfo.getClientType()))
                                                .flatMap(afterChain -> {
                                                    return Mono.fromCallable(() -> DefaultLlmProviderStreamResponseExchange.builder()
                                                                    .clientType(resourceDataInfo.getClientType())
                                                                    .attributes(resourceData.getT2())
                                                                    .llmProviderInfo(resourceDataInfo.getLlmProviderInfo())
                                                                    .executionContext(resourceDataInfo.getExecutionContext())
                                                                    .error(err)
                                                                    .build())
                                                            .flatMap(afterChain::next);
                                                })
                                                .then(Mono.defer(() -> Mono.fromRunnable(resourceData.getT2()::clear)));
                                    },
                                    resourceData -> Mono.defer(() -> Mono.fromRunnable(resourceData.getT2()::clear))
                            )
                            .contextWrite(context);
                })
        );
    }

    /**
     * Builds the map of client type → before‑interceptor chain.
     * <p>
     * Each {@link LlmProviderExecutionBeforeInterceptor} can support multiple client types. This method
     * flattens the interceptor List into per‑client tuples, groups them by client type, sorts the
     * per‑type list by order, and finally wraps each sorted list into a
     * {@link LlmProviderExecutionBeforeInterceptorChain} that can invoke interceptors sequentially.
     * The resulting map is stored once and reused for every request.
     * </p>
     *
     * @param beforeInterceptors a non‑null list of before interceptors
     * @return a map from client type to its ordered chain of before interceptors
     */
    private Map<LlmClientType, LlmProviderExecutionBeforeInterceptorChain> initBeforeInterceptorChainMap(
            @NonNull List<LlmProviderExecutionBeforeInterceptor> beforeInterceptors) {
        return beforeInterceptors.stream()
                .flatMap(interceptor -> interceptor.supportedClient()
                        .stream()
                        .map(clientType -> Tuples.of(clientType, interceptor))
                )
                .collect(Collectors.groupingBy(Tuple2::getT1))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<LlmProviderExecutionBeforeInterceptor> beforeInterceptorList = entry.getValue()
                                    .stream()
                                    .map(Tuple2::getT2)
                                    .sorted(Comparator.comparing(LlmProviderExecutionBeforeInterceptor::getOrder))
                                    .toList();
                            return new LlmProviderExecutionBeforeInterceptorChain(beforeInterceptorList);
                        }
                ));
    }

    /**
     * Builds the map of client type → after‑interceptor chain, analogous to
     * {@link #initBeforeInterceptorChainMap(List)} but for
     * {@link LlmProviderExecutionAfterInterceptor} instances.
     *
     * @param afterInterceptors a non‑null list of after interceptors
     * @return a map from client type to its ordered chain of after interceptors
     */
    private Map<LlmClientType, LlmProviderExecutionAfterInterceptorChain> initAfterInterceptorChainMap(
            @NonNull List<LlmProviderExecutionAfterInterceptor> afterInterceptors) {
        return afterInterceptors.stream()
                .flatMap(interceptor -> interceptor.supportedClient()
                        .stream()
                        .map(clientType -> Tuples.of(clientType, interceptor))
                )
                .collect(Collectors.groupingBy(Tuple2::getT1))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<LlmProviderExecutionAfterInterceptor> afterInterceptorList = entry.getValue()
                                    .stream()
                                    .map(Tuple2::getT2)
                                    .sorted(Comparator.comparing(LlmProviderExecutionAfterInterceptor::getOrder))
                                    .toList();
                            return new LlmProviderExecutionAfterInterceptorChain(afterInterceptorList);
                        }
                ));
    }

    /**
     * Creates a resource tuple that holds the original intercepted data and a fresh,
     * thread‑safe attribute map for the current request.
     * <p>
     * The attribute map (a {@link ConcurrentHashMap}) is the shared state across before and after
     * interceptors for a single logical request. It is cleared when the request completes (success,
     * error, or cancellation) as part of the {@code usingWhen} resource cleanup.
     * </p>
     *
     * @param interceptedDataInfo the request metadata that will be passed to interceptors
     * @return a {@link Mono} that emits a tuple of the intercepted data and a modifiable attribute map
     */
    private Mono<Tuple2<InterceptedDataInfo, Map<String, Object>>> initializeResourceData(
            InterceptedDataInfo interceptedDataInfo) {
        return Mono.fromSupplier(() -> Tuples.of(interceptedDataInfo, new ConcurrentHashMap<>()));
    }
}