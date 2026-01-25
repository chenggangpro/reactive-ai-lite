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
package pro.chenggang.project.reactive.ai.lite.client.deepseek.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration properties for Deepseek client.
 * <p>
 * This class holds the configuration settings required to connect and authenticate
 * with Deepseek's API services. It implements {@link InitializingBean} to perform
 * validation after all properties are set.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
@Setter
public class DeepseekClientProperties implements InitializingBean {

    /**
     * The prefix for all Deepseek client properties in configuration files.
     */
    public static final String PREFIX = "reactive.ai.lite.client.deepseek";

    /**
     * Holds the configuration for the chat provider, including API endpoints and certifications.
     */
    private ChatProvider chatProvider = new ChatProvider();

    @Override
    public void afterPropertiesSet() throws Exception {
        this.checkChatProviderProperties(chatProvider);
    }

    /**
     * Performs validation checks on the {@link ChatProvider} properties.
     * It ensures that required fields like base URL, chatCompletionEndpoint, and at least one
     * valid default certification are present.
     *
     * @param chatProvider The chat provider configuration to validate.
     */
    private void checkChatProviderProperties(ChatProvider chatProvider) {
        Assert.notNull(chatProvider, "The chat provider properties are required.");
        Assert.hasLength(chatProvider.getBaseUrl(), "The base-url of Deepseek chat API is required.");
        Assert.hasLength(chatProvider.getChatCompletionEndpoint(), "The chatCompletionEndpoint of Deepseek chat API is required.");
        List<DeepseekCertification> certifications = chatProvider.getCertifications();
        Assert.notEmpty(certifications, "At least one Deepseek certification is required.");
        int defaultCertificationCount = 0;
        Set<String> profiles = new HashSet<>();
        for (DeepseekCertification certification : certifications) {
            Assert.hasText(certification.getToken(), "The token of Deepseek certification is required.");
            Assert.hasText(certification.getProfile(), "The profile of Deepseek certification is required.");
            if (certification.isDefault()) {
                defaultCertificationCount++;
            }
            profiles.add(certification.getProfile());
        }
        Assert.isTrue(defaultCertificationCount > 0, "At least one default Deepseek certification is required.");
        Assert.isTrue(defaultCertificationCount == 1, "Only one default Deepseek certification is allowed.");
        int certificationSize = certifications.size();
        Assert.isTrue(certificationSize <= 1 || profiles.size() == certificationSize, "All Deepseek certification profiles must be unique.");
    }

    /**
     * Configuration properties for Deepseek chat provider.
     * <p>
     * Contains the base URL, chatCompletionEndpoint, and certification details required
     * to connect to the Deepseek chat API.
     * </p>
     */
    @Getter
    @Setter
    public static class ChatProvider {

        /**
         * The base URL for the Deepseek API. Defaults to "https://api.deepseek.com".
         */
        private String baseUrl = "https://api.deepseek.com";
        /**
         * The chatCompletionEndpoint for the chat completions API. Defaults to "/chat/completions".
         */
        private String chatCompletionEndpoint = "/chat/completions";
        /**
         * A flag indicating if this chat provider is the default one. Defaults to true.
         */
        private boolean isDefault = true;
        /**
         * A list of authentication credentials for accessing the API.
         */
        private List<DeepseekCertification> certifications = List.of();
    }

    /**
     * Deepseek certification configuration.
     * <p>
     * Represents authentication credentials and organizational information
     * for accessing Deepseek's API. Each certification can be associated with
     * a specific profile and can be marked as the default certification.
     * </p>
     */
    @Getter
    @Setter
    public static class DeepseekCertification {

        /**
         * A unique name for this certification profile. Defaults to "default-profile".
         */
        private String profile = "default-profile";
        /**
         * The API token for authentication with Deepseek. This is a required field.
         */
        private String token;
        /**
         * A flag indicating if this is the default certification to use.
         * Exactly one certification must be marked as default. Defaults to false.
         */
        private boolean isDefault = false;
    }
}
