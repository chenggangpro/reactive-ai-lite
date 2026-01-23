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
 * Exception thrown when a specified profile for an LLM provider cannot be found.
 * This occurs if the requested profile name does not match any of the available profiles for the provider.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
public class NoProfileFoundLlmClientException extends LlmClientException {

    @Serial
    private static final long serialVersionUID = 3598018502894298913L;

    private final LlmProviderInfo llmProviderInfo;

    private final String pickedProfile;

    /**
     * Constructs a new NoProfileFoundLlmClientException when the picked profile is null.
     *
     * @param llmProviderInfo The information about the LLM provider.
     */
    public NoProfileFoundLlmClientException(@NonNull LlmProviderInfo llmProviderInfo) {
        super("No profile found for LLM provider: " + llmProviderInfo.name() + ". Cause the picked profile is null. Available profiles: " + llmProviderInfo.profiles());
        this.llmProviderInfo = llmProviderInfo;
        this.pickedProfile = null;
    }

    /**
     * Constructs a new NoProfileFoundLlmClientException with the specified provider info and picked profile.
     *
     * @param llmProviderInfo The information about the LLM provider.
     * @param pickedProfile   The name of the profile that was not found.
     */
    public NoProfileFoundLlmClientException(@NonNull LlmProviderInfo llmProviderInfo, @NonNull String pickedProfile) {
        super("No profile found for LLM provider: " + llmProviderInfo.name() + ". Picked profile: " + pickedProfile + ". Available profiles: " + llmProviderInfo.profiles());
        this.llmProviderInfo = llmProviderInfo;
        this.pickedProfile = pickedProfile;
    }
}
