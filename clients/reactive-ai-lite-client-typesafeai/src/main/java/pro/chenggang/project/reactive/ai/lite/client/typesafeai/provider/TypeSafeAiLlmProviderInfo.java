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
package pro.chenggang.project.reactive.ai.lite.client.typesafeai.provider;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.AbstractLlmProviderInfo;

/**
 * Concrete implementation of {@link AbstractLlmProviderInfo} that encapsulates the identity
 * and configuration metadata for the <a href="https://typesafe.ai">TypeSafe AI</a> language model provider.
 * <p>
 * Manages provider metadata including provider name, base URL, system one endpoint, supported
 * profiles, and default status within the reactive AI lite ecosystem.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see AbstractLlmProviderInfo
 * @since 0.1.0
 */
@SuperBuilder
public class TypeSafeAiLlmProviderInfo extends AbstractLlmProviderInfo {

    /**
     * The default provider name for TypeSafe AI.
     */
    public static final String DEFAULT_NAME = "TypeSafeAI";

    /**
     * The default base URL for TypeSafe AI API.
     */
    public static final String DEFAULT_BASE_URL = "https://api.typesafe.ai";

    /**
     * The default SystemOne endpoint for TypeSafe AI.
     */
    public static final String DEFAULT_SYSTEM_ONE_ENDPOINT = "/v1/systemone";

    /**
     * The unique name that identifies this TypeSafe AI provider.
     */
    @NonNull
    private final String name;

    /**
     * Retrieves the unique provider name.
     *
     * @return the provider name as configured, guaranteed not to be null
     */
    @Override
    public String name() {
        return this.name;
    }

}
