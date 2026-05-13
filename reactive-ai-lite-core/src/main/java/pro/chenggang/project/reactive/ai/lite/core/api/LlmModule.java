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
package pro.chenggang.project.reactive.ai.lite.core.api;

import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;


/**
 * Represents a module for Large Language Model (LLM) integration.
 * <p>
 * This interface defines the contract for LLM modules, providing access to the
 * specific type of LLM client implementation being used.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmModule {

    /**
     * Returns the type of LLM client associated with this module.
     * <p>
     * This method identifies which LLM client implementation is being used,
     * allowing for type-specific handling and configuration.
     * </p>
     *
     * @return the {@link LlmClientType} representing the client type of this module
     */
    LlmClientType type();
}
