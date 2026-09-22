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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestions;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Immutable configuration container holding dynamic value providers for building SystemOne requests.
 * <p>
 * Each functional field is evaluated during request construction with the current {@link ExecutionContext},
 * enabling runtime decisions about profile selection, model selection, input state, questions schema,
 * and raw JSON payload customization.
 * </p>
 * <p>
 * Implements {@link ExecutionInfo} to bridge the request configuration layer with the execution engine.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 * @see ExecutionInfo
 * @see ExecutionContext
 */
@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemOneExecutionInfo implements ExecutionInfo {

    /**
     * Flag indicating whether the default profile should be used when no explicit profile is resolved.
     */
    private final boolean defaultProfile;

    /**
     * Resolves the profile name dynamically from the context and available profile names.
     */
    private final BiFunction<ExecutionContext, Set<String>, String> profilePicker;

    /**
     * Supplies the model name for the current SystemOne execution.
     */
    @NonNull
    private final Function<ExecutionContext, String> modelNameConfigure;

    /**
     * Supplies the input state content for the current SystemOne execution.
     */
    private final Function<ExecutionContext, ? extends SystemOneContent<?>> stateConfigure;

    /**
     * Supplies the questions schema for the current SystemOne execution.
     */
    private final Function<ExecutionContext, SystemOneQuestions> questionsConfigure;

    /**
     * Post-processor that can modify the raw JSON request body before dispatch to the provider.
     */
    private final BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;

}
