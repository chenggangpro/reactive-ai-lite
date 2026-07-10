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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.json.JsonMapper;
import pro.chenggang.project.reactive.ai.lite.core.exception.LlmClientException;

import java.util.Map;
import java.util.Objects;

/**
 * Abstract utility class for common JSON serialization and deserialization tasks
 * across the reactive AI framework, providing a pre-configured Jackson {@link ObjectMapper}
 * and convenience methods dealing with the uncertainties of AI provider responses.
 * <p>
 * AI providers may return responses with unexpected structures (e.g., extra fields, empty strings
 * for enums, missing values). The mapper is hardened against such variability by:
 * <ul>
 *     <li>Ignoring unknown properties to avoid deserialization failures on protocol extensions.</li>
 *     <li>Treating empty strings as null objects to handle optional fields that are transmitted
 *         as empty strings rather than omitted or null.</li>
 *     <li>Coercing empty strings to {@code null} specifically for {@code Enum} fields — a common issue
 *         when a service returns an empty string for a {@code finish_reason} or similar status field
 *         instead of omitting it.</li>
 *     <li>Registering all available Jackson modules (e.g., JDK8, JavaTime) so that modern Java types
 *         are handled correctly.</li>
 * </ul>
 * Because this class is abstract and designed purely as a holder for static utilities, it cannot be
 * instantiated.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public abstract class JsonRelatedUtil {

    /**
     * The global, thread-safe {@link ObjectMapper} instance configured for robustness in AI integrations.
     * <p>
     * Configuration details and their rationale:
     * <ul>
     *     <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} is disabled so that
     *     forward-compatible API changes (e.g., new optional fields) do not cause deserialization errors.</li>
     *     <li>{@link SerializationFeature#FAIL_ON_EMPTY_BEANS} is disabled to allow serialization
     *     of placeholder objects without explicit properties, preventing surprising runtime exceptions.</li>
     *     <li>All auto-discoverable Jackson modules (such as <em>jackson-datatype-jdk8</em> and
     *     <em>jackson-datatype-jsr310</em>) are registered via {@link JacksonUtils} to ensure correct
     *     handling of {@link java.util.Optional}, {@link java.time.Instant}, etc.</li>
     *     <li>{@link DeserializationFeature#ACCEPT_EMPTY_STRING_AS_NULL_OBJECT} is enabled to treat
     *     empty strings as {@code null} during deserialization, which is particularly useful when
     *     AI providers use empty strings instead of omitting optional fields.</li>
     *     <li>Coercion of empty string to null for {@link Enum} types is set in a static initializer
     *     block (see below). This addresses a specific reality where JSON responses may contain
     *     {@code "finish_reason": ""} rather than omitting the key entirely.</li>
     * </ul>
     * This instance is intentionally immutable and safe for concurrent use.
     * </p>
     */
    public static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .addModules(JacksonUtils.instantiateAvailableModules())
            .build()
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

    /**
     * Applies additional coercion configuration to handle edge cases in AI provider payloads.
     * <p>
     * By default, an empty string cannot be deserialized as an Enum and would cause a failure.
     * By setting {@code CoercionAction.AsNull} for {@code CoercionInputShape.EmptyString} on
     * {@code Enum.class}, we instruct the mapper to convert an empty string value into {@code null}
     * instead of throwing an exception. This is a common pattern when, for example, a model
     * returns a {@code finish_reason} field as an empty string instead of omitting it.
     * </p>
     */
    static {
        OBJECT_MAPPER.coercionConfigFor(Enum.class).setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
    }

    /**
     * Parses the provided JSON string into a {@code Map<String, Object>} using the
     * globally configured {@link #OBJECT_MAPPER}.
     * <p>
     * A {@code null} or empty input returns an empty, immutable map. The method is designed
     * to be tolerant of typical AI response payloads, including those with extra fields or
     * string representations of null values.
     * </p>
     *
     * @param json the JSON string to parse; may be {@code null}
     * @return a map representing the JSON structure, or an empty map if the input is {@code null}
     * @throws LlmClientException if the JSON is syntactically invalid or otherwise unparseable
     */
    public static Map<String, Object> jsonToMap(String json) {
        if (Objects.isNull(json)) {
            return Map.of();
        }
        return jsonToMap(json, OBJECT_MAPPER);
    }

    /**
     * Parses the provided JSON string into a {@code Map<String, Object>} using the given
     * {@link ObjectMapper}.
     * <p>
     * This variant allows callers to supply a custom mapper with different configuration,
     * e.g., when a particular API contract requires strict handling or additional modules.
     * </p>
     *
     * @param json         the JSON string to parse; must not be {@code null} in normal usage
     * @param objectMapper the Jackson mapper to use for deserialization
     * @return a map representation of the JSON data
     * @throws LlmClientException if deserialization fails for any reason (including mapper-specific
     *                           configuration issues)
     */
    public static Map<String, Object> jsonToMap(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new LlmClientException("Failed to convert to map with json string: " + json, e);
        }
    }
}