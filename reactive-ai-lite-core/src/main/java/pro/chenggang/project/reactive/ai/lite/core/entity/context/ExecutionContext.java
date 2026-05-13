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
package pro.chenggang.project.reactive.ai.lite.core.entity.context;

import lombok.Getter;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.AbstractAttribute;

/**
 * Represents the mutable execution context for reactive AI operations.
 * <p>
 * This class manages the execution state and parsingAttributes during the lifecycle of an AI request.
 * It acts as a thread-safe container for storing contextual metadata and shared data across
 * interceptors and handlers. It also exposes a read-only {@link ExecutionContextView} to
 * safely expose its data to configuration functions.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
public class ExecutionContext extends AbstractAttribute {

    /**
     * A read-only view of the parsingAttributes contained within this execution context.
     * <p>
     * This view is used to safely expose the context data to dynamic configuration
     * functions without allowing modification of the underlying state.
     * </p>
     */
    private final ExecutionContextView contextView;

    /**
     * Constructs a new {@link ExecutionContext}.
     * <p>
     * It initializes the attribute map (inherited from {@link AbstractAttribute})
     * and creates the corresponding read-only view.
     * </p>
     */
    private ExecutionContext() {
        this.contextView = new ExecutionContextView(this.getAttributes());
    }

    /**
     * Creates a new instance of an {@link ExecutionContext}.
     * <p>
     * This factory method is the primary way to instantiate a fresh execution context
     * at the beginning of an AI operation.
     * </p>
     *
     * @return a new, empty {@link ExecutionContext} instance
     */
    public static ExecutionContext newContext() {
        return new ExecutionContext();
    }

}
