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
 * Base interface for all types of LLM client executions (e.g., general, streaming, structured).
 * <p>
 * This interface establishes a common contract for execution handlers, ensuring that
 * all handlers can expose their underlying configuration specification. It serves as
 * the root for the execution strategy hierarchy.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmClientExecution {

    /**
     * Returns the {@link ExecutionSpec} that configures this specific LLM execution.
     * <p>
     * The specification contains all the dynamically evaluated or statically configured
     * details necessary to build the final request to the provider, such as the model name,
     * temperature, messages, and selected profiles.
     * </p>
     *
     * @return the execution specification driving this handler
     */
    ExecutionSpec executionSpec();
}
