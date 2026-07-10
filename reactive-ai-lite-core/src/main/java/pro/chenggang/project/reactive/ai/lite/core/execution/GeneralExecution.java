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
package pro.chenggang.project.reactive.ai.lite.core.execution;

import pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawResponseConverter;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import reactor.core.publisher.Mono;

/**
 * Represents the execution mode for a non-streaming, reactive interaction with a large language model (LLM).
 * <p>
 * In contrast to streaming execution, where tokens are emitted progressively, this mode performs
 * a single request and expects the complete response at once. It is suitable for use cases where
 * the latency of waiting for the full response is acceptable and the consumer requires the entire
 * generation for further processing, such as generating structured outputs, performing computations,
 * or executing tool calls synchronously.
 * </p>
 * <p>
 * Implementations of this interface encapsulate the lifecycle of a non-streaming LLM request,
 * including authentication, model selection, prompt assembly, and response parsing. The returned
 * {@link Mono} from the {@link #execute()} and {@link #executeRaw()} methods allows non-blocking
 * integration into Project Reactor pipelines, enabling composition and error handling without
 * blocking threads.
 * </p>
 * <p>
 * The interface provides two levels of abstraction: the high-level {@link #execute()} method that
 * returns a domain-friendly {@link GeneralResponse}, and the low-level {@link #executeRaw()} that
 * exposes the provider-specific raw response. The default {@link #execute(RawResponseConverter)}
 * method demonstrates a typical integration pattern where the raw response is transformed into a
 * custom type using a converter, promoting clean separation of concerns.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see GeneralResponse
 * @see RawResponse
 * @see RawResponseConverter
 */
public interface GeneralExecution {

    /**
     * Executes the non-streaming LLM request and returns a standardized, high-level response.
     * <p>
     * This method abstracts away provider-specific details (e.g., OpenAI, Azure, Ollama) and
     * transforms the raw API response into a unified {@link GeneralResponse} that includes
     * generated messages, reasoning content, token usage statistics, and tool call information.
     * It is the primary entry point for typical integrations where the caller needs to process
     * the LLM's output without dealing with provider-specific JSON structures.
     * </p>
     * <p>
     * The returned {@link Mono} emits exactly one item upon successful completion, or signals
     * an error if the request fails (e.g., network issues, authentication errors, rate limiting).
     * Because the operation is reactive, it is inherently non-blocking and can be composed
     * with other reactive sequences.
     * </p>
     *
     * @return a {@link Mono} that, when subscribed to, sends the request and emits the
     *         parsed {@link GeneralResponse} upon a successful HTTP response.
     */
    Mono<GeneralResponse> execute();

    /**
     * Executes the non-streaming LLM request and returns the raw, unprocessed provider response.
     * <p>
     * Unlike {@link #execute()}, this method does not perform any conversion or normalization.
     * It provides direct access to the raw JSON string returned by the underlying AI provider's API,
     * along with metadata about the response. This is useful when the application needs to
     * extract non-standard fields, handle provider-specific behaviors, or implement custom
     * parsing logic that goes beyond the common abstraction.
     * </p>
     * <p>
     * The returned {@link Mono} emits a single {@link RawResponse} containing the raw content
     * and contextual information (e.g., HTTP status, headers) of the provider's reply. As with
     * all reactive operations, error handling is conveyed via the Mono's error signal.
     * </p>
     *
     * @return a {@link Mono} emitting the {@link RawResponse} directly from the provider
     */
    Mono<RawResponse> executeRaw();

    /**
     * Executes the non-streaming LLM request and converts the raw response to a custom type
     * using the provided converter.
     * <p>
     * This convenience method combines execution and conversion in a single reactive step.
     * It first calls {@link #executeRaw()} to obtain the raw provider response, then immediately
     * applies the given {@link RawResponseConverter} to transform it into the desired target
     * type {@code R}. This pattern is valuable when the application needs to deserialize the
     * response into a strongly-typed object, perform additional validation, or adapt the output
     * to a domain-specific model.
     * </p>
     * <p>
     * The converter's {@link RawResponseConverter#convert(RawResponse)} method is invoked
     * synchronously on the subscriber's thread once the raw response arrives. If the converter
     * throws an exception, that exception will propagate as an error signal on the Mono.
     * </p>
     *
     * @param <R> the type into which the raw response will be converted
     * @param converter a non-null converter that knows how to transform a {@link RawResponse}
     *                  into an instance of {@code R}
     * @return a {@link Mono} that emits the converted result upon successful execution
     * @see RawResponseConverter
     */
    default <R> Mono<R> execute(RawResponseConverter<R> converter) {
        return executeRaw().map(converter::convert);
    }

}