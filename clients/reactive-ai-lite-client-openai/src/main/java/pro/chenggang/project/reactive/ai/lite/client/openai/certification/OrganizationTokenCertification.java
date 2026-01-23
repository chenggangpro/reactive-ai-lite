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
 * Token certification implementation for OpenAI API with organization and project support.
 * This class provides authentication credentials including bearer token, optional organization ID,
 * and optional project ID for OpenAI API requests.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OrganizationTokenCertification implements TokenCertification {

    @NonNull
    private final String profile;

    @NonNull
    private final String token;

    private final String organizationId;

    private final String projectId;

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
     * Applies the authentication credentials to the provided HTTP headers.
     * This method sets the Authorization header with the bearer token and optionally
     * adds OpenAI-Organization and OpenAI-Project headers if they are configured.
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
