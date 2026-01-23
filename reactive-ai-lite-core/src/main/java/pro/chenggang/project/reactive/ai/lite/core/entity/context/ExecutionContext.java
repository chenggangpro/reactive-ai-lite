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
import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.entity.AttributesAbility;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.TraceId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the execution context for reactive AI operations.
 * <p>
 * This class manages the execution state and attributes during AI operation processing.
 * It provides a thread-safe container for storing contextual information and maintains
 * a unique trace identifier for tracking purposes.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public class ExecutionContext implements AttributesAbility {

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    @Getter
    private final TraceId traceId;

    @Getter
    private final ExecutionContextView contextView;

    private ExecutionContext(@NonNull TraceId traceId) {
        this.traceId = traceId;
        this.contextView = new ExecutionContextView(this);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    /**
     * Creates a new ExecutionContext instance with the specified trace identifier.
     * <p>
     * This factory method is the primary way to instantiate an ExecutionContext.
     * Each context is initialized with an empty attributes map and a read-only view.
     * </p>
     *
     * @param newTraceId the unique trace identifier for the new execution context, must not be null
     * @return a new ExecutionContext instance initialized with the provided trace identifier
     */
    public static ExecutionContext newContextWith(@NonNull TraceId newTraceId) {
        return new ExecutionContext(newTraceId);
    }

}
