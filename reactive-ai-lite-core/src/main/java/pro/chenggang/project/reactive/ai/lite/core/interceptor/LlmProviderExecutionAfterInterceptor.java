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
package pro.chenggang.project.reactive.ai.lite.core.interceptor;

import reactor.core.publisher.Mono;

/**
 * The Llm provider execution after interceptor.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmProviderExecutionAfterInterceptor extends LLmProviderExecutionInterceptor {

    /**
     * Intercept the execution of a llm provider <i>after</i> its successful invocation.
     * <p/>
     * <li>This execution will invoke in <b>DESCENDING</b> order.</li>
     *
     * @param exchange the current llm provider exchange
     * @param chain    the chain
     * @return the {@code Mono<Void>} to indicate when exchange processing is complete
     */
    Mono<Void> interceptAfter(LlmProviderExchange exchange, LlmProviderInterceptorChain chain);

}
