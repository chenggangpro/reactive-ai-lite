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
 * An implementation of {@link TokenCertification} that applies token authentication via URI query parameters.
 * <p>
 * This class provides a mechanism to add authentication tokens (e.g., API keys) directly to URIs
 * as query parameters. It uses Lombok's builder pattern for construction and is typically
 * used when an AI provider expects the API key in the request URL rather than in an HTTP header.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class UriTokenCertification implements TokenCertification {

    /**
     * The profile name associated with this credential configuration.
     */
    @NonNull
    private final String profile;

    /**
     * The query parameter name for the token (e.g., "key", "api_key").
     */
    @NonNull
    private final String name;

    /**
     * The actual authentication token value.
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
     * Returns the name of the query parameter used for the token.
     *
     * @return the query parameter name
     */
    @Override
    public String name() {
        return this.name;
    }

    /**
     * Returns the authentication token value.
     *
     * @return the token string
     */
    @Override
    public String token() {
        return this.token;
    }

    /**
     * Applies this token certification to the provided URI builder by adding it as a query parameter.
     * <p>
     * This method appends a new query parameter to the URI using the configured {@link #name()}
     * as the key and the {@link #token()} as the value.
     * </p>
     *
     * @param uriBuilder the Spring {@link UriBuilder} to which the token will be added
     */
    public void applyTo(@NonNull UriBuilder uriBuilder) {
        uriBuilder.queryParam(name, token);
    }
}
