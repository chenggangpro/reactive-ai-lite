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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSchemaUtilTest {

    static class TestBean {
        public String name;
        public int age;
    }

    @Test
    void testGenerateForType() {
        String schema = JsonSchemaUtil.generateForType(TestBean.class);
        assertThat(schema).contains("\"type\" : \"object\"")
                .contains("\"name\"")
                .contains("\"age\"");
    }

    @Test
    void testUpperCaseTypeValues() {
        String schema = JsonSchemaUtil.generateForType(TestBean.class, JsonSchemaUtil.SchemaOption.UPPER_CASE_TYPE_VALUES);
        assertThat(schema).contains("\"type\" : \"OBJECT\"")
                .contains("\"type\" : \"STRING\"")
                .contains("\"type\" : \"INTEGER\"");
    }

    @Test
    void testGenerateForVoid() {
        String schema = JsonSchemaUtil.generateForType(Void.class);
        assertThat(schema).contains("properties");
    }

    @Test
    void testGenerateWithAllowAdditionalProperties() {
        String schema = JsonSchemaUtil.generateForType(TestBean.class, JsonSchemaUtil.SchemaOption.ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT);
        assertThat(schema).doesNotContain("\"additionalProperties\" : false");
    }
}
