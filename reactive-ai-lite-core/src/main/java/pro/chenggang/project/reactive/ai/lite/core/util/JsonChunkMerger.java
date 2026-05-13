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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Set;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * Utility class for merging JSON chunks, specifically designed for accumulating
 * streamed responses from AI providers.
 * <p>
 * When models return data in a stream, the response is often broken down into
 * smaller JSON objects that need to be aggregated to form the complete response.
 * This class provides a heuristic-based deep merge algorithm to concatenate text
 * fields and merge arrays based on index.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public class JsonChunkMerger {

    /**
     * A set of keys where the merger should unconditionally overwrite the target
     * value with the source value, rather than concatenating them.
     */
    private static final Set<String> TAKE_LATEST_KEY = Set.of("created_at");

    /**
     * Directly reduces the source ObjectNode into the target ObjectNode
     * using a string-equality heuristic.
     * <p>
     * This method mutates the target object in place.
     * </p>
     *
     * @param target the accumulated ObjectNode so far
     * @param source the new chunk ObjectNode to merge in
     * @return the mutated target ObjectNode (useful for chaining or reducing)
     */
    public static ObjectNode merge(ObjectNode target, ObjectNode source) {
        deepMergeHeuristic(target, source);
        return target;
    }

    /**
     * Performs a deep, heuristic-based merge of two JSON objects.
     * <p>
     * The heuristic rules are:
     * 1. If a field is new, copy it directly.
     * 2. If both fields are textual: if they match exactly (metadata) or the key
     * is in {@link #TAKE_LATEST_KEY}, overwrite. If they differ (streaming text), concatenate.
     * 3. If both fields are objects, recursively deep merge.
     * 4. If both fields are arrays, merge them by aligning indices.
     * 5. Otherwise (numbers, booleans, type mismatches), overwrite with the source value.
     * </p>
     *
     * @param targetObj the target object to mutate
     * @param sourceObj the source object containing new data
     */
    private static void deepMergeHeuristic(ObjectNode targetObj, ObjectNode sourceObj) {
        if (!targetObj.isObject() || !sourceObj.isObject()) {
            return;
        }

        for (Map.Entry<String, JsonNode> field : sourceObj.properties()) {
            String key = field.getKey();
            JsonNode sourceValue = field.getValue();
            JsonNode targetValue = targetObj.get(key);

            if (targetValue == null || targetValue.isNull()) {
                // 1. New field: safely copy it over
                targetObj.set(key, sourceValue.deepCopy());
            } else if (targetValue.isTextual() && sourceValue.isTextual()) {
                // 2. THE HEURISTIC: Differentiate metadata vs. streaming tokens
                String targetStr = targetValue.asText();
                String sourceStr = sourceValue.asText();

                if (targetStr.equals(sourceStr) || TAKE_LATEST_KEY.contains(key)) {
                    // Exact match: likely broadcasted metadata (e.g., "model": "gpt-4o")
                    targetObj.put(key, sourceStr);
                } else {
                    // Difference detected: likely a streaming text token, concatenate it
                    targetObj.put(key, targetStr + sourceStr);
                }
            } else if (targetValue.isObject() && sourceValue.isObject()) {
                // 3. Nested objects: recurse
                deepMergeHeuristic((ObjectNode) targetValue, (ObjectNode) sourceValue);
            } else if (targetValue.isArray() && sourceValue.isArray()) {
                // 4. Arrays (Choices/Tool Calls): merge by index alignment
                mergeArrays((ArrayNode) targetValue, (ArrayNode) sourceValue);
            } else {
                // 5. Fallback: overwrite numerical, boolean, or mismatched types
                targetObj.set(key, sourceValue.deepCopy());
            }
        }
    }

    /**
     * Merges two JSON arrays by attempting to align elements by an explicit "index"
     * field, falling back to the positional index if the field is missing.
     *
     * @param targetArray the target array to mutate
     * @param sourceArray the source array containing new elements
     */
    private static void mergeArrays(ArrayNode targetArray, ArrayNode sourceArray) {
        for (int i = 0; i < sourceArray.size(); i++) {
            JsonNode sourceElement = sourceArray.get(i);

            // Align by "index" if present, otherwise fallback to positional index
            int targetIndex = sourceElement.has("index") ? sourceElement.get("index").asInt() : i;

            // Expand the target array if necessary
            while (targetArray.size() <= targetIndex) {
                targetArray.add(OBJECT_MAPPER.createObjectNode());
            }

            JsonNode targetElement = targetArray.get(targetIndex);

            if (targetElement.isObject() && sourceElement.isObject()) {
                deepMergeHeuristic((ObjectNode) targetElement, (ObjectNode) sourceElement);
            } else {
                targetArray.set(targetIndex, sourceElement.deepCopy());
            }
        }
    }
}
