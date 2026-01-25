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
package pro.chenggang.project.reactive.ai.lite.core.tool;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents the response from a tool call execution. This object is sent back to the
 * AI model to provide the result of a function that the model requested to be executed.
 * It contains the ID of the original tool call request and the content of the result.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
@ToString
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LlmToolCallResponse {

    /**
     * The unique identifier for the tool call. This ID is used to correlate the
     * response with the original tool call request made by the AI model.
     */
    private final String id;
    /**
     * The content of the tool's execution result. This is typically a string
     * representation of the output (e.g., JSON) that the AI model can then use
     * to formulate its final response to the user.
     */
    private final String content;

}
