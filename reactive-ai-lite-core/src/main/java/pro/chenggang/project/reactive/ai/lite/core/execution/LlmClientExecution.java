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
package pro.chenggang.project.reactive.ai.lite.core.execution;

import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;

/**
 * Represents a contract for LLM client executions, providing access to the underlying execution specification.
 * Implementations of this interface are responsible for defining how an LLM operation is configured and executed.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmClientExecution {

    /**
     * Returns the {@link ExecutionSpec} that configures this LLM client execution.
     * The specification contains details about the provider, model, and other options.
     *
     * @return The execution specification.
     */
    ExecutionSpec executionSpec();
}
