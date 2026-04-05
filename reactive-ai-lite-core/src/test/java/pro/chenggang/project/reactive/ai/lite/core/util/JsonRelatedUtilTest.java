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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonRelatedUtilTest {

    @Test
    void testObjectMapperConfig() {
        ObjectMapper mapper = JsonRelatedUtil.OBJECT_MAPPER;
        assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
        assertThat(mapper.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)).isTrue();
    }

    @Test
    void testJsonToMap() {
        String json = "{\"name\": \"test\", \"value\": 123}";
        Map<String, Object> map = JsonRelatedUtil.jsonToMap(json);
        assertThat(map).containsEntry("name", "test")
                .containsEntry("value", 123);
    }

    @Test
    void testJsonToMapInvalid() {
        String json = "invalid json";
        assertThatThrownBy(() -> JsonRelatedUtil.jsonToMap(json))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testEmptyStringToEnumAsNull() throws Exception {
        // Test coercion for empty strings to null for Enum types
        String json = "\"\"";
        LlmClientType result = JsonRelatedUtil.OBJECT_MAPPER.readValue(json, LlmClientType.class);
        assertThat(result).isNull();
    }
}
