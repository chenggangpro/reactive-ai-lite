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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

class JsonChunkMergerTest {

    @Test
    void testMergeSimple() {
        ObjectNode target = OBJECT_MAPPER.createObjectNode();
        target.put("text", "Hello");
        ObjectNode source = OBJECT_MAPPER.createObjectNode();
        source.put("text", " World");

        JsonChunkMerger.merge(target, source);
        assertThat(target.get("text").asText()).isEqualTo("Hello World");
    }

    @Test
    void testMergeNewField() {
        ObjectNode target = OBJECT_MAPPER.createObjectNode();
        target.put("a", 1);
        ObjectNode source = OBJECT_MAPPER.createObjectNode();
        source.put("b", 2);

        JsonChunkMerger.merge(target, source);
        assertThat(target.get("a").asInt()).isEqualTo(1);
        assertThat(target.get("b").asInt()).isEqualTo(2);
    }

    @Test
    void testMergeMetadata() {
        ObjectNode target = OBJECT_MAPPER.createObjectNode();
        target.put("model", "gpt-4");
        ObjectNode source = OBJECT_MAPPER.createObjectNode();
        source.put("model", "gpt-4");

        JsonChunkMerger.merge(target, source);
        assertThat(target.get("model").asText()).isEqualTo("gpt-4");
    }

    @Test
    void testMergeTakeLatest() {
        ObjectNode target = OBJECT_MAPPER.createObjectNode();
        target.put("created_at", "old");
        ObjectNode source = OBJECT_MAPPER.createObjectNode();
        source.put("created_at", "new");

        JsonChunkMerger.merge(target, source);
        assertThat(target.get("created_at").asText()).isEqualTo("new");
    }

    @Test
    void testMergeArraysPositional() {
        ObjectNode target = OBJECT_MAPPER.createObjectNode();
        ArrayNode targetArr = target.putArray("items");
        targetArr.addObject().put("val", "a");

        ObjectNode source = OBJECT_MAPPER.createObjectNode();
        ArrayNode sourceArr = source.putArray("items");
        sourceArr.addObject().put("val", "b");

        JsonChunkMerger.merge(target, source);
        assertThat(target.get("items").get(0).get("val").asText()).isEqualTo("ab");
    }

    @Test
    void testMergeArraysWithIndex() {
        ObjectNode target = OBJECT_MAPPER.createObjectNode();
        ArrayNode targetArr = target.putArray("items");
        targetArr.addObject().put("index", 0).put("val", "a");

        ObjectNode source = OBJECT_MAPPER.createObjectNode();
        ArrayNode sourceArr = source.putArray("items");
        sourceArr.addObject().put("index", 1).put("val", "b");

        JsonChunkMerger.merge(target, source);
        assertThat(target.get("items").size()).isEqualTo(2);
        assertThat(target.get("items").get(0).get("val").asText()).isEqualTo("a");
        assertThat(target.get("items").get(1).get("val").asText()).isEqualTo("b");
    }

    @Test
    void testMergeWithNullTargetValue() {
        ObjectNode target = OBJECT_MAPPER.createObjectNode();
        target.putNull("key");
        ObjectNode source = OBJECT_MAPPER.createObjectNode().put("key", "value");
        JsonChunkMerger.merge(target, source);
        assertThat(target.get("key").asText()).isEqualTo("value");
    }

    @Test
    void testMergeArraysWithNonObject() {
        ObjectNode target = OBJECT_MAPPER.createObjectNode();
        target.putArray("arr").add(1);
        ObjectNode source = OBJECT_MAPPER.createObjectNode();
        source.putArray("arr").add(2);
        JsonChunkMerger.merge(target, source);
        assertThat(target.get("arr").get(0).asInt()).isEqualTo(2);
    }
}
