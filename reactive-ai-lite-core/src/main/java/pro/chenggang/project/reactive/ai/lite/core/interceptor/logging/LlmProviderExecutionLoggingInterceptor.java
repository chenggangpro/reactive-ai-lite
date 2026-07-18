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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
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
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * Logs details of each LLM provider call in both synchronous and streaming scenarios.
 * <p>
 * This interceptor implements both {@link LlmProviderExecutionBeforeInterceptor} and
 * {@link LlmProviderExecutionAfterInterceptor} to capture request metadata and response
 * outcomes. It records:
 * <ul>
 *   <li>The target endpoint and raw request body (on request).</li>
 *   <li>The raw response body for general (non-streaming) responses.</li>
 *   <li>The merged JSON chunks for streaming responses.</li>
 *   <li>Timing information: first-stream-chunk latency, total execution cost, and
 *   inter-chunk receiving duration.</li>
 *   <li>Any errors that occur during processing.</li>
 * </ul>
 * Logging is conditional, governed by a {@link Supplier}{@code <Boolean>} that can be
 * dynamically toggled at runtime (e.g., via configuration). The interceptor uses the
 * {@link #ALREADY_LOGGED_ATTR_KEY} attribute to prevent duplicate logging when an error
 * occurs in both the general and streaming interceptors.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProviderExecutionBeforeInterceptor
 * @see LlmProviderExecutionAfterInterceptor
 */
@Slf4j
@RequiredArgsConstructor
public class LlmProviderExecutionLoggingInterceptor implements LlmProviderExecutionBeforeInterceptor, LlmProviderExecutionAfterInterceptor {

    /**
     * Attribute key for storing the instant when execution began.
     * <p>
     * The value is an {@link Instant} placed into the {@link LlmProviderRequestExchange}
     * attributes during the {@code before} phase. It is later retrieved to calculate
     * the total execution duration and first-chunk latency.
     * </p>
     */
    public static final String EXECUTION_INSTANT_ATTR_KEY = LlmProviderExecutionLoggingInterceptor.class.getName() + ".execution-instant";

    /**
     * Attribute key used to mark that logging has already been performed for an exchange.
     * <p>
     * This prevents duplicate logging when the {@code error} signal is present in both
     * the general and streaming response interceptors. The value is a {@link Boolean}
     * stored as {@code true} once logging (including error logging) completes.
     * </p>
     */
    public static final String ALREADY_LOGGED_ATTR_KEY = LlmProviderExecutionLoggingInterceptor.class.getName() + ".already-logged";

    /**
     * The set of {@link LlmClientType}s for which this interceptor applies.
     * <p>
     * By default, it includes all client types. It can be customised via the constructor
     * if selective logging is desired. This field determines whether the interceptor is
     * invoked for a given provider call.
     * </p>
     */
    private final Set<LlmClientType> supportedClient = Set.of(LlmClientType.values());

    /**
     * A supplier providing the current logging enabled flag.
     * <p>
     * Evaluated at each interception point to decide whether to perform logging.
     * This allows dynamic toggling (e.g., based on a configuration property) without
     * static checks.
     * </p>
     */
    private final Supplier<Boolean> isLoggingEnabled;

    /**
     * Returns the client types that this interceptor supports.
     *
     * @return an unmodifiable set of {@link LlmClientType}; never {@code null}
     */
    @Override
    public Set<LlmClientType> supportedClient() {
        return supportedClient;
    }

    /**
     * Returns the order of this interceptor in the chain.
     * <p>
     * A value of {@link Integer#MIN_VALUE} ensures that the logging interceptor is
     * always the first to see the request and the last to see the response, giving
     * accurate end-to-end timing. Any other interceptor can rely on the logged
     * information being present.
     * </p>
     *
     * @return the order value, {@code Integer.MIN_VALUE}
     */
    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    /**
     * Records the start time and logs the request endpoint and body before passing
     * control down the chain.
     * <p>
     * The execution start {@link Instant} is stored in the exchange attributes under
     * {@link #EXECUTION_INSTANT_ATTR_KEY}. The endpoint is logged at INFO level;
     * the raw request body is logged at DEBUG level if enabled.
     * </p>
     *
     * @param exchange the incoming request exchange containing provider and body info
     * @param chain    the request interceptor chain
     * @return a {@link Mono} that completes when the next interceptor finishes
     */
    @Override
    public Mono<Void> interceptBefore(LlmProviderRequestExchange exchange, LlmProviderRequestInterceptorChain chain) {
        if (!isLoggingEnabled.get()) {
            return chain.next(exchange);
        }
        exchange.getAttributes()
                .compute(EXECUTION_INSTANT_ATTR_KEY, (k, v) -> {
                            if (Objects.nonNull(v)) {
                                log.warn("Execution instant attribute already exists in the exchange parsingAttributes, skipping interception.");
                                return v;
                            }
                            return Instant.now();
                        }
                );
        LlmProviderInfo llmProviderInfo = exchange.llmProviderInfo();
        log.info(" ==> ({}) Request endpoint : {}", exchange.clientType(), llmProviderInfo.baseUrl() + llmProviderInfo.endpoint());
        if (log.isDebugEnabled()) {
            log.debug(" ==> Raw request body: {}", exchange.rawRequestBody());
        }
        return chain.next(exchange);
    }

    /**
     * Logs the raw JSON response, any error, and total execution cost after a general
     * (non-streaming) provider call.
     * <p>
     * The raw response body is logged at DEBUG level. If an error is present in the
     * exchange, it is logged at ERROR level. The total duration from the stored
     * execution instant to now is logged at INFO level. To prevent duplicate logging
     * when an error is present, the {@link #ALREADY_LOGGED_ATTR_KEY} is set.
     * </p>
     *
     * @param exchange the response exchange containing the raw body and possible error
     * @param chain    the response interceptor chain
     * @return a {@link Mono} that completes when the next interceptor finishes
     */
    @Override
    public Mono<Void> interceptAfter(LlmProviderGeneralResponseExchange exchange, LlmProviderResponseInterceptorChain chain) {
        if (!isLoggingEnabled.get()) {
            return chain.next(exchange);
        }
        if (Boolean.TRUE.equals(exchange.getAttributes().get(ALREADY_LOGGED_ATTR_KEY))) {
            return chain.next(exchange);
        }
        exchange.getAttributes().put(ALREADY_LOGGED_ATTR_KEY, true);
        if (log.isDebugEnabled()) {
            Optional<Object> optionalRawResponseBody = exchange.rawResponseBody();
            if (optionalRawResponseBody.isPresent()) {
                Object rawResponseBody = optionalRawResponseBody.get();
                if (rawResponseBody instanceof DataBuffer dataBuffer) {
                    log.debug(" <== Raw response body buffer size: {}", dataBuffer.readableByteCount());
                } else if (rawResponseBody instanceof byte[] bytes) {
                    log.debug(" <== Raw response body bytes length: {}", bytes.length);
                } else {
                    log.debug(" <== Raw response body is: {}", rawResponseBody);
                }
            }
        }
        Optional<Throwable> optionalThrowable = exchange.error();
        optionalThrowable.ifPresent(throwable -> log.error(" <== Execution error", throwable));
        Instant executionInstant = exchange.getAttributeOrDefault(EXECUTION_INSTANT_ATTR_KEY, Instant.now());
        Duration costDuration = Duration.between(executionInstant, Instant.now());
        log.info(" <== Execution costs : {}", costDuration);
        return chain.next(exchange);
    }

    /**
     * Intercepts each streaming response chunk to log merged content, timings, and errors.
     * <p>
     * This method handles the asynchronous nature of streaming responses. It subscribes
     * to the raw stream, merges all JSON chunks into a single object (for debug logging),
     * measures the time to receive the first chunk, the inter-chunk duration, and the
     * total execution cost. If an error is present in the exchange before processing the
     * stream, it logs the error immediately and marks the exchange as already logged.
     * The logging task runs on a bounded elastic scheduler to avoid blocking the main
     * response pipeline. The method uses {@code Mono.whenDelayError(Mono, Mono...)} to
     * ensure the chain is still invoked and errors are not swallowed.
     * </p>
     *
     * @param exchange the streaming response exchange containing the raw stream and metadata
     * @param chain    the response interceptor chain
     * @return a {@link Mono} that completes when both logging and the next interceptor finish
     */
    @Override
    public Mono<Void> interceptAfterEach(LlmProviderStreamResponseExchange exchange, LlmProviderResponseInterceptorChain chain) {
        if (!isLoggingEnabled.get()) {
            return chain.next(exchange);
        }
        if (Boolean.TRUE.equals(exchange.getAttributes().get(ALREADY_LOGGED_ATTR_KEY))) {
            return chain.next(exchange);
        }
        Instant executionStartInstant = exchange.getAttributeOrDefault(EXECUTION_INSTANT_ATTR_KEY, Instant.now());
        if (exchange.error().isPresent()) {
            exchange.getAttributes().put(ALREADY_LOGGED_ATTR_KEY, true);
            exchange.error().ifPresent(throwable -> log.error(" <== Execution error", throwable));
            Duration costDuration = Duration.between(executionStartInstant, Instant.now());
            log.info(" <== Execution costs : {} ms", costDuration.toMillis());
            return chain.next(exchange);
        }
        Mono<Void> loggingTask = log.isDebugEnabled() ? this.toDebugLoggingTask(exchange, executionStartInstant) : this.toDefaultLoggingTask(exchange, executionStartInstant);
        return Mono.whenDelayError(loggingTask, chain.next(exchange));
    }

    private Mono<Void> toDebugLoggingTask(LlmProviderStreamResponseExchange exchange, Instant executionStartInstant) {
        Instant[] firstTrunkTime = new Instant[1];
        return exchange.rawStreamResponse()
                .publishOn(Schedulers.boundedElastic())
                .switchOnFirst((signal, flux) -> {
                    if (signal.hasValue()) {
                        firstTrunkTime[0] = Instant.now();
                        Duration costDuration = Duration.between(executionStartInstant, firstTrunkTime[0]);
                        log.debug(" <== Receiving first trunk of stream response content costs : {}", costDuration);
                        Object firstValue = signal.get();
                        if (firstValue instanceof DataBuffer) {
                            return flux.ofType(DataBuffer.class)
                                    .map(dataBuffer -> (long) dataBuffer.readableByteCount())
                                    .reduce(Long::sum)
                                    .map(totalCount -> Tuples.of(DataBuffer.class, totalCount));
                        } else if (firstValue instanceof byte[]) {
                            return flux.ofType(byte[].class)
                                    .map(bytes -> (long) bytes.length)
                                    .reduce(Long::sum)
                                    .map(totalLength -> Tuples.of(byte[].class, totalLength));
                        } else if (firstValue instanceof RawStreamResponse) {
                            return flux.ofType(RawStreamResponse.class)
                                    .mapNotNull(RawStreamResponse::getDataContent)
                                    .filter(Objects::nonNull)
                                    .reduce(OBJECT_MAPPER.createObjectNode(), JsonChunkMerger::merge)
                                    .map(mergedResponse -> Tuples.of(RawStreamResponse.class, mergedResponse));
                        } else if (firstValue instanceof ObjectNode) {
                            return flux.ofType(ObjectNode.class)
                                    .reduce(OBJECT_MAPPER.createObjectNode(), JsonChunkMerger::merge)
                                    .map(mergedResponse -> Tuples.of(ObjectNode.class, mergedResponse));
                        }
                        return flux.count()
                                .map(totalCount -> Tuples.of(Long.class, totalCount));
                    }
                    return flux.then();
                })
                .singleOrEmpty()
                .cast(Tuple2.class)
                .doOnNext(responseContent -> {
                    Instant endTime = Instant.now();
                    if (Objects.nonNull(firstTrunkTime[0])) {
                        Duration receivingCostDuration = Duration.between(firstTrunkTime[0], endTime);
                        log.debug(" <== Receiving trunks costs : {}", receivingCostDuration);
                    }
                    if (log.isDebugEnabled()) {
                        Object key = responseContent.getT1();
                        Object value = responseContent.getT2();
                        if (DataBuffer.class.equals(key)) {
                            log.debug(" <== Stream response buffer total size: {}", value);
                        } else if (byte[].class.equals(key)) {
                            log.debug(" <== Stream response bytes total length: {}", value);
                        } else if (Long.class.equals(key)) {
                            log.debug(" <== Stream response chunks total size: {}", value);
                        } else {
                            log.debug(" <== Merged stream response content: {}", value);
                        }
                    }
                })
                .doOnError(err -> log.error(" <== Stream logging error.", err))
                .doOnCancel(() -> log.warn(" <== Stream execution CANCELLED by client."))
                .doOnSuccess(signalType -> {
                    exchange.getAttributes().put(ALREADY_LOGGED_ATTR_KEY, true);
                    Instant endTime = Instant.now();
                    Duration totalCostDuration = Duration.between(executionStartInstant, endTime);
                    log.info(" <== Execution total costs : {}", totalCostDuration);
                })
                .then();
    }

    private Mono<Void> toDefaultLoggingTask(LlmProviderStreamResponseExchange exchange, Instant executionStartInstant) {
        return exchange.rawStreamResponse()
                .doOnCancel(() -> log.warn(" <== Stream execution CANCELLED by client."))
                .doOnComplete(() -> {
                    exchange.getAttributes().put(ALREADY_LOGGED_ATTR_KEY, true);
                    Instant endTime = Instant.now();
                    Duration totalCostDuration = Duration.between(executionStartInstant, endTime);
                    log.info(" <== Execution total costs : {}", totalCostDuration);
                })
                .then();
    }
}