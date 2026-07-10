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
 * A tool definition provides the model with the metadata necessary to understand both
 * <em>when</em> to use the tool (via its name and description) and <em>how</em> to use it
 * (via the input schema). Implementing this interface allows the AI runtime to discover
 * available operations, decide which one fits the user's intent, and generate properly
 * structured arguments.
 * </p>
 * <p>
 * The contract is intentionally minimal: a unique name, a clear description, and a JSON Schema
 * that describes the expected parameters. An optional {@link #strict() strict} flag hints at
 * how rigidly the generated arguments should adhere to the schema.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see DefaultToolDefinition
 * @see ToolDefinitionBuilder
 */
public interface ToolDefinition {

    /**
     * Gets the unique name that the AI model uses to reference this tool.
     * <p>
     * The name must be unique among all tools provided to a single model invocation.
     * Typically it consists of alphanumeric characters and underscores. The AI runtime
     * uses this name when it decides to call the tool; duplicate names cause ambiguity
     * and may lead to runtime errors or unpredictable behaviour.
     * </p>
     *
     * @return the unique tool name
     */
    String name();

    /**
     * Gets a human-readable description explaining what the tool does and when it should be used.
     * <p>
     * This description is crucial because the model relies on it to match the tool's capabilities
     * with the user's intent. A well‑crafted description should state the tool's purpose, the actions
     * it performs, and any relevant constraints. The more precise the description, the better the AI
     * can decide whether to invoke this tool.
     * </p>
     *
     * @return a natural‑language description of the tool
     */
    String description();

    /**
     * Gets the JSON Schema that defines the structure of the arguments the tool expects.
     * <p>
     * The schema must be a valid JSON Schema object definition. When the AI decides to call the tool,
     * it generates a JSON object that will be validated against this schema (depending on the strictness
     * setting). The schema describes the parameter names, their types, required fields, and any additional
     * constraints (e.g., minimum values, enums).
     * </p>
     *
     * @return a JSON Schema string representing the tool's parameter contract
     */
    String inputSchema();

    /**
     * Indicates whether the AI model should enforce strict adherence to the input schema when generating
     * arguments for this tool.
     * <p>
     * When {@code true}, the provider makes a best effort to guarantee that the generated arguments
     * are valid according to the schema — this reduces the risk of malformed function calls but may
     * slightly constrain the model's expressiveness. When {@code false} or {@code null}, the model
     * may still produce valid JSON, but without a strict guarantee; it could potentially include extra
     * fields or omit optional ones. Returning {@code null} explicitly defers the decision to the
     * provider's default behaviour, which often leans towards permitting flexibility.
     * </p>
     *
     * @return {@code true} to enforce strict schema matching, {@code false} to allow leniency,
     *         or {@code null} to use the provider's default
     */
    default Boolean strict() {
        return null;
    }

    /**
     * Creates a new builder instance for constructing a {@link ToolDefinition}.
     * <p>
     * This factory method provides a fluent entry point for building tool definitions. The returned
     * builder validates all required fields and supports both explicit JSON Schema strings and
     * schema generation from Java types.
     * </p>
     *
     * @return a freshly created {@link ToolDefinitionBuilder} ready for configuration
     * @see ToolDefinitionBuilder
     */
    static ToolDefinitionBuilder newToolDefinition() {
        return new ToolDefinitionBuilder();
    }

    /**
     * A builder class for constructing {@link ToolDefinition} instances.
     * <p>
     * This builder offers a fluent API for configuring every aspect of a tool definition. It ensures
     * that all required information (name, description, and a schema source) is provided before
     * the definition is built. Two schema definition strategies are supported:
     * </p>
     * <ul>
     *   <li>Directly supply a raw JSON Schema string via {@link #inputSchema(String)} — ideal when
     *       you need full control over the schema or already have one generated externally.</li>
     *   <li>Specify a Java {@link Type} via {@link #inputSchemaType(Type, SchemaOption...)} — the
     *       builder automatically derives the JSON Schema from the type's structure, including its
     *       fields, types, and annotations, optionally customised by {@link SchemaOption}s.</li>
     * </ul>
     * <p>
     * Additionally, the builder supports configuring whether the AI should strictly adhere to the
     * schema ({@link #strict(Boolean)}) and automatically validates all inputs to prevent
     * misconfigured tool definitions.
     * </p>
     *
     * @author Gang Cheng
     * @see ToolDefinition
     * @see DefaultToolDefinition
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    class ToolDefinitionBuilder {

        /**
         * The tool's unique name; set via {@link #name(String)}.
         * Must be non‑null and non‑empty.
         */
        private String name;

        /**
         * A human‑readable description of the tool; set via {@link #description(String)}.
         * Must be non‑null and non‑empty.
         */
        private String description;

        /**
         * The raw JSON schema string, set directly via {@link #inputSchema(String)}.
         * If this field is not {@code null} at build time, it takes absolute priority over type‑based
         * schema generation.
         */
        private String inputSchema;

        /**
         * The Java type from which the JSON schema will be generated; set via
         * {@link #inputSchemaType(Type, SchemaOption...)}.
         * Only used when {@link #inputSchema} is {@code null}.
         */
        private Type inputSchemaType;

        /**
         * Options that control how the JSON schema is generated from {@link #inputSchemaType}.
         * Defaults to an empty array. Applied only when the schema is derived from a type.
         */
        private SchemaOption[] schemaOptions = new SchemaOption[0];

        /**
         * Flag that controls strict schema adherence; set via {@link #strict(Boolean)}.
         * {@code null} means the provider's default behaviour should be used.
         */
        private Boolean strict;

        /**
         * Configures the unique name of the tool.
         * <p>
         * The name is exposed to the AI model and must be unique across all tools provided
         * in the same request. A good name is concise and self‑documenting, e.g., {@code "get_weather"}
         * or {@code "create_order"}. It should consist of alphanumeric characters and underscores.
         * </p>
         *
         * @param name the tool name; must not be null
         * @return this builder instance for method chaining
         */
        public ToolDefinitionBuilder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Provides a natural‑language description of the tool's purpose and usage.
         * <p>
         * The description is a primary signal for the AI model to decide whether to invoke this tool.
         * Include information about what the tool does, when it is appropriate to use, and any
         * important side effects or constraints. The more precise the description, the higher the
         * chance the model selects the correct tool for the user's intent.
         * </p>
         *
         * @param description a clear explanation of the tool; must not be null
         * @return this builder instance for method chaining
         */
        public ToolDefinitionBuilder description(@NonNull String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the input schema directly from a JSON string.
         * <p>
         * The string must represent a valid JSON Schema object definition. This method is
         * appropriate when you already have a hand‑crafted or externally generated schema.
         * If both a raw schema and a type are supplied, this raw schema takes precedence.
         * </p>
         *
         * @param inputSchema a JSON Schema string describing the tool's parameters; must not be null
         * @return this builder instance for method chaining
         */
        public ToolDefinitionBuilder inputSchema(@NonNull String inputSchema) {
            this.inputSchema = inputSchema;
            return this;
        }

        /**
         * Defines the input schema by providing a Java {@link Type}, from which a JSON Schema
         * will be automatically generated.
         * <p>
         * The generation uses the provided type's structure, including field names, types, and
         * annotations (e.g., {@code @JsonPropertyDescription}). Optional {@link SchemaOption}s
         * allow fine‑tuning, such as generating descriptions from annotation metadata or controlling
         * the inclusion of nullable fields. This approach is convenient when the tool's parameters
         * are modelled as a simple POJO or Record.
         * </p>
         *
         * @param inputSchemaType the Java type to derive the schema from; must not be null
         * @param schemaOptions   optional generation settings (e.g., {@link SchemaOption#GENERATE_DESCRIPTIONS});
         *                        if {@code null}, an empty array is used
         * @return this builder instance for method chaining
         * @see JsonSchemaUtil#generateForType(Type, SchemaOption...)
         */
        public ToolDefinitionBuilder inputSchemaType(@NonNull Type inputSchemaType, SchemaOption... schemaOptions) {
            this.inputSchemaType = inputSchemaType;
            if (Objects.nonNull(schemaOptions)) {
                this.schemaOptions = schemaOptions;
            }
            return this;
        }

        /**
         * Specifies whether the AI model should strictly adhere to the input schema when generating
         * arguments.
         * <p>
         * A value of {@code true} instructs the provider to make a best effort to produce arguments
         * that are valid according to the schema, reducing the likelihood of malformed function calls.
         * A value of {@code false} or {@code null} allows the model more flexibility, which may be
         * desirable for experimental or loosely‑defined schemas. The exact behaviour when {@code null}
         * is provider‑dependent.
         * </p>
         *
         * @param strict {@code true} for strict adherence, {@code false} to allow leniency,
         *               or {@code null} to use the provider's default
         * @return this builder instance for method chaining
         */
        public ToolDefinitionBuilder strict(@NonNull Boolean strict) {
            this.strict = strict;
            return this;
        }

        /**
         * Builds and returns a fully configured {@link ToolDefinition} after validating all required
         * fields.
         * <p>
         * The build process follows these rules:
         * </p>
         * <ol>
         *   <li>The tool {@code name} and {@code description} must be non‑null and non‑empty.</li>
         *   <li>If {@link #inputSchema} is not {@code null}, it is used as‑is; the type‑based
         *       generation is ignored.</li>
         *   <li>Otherwise, if {@link #inputSchemaType} is set, a JSON Schema is derived from it
         *       using the configured {@link #schemaOptions}.</li>
         *   <li>If neither a raw schema nor a type is provided, an {@link IllegalArgumentException}
         *       is thrown.</li>
         * </ol>
         * <p>
         * This validation ensures that the resulting {@code ToolDefinition} can be safely passed to
         * the AI runtime without causing runtime errors due to missing metadata.
         * </p>
         *
         * @return a new, immutable {@link ToolDefinition} instance (typically {@link DefaultToolDefinition})
         * @throws IllegalArgumentException if {@code name} or {@code description} is blank,
         *                                  or if neither schema source is provided
         * @see DefaultToolDefinition
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