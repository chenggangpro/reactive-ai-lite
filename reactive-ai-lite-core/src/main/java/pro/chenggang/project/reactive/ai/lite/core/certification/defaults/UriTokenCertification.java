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
import org.springframework.web.util.UriBuilder;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;

/**
 * Implementation of {@link TokenCertification} that applies token authentication via URI query parameters.
 * <p>
 * This class provides a mechanism to add authentication tokens to URIs as query parameters.
 * It implements the TokenCertification interface and uses Lombok's builder pattern for construction.
 * </p>
 * <p>
 * The certification includes a profile identifier, parameter name, token value, and a default flag.
 * The token can be applied to any URI builder through the {@link #applyTo(UriBuilder)} method.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class UriTokenCertification implements TokenCertification {

    @NonNull
    private final String profile;

    @NonNull
    private final String name;

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
        return this.name;
    }

    @Override
    public String token() {
        return this.token;
    }

    /**
     * Applies the token certification to the provided URI builder by adding the token as a query parameter.
     * <p>
     * This method adds a query parameter to the URI using the configured name and token values.
     * The resulting URI will include the authentication token in its query string.
     * </p>
     *
     * @param uriBuilder the URI builder to which the token query parameter will be added; must not be null
     */
    public void applyTo(@NonNull UriBuilder uriBuilder) {
        uriBuilder.queryParam(name, token);
    }
}
