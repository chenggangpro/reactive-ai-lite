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
package pro.chenggang.project.reactive.ai.lite.core.message.chunk;

import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;

/**
 * Represents a single, discrete segment of data produced by an AI model during a reactive streaming response.
 * <p>
 * In reactive systems, AI model outputs are often delivered incrementally to reduce latency and memory pressure.
 * Each chunk may contain a portion of the final answer (e.g., a few tokens of generated text), a part of a tool-call
 * specification, or telemetry information such as token usage counts. This interface serves as the common root for all
 * such segment types, enabling uniform processing by pipeline operators (e.g., {@code Flux<StreamDataChunk>}). 
 * </p>
 * <p>
 * The chunk’s {@link #getDataType()} method categorises the payload so that downstream logic can safely cast to the
 * appropriate subtype and extract the relevant information. Implementations are expected to be immutable and
 * context-free, containing only the data generated in that particular step of the stream.
 * </p>
 * <p>
 * Typical implementations include:
 * <ul>
 *     <li>{@code TextChunk} – holds a fragment of natural language text</li>
 *     <li>{@code ToolRequestChunk} – contains a portion of a tool call’s arguments or metadata</li>
 *     <li>{@code UsageChunk} – reports token consumption at the end of a generation</li>
 * </ul>
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 * @see StreamDataType
 */
public interface StreamDataChunk {

    /**
     * Returns the semantic type of the data encapsulated in this chunk.
     * <p>
     * This classification allows dispatchers and aggregators to choose the appropriate handling strategy without
     * resorting to {@code instanceof} checks on every object. The returned value indicates whether the chunk contains
     * plain text, a tool-invocation request, usage statistics, or any other future extension.
     * </p>
     * <p>
     * Implementations must return a constant, well-known {@link StreamDataType} that unambiguously identifies the
     * payload’s nature. This ensures that streaming pipelines remain robust and maintainable even when new chunk
     * types are introduced.
     * </p>
     *
     * @return a non-null {@link StreamDataType} constant that categorises this chunk’s content
     */
    StreamDataType getDataType();
}