package pro.chenggang.project.reactive.ai.lite.core.interceptor.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmRequestData;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.TraceId;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExchange;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionAfterInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionBeforeInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorChain;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
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

    public static final String LLM_CLIENT_TYPE_ATTR_KEY = LlmProviderExecutionLoggingInterceptor.class.getName() + ".llm-client-type";

    private final Set<LlmClientType> supportedClient = Set.of(LlmClientType.values());
    private final Supplier<Boolean> isEnableLogging;

    @Override
    public Mono<Void> interceptBefore(LlmProviderExchange exchange, LlmProviderInterceptorChain chain) {
        if (!isEnableLogging.get()) {
            return chain.next(exchange);
        }
        Map<String, Object> attributes = exchange.getAttributes();
        if (attributes.containsKey(EXECUTION_INSTANT_ATTR_KEY)) {
            log.debug("Execution instant attribute already exists in the exchange attributes, skipping interception.");
            return chain.next(exchange);
        }
        attributes.put(EXECUTION_INSTANT_ATTR_KEY, Instant.now());
        LlmProviderInfo llmProviderInfo = exchange.getLlmProviderInfo();
        LlmRequestData llmRequestData = exchange.getLlmRequestData();
        TraceId traceId = llmRequestData.getTraceId();
        log.info(" ==> [Llm Execution] ({}) Client type : {}", traceId, attributes.get(LLM_CLIENT_TYPE_ATTR_KEY));
        log.info(" ==> [Llm Execution] ({}) Request endpoint : {}", traceId, llmProviderInfo.baseUrl() + llmProviderInfo.endpoint());
        if (log.isDebugEnabled()) {
            llmRequestData.getSummary()
                    .forEach(summaryContent -> {
                        log.debug(" ==> [Llm Execution] ({}) {}", traceId, summaryContent);
                    });
        }
        return chain.next(exchange);
    }

    @Override
    public Mono<Void> interceptAfter(LlmProviderExchange exchange, LlmProviderInterceptorChain chain) {
        if (!isEnableLogging.get()) {
            return chain.next(exchange);
        }
        Instant executionInstant = exchange.getAttributeOrDefault(EXECUTION_INSTANT_ATTR_KEY, Instant.now());
        Duration costDuration = Duration.between(executionInstant, Instant.now());
        Optional<Throwable> optionalThrowable = exchange.getError();
        if (optionalThrowable.isPresent()) {
            log.info(" <== [Llm Execution] ({}) Execution cost : {} ms", exchange.getLlmRequestData().getTraceId(), costDuration.toMillis(), optionalThrowable.get());
        } else {
            log.info(" <== [Llm Execution] ({}) Execution cost : {} ms", exchange.getLlmRequestData().getTraceId(), costDuration.toMillis());
        }
        return chain.next(exchange);
    }

    @Override
    public Set<LlmClientType> supportedClient() {
        return supportedClient;
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
