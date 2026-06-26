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
 * Exception thrown when the required {@link ExecutionContext} is missing or lost
 * during the reactive execution pipeline.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
public class ExecutionContextLossException extends LlmClientException {

    @Serial
    private static final long serialVersionUID = -5817966634060158470L;

    /**
     * Constructs a new {@link ExecutionContextLossException} with a default error message.
     */
    public ExecutionContextLossException() {
        super("Missing running execution context of type " + ExecutionContext.class.getName());
    }
}
