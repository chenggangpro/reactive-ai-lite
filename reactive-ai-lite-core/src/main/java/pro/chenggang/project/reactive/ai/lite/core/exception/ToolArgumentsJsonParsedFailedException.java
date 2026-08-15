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

import lombok.Getter;
import lombok.NonNull;

import java.io.Serial;

/**
 * Exception thrown when the LLM's tool call arguments cannot be parsed as valid JSON.
 *
 * <p>This typically occurs if the LLM produces malformed or incomplete JSON for function arguments
 * and it cannot be recovered by the JSON repair mechanism.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
public class ToolArgumentsJsonParsedFailedException extends LlmClientException {

    /**
     * Unique serial version identifier for serialization compatibility.
     */
    @Serial
    private static final long serialVersionUID = 7880395569503946861L;

    /**
     * The raw, unparsable JSON string arguments returned by the LLM.
     */
    private final String arguments;

    /**
     * Constructs a new ToolArgumentsJsonParsedFailedException with the specified raw arguments and cause.
     *
     * @param arguments the raw arguments string that failed to parse
     * @param cause     the underlying exception (e.g., Jackson JsonProcessingException or repair error)
     */
    public ToolArgumentsJsonParsedFailedException(@NonNull String arguments, @NonNull Throwable cause) {
        super("Failed to parse tool arguments json content: " + arguments, cause);
        this.arguments = arguments;
    }

}