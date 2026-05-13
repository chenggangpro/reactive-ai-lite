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
 * Exception indicating an HTTP error response from an LLM service provider.
 * <p>
 * This exception is thrown when the underlying reactive WebClient receives an
 * HTTP error status code (e.g., 4xx Client Error or 5xx Server Error) from the
 * provider's API. It wraps the original Spring {@link RestClientResponseException}
 * to provide access to the raw HTTP status, headers, and body if needed.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public class ClientResponseErrorException extends LlmClientException {

    /**
     * Unique serial version identifier.
     */
    @Serial
    private static final long serialVersionUID = 3003050810499026975L;

    /**
     * Constructs a new ClientResponseErrorException wrapping the original HTTP error.
     *
     * @param cause the {@link RestClientResponseException} representing the HTTP error response
     */
    public ClientResponseErrorException(@NonNull RestClientResponseException cause) {
        super("Llm client response error: " + cause.getMessage(), cause);
    }
}
