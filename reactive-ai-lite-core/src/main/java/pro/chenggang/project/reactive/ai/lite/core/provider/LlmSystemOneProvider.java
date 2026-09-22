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
package pro.chenggang.project.reactive.ai.lite.core.provider;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SystemOneExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import reactor.core.publisher.Mono;

/**
 * Service provider interface for SystemOne operations within the reactive AI lite framework.
 * <p>
 * SystemOne models (such as TypeSafe AI's Jev) are specialized models designed for fast, structured
 * decision-making in software rather than generative text conversations. They evaluate unstructured data
 * (emails, code, log files) against predefined questions in parallel, outputting strongly typed
 * classifications and calibrated probabilities (e.g. Choice, Score, and Noul/Boolean responses) in 70–500ms.
 * </p>
 * <p>
 * This interface extends {@link LlmProvider} to integrate with the provider registry and execution
 * pipeline, defining the contract for executing both structured {@link SystemOneResponse} and
 * unparsed {@link RawResponse} payloads reactively via {@link Mono}.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProvider
 * @see SystemOneExecutionInfo
 * @see SystemOneResponse
 * @since 0.1.0
 */
public interface LlmSystemOneProvider extends LlmProvider {

    /**
     * Returns the primary capability of this provider, unconditionally {@link Capability#SYSTEM_ONE}.
     *
     * @return always {@link Capability#SYSTEM_ONE}
     */
    @Override
    default Capability capability() {
        return Capability.SYSTEM_ONE;
    }

    /**
     * Executes a SystemOne operation and returns a {@link Mono} emitting the structured result.
     * <p>
     * The provided {@link SystemOneExecutionInfo} carries the runtime configuration, including
     * resolved model names and raw request customizers. The returned {@link SystemOneResponse}
     * encapsulates the standardized output data.
     * </p>
     *
     * @param executionInfo the SystemOne execution configuration; must not be {@code null}
     * @return a {@link Mono} emitting the structured {@link SystemOneResponse} upon completion
     */
    Mono<SystemOneResponse> executeSystemOne(@NonNull SystemOneExecutionInfo executionInfo);

    /**
     * Executes a SystemOne operation and returns a {@link Mono} emitting the raw provider response.
     * <p>
     * This method bypasses higher-level domain parsing, providing direct access to the raw JSON
     * body via {@link RawResponse} for debugging, custom serialization, or provider-specific inspection.
     * </p>
     *
     * @param executionInfo the SystemOne execution configuration; must not be {@code null}
     * @return a {@link Mono} emitting the {@link RawResponse} with the raw provider payload
     */
    Mono<RawResponse> executeSystemOneRaw(@NonNull SystemOneExecutionInfo executionInfo);
}
