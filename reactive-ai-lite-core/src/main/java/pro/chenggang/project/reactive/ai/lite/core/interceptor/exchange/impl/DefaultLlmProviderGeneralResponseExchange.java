package pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.SuperBuilder;
import org.springframework.lang.Nullable;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderGeneralResponseExchange;

import java.util.Optional;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
@SuperBuilder
public class DefaultLlmProviderGeneralResponseExchange extends AbstractLlmProviderExchange implements LlmProviderGeneralResponseExchange {

    @Nullable
    private final ObjectNode rawResponseBody;
    @Nullable
    private final Throwable error;

    @Override
    public Optional<ObjectNode> rawResponseBody() {
        return Optional.ofNullable(this.rawResponseBody);
    }

    @Override
    public Optional<Throwable> error() {
        return Optional.ofNullable(this.error);
    }
}
