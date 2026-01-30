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
