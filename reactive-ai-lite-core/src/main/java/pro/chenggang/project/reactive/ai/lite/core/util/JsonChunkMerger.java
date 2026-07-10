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
 * Standard JSON merge strategies would overwrite text fields on subsequent chunks,
 * losing previously streamed tokens. This class uses a <em>heuristic</em> deep merge
 * algorithm that differentiates between metadata (usually invariant) and streaming
 * tokens (progressive concatenation) by comparing string values. It also handles
 * array elements (like chat choices or tool calls) by aligning them via an optional
 * {@code index} field, ensuring structured data accumulates correctly.
 * </p>
 * <p>
 * The merge is performed in-place on the target node for efficiency, but both nodes
 * are deep-copied where necessary to avoid side effects on the original source.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public class JsonChunkMerger {

    /**
     * A set of field keys that should be overwritten with the latest value
     * rather than concatenated. This handles cases like {@code created_at},
     * where the source value simply supersedes any previous value.
     */
    private static final Set<String> TAKE_LATEST_KEY = Set.of("created_at");

    /**
     * Merges a newly arrived JSON chunk (source) into the accumulated response (target).
     * <p>
     * The target node is mutated in-place; the source node is left unmodified.
     * This method is designed to be used in a stream reduction (e.g., via
     * {@code Mono.reduce}) to gradually build the complete JSON response
     * from individual streaming events.
     * </p>
     *
     * @param target the accumulated JSON object (will be mutated)
     * @param source the new chunk to merge
     * @return the mutated target node for chaining
     */
    public static ObjectNode merge(ObjectNode target, ObjectNode source) {
        deepMergeHeuristic(target, source);
        return target;
    }

    /**
     * Recursively merges every field of the source object into the target
     * using the following heuristic rules:
     * <ol>
     *   <li><b>New field:</b> If the key is absent in the target, deep-copy the source value.</li>
     *   <li><b>Textual fields:</b> If both values are strings, they are compared.
     *       <ul>
     *         <li>If the strings are identical, the key likely represents metadata
     *             that is broadcasted in every chunk (e.g., model name), so we simply keep one copy.</li>
     *         <li>If the strings differ and the key is <em>not</em> in {@link #TAKE_LATEST_KEY},
     *             we assume the source is a streaming text token and concatenate it to the
     *             existing value (i.e., {@code targetStr + sourceStr}).</li>
     *         <li>If the key <em>is</em> in {@code TAKE_LATEST_KEY}, the source value overwrites
     *             the target regardless of equality.</li>
     *       </ul>
     *   </li>
     *   <li><b>Objects:</b> Both are objects? Recurse into them.</li>
     *   <li><b>Arrays:</b> Merge by aligning elements via an {@code "index"} field
     *       or by positional index when absent (see {@link #mergeArrays(ArrayNode, ArrayNode)}).</li>
     *   <li><b>Fallback:</b> For numbers, booleans, or type mismatches, the source value
     *       overwrites the target value with a deep copy.</li>
     * </ol>
     * <p>
     * This method mutates the target node recursively. The source node is never altered.
     * </p>
     *
     * @param targetObj the target object to merge into (may be modified)
     * @param sourceObj the source object providing new data
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
                if (sourceValue != null && !sourceValue.isNull()) {
                    targetObj.set(key, sourceValue.deepCopy());
                }
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
                if (sourceValue != null && !sourceValue.isNull()) {
                    targetObj.set(key, sourceValue.deepCopy());
                }
            }
        }
    }

    /**
     * Merges two JSON arrays element by element. Each element is merged either by
     * aligning on an explicit {@code "index"} field (if present) or by using its
     * positional index in the source array.
     * <p>
     * This is critical for streaming chat completions and tool calls where individual
     * choices or function invocations may appear in different chunks and need to be
     * accumulated in the correct positions.
     * </p>
     * <p>
     * The target array is expanded with empty objects if an index exceeds its current
     * size, and each element is then deep-merged recursively if both are objects.
     * Non-object elements are replaced entirely by the source element.
     * </p>
     *
     * @param targetArray the target array to mutate (may be extended)
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