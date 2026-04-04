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
 * @author Cheng Gang
 * @version 0.1.0
 */
public final class JsonSchemaUtil {

    private static final SchemaGenerator TYPE_SCHEMA_GENERATOR;

    /*
     * Initialize JSON Schema generators with standard presets and Jackson integration.
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
     * {@link SchemaOption}s to modify the resulting schema.
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
     * Processes and applies the specified schema options to the generated schema object.
     *
     * @param schemaOptions the options to apply
     * @param schema        the generated schema node
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
     * Recursively traverses the JSON schema node and converts all "type" property values to uppercase.
     * <p>
     * This is sometimes required for compatibility with specific AI providers that expect
     * uppercase type indicators in the schema.
     * </p>
     *
     * @param node the JSON node to traverse and modify
     */
    public static void convertTypeValuesToUpperCase(ObjectNode node) {
        if (node.isObject()) {
            node.properties().forEach(entry -> {
                JsonNode value = entry.getValue();
                if (value.isObject()) {
                    convertTypeValuesToUpperCase((ObjectNode) value);
                } else if (value.isArray()) {
                    value.forEach(element -> {
                        if (element.isObject() || element.isArray()) {
                            convertTypeValuesToUpperCase((ObjectNode) element);
                        }
                    });
                } else if (value.isTextual() && entry.getKey().equals("type")) {
                    String oldValue = node.get("type").asText();
                    node.put("type", oldValue.toUpperCase());
                }
            });
        } else if (node.isArray()) {
            node.forEach(element -> {
                if (element.isObject() || element.isArray()) {
                    convertTypeValuesToUpperCase((ObjectNode) element);
                }
            });
        }
    }

    /**
     * Options for customizing the generation of JSON Schemas.
     *
     * @author Cheng Gang
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
