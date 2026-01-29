package pro.chenggang.project.reactive.ai.lite.core.interceptor;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmChatRequestData;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LLmProviderInterceptorRegistry {

    <T> Mono<T> interceptMono(@NonNull LlmClientType clientType, @NonNull LlmProviderInfo llmProviderInfo, @NonNull LlmChatRequestData llmChatRequestData, @NonNull Mono<T> monoExecution);

    <T> Flux<T> interceptFlux(@NonNull LlmClientType clientType, @NonNull LlmProviderInfo llmProviderInfo, @NonNull LlmChatRequestData llmChatRequestData, @NonNull Flux<T> fluxExecution);
}
