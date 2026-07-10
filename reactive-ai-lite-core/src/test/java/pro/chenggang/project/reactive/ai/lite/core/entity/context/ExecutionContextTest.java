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

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.api.ClientRequest.ContextMerger;
import reactor.util.context.Context;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionContextTest {

    @Test
    void testExecutionContextCreation() {
        ExecutionContext context = ExecutionContext.newContext();
        assertThat(context.getAttributes()).isEmpty();

        context.getAttributes().put("key1", "value1");
        assertThat(context.<String>getAttribute("key1")).isEqualTo("value1");
        assertThat(context.<String>getAttribute("key2")).isNull();

        assertThat(context.getAttributeOrDefault("key2", "default2")).isEqualTo("default2");
    }

    @Test
    void testAttributesStreamAndForEach() {
        ExecutionContext context = ExecutionContext.newContext();
        context.getAttributes().put("a", 1);
        context.getAttributes().put("b", 2);

        AtomicInteger sum = new AtomicInteger();
        context.attributesStream().forEach(entry -> sum.addAndGet((Integer) entry.getValue()));
        assertThat(sum.get()).isEqualTo(3);

        AtomicInteger sum2 = new AtomicInteger();
        context.forEachAttribute((k, v) -> sum2.addAndGet((Integer) v));
        assertThat(sum2.get()).isEqualTo(3);
    }

    @Test
    void testInitializeExecutionContext_NewContext() {
        Context reactorContext = Context.empty();
        ContextMerger merger = (ctx, parentAttrs) -> {
            ctx.getAttributes().put("merged", "true");
            if (parentAttrs != null) {
                ctx.getAttributes().putAll(parentAttrs);
            }
        };

        Context newReactorContext = ExecutionContext.initializeExecutionContext(reactorContext, Map.of("parent", "parent_val"), merger);

        ExecutionContext resultCtx = newReactorContext.get(ExecutionContext.class);
        assertThat(resultCtx).isNotNull();
        assertThat(resultCtx.<String>getAttribute("merged")).isEqualTo("true");
        assertThat(resultCtx.<String>getAttribute("parent")).isEqualTo("parent_val");
        assertThat(resultCtx.toString()).contains("ExecutionContext", "attributes");
    }

    @Test
    void testInitializeExecutionContext_ExistingContext() {
        ExecutionContext existing = ExecutionContext.newContext();
        existing.getAttributes().put("existing_key", "existing_val");
        Context reactorContext = Context.of(ExecutionContext.class, existing);

        Context newReactorContext = ExecutionContext.initializeExecutionContext(reactorContext, null, null);

        ExecutionContext resultCtx = newReactorContext.get(ExecutionContext.class);
        assertThat(resultCtx).isNotNull();
        assertThat(resultCtx).isNotSameAs(existing);
        assertThat(resultCtx.<String>getAttribute("existing_key")).isEqualTo("existing_val");
    }
}
