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
     * The type of the tool.
     */
    @JsonProperty("type")
    @Builder.Default
    private final Type type = Type.FUNCTION;

    /**
     * The function definition.
     */
    @JsonProperty("function")
    private final Function function;


    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Function {

        @JsonProperty("description")
        private final String description;

        @JsonProperty("name")
        private final String name;

        @JsonProperty("parameters")
        private final Map<String, Object> parameters;

        @JsonProperty("strict")
        private final Boolean strict;

    }

    public enum Type {

        /**
         * Function tool type.
         */
        @JsonProperty("function")
        FUNCTION

    }

}