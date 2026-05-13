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

import org.springframework.core.Ordered;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;

import java.util.Set;

/**
 * A marker interface defining the common contract for an interceptor that modifies
 * or inspects the execution flow of an LLM provider request.
 * <p>
 * Interceptors implement Spring's {@link Ordered} interface to define their relative
 * position in the execution chain. They can specify which types of LLM clients they
 * support (e.g., CHAT only, or all types) so that the framework can dynamically route
 * requests through the appropriate interceptors.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LLmProviderExecutionInterceptor extends Ordered {

    /**
     * Declares the set of client types that this interceptor supports.
     * <p>
     * An interceptor will only be applied to requests originating from a client type
     * included in this set. Returning an empty or null set usually indicates that it
     * supports no clients (and effectively disables the interceptor).
     * </p>
     *
     * @return a {@link Set} of {@link LlmClientType}s this interceptor can process
     */
    Set<LlmClientType> supportedClient();

}
