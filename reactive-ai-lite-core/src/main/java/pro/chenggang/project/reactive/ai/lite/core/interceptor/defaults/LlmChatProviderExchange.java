package pro.chenggang.project.reactive.ai.lite.core.interceptor.defaults;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmChatRequestData;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExchange;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
public class LlmChatProviderExchange implements LlmProviderExchange {

    private final LlmProviderInfo llmProviderInfo;
    private final LlmChatRequestData llmChatRequestData;
    private final Map<String, Object> attributes;
    private final Throwable error;

    private LlmChatProviderExchange(@NonNull LlmProviderInfo llmProviderInfo,
                                    @NonNull LlmChatRequestData llmChatRequestData,
                                    Map<String, Object> attributes,
                                    Throwable error) {
        this.llmProviderInfo = llmProviderInfo;
        this.llmChatRequestData = llmChatRequestData;
        this.attributes = Objects.isNull(attributes) ? new ConcurrentHashMap<>() : attributes;
        this.error = error;
    }

    public static LlmChatProviderExchange newExchange(@NonNull LlmProviderInfo llmProviderInfo,
                                                      @NonNull LlmChatRequestData llmChatRequestData) {
        return new LlmChatProviderExchange(llmProviderInfo, llmChatRequestData, null, null);
    }

    public static LlmChatProviderExchange newExchange(@NonNull LlmProviderInfo llmProviderInfo,
                                                      @NonNull LlmChatRequestData llmChatRequestData,
                                                      Map<String, Object> attributes) {
        return new LlmChatProviderExchange(llmProviderInfo, llmChatRequestData, attributes, null);
    }

    public static LlmChatProviderExchange newExchange(@NonNull LlmProviderInfo llmProviderInfo,
                                                      @NonNull LlmChatRequestData llmChatRequestData,
                                                      Map<String, Object> attributes,
                                                      Throwable error) {
        return new LlmChatProviderExchange(llmProviderInfo, llmChatRequestData, attributes, error);
    }

    @Override
    public LlmChatRequestData getLlmRequestData() {
        return this.llmChatRequestData;
    }

    @Override
    public LlmProviderInfo getLlmProviderInfo() {
        return this.llmProviderInfo;
    }

    @Override
    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }
}
