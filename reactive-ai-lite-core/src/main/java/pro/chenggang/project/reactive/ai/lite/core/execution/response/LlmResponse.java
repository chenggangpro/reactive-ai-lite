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
package pro.chenggang.project.reactive.ai.lite.core.execution.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;

/**
 * Base abstract class representing a response from a Large Language Model (LLM) execution.
 * <p>
 * This class serves as the root of the response hierarchy for all types of LLM interactions
 * (e.g., general, streaming, structured). It ensures that every response object carries
 * a read-only view of the execution context that generated it, allowing downstream consumers
 * to access metadata, correlation IDs, or other parsingAttributes associated with the request.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@SuperBuilder
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class LlmResponse {

    /**
     * A read-only view of the execution context that was used to generate this response.
     * <p>
     * This allows tracing back the response to the specific request configurations
     * and parsingAttributes present at execution time.
     * </p>
     */
    protected final ExecutionContextView contextView;

}
