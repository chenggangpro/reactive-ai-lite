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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.NonNull;

import java.io.Serial;

/**
 * Exception indicating the framework could not extract a structured message from a provider's raw JSON response.
 * This typically occurs when the API response deviates from the expected format (e.g., missing "choices"
 * or "message" nodes in OpenAI‑style responses, or an entirely unrecognized payload). The raw response body
 * is preserved to facilitate debugging and error reporting.
 * <p>
 * The exception offers access to the unparseable {@link ObjectNode} through the {@link #getResponseBody()} method,
 * allowing clients to inspect the problematic JSON structure or log it for later analysis.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
public class ResponseMessageExtractFailedException extends LlmClientException {

    /**
     * Serial version UID to maintain binary compatibility across different versions of this exception class.
     */
    @Serial
    private static final long serialVersionUID = -1794912786778873701L;

    /**
     * The raw JSON response object (as a Jackson <code>ObjectNode</code>) that could not be parsed
     * into the expected message structure. Storing the raw node enables detailed logging and debugging
     * of the unparseable content; the node may contain partial or malformed data.
     */
    private final ObjectNode responseBody;

    /**
     * Constructs an extraction failure exception with the unparseable JSON body.
     * The failure is typically because the expected fields for extracting the message content
     * are missing or the structure is unrecognised. The response body is included in the exception
     * message in pretty‑printed form for immediate inspection.
     *
     * @param responseBody the raw, unparseable JSON response body
     */
    public ResponseMessageExtractFailedException(@NonNull ObjectNode responseBody) {
        super("Failed to extract response message from response body. Response body: \n" + responseBody.toPrettyString());
        this.responseBody = responseBody;
    }

    /**
     * Constructs an extraction failure exception caused by a lower‑level parsing error (e.g., a JSON
     * processing exception). The underlying cause is preserved for chained exception analysis,
     * while the raw JSON body is provided for context.
     *
     * @param responseBody the raw, unparseable JSON response body
     * @param cause        the exception that triggered the failure (e.g., a JSON processing error)
     */
    public ResponseMessageExtractFailedException(@NonNull ObjectNode responseBody, @NonNull Throwable cause) {
        super("Failed to extract response message from response body. Response body: \n" + responseBody.toPrettyString(), cause);
        this.responseBody = responseBody;
    }
}