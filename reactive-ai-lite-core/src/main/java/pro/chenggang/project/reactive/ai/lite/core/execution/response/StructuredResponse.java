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

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.message.AssistantTextMessage;

/**
 * Represents the final, deserialized object resulting from a structured chat request.
 * <p>
 * This class inherits usage and metadata fields from {@link ExtractedLlmResponse}.
 * It provides both the raw parsed {@link AssistantTextMessage} (which contains the
 * original JSON string emitted by the model) as well as the strongly-typed Java
 * object that the string was deserialized into.
 * </p>
 *
 * @param <T> the type of the deserialized structured content
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
@SuperBuilder
public class StructuredResponse<T> extends ExtractedLlmResponse {

    /**
     * The parsed assistant message containing the raw JSON output string.
     */
    private final AssistantTextMessage assistantTextMessage;

    /**
     * The strongly-typed object automatically deserialized from the model's JSON output.
     */
    private final T structuredContent;

}
