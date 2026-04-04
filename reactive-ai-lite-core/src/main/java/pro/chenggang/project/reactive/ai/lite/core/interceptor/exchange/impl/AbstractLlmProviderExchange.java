package pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.impl;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderExchange;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.util.Map;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
@SuperBuilder
public abstract class AbstractLlmProviderExchange implements LlmProviderExchange {

    @NonNull
    protected final Map<String, Object> attributes;

    @NonNull
    protected final LlmClientType clientType;

    @NonNull
    protected final LlmProviderInfo llmProviderInfo;

    @NonNull
    protected final ExecutionContextView executionContextView;

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public ExecutionContextView contextView() {
        return this.executionContextView;
    }

    @Override
    public LlmClientType clientType() {
        return this.clientType;
    }

    @Override
    public LlmProviderInfo llmProviderInfo() {
        return this.llmProviderInfo;
    }
}
