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
 * Utility class for common JSON operations across the framework.
 * <p>
 * This class provides a pre-configured Jackson {@link ObjectMapper} instance tailored
 * for robustness in handling potentially malformed or unexpected responses from AI providers.
 * It also offers convenient methods for common serialization and deserialization tasks,
 * such as converting JSON strings directly to Maps.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public abstract class JsonRelatedUtil {

    /**
     * A global, pre-configured {@link ObjectMapper} instance.
     * <p>
     * This instance is configured to:
     * <ul>
     *     <li>Ignore unknown properties during deserialization.</li>
     *     <li>Not fail when serializing empty beans.</li>
     *     <li>Register standard modules (like JDK8, JavaTime) via {@link JacksonUtils}.</li>
     *     <li>Accept empty strings as null objects.</li>
     * </ul>
     * </p>
     */
    public static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .addModules(JacksonUtils.instantiateAvailableModules())
            .build()
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

    static {
        // Configure coercion for empty strings to null for Enum types
        // This fixes issues where AI providers return empty strings for enum
        // fields (like finish_reason) instead of omitting the field.
        OBJECT_MAPPER.coercionConfigFor(Enum.class).setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
    }

    /**
     * Parses a JSON string into a {@code Map<String, Object>} using the global {@link #OBJECT_MAPPER}.
     *
     * @param json the JSON string to parse
     * @return a Map representation of the JSON data
     * @throws RuntimeException if deserialization fails
     */
    public static Map<String, Object> jsonToMap(String json) {
        if (Objects.isNull(json)) {
            return Map.of();
        }
        return jsonToMap(json, OBJECT_MAPPER);
    }

    /**
     * Parses a JSON string into a {@code Map<String, Object>} using the provided {@link ObjectMapper}.
     *
     * @param json         the JSON string to parse
     * @param objectMapper the object mapper to use for parsing
     * @return a Map representation of the JSON data
     * @throws RuntimeException if deserialization fails
     */
    public static Map<String, Object> jsonToMap(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new LlmClientException("Failed to convert to map with json string: " + json, e);
        }
    }
}
