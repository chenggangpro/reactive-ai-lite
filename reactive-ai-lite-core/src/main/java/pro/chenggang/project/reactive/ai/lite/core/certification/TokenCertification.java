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
 * Token-based certification interface for AI provider authentication.
 * <p>
 * This interface extends {@link ProviderCertification} to provide a standard
 * token-based authentication mechanism for accessing AI service providers.
 * It defines methods to retrieve the certification's unique name and the
 * actual authentication token (e.g., an API key).
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface TokenCertification extends ProviderCertification {

    /**
     * Returns the name of this token certification.
     * <p>
     * The name uniquely identifies this specific credential configuration,
     * often corresponding to a specific provider or environment setup.
     * </p>
     *
     * @return the certification name
     */
    String name();

    /**
     * Returns the authentication token for this certification.
     * <p>
     * The token (such as an API key) is used to authenticate HTTP requests
     * sent to the AI service provider. It is highly sensitive and should
     * be handled securely, ensuring it is not exposed in application logs
     * or error messages.
     * </p>
     *
     * @return the authentication token string
     */
    String token();
}