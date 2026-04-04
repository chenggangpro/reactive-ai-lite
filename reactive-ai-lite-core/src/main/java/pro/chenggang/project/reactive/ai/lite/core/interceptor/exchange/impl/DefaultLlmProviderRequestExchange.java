package pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderRequestExchange;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
@SuperBuilder
public class DefaultLlmProviderRequestExchange extends AbstractLlmProviderExchange implements LlmProviderRequestExchange {

    @NonNull
    private final ObjectNode rawRequestBody;

    @Override
    public ObjectNode rawRequestBody() {
        return this.rawRequestBody;
    }

}
