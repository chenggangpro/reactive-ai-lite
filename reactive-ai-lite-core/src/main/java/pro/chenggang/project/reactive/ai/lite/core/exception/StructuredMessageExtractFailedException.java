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
 * Exception thrown when the framework fails to extract or parse the expected message structure
 * from the raw JSON response returned by an LLM provider.
 * <p>
 * This typically occurs if the provider's API changes unexpectedly, or if a malformed
 * JSON response is received that doesn't conform to the expected format (e.g., missing
 * "choices" or "message" fields). It provides access to the raw response body for debugging.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
public class StructuredMessageExtractFailedException extends LlmClientException {

    /**
     * Unique serial version identifier.
     */
    @Serial
    private static final long serialVersionUID = 4660326358199943232L;

    /**
     * The raw JSON response body that failed to be parsed.
     */
    private final ObjectNode responseBody;

    /**
     * The raw JSON content that failed to be pared as structured result
     */
    private final String content;

    /**
     * Constructs a new exception indicating a failure to extract the response message.
     *
     * @param responseBody the raw, unparseable JSON response body
     * @param content      the raw content
     */
    public StructuredMessageExtractFailedException(@NonNull ObjectNode responseBody, String content) {
        super("Failed to deserialize structured content: " + content + " . Response body: \n" + responseBody.toPrettyString());
        this.responseBody = responseBody;
        this.content = content;
    }

    /**
     * Constructs a new exception with an underlying cause for the extraction failure.
     *
     * @param responseBody the raw, unparseable JSON response body
     * @param content      the raw content
     * @param cause        the exception that triggered the failure (e.g., a JSON processing error)
     */
    public StructuredMessageExtractFailedException(@NonNull ObjectNode responseBody, String content, @NonNull Throwable cause) {
        super("Failed to extract response message from response body. Response body: \n" + responseBody.toPrettyString(), cause);
        this.responseBody = responseBody;
        this.content = content;
    }
}
