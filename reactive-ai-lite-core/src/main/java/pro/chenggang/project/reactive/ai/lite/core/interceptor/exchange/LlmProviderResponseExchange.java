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
 * Represents the exchange context for an inbound response received from an LLM provider,
 * forming the basis for both general and streaming response interception.
 * <p>
 * In the reactive AI lite framework, every interaction with an external LLM provider
 * is modeled through exchange objects that carry request/response metadata and payloads.
 * This interface extends {@link LlmProviderExchange} to specialize in the response phase,
 * allowing interceptors to inspect and process the result (or failure) of the provider invocation.
 * </p>
 * <p>
 * The presence of this contract enables a consistent interceptor API where the same
 * interception logic can be applied to both synchronous and reactive streaming response flows.
 * Interceptor implementations can rely on this type to obtain the final response data
 * and to react to any execution errors without coupling to the underlying transport details.
 * </p>
 * <p>
 * One key design feature is the exposure of a potential {@link Throwable} via {@link #error()}.
 * In a reactive pipeline, an error may occur before a successful response is produced,
 * or even during streaming. By surfacing the error as an {@code Optional}, interceptors can
 * centrally handle failures (e.g., logging, metrics, fallback decisions) without requiring
 * separate exception-handling logic at the call site.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProviderExchange
 */
public interface LlmProviderResponseExchange extends LlmProviderExchange {

    /**
     * Provides access to an error that may have been raised during the request execution.
     * <p>
     * In the interception chain, this method is crucial for differentiating between a
     * successful response and a failure scenario. When the LLM provider call succeeds,
     * the returned {@code Optional} is empty; when an exception is thrown at any stage
     * (e.g., network failure, timeout, invalid response), the {@code Optional} contains
     * the corresponding {@link Throwable}. This design avoids separate error interceptors
     * and enables a unified processing model where all response outcomes – success or failure –
     * are handled within the same interceptor.
     * </p>
     * <p>
     * Typical usage in an interceptor might involve:
     * <ul>
     *   <li>Logging the error with contextual information (request ID, timestamp).</li>
     *   <li>Incrementing failure metrics or triggering alerts.</li>
     *   <li>Performing custom fallback logic, such as retrying or returning a cached response.</li>
     *   <li>Modifying the behavior further down the interceptor chain based on the error presence.</li>
     * </ul>
     * </p>
     *
     * @return an {@link Optional} wrapping a {@link Throwable} if the execution failed,
     *         or an empty {@code Optional} if it succeeded without errors.
     */
    Optional<Throwable> error();
}