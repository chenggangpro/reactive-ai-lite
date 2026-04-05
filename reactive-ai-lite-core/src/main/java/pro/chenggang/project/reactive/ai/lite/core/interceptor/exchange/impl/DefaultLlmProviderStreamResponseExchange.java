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
