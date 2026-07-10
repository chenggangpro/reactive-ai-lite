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
 * Defines a contract for interceptors that can observe or modify the execution flow
 * of a request to an LLM provider.
 * <p>
 * Implementations are ordered via Spring's {@link Ordered} interface, allowing the
 * framework to arrange them in a deterministic order. The {@link #supportedClient()}
 * method declares which LLM client types this interceptor is designed to work with.
 * This dual mechanism enables the framework to apply only the relevant interceptors
 * for each request, reducing unnecessary processing and making it possible to have
 * specialized interceptors for different LLM providers or client types.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see Ordered
 * @see LlmClientType
 */
public interface LlmProviderExecutionInterceptor extends Ordered {

    /**
     * Returns the set of {@link LlmClientType LLM client types} that this
     * interceptor is intended to handle.
     * <p>
     * During interception processing, the framework uses this information to decide
     * whether to include this interceptor in the chain for a particular request.
     * Returning an empty set or {@code null} effectively disables the interceptor,
     * as it will never match any client type. This filtering step improves performance
     * and allows developers to constrain an interceptor to specific providers without
     * having to put conditional logic inside the interceptor implementation.
     * </p>
     *
     * @return a set of supported client types, or {@code null}/empty set to indicate
     *         that this interceptor should not be applied
     */
    Set<LlmClientType> supportedClient();

}