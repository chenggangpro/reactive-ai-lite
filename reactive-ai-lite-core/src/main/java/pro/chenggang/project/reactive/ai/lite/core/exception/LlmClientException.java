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

import java.io.Serial;

/**
 * Root exception for all errors arising from interactions with the LLM client.
 * <p>
 * Being an unchecked {@link RuntimeException}, it avoids forced exception handling in
 * reactive streams and functional pipelines, aligning with the non-blocking nature of
 * the reactive AI client. This class is intended to be extended by more specific
 * exceptions for configuration problems, network failures, response parsing errors,
 * or query processing issues.
 * </p>
 * <p>
 * All constructors delegate to the parent {@link RuntimeException}, enabling
 * standard message and cause chaining.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public class LlmClientException extends RuntimeException {

    /**
     * Controls serialization compatibility across different versions of this exception.
     * <p>
     * Keeping this field private with a fixed value ensures that serialized exception
     * instances remain deserializable even if fields change, as long as the class structure
     * remains compatible.
     * </p>
     */
    @Serial
    private static final long serialVersionUID = 6430987450515923414L;

    /**
     * Creates an exception with only a descriptive message.
     * <p>
     * Use this constructor when the error condition is fully described by the message
     * and no lower‑level cause is available.
     * </p>
     *
     * @param message the detail message explaining the error
     */
    public LlmClientException(String message) {
        super(message);
    }

    /**
     * Creates an exception with both a descriptive message and an underlying cause,
     * preserving the full stack trace and enabling root‑cause analysis.
     * <p>
     * This is the most common constructor used when catching another exception
     * (e.g., an {@link java.io.IOException}) during client operations.
     * </p>
     *
     * @param message the detail message explaining the error
     * @param cause   the underlying cause of the error
     */
    public LlmClientException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception that wraps a lower‑level cause without adding a custom message.
     * <p>
     * This constructor is convenient when the original exception already conveys
     * sufficient information and you simply want to propagate it as an LLM‑client
     * exception.
     * </p>
     *
     * @param cause the original throwable that triggered this exception
     */
    public LlmClientException(Throwable cause) {
        super(cause);
    }

}