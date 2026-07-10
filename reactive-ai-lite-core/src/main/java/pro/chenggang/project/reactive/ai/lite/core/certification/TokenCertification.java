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
 * Defines a token-based certification used for authenticating requests to AI service providers.
 * <p>
 * This interface extends {@link ProviderCertification} to support a common authentication
 * pattern where an opaque token (such as an API key or bearer token) is presented to the
 * provider to prove identity and authorize access. Implementations typically hold the
 * token securely and expose it for inclusion in HTTP Authorization headers.
 * <p>
 * The {@link #name()} method provides an identifier for the certification instance,
 * allowing applications with multiple providers or environments (e.g., development,
 * staging, production) to distinguish between credentials easily. The {@link #token()}
 * method returns the actual secret material; therefore, implementations must guard
 * against accidental logging or exposure.
 * <p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ProviderCertification
 */
public interface TokenCertification extends ProviderCertification {

    /**
     * Returns the logical name of this token certification.
     * <p>
     * This name is used to uniquely identify a set of credentials within an application
     * context. For example, it might correspond to a provider key ("openai", "azure") or
     * a profile name ("default", "admin"). The value is often derived from configuration
     * properties and helps route the correct token to the correct provider at runtime.
     *
     * @return a non-null, non-empty string identifying the certification
     */
    String name();

    /**
     * Provides the actual authentication token (e.g., API key, JWT) to be sent to the
     * AI service provider.
     * <p>
     * This method returns the secret credential that the framework will embed into
     * outgoing HTTP requests—typically as a {@code Bearer} token in the
     * {@code Authorization} header or as a custom header (such as {@code api-key}).
     * Because the returned value is highly sensitive, implementations must ensure it
     * is never written to logs, error messages, or serialized in plaintext without
     * explicit sanitization.
     *
     * @return the raw authentication token; never {@code null}
     */
    String token();
}