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
package pro.chenggang.project.reactive.ai.lite.core.execution.converter;

import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;

/**
 * A functional interface that defines a transformation from a raw, provider‑specific
 * streaming chunk into a structured, domain‑level response object.
 *
 * <p>
 * In streaming AI operations, language model providers typically send partial results
 * via <a href="https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events">Server‑Sent Events</a> (SSE).
 * Each event usually contains a JSON chunk that may represent a token, a status update,
 * or a fully assembled message fragment. The raw structure is often generic (e.g.,
 * {@code {"text":"...", "index":...}}) and needs to be mapped into an application‑specific
 * type that the caller can work with directly, such as a {@code ChatResponse},
 * {@code CompletionResult}, or a custom data transfer object.
 * </p>
 *
 * <p>
 * This interface serves as that mapping contract. It is designed to be passed to
 * {@link pro.chenggang.project.reactive.ai.lite.core.execution.StreamExecution#execute(RawStreamResponseConverter)},
 * where the streaming pipeline will apply {@link #convert(RawStreamResponse)} to every
 * {@link RawStreamResponse} emitted by the underlying reactive stream. By decoupling the
 * parsing logic from the execution framework, developers can:
 * </p>
 *
 * <ul>
 *   <li>Handle provider‑specific payload shapes without modifying the core library.</li>
 *   <li>Perform custom deserialization, error handling, or data enrichment per chunk.</li>
 *   <li>Adapt the same raw stream to multiple target types (e.g., different views of the
 *       same SSE data).</li>
 *   <li>Integrate with reactive back‑pressure by returning non‑null values that
 *       downstream operators can consume efficiently.</li>
 * </ul>
 *
 * <p>
 * Implementations should be stateless and thread‑safe, as a single converter instance is
 * often reused across many concurrent stream execution requests. The conversion method is
 * expected to be a pure function: given the same input, it should always produce the same
 * output.
 * </p>
 *
 * @param <STREAM_RESPONSE> the target type that each raw chunk will be converted into
 * @author Gang Cheng
 * @version 0.1.0
 * @see RawStreamResponse
 * @see pro.chenggang.project.reactive.ai.lite.core.execution.StreamExecution#execute(RawStreamResponseConverter)
 * @since 0.1.0
 */
@FunctionalInterface
public interface RawStreamResponseConverter<STREAM_RESPONSE> {

    /**
     * Transforms a raw stream chunk into a strongly‑typed response object.
     *
     * <p>
     * The {@link RawStreamResponse} typically wraps the original JSON body extracted from
     * the SSE event, along with metadata such as event type, sequence number, or raw bytes.
     * The implementation is responsible for parsing the JSON, validating its structure,
     * and mapping it to the desired {@code STREAM_RESPONSE} type. This method is called
     * once per chunk emitted by the upstream reactive stream, and the returned object
     * becomes the output of the stream.
     * </p>
     *
     * <p>
     * Implementors can leverage JSON libraries (e.g., Jackson, Gson) or any custom parsing
     * logic. It is recommended to handle malformed or incomplete chunks gracefully, for
     * example by returning a default/empty response or by throwing a checked exception
     * that is propagated as a stream error. Common strategies include:
     * </p>
     *
     * <ol>
     *   <li><b>Partial aggregation:</b> Accumulate text tokens into a buffer and emit
     *       aggregated responses only when a logical boundary is reached.</li>
     *   <li><b>Passthrough:</b> Convert the raw JSON into a lightweight DTO that preserves
     *       all fields for downstream use.</li>
     *   <li><b>Transformation with context:</b> Enrich each chunk with additional
     *       metadata, such as elapsed time or a correlation ID.</li>
     * </ol>
     *
     * @param rawStreamResponse the unparsed stream chunk; never {@code null} in normal
     *                          execution, but defensive implementations may guard against
     *                          unusual upstream behavior
     * @return the converted response of type {@code STREAM_RESPONSE}; must not be
     *         {@code null} to maintain downstream flow
     * @throws RuntimeException if conversion fails; the exception will signal an error
     *                          on the reactive stream and terminate the subscription
     */
    STREAM_RESPONSE convert(RawStreamResponse rawStreamResponse);

}