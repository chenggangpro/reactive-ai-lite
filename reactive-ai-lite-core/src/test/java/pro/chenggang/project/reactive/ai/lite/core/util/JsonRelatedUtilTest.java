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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.exception.LlmClientException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonRelatedUtilTest {

    @Test
    void testJsonToMapWithValidJson() {
        String json = "{\"key\":\"value\",\"number\":123}";
        Map<String, Object> result = JsonRelatedUtil.jsonToMap(json);

        assertThat(result).isNotNull()
                .hasSize(2)
                .containsEntry("key", "value")
                .containsEntry("number", 123);
    }

    @Test
    void testJsonToMapWithNullJson() {
        Map<String, Object> result = JsonRelatedUtil.jsonToMap(null);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void testJsonToMapWithInvalidJson() {
        String invalidJson = "{invalid_json";

        assertThatThrownBy(() -> JsonRelatedUtil.jsonToMap(invalidJson))
                .isInstanceOf(LlmClientException.class)
                .hasMessageContaining("Failed to convert to map with json string");
    }

    @Test
    void testJsonToMapWithCustomMapper() {
        String json = "{\"custom\":\"mapper\"}";
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> result = JsonRelatedUtil.jsonToMap(json, mapper);

        assertThat(result).isNotNull()
                .hasSize(1)
                .containsEntry("custom", "mapper");
    }

    @Test
    void testEnumCoercion() throws Exception {
        // Since JsonRelatedUtil configures OBJECT_MAPPER to coerce empty strings to null for enums,
        // we should test this specific configuration.
        String json = "{\"testEnum\":\"\"}";
        
        TestDto result = JsonRelatedUtil.OBJECT_MAPPER.readValue(json, TestDto.class);
        
        assertThat(result).isNotNull();
        assertThat(result.getTestEnum()).isNull();
    }
    
    @Test
    void testEnumCoercionValidValue() throws Exception {
        String json = "{\"testEnum\":\"VALUE_ONE\"}";
        
        TestDto result = JsonRelatedUtil.OBJECT_MAPPER.readValue(json, TestDto.class);
        
        assertThat(result).isNotNull();
        assertThat(result.getTestEnum()).isEqualTo(TestEnum.VALUE_ONE);
    }

    static class TestDto {
        private TestEnum testEnum;

        public TestEnum getTestEnum() {
            return testEnum;
        }

        public void setTestEnum(TestEnum testEnum) {
            this.testEnum = testEnum;
        }
    }

    enum TestEnum {
        VALUE_ONE, VALUE_TWO
    }
}
