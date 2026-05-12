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
 * @author Cheng Gang
 * @version 0.1.0
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OutputConfig {

    @JsonProperty("format")
    private final OutputFormat format;


    @JsonInclude(Include.NON_NULL)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class OutputFormat {
        /**
         * The output config type
         */
        @JsonProperty("type")
        private final String type = "json_schema";

        /**
         * JSON schema object that describes the format of the JSON object. Only applicable
         * when type is 'json_schema'.
         */
        @JsonProperty("schema")
        private final Map<String, Object> jsonSchema;
    }

}
