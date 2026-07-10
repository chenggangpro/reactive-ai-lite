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
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * Represents the {@code response_format} parameter in a Deepseek API chat completion request.
 * This configuration controls the output modality of the model generation: plain text or
 * structured JSON. When JSON output is requested, an optional JSON Schema can be supplied
 * to enforce a specific shape and consistency.
 * <p>
 * Instances of this class are designed to be serialized as part of the request payload
 * and are typically created via the associated builder. Jackson annotations ensure seamless
 * JSON (de)serialization, while Lombok generates the required infrastructure.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ResponseFormat {

    /**
     * Specifies the desired output format type.
     * Must be one of {@link Type#TEXT} (plain text) or {@link Type#JSON_OBJECT} (valid JSON).
     * The {@code TEXT} type is the default when this object is not provided.
     */
    @JsonProperty("type")
    private final Type type;

    /**
     * Optional JSON Schema definition that describes the structure of the generated JSON object.
     * This field is only applicable when {@link #type} is {@link Type#JSON_OBJECT};
     * it is ignored for {@link Type#TEXT} responses.
     * <p>
     * The schema is provided as a {@code Map<String, Object>} allowing flexible definition
     * of properties, required fields, and nested structures. A {@code strict} flag can be
     * set to enforce exact adherence to the schema.
     * </p>
     */
    @JsonProperty("json_schema")
    private final JsonSchema jsonSchema;

    /**
     * Enumeration of the allowed response format types for the Deepseek API.
     * Each constant maps to a recognized string value via Jackson annotations.
     */
    public enum Type {

        /**
         * Instructs the model to generate a plain text response.
         * This is the default behaviour when no {@code response_format} is specified.
         */
        @JsonProperty("text")
        TEXT,

        /**
         * Enables JSON mode, guaranteeing that the model’s output is a syntactically
         * valid JSON object. The model will attempt to produce JSON that conforms
         * to the optional JSON Schema provided via {@link ResponseFormat#jsonSchema}.
         */
        @JsonProperty("json_object")
        JSON_OBJECT,

    }

    /**
     * Encapsulates the JSON Schema definition for structured JSON output.
     * <p>
     * This object is only relevant when the response format type is
     * {@link Type#JSON_OBJECT}. It provides a {@code schema} map that describes
     * the expected JSON structure and an optional {@code strict} flag that
     * toggles whether the model must strictly adhere to that schema.
     * </p>
     */
    @JsonInclude(Include.NON_NULL)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class JsonSchema {

        /**
         * A map representation of a JSON Schema describing the desired structure
         * and constraints of the generated JSON object.
         * <p>
         * The map should conform to the JSON Schema specification (e.g., containing
         * keys like {@code "type"}, {@code "properties"}, {@code "required"}, etc.).
         * The exact key‑value pairs determine how the model constructs its output.
         * </p>
         */
        @JsonProperty("schema")
        private final Map<String, Object> schema;

        /**
         * If set to {@code true}, instructs the model to strictly follow the
         * provided JSON Schema. When {@code false} or absent, the model may
         * produce output that is valid JSON but does not exactly match every
         * constraint of the schema.
         */
        @JsonProperty("strict")
        private final Boolean strict;

    }

}