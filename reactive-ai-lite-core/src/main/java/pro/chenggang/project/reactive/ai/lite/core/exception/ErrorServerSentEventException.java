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
import org.springframework.lang.Nullable;

import java.io.Serial;
import java.util.Objects;

/**
 * Exception thrown when a Server-Sent Event (SSE) stream delivers an error event.
 * <p>
 * In reactive AI or LLM clients, the server may push structured error events via SSE.
 * This exception captures the parsed error payload, providing direct access to the
 * error type, message, and raw JSON content. Clients can use this information for
 * recovery, logging, or custom error handling logic without parsing the event again.
 * It extends {@link LlmClientException} to fit into the existing LLM error hierarchy.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
public class ErrorServerSentEventException extends LlmClientException {

    /**
     * Serialization version identifier to ensure consistent deserialization
     * across different versions of this exception class.
     */
    @Serial
    private static final long serialVersionUID = 3975817711871345763L;

    /**
     * The error type identifier from the SSE error payload, such as "invalid_request_error".
     * This field is optional and can be {@code null} if not provided by the server.
     * It typically helps classify the error category for programmatic handling.
     */
    private final String type;

    /**
     * A human-readable error message from the SSE error event, describing what went wrong.
     * May be {@code null} if the error event omitted a message.
     */
    private final String message;

    /**
     * The complete JSON content of the error event, stored as an {@link ObjectNode}
     * for detailed inspection or logging. This preserves the original structure, allowing
     * access to any additional fields beyond type and message. Can be {@code null} if the
     * server sent an error event without a JSON body.
     */
    private final ObjectNode errorJsonContent;

    /**
     * Constructs a new {@code ErrorServerSentEventException} from the parsed SSE error details.
     * <p>
     * The parent exception message is composed to include the pretty-printed JSON content
     * (or the string "null" if no JSON is available) for immediate debugging visibility.
     * All parameters are nullable to accommodate incomplete or empty error events.
     *
     * @param type             the error type from the event, may be {@code null}
     * @param message          the error message from the event, may be {@code null}
     * @param errorJsonContent the raw JSON content of the error, may be {@code null}
     */
    public ErrorServerSentEventException(@Nullable String type, @Nullable String message, @Nullable ObjectNode errorJsonContent) {
        super("The SSE event is error. Event error content: " + (Objects.isNull(errorJsonContent) ? "null" : "\n" + errorJsonContent.toPrettyString()));
        this.type = type;
        this.message = message;
        this.errorJsonContent = errorJsonContent;
    }
}