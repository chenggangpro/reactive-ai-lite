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
package pro.chenggang.project.reactive.ai.lite.core.interceptor;

import pro.chenggang.project.reactive.ai.lite.core.entity.AttributesAbility;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmRequestData;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.util.Optional;

/**
 * The Llm Provider Interceptor Exchange.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmProviderExchange extends AttributesAbility {

    /**
     * Gets the LLM request data.
     *
     * @return the llm request data
     */
    LlmRequestData getLlmRequestData();

    /**
     * Gets the LLM provider info.
     *
     * @return the llm provider info
     */
    LlmProviderInfo getLlmProviderInfo();

    /**
     * Gets error.
     *
     * @return the error
     */
    Optional<Throwable> getError();

}
