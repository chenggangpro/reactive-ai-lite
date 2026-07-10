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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JsonSchemaUtilTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testGenerateForTypeNullType() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> JsonSchemaUtil.generateForType(null))
                .withMessage("type cannot be null");
    }

    @Test
    void testGenerateForTypeVoid() throws Exception {
        String schemaStr = JsonSchemaUtil.generateForType(Void.class);
        JsonNode schemaNode = mapper.readTree(schemaStr);

        assertThat(schemaNode.has("properties")).isTrue();
        assertThat(schemaNode.get("properties").isEmpty()).isTrue();
        assertThat(schemaNode.get("additionalProperties").asBoolean()).isFalse();
    }

    @Test
    void testGenerateForTypeDefaultOptions() throws Exception {
        String schemaStr = JsonSchemaUtil.generateForType(TestClass.class);
        JsonNode schemaNode = mapper.readTree(schemaStr);

        assertThat(schemaNode.get("type").asText()).isEqualTo("object");
        assertThat(schemaNode.get("properties").has("name")).isTrue();
        assertThat(schemaNode.get("properties").get("name").get("type").asText()).isEqualTo("string");
        assertThat(schemaNode.get("additionalProperties").asBoolean()).isFalse();
    }

    @Test
    void testGenerateForTypeAllowAdditionalProperties() throws Exception {
        String schemaStr = JsonSchemaUtil.generateForType(TestClass.class, JsonSchemaUtil.SchemaOption.ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT);
        JsonNode schemaNode = mapper.readTree(schemaStr);

        assertThat(schemaNode.has("additionalProperties")).isFalse(); // when allowed, it's typically omitted or not false
    }

    @Test
    void testGenerateForTypeUpperCaseTypeValues() throws Exception {
        String schemaStr = JsonSchemaUtil.generateForType(TestClass.class, JsonSchemaUtil.SchemaOption.UPPER_CASE_TYPE_VALUES);
        JsonNode schemaNode = mapper.readTree(schemaStr);

        assertThat(schemaNode.get("type").asText()).isEqualTo("OBJECT");
        assertThat(schemaNode.get("properties").get("name").get("type").asText()).isEqualTo("STRING");
        assertThat(schemaNode.get("properties").get("items").get("type").asText()).isEqualTo("ARRAY");
        assertThat(schemaNode.get("properties").get("items").get("items").get("type").asText()).isEqualTo("STRING");
    }

    @Test
    void testConvertTypeValuesToUpperCaseArray() throws Exception {
        String json = "[{\"type\":\"string\"}, {\"type\":\"integer\"}]";
        JsonNode node = mapper.readTree(json);
        
        JsonSchemaUtil.convertTypeValuesToUpperCase(node);
        
        assertThat(node.get(0).get("type").asText()).isEqualTo("STRING");
        assertThat(node.get(1).get("type").asText()).isEqualTo("INTEGER");
    }

    static class TestClass {
        @JsonProperty(required = true)
        private String name;
        private java.util.List<String> items;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public java.util.List<String> getItems() { return items; }
        public void setItems(java.util.List<String> items) { this.items = items; }
    }
}
