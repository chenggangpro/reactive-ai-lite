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
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/**
 * A default, immutable implementation of the {@link ToolDefinition} interface.
 * <p>
 * This class provides a concrete representation of a tool's metadata, including its name,
 * description, and input parameter schema. It is designed to be immutable and thread-safe,
 * built using Lombok's {@code @Builder} for easy instantiation and configured for JSON
 * serialization/deserialization.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultToolDefinition implements ToolDefinition {

    /**
     * The unique name of the tool.
     */
    private final String name;

    /**
     * A description of what the tool does and when it should be used.
     */
    private final String description;

    /**
     * The JSON schema defining the required input structure for the tool.
     */
    private final String inputSchema;

    /**
     * Flag indicating whether the model should strictly adhere to the input schema.
     */
    private final Boolean strict;

    /**
     * Gets the name of the tool.
     *
     * @return the unique name of the tool
     */
    @Override
    public String name() {
        return this.name;
    }

    /**
     * Gets a human-readable description of what the tool does.
     *
     * @return the description of the tool
     */
    @Override
    public String description() {
        return this.description;
    }

    /**
     * Gets the JSON schema that defines the input parameters for the tool.
     * <p>
     * If no schema was provided during construction, this returns an empty JSON object "{}".
     * </p>
     *
     * @return a string representation of the JSON schema, never null
     */
    @Override
    public String inputSchema() {
        return this.inputSchema == null ? "{}" : this.inputSchema;
    }

    /**
     * Specifies whether the AI model should strictly adhere to the provided input schema.
     *
     * @return {@code true} for strict adherence, {@code false} to disable, or {@code null} to use provider defaults
     */
    @Override
    public Boolean strict() {
        return this.strict;
    }

}
