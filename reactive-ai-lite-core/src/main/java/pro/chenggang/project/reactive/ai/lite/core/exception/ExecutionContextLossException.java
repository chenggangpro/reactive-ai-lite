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
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;

import java.io.Serial;

/**
 * Exception indicating that an {@link ExecutionContext} required for the reactive AI pipeline
 * could not be found or was lost unexpectedly. This typically occurs when a chain of reactive
 * operations expects the context to be propagated but it is missing, leading to inability to
 * proceed with the execution. Extends {@link LlmClientException} to categorize it as a client-side error.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
public class ExecutionContextLossException extends LlmClientException {

    /**
     * Serial version UID for this serializable class.
     * Required for ensuring compatibility during deserialization.
     */
    @Serial
    private static final long serialVersionUID = -5817966634060158470L;

    /**
     * Constructs a new exception with a descriptive message indicating the expected execution context class.
     * This helps in debugging scenarios where the reactive pipeline loses its context,
     * pointing to the specific type expected.
     */
    public ExecutionContextLossException() {
        super("Missing running execution context of type " + ExecutionContext.class.getName());
    }
}