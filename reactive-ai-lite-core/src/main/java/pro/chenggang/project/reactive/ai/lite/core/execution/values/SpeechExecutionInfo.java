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

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Immutable configuration container holding dynamic value providers for building LLM text-to-speech requests.
 * <p>
 * Each function field is called during request construction with the current {@link ExecutionContext},
 * enabling runtime decisions about model selection, text to synthesize, voice, speed, and format.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ExecutionInfo
 * @see ExecutionContext
 */
@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SpeechExecutionInfo implements ExecutionInfo {

    /**
     * Flag indicating whether a default speech profile should be used when no explicit profile is resolved.
     */
    private final boolean defaultProfile;

    /**
     * Resolves the speech profile name from the context and the set of all available profiles.
     */
    private final BiFunction<ExecutionContext, Set<String>, String> profilePicker;

    /**
     * Supplies the speech model name for the current execution (e.g. tts-1).
     */
    @NonNull
    private final Function<ExecutionContext, String> modelNameConfigure;

    /**
     * Post-processor that can modify the raw JSON request body before it is sent to the LLM provider.
     */
    private final BiConsumer<ExecutionContext, ObjectNode> rawRequestCustomizerConfigure;

    /**
     * Dynamically provides the text to be synthesized into speech.
     */
    private final Function<ExecutionContext, String> inputTextConfigure;

    /**
     * Dynamically determines the voice to use (e.g., alloy, echo, fable, onyx, nova, and shimmer).
     */
    private final Function<ExecutionContext, String> voiceConfigure;

    /**
     * Dynamically determines the audio speed (0.25 to 4.0).
     */
    private final Function<ExecutionContext, Double> speedConfigure;

    /**
     * Dynamically determines the response format (e.g., mp3, opus, aac, flac).
     */
    private final Function<ExecutionContext, String> responseFormatConfigure;

}
