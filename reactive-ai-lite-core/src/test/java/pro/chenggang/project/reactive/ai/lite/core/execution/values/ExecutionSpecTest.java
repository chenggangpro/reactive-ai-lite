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
package pro.chenggang.project.reactive.ai.lite.core.execution.values;

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionSpecTest {

    @Test
    void testNewExecutionContext() {
        ExecutionSpec spec = ExecutionSpec.builder()
                .llmClientType(LlmClientType.CHAT)
                .modelNameConfigure(ctx -> "gpt-4")
                .parentAttributes(Collections.singletonMap("key", "val"))
                .contextConfigure(ctx -> ctx.getAttributes().put("custom", "config"))
                .build();

        ExecutionContext context = spec.newExecutionContext();
        assertThat(context.getAttributes()).containsEntry("key", "val")
                .containsEntry("custom", "config");
    }

    @Test
    void testNewExecutionInfo() {
        ExecutionSpec spec = ExecutionSpec.builder()
                .llmClientType(LlmClientType.CHAT)
                .modelNameConfigure(ctx -> "gpt-4")
                .build();

        ExecutionContext context = ExecutionContext.newContext();
        ExecutionInfo info = spec.newExecutionInfo(context);
        assertThat(info.getExecutionContext()).isEqualTo(context);
        assertThat(info.getModelNameConfigure()).isEqualTo(spec.getModelNameConfigure());
    }
}
