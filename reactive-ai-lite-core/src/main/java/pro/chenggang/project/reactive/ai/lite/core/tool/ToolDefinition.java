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

/**
 * Defines the contract for a tool or function that can be called by an AI model.
 * A tool definition provides the model with the necessary metadata—such as its name,
 * a description of its purpose, and the schema for its input parameters—to understand
 * when and how to use the tool.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface ToolDefinition {

    /**
     * Gets the name of the tool.
     * <p>
     * The name should be unique among the set of tools provided to the model. It typically
     * consists of alphanumeric characters and underscores.
     *
     * @return The unique name of the tool.
     */
    String name();

    /**
     * Gets a human-readable description of what the tool does.
     * <p>
     * This description helps the AI model decide whether to call this tool based on the
     * user's request. It should clearly explain the tool's functionality and use cases.
     *
     * @return The description of the tool.
     */
    String description();

    /**
     * Gets the JSON schema that defines the input parameters for the tool.
     * <p>
     * The model uses this schema to generate the correct arguments when it decides to
     * call the tool. The schema should be a valid JSON Schema object definition.
     *
     * @return A string representation of the JSON schema for the tool's input.
     */
    String inputSchema();

    /**
     * Specifies whether the AI model should strictly adhere to the provided input schema
     * when generating arguments for the tool call.
     * <p>
     * When set to {@code true}, the model will make a best effort to generate a valid JSON
     * object that conforms to the schema. The default behavior (when returning {@code null})
     * is determined by the specific AI provider.
     *
     * @return {@code true} to enforce strict schema adherence, or {@code null} to use the
     * provider's default behavior.
     */
    default Boolean strict() {
        return null;
    }

    /**
     * Gets a unique identifier for the tool.
     * <p>
     * The identifier is actual name which submitted into the AI model.
     * The tool-calling response will include this identifier inside {@link LlmToolCallRequest}
     *
     * @return The unique identifier for the tool.
     */
    String identifier();
}
