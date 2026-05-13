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
 * An implementation of {@link TokenCertification} that applies Bearer token authentication via HTTP headers.
 * <p>
 * This class encapsulates the profile, the raw token, and the default status. It provides
 * a specific mechanism to apply the token to HTTP requests by injecting an {@code Authorization: Bearer <token>}
 * header, which is the most common authentication method for LLM provider APIs.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BearerTokenCertification implements TokenCertification {

    /**
     * The profile name associated with this credential configuration.
     */
    @NonNull
    private final String profile;

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
     * <p>
     * For Bearer tokens, this is always "Authorization" ({@link HttpHeaders#AUTHORIZATION}).
     * </p>
     *
     * @return the header name
     */
    @Override
    public String name() {
        return HttpHeaders.AUTHORIZATION;
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
     * Applies the Bearer token authentication to the provided HTTP headers.
     * <p>
     * This method adds the {@code Authorization} header with the value format {@code Bearer <token>}.
     * </p>
     *
     * @param httpHeaders the Spring {@link HttpHeaders} object to modify
     */
    public void applyTo(@NonNull HttpHeaders httpHeaders) {
        httpHeaders.put(name(), List.of("Bearer " + token()));
    }
}
