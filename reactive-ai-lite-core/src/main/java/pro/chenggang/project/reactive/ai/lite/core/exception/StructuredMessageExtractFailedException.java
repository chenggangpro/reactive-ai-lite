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
 * Exception thrown when the framework fails to extract or parse the expected structured message content
 * from the raw JSON response returned by an LLM provider.
 * <p>
 * This exception commonly occurs in scenarios where:
 * <ul>
 *     <li>The provider's API response structure changes unexpectedly, yielding a JSON that lacks required fields (e.g. {@code choices[].message}).</li>
 *     <li>The content field within the response contains text that cannot be deserialized into the desired Java type (e.g. due to syntax errors, unexpected tokens, or a mismatch between the model's output and the expected schema).</li>
 *     <li>Jackson's {@code ObjectMapper} throws a {@link com.fasterxml.jackson.core.JsonProcessingException JsonProcessingException} while converting the raw content into a structured object.</li>
 * </ul>
 * The exception carries the full response body and the problematic content string, enabling comprehensive
 * debugging and logging of the exact state that caused the failure.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
public class StructuredMessageExtractFailedException extends LlmClientException {

    /**
     * Unique serial version identifier for serialization compatibility.
     */
    @Serial
    private static final long serialVersionUID = 4660326358199943232L;

    /**
     * The raw JSON response body (as an {@link ObjectNode}) received from the LLM provider.
     * This field is captured at the moment of failure to allow detailed inspection of the
     * provider's response that led to the exception. It is kept as a Jackson tree node
     * for convenient programmatic access and pretty-printing.
     */
    private final ObjectNode responseBody;

    /**
     * The raw textual content extracted from the response (typically from a field like
     * {@code choices[0].message.content}) that could not be deserialized into the
     * expected structured type. Storing this string helps pinpoint whether the failure
     * was due to malformed JSON within the content or an incompatible schema.
     */
    private final String content;

    /**
     * Constructs a new exception indicating a failure to deserialize the structured content.
     * <p>This variant is used when the deserialisation attempt itself fails, and there is no
     * underlying cause to wrap. The exception message includes the content string for
     * quick identification of the problematic output.</p>
     *
     * @param responseBody the full JSON response body (never {@code null})
     * @param content      the raw content string that couldn't be deserialized (may be {@code null})
     */
    public StructuredMessageExtractFailedException(@NonNull ObjectNode responseBody, String content) {
        super("Failed to deserialize structured content: " + content + " . Response body: \n" + responseBody.toPrettyString());
        this.responseBody = responseBody;
        this.content = content;
    }

    /**
     * Constructs a new exception with an underlying cause, typically when a
     * {@link com.fasterxml.jackson.core.JsonProcessingException JsonProcessingException} or
     * similar serialization error is thrown during deserialization.
     * <p>This constructor is preferred when a lower‑level library throws an exception that
     * helps explain the extraction failure. The exception message incorporates the
     * pretty‑printed response body for context.</p>
     *
     * @param responseBody the full JSON response body (never {@code null})
     * @param content      the raw content string that caused the failure (may be {@code null})
     * @param cause        the underlying exception that triggered this failure (never {@code null})
     */
    public StructuredMessageExtractFailedException(@NonNull ObjectNode responseBody, String content, @NonNull Throwable cause) {
        super("Failed to extract response message from response body. Response body: \n" + responseBody.toPrettyString(), cause);
        this.responseBody = responseBody;
        this.content = content;
    }
}