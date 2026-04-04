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
 * Represents the raw, unparsed JSON response body returned by the LLM provider.
 * <p>
 * This class extends {@link LlmResponse} to include the raw provider-specific
 * JSON payload as a Jackson {@link ObjectNode}. It is primarily used when
 * developers need to bypass standard parsing to access experimental features,
 * non-standard metadata, or handle specific provider quirks directly.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
@SuperBuilder
public class RawResponse extends LlmResponse {

    /**
     * The raw, unparsed JSON body received from the provider.
     */
    private final ObjectNode responseBody;

}
