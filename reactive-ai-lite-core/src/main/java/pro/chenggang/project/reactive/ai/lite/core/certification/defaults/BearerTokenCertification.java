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
package pro.chenggang.project.reactive.ai.lite.core.certification.defaults;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;

import java.util.List;

/**
 * A {@link TokenCertification} implementation that authenticates HTTP requests using a
 * Bearer token according to the OAuth 2.0 Authorization Framework (RFC 6750).
 * <p>
 * This credential type is the most common mechanism for accessing LLM provider APIs
 * (e.g., OpenAI, Azure OpenAI). The {@link #applyTo(HttpHeaders)} method injects an
 * {@code Authorization: Bearer <token>} header into the provided {@code HttpHeaders}.
 * <p>
 * Instances are typically created via the {@link Builder} (provided by Lombok) and
 * are immutable thanks to the {@code @RequiredArgsConstructor(access = AccessLevel.PRIVATE)}
 * combined with {@code @Builder}. Multiple instances can coexist, each identified by a
 * distinct {@link #profile()} string, and one can be marked as the default via
 * {@link #isDefault}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see TokenCertification
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BearerTokenCertification implements TokenCertification {

    /**
     * A logical name for this certification configuration.
     * <p>
     * Used to distinguish between different API keys or providers in a multi‑provider
     * environment. When an application configuration specifies a profile name, the
     * framework can select the appropriate certification instance.
     */
    @NonNull
    private final String profile;

    /**
     * The raw authentication token (e.g., an API key or OAuth2 access token).
     * <p>
     * This value is used directly in the {@code Authorization} header and should be
     * treated as a sensitive secret. It must never be logged or exposed in plain text.
     */
    @NonNull
    private final String token;

    /**
     * Whether this certification should be treated as the default.
     * <p>
     * When no explicit profile is requested (e.g., in a single‑provider setup or when
     * a default is configured), the framework will use the default certification. In a
     * context with multiple certifications, exactly one should be marked as default.
     */
    private final boolean isDefault;

    /**
     * Returns the profile name that identifies this certification.
     * <p>
     * This name is typically defined in configuration (e.g., {@code spring.ai.openai.profile})
     * and allows the application to load the correct API key when multiple AI service
     * providers are configured.
     *
     * @return the profile identifier; never {@code null}
     */
    @Override
    public String profile() {
        return this.profile;
    }

    /**
     * Indicates whether this certification is the default among multiple configurations.
     * <p>
     * The default certification is used when no specific profile is selected, for example
     * when autoconfiguration creates a single bean without an explicit profile.
     *
     * @return {@code true} if this certification is the default, {@code false} otherwise
     */
    @Override
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Returns the name of the HTTP header that will carry the authentication token.
     * <p>
     * For Bearer tokens, the header name is always {@link HttpHeaders#AUTHORIZATION}
     * ("Authorization") as specified by RFC 7235 and the Bearer authentication scheme.
     * Other {@link TokenCertification} implementations may return different header names
     * (e.g., {@code x-api-key}).
     *
     * @return the header name, never {@code null}
     */
    @Override
    public String name() {
        return HttpHeaders.AUTHORIZATION;
    }

    /**
     * Returns the raw token value that will be placed in the authentication header.
     * <p>
     * The returned string is the exact API key or access token; it does <em>not</em>
     * include the bearer scheme prefix. The full header value is constructed by
     * {@link #applyTo(HttpHeaders)} as {@code "Bearer " + token()}.
     *
     * @return the authentication token; never {@code null}
     */
    @Override
    public String token() {
        return this.token;
    }

    /**
     * Applies the Bearer token to the supplied HTTP headers.
     * <p>
     * Adds an {@code Authorization} header with the value {@code "Bearer <token>"},
     * where {@code <token>} is obtained from {@link #token()}. This header value
     * conforms to the OAuth 2.0 Bearer Token Usage specification (RFC 6750).
     * <p>
     * Before calling this method, ensure that the {@code HttpHeaders} instance has
     * been properly initialized (e.g., from a {@link org.springframework.web.reactive.function.client.WebClient}).
     * Existing headers are preserved; only the {@code Authorization} header is overwritten.
     *
     * @param httpHeaders the mutable {@link HttpHeaders} to which the Bearer header will be added;
     *                    must not be {@code null}
     */
    public void applyTo(@NonNull HttpHeaders httpHeaders) {
        httpHeaders.put(name(), List.of("Bearer " + token()));
    }
}