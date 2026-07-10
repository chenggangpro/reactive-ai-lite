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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.SuperBuilder;
import org.springframework.lang.Nullable;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderGeneralResponseExchange;

import java.util.Optional;

/**
 * Default implementation of {@link LlmProviderGeneralResponseExchange}, specializing
 * {@link AbstractLlmProviderExchange} for non‑streaming (general) LLM provider responses.
 * <p>
 * In the interceptor exchange model, a general response carries the complete JSON body
 * returned by the LLM provider after a synchronous or non‑streaming call. This class
 * embraces a “success‑or‑failure” contract: either the raw JSON payload is available,
 * or an exception describes why the exchange failed. Using {@link Optional} accessors
 * enforces explicit handling of both branches by downstream processors.
 * </p>
 * <p>
 * The {@link #rawResponseBody} is intentionally typed as a Jackson {@link ObjectNode}
 * to allow flexible, path‑based inspection without carrying a concrete model. Any error
 * encountered during the exchange (network, timeout, deserialization, provider‑specific
 * error response) is captured in {@link #error} so that interceptors can react to failures
 * uniformly.
 * </p>
 * <p>
 * Instances are created via the {@link SuperBuilder} from the parent class. Typical
 * construction:
 * <pre>
 *   DefaultLlmProviderGeneralResponseExchange.builder()
 *       .startTime(...)
 *       .connectionInfo(...)
 *       .rawResponseBody(responseJson)
 *       .build();
 * </pre>
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@SuperBuilder
public class DefaultLlmProviderGeneralResponseExchange extends AbstractLlmProviderExchange implements LlmProviderGeneralResponseExchange {

    /**
     * The complete JSON response body received from the LLM provider, expressed as a
     * Jackson {@link ObjectNode}. This field is {@code null} when the exchange did not
     * produce a valid response (i.e., when {@link #error} is present). The node preserves
     * the original tree structure, enabling interceptors to extract data points without
     * depending on provider‑specific POJOs.
     */
    @Nullable
    private final ObjectNode rawResponseBody;

    /**
     * Any exception thrown during the LLM provider exchange, {@code null} if the request
     * completed successfully. This can capture a wide range of failures: low‑level I/O
     * errors, HTTP error statuses, JSON parsing exceptions, or provider error messages
     * transformed into exceptions. The presence of this field indicates that the
     * {@link #rawResponseBody} is absent and the exchange should be treated as failed.
     */
    @Nullable
    private final Throwable error;

    /**
     * Returns the raw JSON response body if the exchange completed successfully;
     * otherwise {@link Optional#empty()}. This method enforces the invariant that a
     * successful response is never null but may be missing entirely when an error
     * occurred.
     *
     * @return an {@link Optional} wrapping the {@link ObjectNode}, or empty on failure
     */
    @Override
    public Optional<ObjectNode> rawResponseBody() {
        return Optional.ofNullable(this.rawResponseBody);
    }

    /**
     * Returns the exception that prevented a successful exchange, if one occurred.
     * When no error happened, the returned {@link Optional} is empty, meaning
     * {@link #rawResponseBody()} will contain the response.
     *
     * @return an {@link Optional} wrapping the {@link Throwable} error, or empty
     */
    @Override
    public Optional<Throwable> error() {
        return Optional.ofNullable(this.error);
    }
}