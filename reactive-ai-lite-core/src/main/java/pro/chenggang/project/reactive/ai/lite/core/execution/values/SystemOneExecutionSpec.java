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

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestions;

import java.util.function.Function;

/**
 * Specification for configuring and building {@link SystemOneExecutionInfo} instances.
 * <p>
 * This class extends {@link ExecutionSpec} to provide execution-time resolution of provider,
 * profile, model, input state, questions schema, and raw request customization for SystemOne calls.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 * @see ExecutionSpec
 * @see SystemOneExecutionInfo
 */
@Getter
@SuperBuilder
public class SystemOneExecutionSpec extends ExecutionSpec<SystemOneExecutionInfo> {

    /**
     * Supplies the input state content for the SystemOne execution.
     */
    private final Function<ExecutionContext, ? extends SystemOneContent<?>> stateConfigure;

    /**
     * Supplies the questions schema for the SystemOne execution.
     */
    private final Function<ExecutionContext, SystemOneQuestions> questionsConfigure;

    /**
     * Constructs a new {@link SystemOneExecutionInfo} binding the configuration to the runtime
     * {@link ExecutionContext}.
     *
     * @param executionContext the runtime execution context; must not be null
     * @return a fully populated {@link SystemOneExecutionInfo} instance
     */
    @Override
    public SystemOneExecutionInfo newExecutionInfo(@NonNull ExecutionContext executionContext) {
        return SystemOneExecutionInfo.builder()
                .profilePicker(this.getProfilePicker())
                .defaultProfile(this.isDefaultProfile())
                .modelNameConfigure(this.getModelNameConfigure())
                .stateConfigure(this.getStateConfigure())
                .questionsConfigure(this.getQuestionsConfigure())
                .rawRequestCustomizerConfigure(this.getRawRequestCustomizerConfigure())
                .build();
    }
}
