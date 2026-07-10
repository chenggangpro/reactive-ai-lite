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
package pro.chenggang.project.reactive.ai.lite.client.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * Represents a tool that the OpenAI model may call.
 * Currently, only function tools are supported as defined by the OpenAI API.
 * This class encapsulates the structure of a function tool and its nested definition,
 * enabling precise JSON serialization for chat completion requests.
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
     * The type of the tool. Must be {@link Type#FUNCTION}.
     * This field is mapped to the "type" JSON property and defaults to {@link Type#FUNCTION}.
     */
    @JsonProperty("type")
    @Builder.Default
    private final Type type = Type.FUNCTION;

    /**
     * The specific function definition associated with this tool.
     * Contains the function name, description, and parameter schema.
     */
    @JsonProperty("function")
    private final Function function;

    /**
     * Encapsulates the metadata and schema of a callable function.
     * Maps to the "function" object within the tool definition of the OpenAI API.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Function {

        /**
         * A detailed description of what the function does, used by the model to choose when and how to call the function.
         */
        @JsonProperty("description")
        private final String description;

        /**
         * The exact name of the function to be called. Must match the function's internal identifier.
         */
        @JsonProperty("name")
        private final String name;

        /**
         * The JSON Schema formatted parameters the function accepts.
         * Represented as a generic map structure to allow dynamic schema definitions.
         */
        @JsonProperty("parameters")
        private final Map<String, Object> parameters;

        /**
         * Whether to enable strict schema adherence when generating the function call arguments.
         * If true, the model will output arguments that exactly match the provided parameters schema.
         */
        @JsonProperty("strict")
        private final Boolean strict;

    }

    /**
     * Defines the supported tool types by the OpenAI API.
     */
    public enum Type {

        /**
         * Represents a tool type that calls a custom function.
         */
        @JsonProperty("function")
        FUNCTION

    }

}