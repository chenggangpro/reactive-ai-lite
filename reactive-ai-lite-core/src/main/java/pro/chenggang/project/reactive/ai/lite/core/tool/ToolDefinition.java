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
import org.springframework.util.StringUtils;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonSchemaUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonSchemaUtil.SchemaOption;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Defines the contract for a tool or function that can be called by an AI model.
 * <p>
 * A tool definition provides the model with the necessary metadata—such as its name,
 * a description of its purpose, and the schema for its input parameters—to understand
 * when and how to use the tool.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface ToolDefinition {

    /**
     * Gets the name of the tool.
     * <p>
     * The name should be unique among the set of tools provided to the model. It typically
     * consists of alphanumeric characters and underscores.
     * </p>
     *
     * @return the unique name of the tool
     */
    String name();

    /**
     * Gets a human-readable description of what the tool does.
     * <p>
     * This description helps the AI model decide whether to call this tool based on the
     * user's request. It should clearly explain the tool's functionality and use cases.
     * </p>
     *
     * @return the description of the tool
     */
    String description();

    /**
     * Gets the JSON schema that defines the input parameters for the tool.
     * <p>
     * The model uses this schema to generate the correct arguments when it decides to
     * call the tool. The schema should be a valid JSON Schema object definition.
     * </p>
     *
     * @return a string representation of the JSON schema for the tool's input
     */
    String inputSchema();

    /**
     * Specifies whether the AI model should strictly adhere to the provided input schema
     * when generating arguments for the tool call.
     * <p>
     * When set to {@code true}, the model will make a best effort to generate a valid JSON
     * object that conforms to the schema. The default behavior (when returning {@code null})
     * is determined by the specific AI provider.
     * </p>
     *
     * @return {@code true} to enforce strict schema adherence, or {@code null} to use the
     *         provider's default behavior
     */
    default Boolean strict() {
        return null;
    }

    /**
     * Creates a new builder instance for constructing a {@link ToolDefinition}.
     * <p>
     * This factory method provides a convenient entry point for creating tool definitions
     * using a fluent builder API.
     * </p>
     *
     * @return a new {@link ToolDefinitionBuilder} instance ready to be configured
     * @see ToolDefinitionBuilder
     */
    static ToolDefinitionBuilder newToolDefinition() {
        return new ToolDefinitionBuilder();
    }

    /**
     * A builder class for constructing {@link ToolDefinition} instances.
     * <p>
     * This builder provides a fluent API for configuring tool definitions with various properties
     * such as name, description, input schema, and strictness settings. It supports two approaches
     * for defining the input schema: directly providing a JSON schema string or specifying a Java
     * type from which the schema will be automatically generated.
     * </p>
     *
     * @see ToolDefinition
     * @see DefaultToolDefinition
     * @author Cheng Gang
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    class ToolDefinitionBuilder {

        private String name;
        private String description;
        private String inputSchema;
        private Type inputSchemaType;
        private SchemaOption[] schemaOptions = new SchemaOption[0];
        private Boolean strict;

        /**
         * Sets the name of the tool.
         *
         * @param name the name of the tool
         * @return this builder instance for chaining
         */
        public ToolDefinitionBuilder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the description of the tool.
         *
         * @param description a human-readable description of the tool's purpose
         * @return this builder instance for chaining
         */
        public ToolDefinitionBuilder description(@NonNull String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the input schema directly from a JSON string.
         *
         * @param inputSchema a string containing the JSON schema for the tool's parameters
         * @return this builder instance for chaining
         */
        public ToolDefinitionBuilder inputSchema(@NonNull String inputSchema) {
            this.inputSchema = inputSchema;
            return this;
        }

        /**
         * Sets the input schema by specifying a Java {@link Type}.
         * <p>
         * The JSON schema will be generated automatically from this type using the configured schema options.
         * </p>
         *
         * @param inputSchemaType the Java type to generate the schema from
         * @param schemaOptions   optional settings to customize schema generation
         * @return this builder instance for chaining
         */
        public ToolDefinitionBuilder inputSchemaType(@NonNull Type inputSchemaType, SchemaOption... schemaOptions) {
            this.inputSchemaType = inputSchemaType;
            if (Objects.nonNull(schemaOptions)) {
                this.schemaOptions = schemaOptions;
            }
            return this;
        }

        /**
         * Sets whether the model should strictly follow the function's schema.
         *
         * @param strict if {@code true}, the model is constrained to generate arguments matching the schema
         * @return this builder instance for chaining
         */
        public ToolDefinitionBuilder strict(@NonNull Boolean strict) {
            this.strict = strict;
            return this;
        }

        /**
         * Builds and returns a new {@link ToolDefinition} instance.
         * <p>
         * It requires that the name, description, and either {@code inputSchema} or
         * {@code inputSchemaType} are set. If {@code inputSchemaType} is provided,
         * it generates the schema automatically.
         * </p>
         *
         * @return a new, configured {@link ToolDefinition} (typically a {@link DefaultToolDefinition})
         * @throws IllegalArgumentException if required fields are missing or if neither schema nor schema type is provided
         */
        public ToolDefinition build() {
            if (!StringUtils.hasText(this.name)) {
                throw new IllegalArgumentException("Name of tool definition cannot be null or empty.");
            }
            if (!StringUtils.hasText(this.description)) {
                throw new IllegalArgumentException("Description of tool definition cannot be null or empty.");
            }
            if (Objects.nonNull(this.inputSchema)) {
                return DefaultToolDefinition.builder()
                        .name(this.name)
                        .description(this.description)
                        .inputSchema(this.inputSchema)
                        .strict(this.strict)
                        .build();
            }
            if (Objects.nonNull(this.inputSchemaType)) {
                String inputSchemaByType = JsonSchemaUtil.generateForType(inputSchemaType, schemaOptions);
                return DefaultToolDefinition.builder()
                        .name(this.name)
                        .description(this.description)
                        .inputSchema(inputSchemaByType)
                        .strict(this.strict)
                        .build();
            }
            throw new IllegalArgumentException("Either inputSchema or inputSchemaType must be provided.");
        }
    }
}
