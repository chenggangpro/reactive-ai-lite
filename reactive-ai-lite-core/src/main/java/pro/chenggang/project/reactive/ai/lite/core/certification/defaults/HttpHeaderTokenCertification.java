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
 * A flexible {@link TokenCertification} implementation for injecting custom HTTP header-based authentication into requests.
 * <p>
 * Unlike the standard Bearer token model, many AI provider APIs (such as Anthropic’s Claude) expect the API key to be
 * sent in a header named <em>other</em> than {@code Authorization} (e.g., {@code x-api-key}) and with the raw token
 * as the header value without a prefix. This class decouples the header name from the token value, allowing the
 * {@link #name()} method to return the exact HTTP header name and {@link #token()} to return the plain‑text token.
 * </p>
 * <p>
 * Multiple instances can be created through the Lombok {@code @Builder} to support different profiles, with one
 * designated as the default via {@code isDefault}. The default instance is automatically picked up by the framework
 * when no explicit profile is requested. The builder provides a read‑friendly way to configure the required
 * {@code profile} and {@code token} fields while leaving {@code headerName} optional (defaults to {@code null}
 * if not set – the actual header name should be supplied unless the provider does not need one).
 * </p>
 * <p>
 * The {@link #applyTo(HttpHeaders)} convenience method directly adds the configured header with the token value
 * to the provided Spring HTTP headers, making integration with WebClient filters straightforward.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see TokenCertification
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpHeaderTokenCertification implements TokenCertification {

    /**
     * A unique identifier for this certification profile, used for selection when multiple configurations exist.
     * <p>
     * When calling a provider REST endpoint, the client can request a specific {@code profile} name; the framework
     * then resolves the matching {@link TokenCertification} instance. This field holds that distinguishing name.
     * </p>
     */
    @NonNull
    private final String profile;

    /**
     * The exact HTTP header name to be used for authentication, e.g. {@code "x-api-key"} or {@code "Authorization"}.
     * <p>
     * This is the value returned by {@link #name()}. If the provider expects the token in a header other than
     * “Authorization”, this field should be set accordingly. In the standard Bearer scenario this would be
     * “Authorization”, but the {@link HttpHeaderTokenCertification} gives full control over both name and value.
     * </p>
     */
    private final String headerName;

    /**
     * The raw authentication token, typically an API key or secret.
     * <p>
     * This value is placed directly as the header value when {@link #applyTo(HttpHeaders)} is called. No prefix
     * (like “Bearer”) is added, enabling direct use with providers that accept the key as‑is.
     * </p>
     */
    @NonNull
    private final String token;

    /**
     * Flag indicating whether this instance should be considered the default certification.
     * <p>
     * When multiple {@code HttpHeaderTokenCertification} beans are present, the one with {@code isDefault = true}
     * is automatically chosen by the framework unless a specific profile is requested. This avoids the need for
     * explicit profile selection in simple single‑provider setups.
     * </p>
     */
    private final boolean isDefault;

    /**
     * Returns the profile identifier assigned to this certification, enabling named lookup.
     * <p>
     * The profile name is used by the framework to resolve the correct authentication details when multiple
     * configurations are registered. It must be unique across all {@link TokenCertification} beans.
     * </p>
     *
     * @return the profile name, never {@code null}
     */
    @Override
    public String profile() {
        return this.profile;
    }

    /**
     * Tells the framework whether this instance serves as the fallback default certification.
     * <p>
     * If a consumer does not explicitly specify a profile, the default certification is used. This design
     * simplifies configuration for scenarios with only one AI provider or when one provider is overwhelmingly
     * dominant.
     * </p>
     *
     * @return {@code true} if this is the default, {@code false} otherwise
     */
    @Override
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Returns the header name that will be injected into HTTP requests, as defined by this certification.
     * <p>
     * This method deliberately overrides the contract of {@link TokenCertification#name()} to return the
     * HTTP header name directly, instead of a token type. For example, for Anthropic’s API this would be
     * {@code "x-api-key"}. The companion method {@link #token()} returns the raw token value, and together
     * they form the complete header entry added by {@link #applyTo(HttpHeaders)}.
     * </p>
     *
     * @return the HTTP header name, may be {@code null} if no header name was configured
     */
    @Override
    public String name() {
        return this.headerName;
    }

    /**
     * Returns the raw authentication token that will be placed into the custom header.
     * <p>
     * Unlike standard Bearer implementations, no “Bearer” prefix is prepended. The returned string is the
     * exact value that should appear as the header value. If the provider requires a prefix, it must be
     * included in the configured token itself.
     * </p>
     *
     * @return the plain‑text token, never {@code null}
     */
    @Override
    public String token() {
        return this.token;
    }

    /**
     * Convenience method that immediately sets the custom authentication header on the given Spring {@link HttpHeaders}.
     * <p>
     * The header name (obtained from {@link #name()}) and the raw token (from {@link #token()}) are added as a
     * single‑element list. This is the primary integration point: WebClient filters or interceptors simply call
     * this method to attach the credentials to outgoing requests.
     * </p>
     *
     * @param httpHeaders the mutable HTTP headers where the authentication header is to be inserted; must not be {@code null}
     */
    public void applyTo(@NonNull HttpHeaders httpHeaders) {
        httpHeaders.put(name(), List.of(token()));
    }
}