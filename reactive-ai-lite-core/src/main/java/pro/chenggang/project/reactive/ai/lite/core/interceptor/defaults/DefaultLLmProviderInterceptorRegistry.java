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

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmChatRequestData;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LLmProviderInterceptorRegistry;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionAfterInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionBeforeInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
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

import static pro.chenggang.project.reactive.ai.lite.core.interceptor.defaults.LlmChatProviderExchange.newExchange;
import static pro.chenggang.project.reactive.ai.lite.core.interceptor.logging.LlmProviderExecutionLoggingInterceptor.LLM_CLIENT_TYPE_ATTR_KEY;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
public class DefaultLLmProviderInterceptorRegistry implements LLmProviderInterceptorRegistry {

    private final Map<LlmClientType, LlmProviderExecutionBeforeInterceptorChain> beforeInterceptorChainMap;
    private final Map<LlmClientType, LlmProviderExecutionAfterInterceptorChain> afterInterceptorChainMap;

    public DefaultLLmProviderInterceptorRegistry(@NonNull List<LlmProviderExecutionBeforeInterceptor> beforeInterceptors, @NonNull List<LlmProviderExecutionAfterInterceptor> afterInterceptors) {
        this.beforeInterceptorChainMap = initBeforeInterceptorChainMap(beforeInterceptors);
        this.afterInterceptorChainMap = initAfterInterceptorChainMap(afterInterceptors);
    }

    @Override
    public <T> Mono<T> interceptMono(@NonNull LlmClientType clientType, @NonNull LlmProviderInfo llmProviderInfo, @NonNull LlmChatRequestData llmChatRequestData, @NonNull Mono<T> monoExecution) {
        return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView)
                .defaultIfEmpty(Context.empty())
                .flatMap(context -> {
                    return Mono.usingWhen(
                                    initializeAttributes(clientType),
                                    attributes -> {
                                        return Mono.justOrEmpty(this.beforeInterceptorChainMap.get(clientType))
                                                .flatMap(chain -> {
                                                    return Mono.defer(() -> {
                                                        LlmChatProviderExchange exchange = newExchange(llmProviderInfo, llmChatRequestData, attributes);
                                                        return chain.next(exchange);
                                                    });
                                                })
                                                .then(monoExecution);
                                    },
                                    attributes -> invokeOnComplete(clientType, llmProviderInfo, llmChatRequestData, attributes),
                                    (attributes, err) -> invokeOnError(clientType, llmProviderInfo, llmChatRequestData, attributes, err),
                                    attributes -> invokeOnCancel(clientType, llmProviderInfo, llmChatRequestData, attributes)
                            )
                            .contextWrite(context);
                })
        );
    }

    @Override
    public <T> Flux<T> interceptFlux(@NonNull LlmClientType clientType, @NonNull LlmProviderInfo llmProviderInfo, @NonNull LlmChatRequestData llmChatRequestData, @NonNull Flux<T> fluxExecution) {
        return Flux.deferContextual(contextView -> Mono.justOrEmpty(contextView)
                .defaultIfEmpty(Context.empty())
                .flatMapMany(context -> {
                    return Flux.usingWhen(
                                    initializeAttributes(clientType),
                                    attributes -> {
                                        return Mono.justOrEmpty(this.beforeInterceptorChainMap.get(clientType))
                                                .flatMap(chain -> {
                                                    return Mono.defer(() -> {
                                                        LlmChatProviderExchange exchange = newExchange(llmProviderInfo, llmChatRequestData, attributes);
                                                        return chain.next(exchange);
                                                    });
                                                })
                                                .thenMany(fluxExecution);
                                    },
                                    attributes -> invokeOnComplete(clientType, llmProviderInfo, llmChatRequestData, attributes),
                                    (attributes, err) -> invokeOnError(clientType, llmProviderInfo, llmChatRequestData, attributes, err),
                                    attributes -> invokeOnCancel(clientType, llmProviderInfo, llmChatRequestData, attributes)
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

    private Mono<Map<String, Object>> initializeAttributes(LlmClientType clientType) {
        return Mono.fromSupplier(() -> {
            Map<String, Object> attributes = new ConcurrentHashMap<>();
            attributes.put(LLM_CLIENT_TYPE_ATTR_KEY, clientType);
            return attributes;
        });
    }

    private Mono<Void> invokeOnComplete(LlmClientType clientType, LlmProviderInfo llmProviderInfo, LlmChatRequestData llmChatRequestData, Map<String, Object> attributes) {
        return Mono.justOrEmpty(this.afterInterceptorChainMap.get(clientType))
                .flatMap(afterChain -> {
                    LlmChatProviderExchange exchange = newExchange(llmProviderInfo, llmChatRequestData, attributes);
                    return afterChain.next(exchange);
                })
                .then(Mono.defer(() -> Mono.fromRunnable(attributes::clear)));
    }

    private Mono<Void> invokeOnError(LlmClientType clientType, LlmProviderInfo llmProviderInfo, LlmChatRequestData llmChatRequestData, Map<String, Object> attributes, Throwable err) {
        return Mono.justOrEmpty(this.afterInterceptorChainMap.get(clientType))
                .flatMap(afterChain -> {
                    LlmChatProviderExchange exchange = newExchange(llmProviderInfo, llmChatRequestData, attributes, err);
                    return afterChain.next(exchange);
                })
                .then(Mono.defer(() -> Mono.fromRunnable(attributes::clear)));
    }

    private Mono<Void> invokeOnCancel(LlmClientType clientType, LlmProviderInfo llmProviderInfo, LlmChatRequestData llmChatRequestData, Map<String, Object> attributes) {
        return Mono.justOrEmpty(this.afterInterceptorChainMap.get(clientType))
                .flatMap(afterChain -> {
                    LlmChatProviderExchange exchange = newExchange(llmProviderInfo, llmChatRequestData, attributes);
                    return afterChain.next(exchange);
                })
                .then(Mono.defer(() -> Mono.fromRunnable(attributes::clear)));
    }
}
