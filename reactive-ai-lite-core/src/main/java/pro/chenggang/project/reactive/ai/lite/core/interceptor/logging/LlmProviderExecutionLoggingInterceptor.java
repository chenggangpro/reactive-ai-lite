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
package pro.chenggang.project.reactive.ai.lite.core.interceptor.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionAfterInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionBeforeInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderRequestInterceptorChain;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderResponseInterceptorChain;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderGeneralResponseExchange;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderRequestExchange;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderStreamResponseExchange;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonChunkMerger;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * The LLM provider execution logging interceptor.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class LlmProviderExecutionLoggingInterceptor implements LlmProviderExecutionBeforeInterceptor, LlmProviderExecutionAfterInterceptor {

    /**
     * The constant EXECUTION_INSTANT_ATTR_KEY for saving the execution instant in the RSocket exchange attributes.
     */
    public static final String EXECUTION_INSTANT_ATTR_KEY = LlmProviderExecutionLoggingInterceptor.class.getName() + ".execution-instant";

    private final Set<LlmClientType> supportedClient = Set.of(LlmClientType.values());
    private final Supplier<Boolean> isEnableLogging;

    @Override
    public Set<LlmClientType> supportedClient() {
        return supportedClient;
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public Mono<Void> interceptBefore(LlmProviderRequestExchange exchange, LlmProviderRequestInterceptorChain chain) {
        if (!isEnableLogging.get()) {
            return chain.next(exchange);
        }
        Map<String, Object> attributes = exchange.getAttributes();
        exchange.getAttributes()
                .compute(EXECUTION_INSTANT_ATTR_KEY, (k, v) -> {
                            if (Objects.nonNull(v)) {
                                log.warn("Execution instant attribute already exists in the exchange attributes, skipping interception.");
                                return v;
                            }
                            return Instant.now();
                        }
                );
        LlmProviderInfo llmProviderInfo = exchange.llmProviderInfo();
        log.info("  ==> [Llm Execution] Client type : {}", exchange.clientType());
        log.info("  ==> [Llm Execution] Request endpoint : {}", llmProviderInfo.baseUrl() + llmProviderInfo.endpoint());
        if (log.isDebugEnabled()) {
            log.debug("  ==> [Llm Execution] Raw request body: {}", exchange.rawRequestBody());
        }
        return chain.next(exchange);
    }

    @Override
    public Mono<Void> interceptAfter(LlmProviderGeneralResponseExchange exchange, LlmProviderResponseInterceptorChain chain) {
        if (!isEnableLogging.get()) {
            return chain.next(exchange);
        }
        if (log.isDebugEnabled()) {
            log.debug("  ==> [Llm Execution] Raw response body: {}", exchange.rawResponseBody());
        }
        Optional<Throwable> optionalThrowable = exchange.error();
        optionalThrowable.ifPresent(throwable -> log.error(" <== [Llm Execution] Execution error", throwable));
        Instant executionInstant = exchange.getAttributeOrDefault(EXECUTION_INSTANT_ATTR_KEY, Instant.now());
        Duration costDuration = Duration.between(executionInstant, Instant.now());
        log.info(" <== [Llm Execution] Execution cost : {} ms", costDuration.toMillis());
        return chain.next(exchange);
    }

    @Override
    public Mono<Void> interceptAfterEach(LlmProviderStreamResponseExchange exchange, LlmProviderResponseInterceptorChain chain) {
        if (!isEnableLogging.get()) {
            return chain.next(exchange);
        }
        return exchange.rawStreamResponse()
                .map(RawStreamResponse::getDataContent)
                .filter(Objects::nonNull)
                .reduce(OBJECT_MAPPER.createObjectNode(), JsonChunkMerger::merge)
                .doOnNext(mergedNode -> {
                    if (log.isDebugEnabled()) {
                        log.debug(" <== [Llm Execution] Merged stream response content: {}", mergedNode);
                    }
                })
                .then(chain.next(exchange));
    }
}
