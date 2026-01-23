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
 * This interface extends {@link ProviderCertification} to provide token-based
 * authentication mechanism for AI service providers. It defines methods to
 * retrieve the certification name and the authentication token.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface TokenCertification extends ProviderCertification {

    /**
     * Returns the name of this token certification.
     * <p>
     * The name typically identifies the certification configuration or
     * the associated AI provider.
     * </p>
     *
     * @return the certification name
     */
    String name();

    /**
     * Returns the authentication token for this certification.
     * <p>
     * The token is used to authenticate requests to the AI service provider.
     * This should be kept secure and not exposed in logs or error messages.
     * </p>
     *
     * @return the authentication token
     */
    String token();
}
