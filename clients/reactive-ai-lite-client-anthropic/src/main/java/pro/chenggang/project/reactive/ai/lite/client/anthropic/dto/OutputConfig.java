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
package pro.chenggang.project.reactive.ai.lite.client.anthropic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * Represents the output configuration for an Anthropic API request, defining how the
 * model should format its response. Currently only supports enforcing a JSON schema
 * on the output, which guarantees that the model’s response is a valid JSON object
 * matching the provided schema. This is achieved by setting the {@code format} property
 * to a {@link OutputFormat} instance tưhat describes the JSON schema specification.
 * <p>
 * The {@link OutputFormat} inner class is used to specify the format details, including
 * the type (fixed to {@code "json_schema"}) and the schema itself as a Jackson map.
 * This approach leverages Jackson’s serialization to produce the exact JSON structure
 * expected by the Anthropic API.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see OutputFormat
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OutputConfig {

    /**
     * The output format configuration. When provided, the model will attempt to
     * generate output that adheres to the given JSON schema. The field is serialized
     * under the {@code "format"} key in the JSON request body, mapping directly
     * to Anthropic’s output format parameter.
     */
    @JsonProperty("format")
    private final OutputFormat format;

    /**
     * Defines a JSON Schema output format specification for the model response.
     * This inner class encapsulates the format type and the schema definition map
     * that the model must use to structure its output.
     * <p>
     * The {@code type} is fixed to {@code "json_schema"} as required by the Anthropic
     * API. The {@code jsonSchema} map contains the actual JSON schema definition,
     * which will be serialized as a JSON object. Only applicable when the parent
     * {@code OutputConfig} includes this format.
     */
    @JsonInclude(Include.NON_NULL)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class OutputFormat {

        /**
         * The type of the output format, permanently set to {@code "json_schema"}.
         * This fixed value instructs the Anthropic API that the output should conform
         * to the accompanying JSON schema. The constant is predefined because Anthropic
         * currently supports only this specific output format type.
         */
        @JsonProperty("type")
        private final String type = "json_schema";

        /**
         * The JSON schema object that describes the exact structure of the expected
         * output. The map entries represent the schema properties and constraints,
         * which are serialized as a JSON object. This schema is only applicable when
         * the format type is {@code "json_schema"}.
         * <p>
         * For example, a minimal schema ensuring an object with a "name" string
         * could be represented as:
         * <pre>
         * {
         *   "type": "object",
         *   "properties": {
         *     "name": { "type": "string" }
         *   },
         *   "required": ["name"]
         * }
         * </pre>
         */
        @JsonProperty("schema")
        private final Map<String, Object> jsonSchema;
    }
}