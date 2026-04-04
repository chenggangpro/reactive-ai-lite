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
package pro.chenggang.project.reactive.ai.lite.core.exception;

import lombok.Getter;
import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.io.Serial;

/**
 * Exception thrown when a valid configuration profile cannot be resolved for a provider.
 * <p>
 * This occurs during request execution if the dynamic profile selection logic
 * (the picker function) returns null, or if it returns a profile name that does
 * not exist in the provider's registered {@link pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification}s.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
public class NoProfileFoundLlmClientException extends LlmClientException {

    /**
     * Unique serial version identifier.
     */
    @Serial
    private static final long serialVersionUID = 3598018502894298913L;

    /**
     * The provider info against which the profile lookup failed.
     */
    private final LlmProviderInfo llmProviderInfo;

    /**
     * The name of the profile that was requested but not found (may be null).
     */
    private final String pickedProfile;

    /**
     * Constructs a new exception indicating that the profile selection logic yielded null.
     *
     * @param llmProviderInfo the metadata of the provider that was selected
     */
    public NoProfileFoundLlmClientException(@NonNull LlmProviderInfo llmProviderInfo) {
        super("No profile found for LLM provider: " + llmProviderInfo.name() + ". Cause the picked profile is null. Available profiles: " + llmProviderInfo.profiles());
        this.llmProviderInfo = llmProviderInfo;
        this.pickedProfile = null;
    }

    /**
     * Constructs a new exception indicating that a specific requested profile does not exist.
     *
     * @param llmProviderInfo the metadata of the provider
     * @param pickedProfile   the specific profile name that could not be resolved
     */
    public NoProfileFoundLlmClientException(@NonNull LlmProviderInfo llmProviderInfo, @NonNull String pickedProfile) {
        super("No profile found for LLM provider: " + llmProviderInfo.name() + ". Picked profile: " + pickedProfile + ". Available profiles: " + llmProviderInfo.profiles());
        this.llmProviderInfo = llmProviderInfo;
        this.pickedProfile = pickedProfile;
    }
}
