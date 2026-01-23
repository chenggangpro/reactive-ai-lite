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
 * Bearer token certification implementation that provides HTTP Authorization header with Bearer token authentication.
 * <p>
 * This class implements the {@link TokenCertification} interface to support Bearer token-based authentication
 * for HTTP requests. It encapsulates the profile, token, and default status information, and provides
 * functionality to apply the Bearer token to HTTP headers.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BearerTokenCertification implements TokenCertification {

    @NonNull
    private final String profile;

    @NonNull
    private final String token;

    private final boolean isDefault;

    @Override
    public String profile() {
        return this.profile;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public String name() {
        return HttpHeaders.AUTHORIZATION;
    }

    @Override
    public String token() {
        return this.token;
    }

    /**
     * Applies the Bearer token authentication to the provided HTTP headers.
     * <p>
     * This method adds the Authorization header with the Bearer token scheme to the given
     * {@link HttpHeaders} object. The format is "Bearer {token}".
     * </p>
     *
     * @param httpHeaders the HTTP headers to which the Bearer token will be applied, must not be {@code null}
     */
    public void applyTo(@NonNull HttpHeaders httpHeaders) {
        httpHeaders.put(name(), List.of("Bearer " + token()));
    }
}
