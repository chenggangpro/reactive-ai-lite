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

import java.util.function.Function;

/**
 * Provides the specification for configuring and building {@link SpeechExecutionInfo} instances
 * used to invoke speech model services. This spec extends the base {@link ExecutionSpec} and adds
 * speech-specific configuration capabilities.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ExecutionSpec
 * @see SpeechExecutionInfo
 */
@Getter
@SuperBuilder
public class SpeechExecutionSpec extends ExecutionSpec<SpeechExecutionInfo> {

    /**
     * A function that dynamically extracts the text to be synthesized from the 
     * {@link ExecutionContext} at the time of execution.
     */
    private final Function<ExecutionContext, String> inputTextConfigure;

    /**
     * A function that determines the desired voice from the 
     * {@link ExecutionContext} at execution time.
     */
    private final Function<ExecutionContext, String> voiceConfigure;

    /**
     * A function that determines the desired speed from the 
     * {@link ExecutionContext} at execution time.
     */
    private final Function<ExecutionContext, Double> speedConfigure;

    /**
     * A function that determines the desired response format from the 
     * {@link ExecutionContext} at execution time.
     */
    private final Function<ExecutionContext, String> responseFormatConfigure;

    /**
     * Constructs a new {@link SpeechExecutionInfo} by transferring all configured 
     * functions and profile settings from this specification.
     *
     * @param executionContext the current execution context; must not be null.
     * @return a fully configured {@link SpeechExecutionInfo}
     */
    @Override
    public SpeechExecutionInfo newExecutionInfo(@NonNull ExecutionContext executionContext) {
        return SpeechExecutionInfo.builder()
                .profilePicker(this.getProfilePicker())
                .defaultProfile(this.isDefaultProfile())
                .modelNameConfigure(this.getModelNameConfigure())
                .rawRequestCustomizerConfigure(this.getRawRequestCustomizerConfigure())
                .inputTextConfigure(this.inputTextConfigure)
                .voiceConfigure(this.voiceConfigure)
                .speedConfigure(this.speedConfigure)
                .responseFormatConfigure(this.responseFormatConfigure)
                .build();
    }
}
