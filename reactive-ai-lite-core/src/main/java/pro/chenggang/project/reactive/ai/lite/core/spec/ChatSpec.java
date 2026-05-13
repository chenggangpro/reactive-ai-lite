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
package pro.chenggang.project.reactive.ai.lite.core.spec;

import pro.chenggang.project.reactive.ai.lite.core.execution.GeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.StreamExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.StructuredExecution;

/**
 * Defines the contract for chat operations, offering different execution strategies.
 * <p>
 * This interface serves as an entry point to specify how a chat request should be executed,
 * providing access to general, streaming, and structured response handlers.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface ChatSpec {

    /**
     * Returns the execution handler for a general chat request.
     * <p>
     * This is typically used for standard, non-streaming request/response interactions
     * where the entire response is received at once.
     * </p>
     *
     * @return a {@link GeneralExecution} instance for handling standard chat requests
     */
    GeneralExecution general();

    /**
     * Returns the execution handler for a streaming chat request.
     * <p>
     * This is used when the response from the AI model is expected to be a stream of data,
     * allowing for real-time processing of the response as it arrives.
     * </p>
     *
     * @return a {@link StreamExecution} instance for handling streaming chat requests
     */
    StreamExecution stream();

    /**
     * Returns the execution handler for a structured chat request.
     * <p>
     * This is used when the response is expected to be in a specific structured format (e.g., JSON)
     * that can be automatically mapped to a plain old Java object (POJO).
     * </p>
     *
     * @return a {@link StructuredExecution} instance for handling structured chat requests
     */
    StructuredExecution structured();
}
