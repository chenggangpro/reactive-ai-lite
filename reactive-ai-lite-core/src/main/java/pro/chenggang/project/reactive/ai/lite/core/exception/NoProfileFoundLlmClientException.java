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
 * Exception thrown when the framework cannot map a provider to a usable
 * {@link pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification}
 * because the required configuration profile is missing.
 * <p>
 * The root cause is always a failure in the dynamic profile resolution step:
 * either the profile‑picker function returned {@code null} (meaning no candidate
 * could be determined), or it returned a name that is not registered among the
 * provider's profiles. In both cases the exception carries the {@link LlmProviderInfo}
 * so that callers can inspect the available profiles and decide how to handle
 * the misconfiguration.
 * </p>
 * <p>
 * This exception is typically thrown from
 * {@link pro.chenggang.project.reactive.ai.lite.core.template.LlmClientTemplate}
 * at the moment a request is about to be executed, guaranteeing a clear failure
 * before any network round‑trip.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
public class NoProfileFoundLlmClientException extends LlmClientException {

    /**
     * Unique serial version identifier for {@link java.io.Serializable} compatibility.
     * Ensures that a deserialized exception matches the exact class definition used
     * at serialization time.
     */
    @Serial
    private static final long serialVersionUID = 3598018502894298913L;

    /**
     * The provider metadata that was being processed when the lookup failed.
     * Contains the provider's identifier and the full list of registered profiles.
     */
    private final LlmProviderInfo llmProviderInfo;

    /**
     * The specific profile name that could not be found, or {@code null}
     * when the picker function itself returned {@code null}.
     * <p>
     * When non‑null, the value is exactly the string that was returned by the
     * user‑supplied picker but was absent from {@link LlmProviderInfo#profiles()}.
     * </p>
     */
    private final String pickedProfile;

    /**
     * Constructs an exception for the case where the profile‑picker function
     * returned {@code null}, indicating that it could not determine any candidate
     * for the current provider.
     * <p>
     * The resulting message includes the provider name and a list of the
     * profiles that are actually available, aiding diagnostics. The
     * {@link #pickedProfile} field is set to {@code null} to differentiate
     * this scenario from a non‑existent named profile.
     * </p>
     *
     * @param llmProviderInfo the provider that was being resolved; must not be {@code null}
     */
    public NoProfileFoundLlmClientException(@NonNull LlmProviderInfo llmProviderInfo) {
        super("No profile found for LLM provider: " + llmProviderInfo.name() + ". Cause the picked profile is null. Available profiles: " + llmProviderInfo.profiles());
        this.llmProviderInfo = llmProviderInfo;
        this.pickedProfile = null;
    }

    /**
     * Constructs an exception for the case where the profile‑picker returned a
     * specific name that does not exist among the provider's registered profiles.
     * <p>
     * The message explicitly states the requested profile and the set of
     * available ones, making it easy to identify typos or configuration drift.
     * </p>
     *
     * @param llmProviderInfo the provider that was being resolved; must not be {@code null}
     * @param pickedProfile   the profile name that was returned but not found; must not be {@code null}
     */
    public NoProfileFoundLlmClientException(@NonNull LlmProviderInfo llmProviderInfo, @NonNull String pickedProfile) {
        super("No profile found for LLM provider: " + llmProviderInfo.name() + ". Picked profile: " + pickedProfile + ". Available profiles: " + llmProviderInfo.profiles());
        this.llmProviderInfo = llmProviderInfo;
        this.pickedProfile = pickedProfile;
    }
}