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
package pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange;

import java.util.Optional;

/**
 * The base data exchange specifically for inbound responses from the LLM provider.
 * <p>
 * This interface extends {@link LlmProviderExchange} to provide a common foundation
 * for both general and streaming response interceptions. It includes the capability
 * to surface any errors or exceptions that may have occurred during the execution of the request.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmProviderResponseExchange extends LlmProviderExchange {

    /**
     * Retrieves an optional error that may have occurred during the request execution.
     * <p>
     * Interceptors can check this to perform error-specific logging or handling.
     * If empty, the execution succeeded.
     * </p>
     *
     * @return an {@link Optional} containing a {@link Throwable} if an error occurred, otherwise empty
     */
    Optional<Throwable> error();
}
