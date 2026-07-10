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
package pro.chenggang.project.reactive.ai.lite.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import org.springframework.util.Assert;

import java.lang.reflect.Type;
import java.util.stream.Stream;

/**
 * Utility class for generating JSON schemas from Java types.
 * <p>
 * This class uses the Victools JSON Schema Generator to dynamically create JSON schemas
 * corresponding to Java classes or parameterized types. These schemas are commonly used
 * to describe tool input parameters or structured output formats to AI models.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public final class JsonSchemaUtil {

    /**
     * The singleton {@link SchemaGenerator} instance used for producing JSON schemas from arbitrary Java types.
     * <p>
     * This generator is configured with:
     * <ul>
     *     <li>JSON Schema Draft 2020-12</li>
     *     <li>Plain JSON preset, avoiding schema version indicators</li>
     *     <li>Jackson module integration that respects {@code @JsonProperty(required=true)} annotations</li>
     *     <li>Standard format values (e.g., date-time, email) and OpenAPI format extensions</li>
     *     <li>Plain definition keys for clean, readable schemas</li>
     * </ul>
     * These settings ensure the generated schemas are compatible with AI tool-calling specifications and provide
     * sufficient annotations for model interpretation.
     */
    private static final SchemaGenerator TYPE_SCHEMA_GENERATOR;

    /*
     * Build the generator with Draft 2020-12 and PLAIN_JSON preset; integrates Jackson modules
     * to handle JSON annotations and includes format extensions for richer schema descriptions.
     */
    static {
        Module jacksonModule = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED);
        SchemaGeneratorConfig subtypeConfig = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12,
                OptionPreset.PLAIN_JSON
        )
                .with(jacksonModule)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .with(Option.STANDARD_FORMATS)
                .with(Option.PLAIN_DEFINITION_KEYS)
                .without(Option.SCHEMA_VERSION_INDICATOR)
                .build();

        TYPE_SCHEMA_GENERATOR = new SchemaGenerator(subtypeConfig);
    }

    /**
     * Generates a JSON Schema for the given Java type.
     * <p>
     * This method analyzes the provided {@link Type} and produces a JSON string representing
     * its schema according to JSON Schema Draft 2020-12. It applies any specified
     * {@link SchemaOption}s to modify the resulting schema. Special handling is applied for
     * {@code Void} types: if no properties are present, an empty {@code "properties"} object is
     * inserted, ensuring that AI models can interpret the schema as “no additional input required”.
     * </p>
     *
     * @param type          the Java type to generate the schema for
     * @param schemaOptions optional settings to customize the generated schema
     * @return a formatted JSON string representing the schema
     * @throws IllegalArgumentException if the type is null
     */
    public static String generateForType(Type type, SchemaOption... schemaOptions) {
        Assert.notNull(type, "type cannot be null");
        ObjectNode schema = TYPE_SCHEMA_GENERATOR.generateSchema(type);
        if ((type == Void.class) && !schema.has("properties")) {
            schema.putObject("properties");
        }
        processSchemaOptions(schemaOptions, schema);
        return schema.toPrettyString();
    }

    /**
     * Applies the requested {@link SchemaOption}s to the generated JSON schema node.
     * <p>
     * By default, this method enforces {@code "additionalProperties": false} unless
     * {@link SchemaOption#ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT} is present. It also
     * optionally converts all {@code "type"} values to uppercase if
     * {@link SchemaOption#UPPER_CASE_TYPE_VALUES} is requested, ensuring compatibility
     * with AI providers that expect uppercase type identifiers.
     *
     * @param schemaOptions the options to process; never {@code null}
     * @param schema the schema object node to mutate
     */
    private static void processSchemaOptions(SchemaOption[] schemaOptions, ObjectNode schema) {
        if (Stream.of(schemaOptions)
                .noneMatch(option -> option == SchemaOption.ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT)) {
            schema.put("additionalProperties", false);
        }
        if (Stream.of(schemaOptions).anyMatch(option -> option == SchemaOption.UPPER_CASE_TYPE_VALUES)) {
            convertTypeValuesToUpperCase(schema);
        }
    }

    /**
     * Recursively transforms all {@code "type"} property values in the given JSON node to uppercase.
     * <p>
     * This operation exists because some AI model providers (e.g., certain versions of OpenAI-compatible APIs)
     * expect type keywords like {@code "OBJECT"} and {@code "STRING"} rather than the standard lowercase
     * form. By applying this transformation, the generated schema remains compliant with those providers
     * without altering the base generation logic.
     *
     * @param node the root node of the JSON schema to process; will be mutated in place
     */
    public static void convertTypeValuesToUpperCase(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            JsonNode typeNode = objNode.get("type");
            if (typeNode != null && typeNode.isTextual()) {
                objNode.put("type", typeNode.asText().toUpperCase());
            }
            objNode.forEach(JsonSchemaUtil::convertTypeValuesToUpperCase);
        } else if (node.isArray()) {
            node.forEach(JsonSchemaUtil::convertTypeValuesToUpperCase);
        }
    }

    /**
     * Options for customizing the generation of JSON Schemas.
     *
     * @author Gang Cheng
     */
    public enum SchemaOption {

        /**
         * By default, the generator sets "additionalProperties" to false. This option overrides
         * that behavior, allowing the described object to contain arbitrary additional properties.
         */
        ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT,

        /**
         * Converts the values of "type" fields in the schema to uppercase (e.g., "string" becomes "STRING").
         */
        UPPER_CASE_TYPE_VALUES

    }

}