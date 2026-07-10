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

import com.fasterxml.jackson.databind.Module;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonUtilsTest {

    @Test
    void testInstantiateAvailableModules() {
        List<Module> modules = JacksonUtils.instantiateAvailableModules();
        assertThat(modules).isNotNull();
        
        // As long as jackson-datatype-jsr310 is in the classpath, JavaTimeModule should be present
        assertThat(modules)
            .extracting(m -> m.getClass().getSimpleName())
            .contains("JavaTimeModule");
    }
}
