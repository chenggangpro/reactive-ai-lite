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
 * Defines the contract for chat operations, offering distinct execution strategies
 * for different interaction patterns.
 * <p>
 * This interface follows the specification pattern, acting as a configuration entry
 * point. It separates the concerns of how a chat request is dispatched and how the
 * response is consumed: synchronously, as a reactive stream, or as a typed structure.
 * Implementations provide the actual transport and processing logic while hiding the
 * underlying protocols.
 * </p>
 * <p>
 * Typical usage involves obtaining an instance of {@code ChatSpec} (usually from a
 * factory or configuration) and then selecting the appropriate execution method:
 * </p>
 * <ul>
 *   <li>{@link #general()} – for simple request-response cycles where the entire
 *       answer is needed before processing.</li>
 *   <li>{@link #stream()} – for real-time, incremental delivery of the response,
 *       enabling low-latency feedback for the user.</li>
 *   <li>{@link #structured()} – when the AI's textual answer must be mapped to a
 *       pre‑defined Java object (POJO), e.g., for extracting structured data.</li>
 * </ul>
 * Each method returns a dedicated execution handler that further configures and
 * initiates the actual interaction.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface ChatSpec {

    /**
     * Provides the execution handler for a standard (non-streaming) chat request.
     * <p>
     * In this mode the full response from the AI model is collected before it is
     * returned to the caller. This is the simplest usage pattern and is appropriate
     * when:
     * <ul>
     *   <li>The expected response size is small to moderate.</li>
     *   <li>Immediate processing of the complete answer is required.</li>
     *   <li>The application can afford to wait for the entire response.</li>
     * </ul>
     * </p>
     *
     * @return a {@link GeneralExecution} instance that can be further configured
     *         (for example, with the prompt and model parameters) and finally
     *         executed to obtain the full chat response
     */
    GeneralExecution general();

    /**
     * Provides the execution handler for a streaming chat request.
     * <p>
     * This mode enables incremental consumption of the AI model’s output, typically
     * using protocols such as Server‑Sent Events (SSE). It is the preferred choice
     * when:
     * <ul>
     *   <li>The response is large and a progressive user experience is desired.</li>
     *   <li>Low‑latency display of partial results is required (e.g., typing animation).</li>
     *   <li>The client wants to process chunks as soon as they arrive, potentially
     *       cancelling the stream early.</li>
     * </ul>
     * </p>
     *
     * @return a {@link StreamExecution} instance that allows configuration of stream‑specific
     *         options and subscribes to a reactive stream of response chunks
     */
    StreamExecution stream();

    /**
     * Provides the execution handler for a structured chat request that results in
     * a typed Java object.
     * <p>
     * This mode instructs the AI model to produce output conforming to a specified
     * schema (for example, a JSON Schema). The response is then automatically
     * deserialized into a plain old Java object (POJO) of the caller’s choice. It
     * is ideal for:
     * <ul>
     *   <li>Extracting structured information (e.g., lists, key‑value pairs) from a
     *       free‑form conversation.</li>
     *   <li>Integrating AI outputs directly into business logic without manual parsing.</li>
     *   <li>Enforcing a contract between the AI and the application, ensuring data
     *       integrity and type safety.</li>
     * </ul>
     * </p>
     * <p>
     * The returned {@link StructuredExecution} typically exposes a generic method
     * (such as {@code as(Class<T>)}) that accepts the target type and returns a
     * completion stage with the parsed object.
     * </p>
     *
     * @return a {@link StructuredExecution} instance for configuring and executing
     *         a request that yields a structured, typed response
     */
    StructuredExecution structured();
}