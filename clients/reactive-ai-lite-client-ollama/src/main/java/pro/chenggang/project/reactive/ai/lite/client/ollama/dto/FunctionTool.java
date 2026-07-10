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
package pro.chenggang.project.reactive.ai.lite.client.ollama.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * Represents a tool that can be invoked as a function in an Ollama interaction.
 * This DTO maps to the tool definition expected by the Ollama API for function calling.
 * It holds the tool type (always {@code function}) and the detailed function definition,
 * including its name, description, parameters, and optional strict mode flag.
 * <p>
 *     Instances are created via a builder and are immutable; all fields are required.
 *     Jackson serializes only non-null values.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FunctionTool {

    /**
     * The type of the tool, fixed to {@link Type#FUNCTION} to indicate
     * that this tool executes a function. This field is always set to
     * {@code "function"} in the JSON output.
     */
    @JsonProperty("type")
    @Builder.Default
    private final Type type = Type.FUNCTION;

    /**
     * The definition of the function, including its name, description,
     * accepted parameters schema, and strict mode settings.
     */
    @JsonProperty("function")
    private final Function function;

    /**
     * Inner class holding the details of the callable function.
     * This mirrors the {@code function} object in the Ollama tool JSON schema.
     * It contains the function's description, name, parameter specification,
     * and a flag to enforce strict adherence to the parameters schema.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Function {

        /**
         * A human-readable description of what the function does.
         * This helps the LLM to decide when to call the tool.
         */
        @JsonProperty("description")
        private final String description;

        /**
         * The unique name of the function. This will be used by the LLM
         * to specify which tool to invoke.
         */
        @JsonProperty("name")
        private final String name;

        /**
         * A JSON Schema object describing the parameters the function accepts.
         * The map key-value pairs represent the schema structure, typically with
         * "type", "properties", and "required" fields.
         */
        @JsonProperty("parameters")
        private final Map<String, Object> parameters;

        /**
         * When set to {@code true}, the model is forced to reply only with
         * valid JSON that strictly follows the parameters schema.
         * This enhances reliability of structured output.
         */
        @JsonProperty("strict")
        private final Boolean strict;

    }

    /**
     * The supported tool types. Currently only {@link #FUNCTION} is available,
     * indicating that the tool is a callable function.
     */
    public enum Type {

        /**
         * Function tool type, which means the tool executes a specific function
         * defined by the accompanying {@link Function} object.
         */
        @JsonProperty("function")
        FUNCTION

    }

}