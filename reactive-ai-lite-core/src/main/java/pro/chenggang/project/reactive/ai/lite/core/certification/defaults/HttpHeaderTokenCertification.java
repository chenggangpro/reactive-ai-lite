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
 * An implementation of {@link TokenCertification} that applies custom token authentication via HTTP headers.
 * <p>
 * This class encapsulates the profile, the header name, the raw token, and the default status.
 * It provides a flexible mechanism to apply the token to HTTP requests by injecting a customized
 * header, which is useful for providers that do not use the standard Bearer token format
 * (e.g., Anthropic's "x-api-key").
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpHeaderTokenCertification implements TokenCertification {

    /**
     * The profile name associated with this credential configuration.
     */
    @NonNull
    private final String profile;

    /**
     * The name of the HTTP header used for authentication.
     */
    private final String headerName;

    /**
     * The raw authentication token (API key).
     */
    @NonNull
    private final String token;

    /**
     * Indicates whether this certification is the default configuration.
     */
    private final boolean isDefault;

    /**
     * Returns the profile name.
     *
     * @return the profile string
     */
    @Override
    public String profile() {
        return this.profile;
    }

    /**
     * Returns whether this is the default certification.
     *
     * @return true if default, false otherwise
     */
    @Override
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Returns the name of the HTTP header used for authentication.
     *
     * @return the header name
     */
    @Override
    public String name() {
        return this.headerName;
    }

    /**
     * Returns the raw authentication token value.
     *
     * @return the token string
     */
    @Override
    public String token() {
        return this.token;
    }

    /**
     * Applies the custom token authentication to the provided HTTP headers.
     * <p>
     * This method adds the configured header name with the raw token value to the headers.
     * </p>
     *
     * @param httpHeaders the Spring {@link HttpHeaders} object to modify
     */
    public void applyTo(@NonNull HttpHeaders httpHeaders) {
        httpHeaders.put(name(), List.of(token()));
    }
}
