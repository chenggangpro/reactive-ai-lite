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
 * Interface for provider certification configuration.
 * <p>
 * This interface defines the contract for provider certification implementations,
 * allowing different providers to specify their profile and default status.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface ProviderCertification {

    /**
     * Returns the profile name associated with this provider certification.
     * <p>
     * The profile typically identifies the specific configuration or environment
     * for the provider certification.
     * </p>
     *
     * @return the profile name as a String
     */
    String profile();

    /**
     * Indicates whether this provider certification is the default one.
     * <p>
     * When multiple provider certifications are available, the default certification
     * will be used when no specific certification is explicitly requested.
     * </p>
     *
     * @return {@code true} if this is the default provider certification, {@code false} otherwise
     */
    boolean isDefault();
}
