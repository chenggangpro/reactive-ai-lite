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
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.util.StringUtils;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonSchemaUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonSchemaUtil.SchemaOption;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * A default, immutable implementation of the {@link ToolDefinition} interface.
 * This class provides a concrete representation of a tool's metadata, including its name,
 * description, and input parameter schema. Instances are created using the associated
 * {@link DefaultToolDefinitionBuilder}.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultToolDefinition implements ToolDefinition {

    @ToString.Include
    private final String identifier;
    @ToString.Include
    private final String name;
    @ToString.Include
    private final String description;
    private final String inputSchema;
    private final Boolean strict;

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String description() {
        return this.description;
    }

    @Override
    public String inputSchema() {
        return this.inputSchema;
    }

    @Override
    public Boolean strict() {
        return this.strict;
    }

    @Override
    public String identifier() {
        return StringUtils.hasText(this.identifier) ? this.identifier : this.name;
    }

    /**
     * Creates a new builder instance for constructing a {@link DefaultToolDefinition}.
     *
     * @return A new {@link DefaultToolDefinitionBuilder}.
     */
    public static DefaultToolDefinitionBuilder builder() {
        return new DefaultToolDefinitionBuilder();
    }

    /**
     * A builder for creating instances of {@link DefaultToolDefinition}.
     * It allows for fluent configuration of the tool's properties and can generate
     * the input schema from either a raw JSON string or a Java {@link Type}.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DefaultToolDefinitionBuilder {

        private String name;
        private String description;
        private String inputSchema;
        private Type inputSchemaType;
        private SchemaOption[] schemaOptions;
        private Boolean strict;
        private String identifier;

        /**
         * Sets the identifier of the tool. If no identifier is provided, the name will be used.
         *
         * @param identifier the identifier of the tool.
         * @return This builder instance for chaining.
         */
        public DefaultToolDefinitionBuilder identifier(@NonNull String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Sets the name of the tool.
         *
         * @param name The name of the tool.
         * @return This builder instance for chaining.
         */
        public DefaultToolDefinitionBuilder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the description of the tool.
         *
         * @param description A human-readable description of the tool's purpose.
         * @return This builder instance for chaining.
         */
        public DefaultToolDefinitionBuilder description(@NonNull String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the input schema directly from a JSON string.
         *
         * @param inputSchema A string containing the JSON schema for the tool's parameters.
         * @return This builder instance for chaining.
         */
        public DefaultToolDefinitionBuilder inputSchema(@NonNull String inputSchema) {
            this.inputSchema = inputSchema;
            return this;
        }

        /**
         * Sets the input schema by specifying a Java {@link Type}. The JSON schema will be
         * generated automatically from this type.
         *
         * @param inputSchemaType The Java type (e.g., a class or ParameterizedType) to generate the schema from.
         * @param schemaOptions   Optional settings to customize schema generation.
         * @return This builder instance for chaining.
         */
        public DefaultToolDefinitionBuilder inputSchemaType(@NonNull Type inputSchemaType, SchemaOption... schemaOptions) {
            this.inputSchemaType = inputSchemaType;
            if (Objects.nonNull(schemaOptions)) {
                this.schemaOptions = schemaOptions;
            }
            return this;
        }

        /**
         * Sets whether the model should strictly follow the function's schema.
         *
         * @param strict If {@code true}, the model is constrained to generate arguments matching the schema.
         * @return This builder instance for chaining.
         */
        public DefaultToolDefinitionBuilder strict(@NonNull Boolean strict) {
            this.strict = strict;
            return this;
        }

        /**
         * Builds and returns a new {@link DefaultToolDefinition} instance.
         * <p>
         * It requires either {@code inputSchema} or {@code inputSchemaType} to be set.
         * If {@code inputSchemaType} is provided, it will generate the schema automatically.
         *
         * @return A new, configured {@link DefaultToolDefinition}.
         * @throws IllegalArgumentException if neither {@code inputSchema} nor {@code inputSchemaType} is provided.
         */
        public DefaultToolDefinition build() {
            if (Objects.nonNull(this.inputSchema)) {
                return new DefaultToolDefinition(identifier, name, description, inputSchema, strict);
            }
            if (Objects.nonNull(this.inputSchemaType)) {
                String inputSchema = JsonSchemaUtil.generateForType(inputSchemaType, schemaOptions);
                return new DefaultToolDefinition(identifier, name, description, inputSchema, strict);
            }
            throw new IllegalArgumentException("Either inputSchema or inputSchemaType must be provided.");
        }
    }
}
