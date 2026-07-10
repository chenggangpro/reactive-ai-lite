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
package pro.chenggang.project.reactive.ai.lite.client.deepseek.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * Represents a tool definition for the DeepSeek API, specifically for enabling function calling.
 * <p>
 * A function tool allows the model to invoke a defined function with structured arguments.
 * The {@code type} is always {@link Type#FUNCTION}, and the {@code function} property contains
 * the schema describing the function's name, description, parameters, and strict mode enforcement.
 * <p>
 * Instances are created via the builder pattern (Lombok {@link Builder}) and are immutable.
 * The constructor is private to enforce builder usage.
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
     * The type of the tool, which is always {@link Type#FUNCTION}.
     * <p>
     * This field is a constant indicating that this tool is a function definition,
     * as required by the DeepSeek API to differentiate between potential future tool types.
     */
    @JsonProperty("type")
    @Builder.Default
    private final Type type = Type.FUNCTION;

    /**
     * The function definition containing the name, description, parameters schema,
     * and strict mode flag.
     * <p>
     * This object is serialized as the {@code function} JSON property and provides the
     * metadata needed for the model to understand when and how to call the function.
     */
    @JsonProperty("function")
    private final Function function;

    /**
     * The detailed definition of a function that can be called by the model.
     * <p>
     * Includes the function's name, a natural language description, a JSON Schema for the parameters,
     * and a flag to enforce strict parameter validation. These details guide the model in generating
     * valid function call requests.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Function {

        /**
         * A natural language description of what the function does.
         * <p>
         * Helps the model decide when to call the function and to understand its purpose.
         */
        @JsonProperty("description")
        private final String description;

        /**
         * The name of the function.
         * <p>
         * Must be a valid identifier and unique within the tool list. The model uses this name
         * to specify which function to invoke in the {@code tool_calls} response.
         */
        @JsonProperty("name")
        private final String name;

        /**
         * JSON Schema object describing the parameters the function accepts.
         * <p>
         * The keys are JSON Schema properties (e.g., {@code type}, {@code properties}, {@code required})
         * that define the expected arguments. This schema is used to constrain the model's output
         * and validate the generated function call arguments.
         */
        @JsonProperty("parameters")
        private final Map<String, Object> parameters;

        /**
         * When set to {@code true}, enforces strict JSON Schema validation on the model's output.
         * <p>
         * In strict mode, the model is required to output arguments that exactly match the provided
         * schema. If omitted or {@code false}, the model may still produce valid JSON but with more lenience.
         * This aligns with OpenAI's strict function calling behavior.
         */
        @JsonProperty("strict")
        private final Boolean strict;

    }

    /**
     * Enumerates the possible types of tools.
     * <p>
     * Currently only {@link #FUNCTION} is supported, representing a function-calling tool.
     */
    public enum Type {

        /**
         * Function tool type, indicating that the tool is a function definition.
         */
        @JsonProperty("function")
        FUNCTION

    }

}