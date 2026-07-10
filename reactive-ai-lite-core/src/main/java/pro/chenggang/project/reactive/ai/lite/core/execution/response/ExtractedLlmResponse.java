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
package pro.chenggang.project.reactive.ai.lite.core.execution.response;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.entity.usage.Usage;

/**
 * An abstract container for standardized LLM responses that carries both
 * the high-level extracted metadata and the raw provider output.
 * <p>
 * This class extends {@link LlmResponse} to provide a common super type for
 * all parsed response types (e.g., chat, embedding, etc.). It captures two
 * universally important pieces of information:
 * <ul>
 *   <li>{@link #usage} – detailed token consumption statistics, essential for
 *       cost tracking, monitoring, and quota management.</li>
 *   <li>{@link #rawResponseBody} – the full, provider-specific JSON response,
 *       preserved for debugging, audit trails, or custom processing logic.</li>
 * </ul>
 * <p>
 * By keeping the raw body alongside extracted data, the framework enables
 * downstream consumers to access any additional provider-specific fields
 * without requiring model changes. The presence of usage information ensures
 * that billing and rate-limit decisions can be made uniformly across
 * different LLM integrations.
 * <p>
 * Subclasses must define their own specific extracted content (e.g., chat
 * messages, embeddings) while inheriting this foundational layer.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmResponse
 * @see Usage
 */
@Getter
@SuperBuilder
public abstract class ExtractedLlmResponse extends LlmResponse {

    /**
     * The detailed token usage statistics reported by the LLM for this
     * particular generation.
     * <p>
     * This field aggregates the number of prompt tokens, completion tokens,
     * and total tokens consumed. It is typically derived from the provider's
     * response and provides a standardized representation via
     * {@link Usage}. Accurate usage tracking enables cost estimation,
     * performance analysis, and automatic enforcement of rate limits.
     * <p>
     * If the underlying LLM provider does not supply usage information,
     * this field may be {@code null} or contain a usage object with zero
     * values, depending on the integration's behavior.
     */
    protected final Usage usage;

    /**
     * The raw, provider-specific JSON response body as received from the LLM
     * gateway.
     * <p>
     * Storing the unparsed JSON allows consumers to access low-level fields
     * that are not covered by the generic extracted response structure. This
     * is particularly useful for debugging issues, implementing custom
     * deserialization logic, or building adapters that rely on
     * provider‑unique properties.
     * <p>
     * The underlying JSON tree ({@link ObjectNode}) is mutable, but it is
     * recommended to treat it as read‑only to avoid inconsistencies with
     * the extracted data.
     */
    protected final ObjectNode rawResponseBody;

}