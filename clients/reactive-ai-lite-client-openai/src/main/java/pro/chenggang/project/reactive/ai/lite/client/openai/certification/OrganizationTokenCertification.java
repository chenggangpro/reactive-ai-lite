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
package pro.chenggang.project.reactive.ai.lite.client.openai.certification;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;

import java.util.List;

/**
 * Token certification for OpenAI API requests with optional organization and project scoping.
 * This implementation extends {@link TokenCertification} to include the standard Bearer token
 * for authentication, and adds support for OpenAI-specific {@code OpenAI-Organization} and
 * {@code OpenAI-Project} headers. When these optional fields are provided, they are included
 * in the HTTP request headers to scope the API call to a particular organization or project
 * within the OpenAI ecosystem.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OrganizationTokenCertification implements TokenCertification {

    /**
     * Configuration profile name that uniquely identifies this certification instance.
     */
    @NonNull
    private final String profile;

    /**
     * Bearer token used for authenticating with the OpenAI API.
     */
    @NonNull
    private final String token;

    /**
     * Optional OpenAI organization identifier. When set, the {@code OpenAI-Organization} header
     * is added to API requests, scoping the call to that organization.
     */
    private final String organizationId;

    /**
     * Optional OpenAI project identifier. When set, the {@code OpenAI-Project} header
     * is added to API requests, scoping the call to that project.
     */
    private final String projectId;

    /**
     * Whether this certification profile is the default among all configured profiles.
     */
    private final boolean isDefault;

    /**
     * Returns the profile name associated with this certification.
     *
     * @return the profile name
     */
    @Override
    public String profile() {
        return this.profile;
    }

    /**
     * Indicates whether this profile is the default authentication.
     *
     * @return true if this is the default profile, false otherwise
     */
    @Override
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Provides the header name used for the authentication token.
     * For OpenAI, this is always "Authorization" as per HTTP standard.
     *
     * @return the header name ("Authorization")
     */
    @Override
    public String name() {
        return HttpHeaders.AUTHORIZATION;
    }

    /**
     * Returns the raw Bearer token string.
     *
     * @return the authentication token
     */
    @Override
    public String token() {
        return this.token;
    }

    /**
     * Applies the authentication credentials to the given HTTP headers.
     * <p>This method sets the Authorization header to "Bearer " followed by the token.
     * Additionally, if an organization ID or project ID is configured, the corresponding
     * OpenAI-specific headers ({@code OpenAI-Organization} and {@code OpenAI-Project}) are added.
     * The organization and project headers allow the API call to be scoped to a specific
     * organizational or project context within an OpenAI account.</p>
     *
     * @param httpHeaders the HTTP headers to which the authentication credentials will be applied
     */
    public void applyTo(@NonNull HttpHeaders httpHeaders) {
        httpHeaders.put(name(), List.of("Bearer " + token()));
        if (StringUtils.hasText(organizationId)) {
            httpHeaders.put("OpenAI-Organization", List.of(organizationId));
        }
        if (StringUtils.hasText(projectId)) {
            httpHeaders.put("OpenAI-Project", List.of(projectId));
        }
    }
}