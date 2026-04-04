
package pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.impl;

import lombok.experimental.SuperBuilder;
import org.springframework.lang.Nullable;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderStreamResponseExchange;
import reactor.core.publisher.Flux;

import java.util.Optional;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
@SuperBuilder
public class DefaultLlmProviderStreamResponseExchange extends AbstractLlmProviderExchange implements LlmProviderStreamResponseExchange {

    @Nullable
    private final Flux<RawStreamResponse> rawStreamResponse;
    @Nullable
    private final Throwable error;

    @Override
    public Flux<RawStreamResponse> rawStreamResponse() {
        return this.rawStreamResponse;
    }

    @Override
    public Optional<Throwable> error() {
        return Optional.ofNullable(error);
    }
}
