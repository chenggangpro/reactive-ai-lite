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
package pro.chenggang.project.reactive.ai.lite.core.spec;

import pro.chenggang.project.reactive.ai.lite.core.execution.SystemOneExecution;

/**
 * Specification interface for SystemOne operations, acting as the entry point to a fluent,
 * reactive DSL for SystemOne interactions.
 * <p>
 * Decouples configuration from execution mechanics. By exposing {@link #general()}, it allows
 * callers to obtain a {@link SystemOneExecution} builder for configuring and executing requests.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 * @see SystemOneExecution
 */
public interface SystemOneSpec {

    /**
     * Returns a {@link SystemOneExecution} instance representing the general execution strategy
     * for SystemOne requests.
     *
     * @return a non-null {@link SystemOneExecution} instance
     */
    SystemOneExecution general();
}
