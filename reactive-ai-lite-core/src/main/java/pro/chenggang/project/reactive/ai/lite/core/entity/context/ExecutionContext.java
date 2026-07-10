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
import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.reactive.ai.lite.core.api.ClientRequest.ContextMerger;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.AbstractAttribute;
import reactor.util.context.Context;

import java.util.Map;
import java.util.Objects;

/**
 * Mutable execution context that orchestrates state and attribute sharing during the lifecycle
 * of a reactive AI request.
 * <p>
 * The {@code ExecutionContext} serves as the central, thread-safe container for all contextual information
 * flowing through the reactive pipeline. It extends {@link AbstractAttribute} to inherit a concurrent
 * map of key-value attributes, enabling interceptor, provider, and execution engine components to
 * exchange data dynamically without coupling. The mutable nature supports progressive enrichment of the
 * context as the request evolves, while the Reactor {@link Context} binding guarantees safe propagation
 * across reactive operators.
 * </p>
 * <p>
 * Typical usage involves creating a fresh context at the start of a request, populating it with initial
 * attributes (e.g., trace ID, user properties), and then using the {@link #initializeExecutionContext(Context, Map, ContextMerger)}
 * utility to integrate it into the reactive chain. The context can be read or updated at any stage by
 * subscribing to the Reactor context, making it the backbone for cross-cutting concerns like logging,
 * monitoring, and audit.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see AbstractAttribute
 * @see Context
 * @see ContextMerger
 */
@Slf4j
@Getter
public class ExecutionContext extends AbstractAttribute {

    /**
     * Private constructor to enforce factory-based instantiation.
     * <p>
     * All instances are created via the static {@link #newContext()} method to guarantee a consistent
     * initialization path. The constructor only invokes the superclass constructor to set up the
     * underlying {@link java.util.concurrent.ConcurrentHashMap}-based attribute store.
     * </p>
     */
    private ExecutionContext() {
    }

    /**
     * Creates a new, empty {@link ExecutionContext}.
     * <p>
     * This factory method is the entry point for acquiring a fresh context at the beginning of an
     * AI request pipeline. The returned context contains no attributes and is fully thread-safe,
     * ready to be populated and bound to the reactive chain.
     * </p>
     *
     * @return a new {@link ExecutionContext} instance with an empty attribute map
     */
    public static ExecutionContext newContext() {
        return new ExecutionContext();
    }

    /**
     * Ensures the presence of an {@link ExecutionContext} in the Reactor {@link Context} and merges
     * parent attributes if applicable.
     * <p>
     * This method performs a critical initialization step before the request pipeline starts. It attempts
     * to retrieve an existing {@code ExecutionContext} from the reactor context; if one already exists
     * (for example, set by an outer operator), it copies all its attributes into a new context to preserve
     * historical state. Then, if a {@link ContextMerger} is provided, it merges the given
     * {@code parentAttributes} (often coming from a parent request or a default configuration) into the
     * execution context, allowing custom override or enrichment logic. Finally, the (possibly updated)
     * context is put back into the reactor context for downstream consumers.
     * </p>
     * <p>
     * This design supports both shared (nested) request scenarios where an outer context may already exist,
     * and standalone scenarios where a brand-new context is needed. The merger abstraction decouples the
     * merging strategy from the core framework.
     * </p>
     *
     * @param context          the current Reactor {@link Context} at the point of initialization
     * @param parentAttributes potential attributes from a parent request or configuration (nullable)
     * @param contextConfigure a merger to combine parent attributes into the execution context (nullable)
     * @return the Reactor {@link Context} augmented with the initialized {@link ExecutionContext}
     */
    public static Context initializeExecutionContext(Context context, Map<String, Object> parentAttributes, ContextMerger contextConfigure) {
        ExecutionContext executionContext = ExecutionContext.newContext();
        context.<ExecutionContext>getOrEmpty(ExecutionContext.class)
                .ifPresent(existing -> executionContext.getAttributes().putAll(existing.getAttributes()));
        if (Objects.nonNull(contextConfigure)) {
            contextConfigure.merge(executionContext, parentAttributes);
        }
        log.debug("Initial execution context: {}", executionContext);
        return context.put(ExecutionContext.class, executionContext);
    }

    /**
     * Returns a concise, human-readable representation of the execution context.
     * <p>
     * The string includes the simple class name, the object's identity hash code, and the number of
     * stored attributes. This format aids debugging by quickly revealing the context identity and its
     * cardinality without exposing sensitive attribute data. The hash code helps distinguish between
     * multiple context instances in log trails.
     * </p>
     *
     * @return a string representation of the form {@code ExecutionContext@1a2b3c4d with 5 attributes}
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(this.hashCode()) + " with " + this.getAttributes().size() + " attributes";
    }
}