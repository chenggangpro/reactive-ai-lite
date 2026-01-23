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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents a request from an AI model to call a specific tool or function.
 * When the model determines that it needs to execute a function to fulfill a user's
 * request, it generates an instance of this class. The application is then responsible
 * for executing the specified function with the provided arguments.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LlmToolCallRequest {

    /**
     * A unique identifier for this tool call request. This ID must be included in the
     * corresponding {@link LlmToolCallResponse} to allow the model to correlate the
     * request with its result.
     */
    private final String id;

    /**
     * The name of the tool or function that the model wants to execute. This name
     * should match one of the tools defined in the {@link ToolDefinition} provided
     * in the initial request.
     */
    private final String name;

    /**
     * The type of the tool being called. For most modern AI models, this is
     * typically "function".
     */
    private final String type;

    /**
     * The raw, un-parsed string of arguments for the function call, as generated
     * by the model. This can be useful for debugging or for cases where custom
     * argument parsing is required.
     */
    private final String rawArgs;

    /**
     * The parsed arguments for the function call, represented as a Jackson
     * {@link JsonNode}. This provides a structured way to access the parameters
     * required by the tool.
     */
    private final JsonNode args;


}
