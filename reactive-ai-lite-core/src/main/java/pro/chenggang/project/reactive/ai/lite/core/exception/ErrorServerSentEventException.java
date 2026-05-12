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
 * Exception thrown when a Server-Sent Event (SSE) contains an error payload.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
public class ErrorServerSentEventException extends LlmClientException {

    @Serial
    private static final long serialVersionUID = 3975817711871345763L;

    /**
     * The error type content
     */
    private final String type;

    /**
     * The error message content
     */
    private final String message;

    /**
     * The raw JSON content of the error event.
     */
    private final ObjectNode errorJsonContent;

    /**
     * Constructs a new ErrorServerSentEventException with the specified error content.
     *
     * @param errorJsonContent the JSON node containing error details
     */
    public ErrorServerSentEventException(@Nullable String type, @Nullable String message, @Nullable ObjectNode errorJsonContent) {
        super("The SSE event is error. Event error content: " + (Objects.isNull(errorJsonContent) ? "null" : "\n" + errorJsonContent.toPrettyString()));
        this.type = type;
        this.message = message;
        this.errorJsonContent = errorJsonContent;
    }
}
