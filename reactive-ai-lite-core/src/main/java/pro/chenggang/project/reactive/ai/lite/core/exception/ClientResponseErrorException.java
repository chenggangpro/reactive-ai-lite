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
package pro.chenggang.project.reactive.ai.lite.core.exception;

import lombok.NonNull;
import org.springframework.web.client.RestClientResponseException;

import java.io.Serial;

/**
 * Signals that an HTTP call to an LLM provider returned an error status code.
 * <p>
 * This exception is thrown when the underlying HTTP client (e.g., Spring's {@code RestClient})
 * receives a non‑2xx response, such as a 4xx client error (invalid API key, rate limit) or
 * a 5xx server error (temporary outage). By extending {@link LlmClientException}, it
 * enriches the project’s exception model with HTTP‑specific details, allowing callers to
 * implement fine‑grained error handling logic (retries, fallback, etc.).
 * </p>
 * <p>
 * The original HTTP error is always available via {@link #getCause()} as a
 * {@link RestClientResponseException}, giving full access to the status code,
 * response headers, and body.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public class ClientResponseErrorException extends LlmClientException {

    /**
     * Serial version identifier used to ensure binary compatibility when serializing
     * instances of this exception across different JVM versions or after class structure
     * changes. This value should be explicitly incremented when incompatible modifications
     * (e.g., addition/removal of fields) are introduced.
     */
    @Serial
    private static final long serialVersionUID = 3003050810499026975L;

    /**
     * Constructs a new {@code ClientResponseErrorException} wrapping the underlying HTTP
     * error response.
     * <p>
     * The message is prefixed with {@code "Llm client response error:"} for immediate
     * recognition in logs. The provided {@link RestClientResponseException} is stored as
     * the root cause, preserving all HTTP metadata (status, headers, body) and enabling
     * more precise error analysis.
     * </p>
     *
     * @param cause the original Spring HTTP exception representing the error response;
     *              must not be {@code null}
     */
    public ClientResponseErrorException(@NonNull RestClientResponseException cause) {
        super("Llm client response error: " + cause.getMessage(), cause);
    }
}