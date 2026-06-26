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
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LLmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionAfterInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionBeforeInterceptor;
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
import java.util.stream.Collectors;

import static pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderRequestExchange.RAW_REQUEST_BODY_ATTRIBUTE_KEY;

/**
 * The default implementation of the {@link LLmProviderInterceptorRegistry}.
 * <p>
 * This class orchestrates the execution of interceptor chains around the core LLM request.
 * It initializes and sorts the "before" and "after" interceptor chains per client type.
 * During request execution, it guarantees that "before" interceptors are called prior to
 * the HTTP request, and "after" interceptors are called when the response arrives or an
 * error occurs. It uses Reactor's {@link Mono#usingWhen} to ensure that interceptor
 * resources (like shared attribute maps) are properly cleaned up.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public class DefaultLLmProviderInterceptorRegistry implements LLmProviderInterceptorRegistry {

    private final Map<LlmClientType, LlmProviderExecutionBeforeInterceptorChain> beforeInterceptorChainMap;
    private final Map<LlmClientType, LlmProviderExecutionAfterInterceptorChain> afterInterceptorChainMap;

    /**
     * Constructs a new {@link DefaultLLmProviderInterceptorRegistry}.
     *
     * @param beforeInterceptors the list of before interceptors
     * @param afterInterceptors  the list of after interceptors
     */
    public DefaultLLmProviderInterceptorRegistry(@NonNull List<LlmProviderExecutionBeforeInterceptor> beforeInterceptors, @NonNull List<LlmProviderExecutionAfterInterceptor> afterInterceptors) {
        this.beforeInterceptorChainMap = initBeforeInterceptorChainMap(beforeInterceptors);
        this.afterInterceptorChainMap = initAfterInterceptorChainMap(afterInterceptors);
    }

    /**
     * Applies interceptors around a general (non-streaming) request execution.
     *
     * @param interceptedDataInfo the request metadata and payload
     * @param generalExecution    the core execution logic
     * @return the intercepted execution Mono
     */
    @Override
    public Mono<ObjectNode> interceptGeneral(@NonNull InterceptedDataInfo interceptedDataInfo, @NonNull Mono<ObjectNode> generalExecution) {
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
                                                .doOnNext(rawResponseData -> resourceData.getT2().put(RAW_REQUEST_BODY_ATTRIBUTE_KEY, rawResponseData));
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
                                                                    .rawResponseBody((ObjectNode) resourceData.getT2().get(RAW_REQUEST_BODY_ATTRIBUTE_KEY))
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
                                                                    .rawResponseBody((ObjectNode) resourceData.getT2().get(RAW_REQUEST_BODY_ATTRIBUTE_KEY))
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
     *
     * @param interceptedDataInfo the request metadata and payload
     * @param streamExecution     the core execution logic returning a stream
     * @return the intercepted execution Flux
     */
    @Override
    public Flux<RawStreamResponse> interceptStream(@NonNull LLmProviderInterceptorRegistry.InterceptedDataInfo interceptedDataInfo, @NonNull Flux<RawStreamResponse> streamExecution) {
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
                                                                .cast(RawStreamResponse.class);
                                                        return Flux.just(interceptorExecution, rawStreamResponseFlux)
                                                                .flatMap(Flux::from);
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

    private Map<LlmClientType, LlmProviderExecutionBeforeInterceptorChain> initBeforeInterceptorChainMap(@NonNull List<LlmProviderExecutionBeforeInterceptor> beforeInterceptors) {
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

    private Map<LlmClientType, LlmProviderExecutionAfterInterceptorChain> initAfterInterceptorChainMap(@NonNull List<LlmProviderExecutionAfterInterceptor> afterInterceptors) {
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

    private Mono<Tuple2<InterceptedDataInfo, Map<String, Object>>> initializeResourceData(InterceptedDataInfo interceptedDataInfo) {
        return Mono.fromSupplier(() -> Tuples.of(interceptedDataInfo, new ConcurrentHashMap<>()));
    }
}
