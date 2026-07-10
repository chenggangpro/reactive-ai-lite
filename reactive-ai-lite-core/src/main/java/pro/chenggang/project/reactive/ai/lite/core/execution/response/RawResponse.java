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

/**
 * A specialized {@link LlmResponse} that retains the original, unparsed JSON payload
 * from the LLM provider as a Jackson {@link ObjectNode}. This class is intended for
 * scenarios where developers require direct access to the provider’s native response
 * structure—for example, to explore experimental fields, extract non‑standard metadata,
 * implement custom parsing, or handle provider‑specific quirks without losing the
 * original data.
 * <p>
 * Because the raw response body can be large and varies between providers, this
 * wrapper decouples standard response processing from low‑level JSON manipulation.
 * Clients should use the generated builder via the {@link SuperBuilder} annotation
 * to construct instances safely, supplying both the common fields inherited from
 * {@link LlmResponse} and the {@code responseBody}.
 * </p>
 * <p>
 * <strong>Usage note:</strong> Storing the full JSON tree may increase memory
 * pressure in high‑throughput applications. Consider whether a streaming or
 * deferred‑parsing approach is more appropriate for your use case.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmResponse
 * @see com.fasterxml.jackson.databind.node.ObjectNode
 */
@Getter
@SuperBuilder
public class RawResponse extends LlmResponse {

    /**
     * The raw, unparsed JSON response body received from the LLM provider,
     * represented as a Jackson tree model ({@link ObjectNode}) to enable
     * dynamic and efficient navigation without the need for POJO binding.
     * <p>
     * This field captures the provider's original payload in its entirety,
     * preserving nested structures, arrays, and any additional fields that may
     * not be covered by the standard response model. It is intended for
     * advanced or diagnostic scenarios where direct inspection of the JSON
     * is required.
     * </p>
     */
    private final ObjectNode responseBody;

}