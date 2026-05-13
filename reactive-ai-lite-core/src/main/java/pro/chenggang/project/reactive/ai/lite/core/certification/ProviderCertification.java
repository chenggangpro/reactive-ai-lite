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
 * Base interface for provider certification configurations.
 * <p>
 * This interface defines the core contract that all provider certification
 * implementations must adhere to. It allows different credential setups to
 * self-report the profile they belong to and whether they act as the default
 * configuration for that provider.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface ProviderCertification {

    /**
     * Returns the profile name associated with this provider certification.
     * <p>
     * A profile acts as a grouping or environment identifier for credentials
     * (e.g., "default", "development", "production"). During execution, the
     * framework selects the appropriate certification based on the active profile.
     * </p>
     *
     * @return the profile name string
     */
    String profile();

    /**
     * Indicates whether this provider certification is the designated default.
     * <p>
     * When multiple credential profiles are registered for a single provider,
     * the framework falls back to using the default certification when no
     * explicit profile selection logic is provided by the user.
     * </p>
     *
     * @return {@code true} if this is the default certification, {@code false} otherwise
     */
    boolean isDefault();
}