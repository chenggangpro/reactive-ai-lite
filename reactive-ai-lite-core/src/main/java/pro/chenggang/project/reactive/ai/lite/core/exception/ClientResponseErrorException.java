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
 * Exception for LLM client response errors.
 * This exception is thrown when the underlying HTTP client receives an error response from the LLM service.
 * It wraps the original {@link RestClientResponseException}.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public class ClientResponseErrorException extends LlmClientException {

    @Serial
    private static final long serialVersionUID = 3003050810499026975L;

    /**
     * Constructs a new ClientResponseErrorException with the specified cause.
     *
     * @param cause the {@link RestClientResponseException} that is the cause of this exception
     */
    public ClientResponseErrorException(@NonNull RestClientResponseException cause) {
        super("Llm client response error: " + cause.getMessage(), cause);
    }
}
