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
 * @author Cheng Gang
 * @version 0.1.0
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ResponseFormat {

    /**
     * Type Must be one of 'text', 'json_object'.
     */
    @JsonProperty("type")
    private final Type type;

    /**
     * JSON schema object that describes the format of the JSON object. Only applicable
     * when type is 'json_schema'.
     */
    @JsonProperty("json_schema")
    private final JsonSchema jsonSchema;

    public enum Type {

        /**
         * Generates a text response. (default)
         */
        @JsonProperty("text")
        TEXT,

        /**
         * Enables JSON mode, which guarantees the message the model generates is valid
         * JSON.
         */
        @JsonProperty("json_object")
        JSON_OBJECT,

    }

    /**
     * JSON schema object that describes the format of the JSON object. Applicable for the
     * 'json_schema' type only.
     */
    @JsonInclude(Include.NON_NULL)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class JsonSchema {

        @JsonProperty("schema")
        private final Map<String, Object> schema;

        @JsonProperty("strict")
        private final Boolean strict;

    }

}
