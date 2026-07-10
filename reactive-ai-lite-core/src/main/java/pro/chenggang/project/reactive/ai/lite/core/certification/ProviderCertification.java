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
package pro.chenggang.project.reactive.ai.lite.core.certification;

/**
 * Central abstraction for representing a provider’s certification (credential) configuration.
 * <p>
 * Implementations supply the identity of the profile to which this particular set of credentials
 * belongs and indicate whether the configuration should serve as the default for its provider.
 * The reactive AI lite framework relies on this interface to resolve the correct certification
 * based on the active profile; if no profile is explicitly selected, the provider’s default
 * certification is used. This decouples credential management from the core execution logic and
 * enables multi-environment or multi-tenant configurations for a single provider.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface ProviderCertification {

    /**
     * Returns the profile name associated with this provider certification.
     * <p>
     * The profile acts as a logical grouping or environment identifier for credentials
     * (e.g., {@code "default"}, {@code "development"}, {@code "production"}). During service
     * execution, the framework selects the appropriate certification by matching the request’s
     * active profile with the value returned here. Different implementations can supply the
     * same profile name for the same logical environment, but typically only one certification
     * per profile per provider is allowed.
     * </p>
     *
     * @return the profile name string; never {@code null}
     */
    String profile();

    /**
     * Indicates whether this provider certification is the designated default for its provider.
     * <p>
     * When multiple certifications are registered for a single provider, the framework falls
     * back to using the default when no explicit profile selection logic is provided. Only one
     * certification per provider should return {@code true} from this method. Marking a
     * certification as default ensures that a provider can always be used without requiring a
     * specific profile selection.
     * </p>
     *
     * @return {@code true} if this is the default certification, {@code false} otherwise
     */
    boolean isDefault();
}