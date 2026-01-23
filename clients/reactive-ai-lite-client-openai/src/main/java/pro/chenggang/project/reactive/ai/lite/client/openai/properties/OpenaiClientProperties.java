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
package pro.chenggang.project.reactive.ai.lite.client.openai.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration properties for OpenAI client.
 * <p>
 * This class holds the configuration settings required to connect and authenticate
 * with OpenAI's API services. It implements {@link InitializingBean} to perform
 * validation after all properties are set.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
@Setter
public class OpenaiClientProperties implements InitializingBean {

    /**
     * The prefix for all OpenAI client properties in configuration files.
     */
    public static final String PREFIX = "reactive.ai.lite.client.openai";

    /**
     * Holds the configuration for the chat provider, including API endpoints and authentication.
     */
    private ChatProvider chatProvider = new ChatProvider();

    @Override
    public void afterPropertiesSet() throws Exception {
        this.checkChatProviderProperties(chatProvider);
    }

    /**
     * Performs validation checks on the {@link ChatProvider} properties.
     * It ensures that required fields like base URL, endpoint, and at least one
     * valid default certification are present.
     *
     * @param chatProvider The chat provider configuration to validate.
     */
    private void checkChatProviderProperties(ChatProvider chatProvider) {
        Assert.notNull(chatProvider, "The chat provider properties are required.");
        Assert.hasLength(chatProvider.getBaseUrl(), "The base-url of OpenAI chat API is required.");
        Assert.hasLength(chatProvider.getEndpoint(), "The endpoint of OpenAI chat API is required.");
        List<OpenaiCertification> certifications = chatProvider.getCertifications();
        Assert.notEmpty(certifications, "At least one OpenAI certification is required.");
        int defaultCertificationCount = 0;
        Set<String> profiles = new HashSet<>();
        for (OpenaiCertification certification : certifications) {
            Assert.hasText(certification.getToken(), "The token of OpenAI certification is required.");
            Assert.hasText(certification.getProfile(), "The profile of OpenAI certification is required.");
            if (certification.isDefault()) {
                defaultCertificationCount++;
            }
            profiles.add(certification.getProfile());
            if (Objects.nonNull(certification.getOrganizationId()) || Objects.nonNull(certification.getProjectId())) {
                Assert.hasText(certification.getOrganizationId(), "The organizationId is required when projectId is specified.");
                Assert.hasText(certification.getProjectId(), "The projectId is required when organizationId is specified.");
            }
        }
        Assert.isTrue(defaultCertificationCount > 0, "At least one default OpenAI certification is required.");
        Assert.isTrue(defaultCertificationCount == 1, "Only one default OpenAI certification is allowed.");
        int certificationSize = certifications.size();
        Assert.isTrue(certificationSize <= 1 || profiles.size() == certificationSize, "All OpenAI certification profiles must be unique.");
    }

    /**
     * Configuration properties for OpenAI chat provider.
     * <p>
     * Contains the base URL, endpoint, and certification details required
     * to connect to the OpenAI chat API.
     * </p>
     */
    @Getter
    @Setter
    public static class ChatProvider {

        /**
         * The base URL for the OpenAI API. Defaults to "https://api.openai.com".
         */
        private String baseUrl = "https://api.openai.com";
        /**
         * The endpoint for the chat completions API. Defaults to "/v1/chat/completions".
         */
        private String endpoint = "/v1/chat/completions";
        /**
         * A flag indicating if this chat provider is the default one. Defaults to true.
         */
        private boolean isDefault = true;
        /**
         * A list of authentication credentials for accessing the API.
         */
        private List<OpenaiCertification> certifications = List.of();
    }

    /**
     * OpenAI certification configuration.
     * <p>
     * Represents authentication credentials and organizational information
     * for accessing OpenAI's API. Each certification can be associated with
     * a specific profile and can be marked as the default certification.
     * </p>
     */
    @Getter
    @Setter
    public static class OpenaiCertification {

        /**
         * A unique name for this certification profile. Defaults to "default-profile".
         */
        private String profile = "default-profile";
        /**
         * The API token for authentication with OpenAI. This is a required field.
         */
        private String token;
        /**
         * The identifier of the organization associated with the API key.
         */
        private String organizationId;
        /**
         * The identifier of the project associated with the API key.
         */
        private String projectId;
        /**
         * A flag indicating if this is the default certification to use.
         * Exactly one certification must be marked as default. Defaults to false.
         */
        private boolean isDefault = false;
    }
}
