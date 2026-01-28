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
package pro.chenggang.project.reactive.ai.lite.core.provider.defaults;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.util.Objects;
import java.util.Set;

/**
 * An abstract base class for implementing {@link LlmProviderInfo}.
 * It provides common functionality for managing provider metadata, such as default status,
 * associated profiles, and a list of supported models.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Slf4j
@SuperBuilder
public abstract class AbstractLlmProviderInfo implements LlmProviderInfo {

    private final boolean isDefault;
    private final Set<String> profiles;
    private final Set<String> supportedModels;

    protected AbstractLlmProviderInfo(boolean isDefault, @NonNull Set<String> profiles, Set<String> supportedModels) {
        this.isDefault = isDefault;
        this.profiles = profiles;
        if (Objects.nonNull(supportedModels) && !supportedModels.isEmpty()) {
            this.supportedModels = Set.copyOf(supportedModels);
        } else {
            log.info("No supported models provided. Assuming all models are supported.");
            this.supportedModels = Set.of();
        }
    }

    @Override
    public boolean supportModel(@NonNull String modelName) {
        if (supportedModels.isEmpty()) {
            return true;
        }
        return supportedModels.contains(modelName);
    }

    @Override
    public boolean isDefault() {
        return this.isDefault;
    }

    @Override
    public Set<String> profiles() {
        return this.profiles;
    }

    @Override
    public String toString() {
        return "LlmProviderInfo{name=" + name() + ", profiles=" + profiles + ", supportedModels=" + supportedModels + ", isDefault=" + isDefault + "}";
    }
}
