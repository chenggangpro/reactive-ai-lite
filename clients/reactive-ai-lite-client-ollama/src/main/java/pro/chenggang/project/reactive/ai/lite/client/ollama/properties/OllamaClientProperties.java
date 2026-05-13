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
package pro.chenggang.project.reactive.ai.lite.client.ollama.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration properties for Ollama client.
 * <p>
 * This class holds the configuration settings required to connect and authenticate
 * with Ollama's API services. It implements {@link InitializingBean} to perform
 * validation after all properties are set.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@Setter
public class OllamaClientProperties implements InitializingBean {

    /**
     * The prefix for all Ollama client properties in configuration files.
     */
    public static final String PREFIX = "reactive.ai.lite.client.ollama";

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
        Assert.hasLength(chatProvider.getBaseUrl(), "The base-url of Ollama chat API is required.");
        Assert.hasLength(chatProvider.getChatCompletionEndpoint(), "The chatCompletionEndpoint of Ollama chat API is required.");
        List<OllamaCertification> certifications = chatProvider.getCertifications();
        if (CollectionUtils.isEmpty(certifications)) {
            return;
        }
        int defaultCertificationCount = 0;
        Set<String> profiles = new HashSet<>();
        for (OllamaCertification certification : certifications) {
            Assert.hasText(certification.getToken(), "The token of Ollama certification is required.");
            Assert.hasText(certification.getProfile(), "The profile of Ollama certification is required.");
            if (certification.isDefault()) {
                defaultCertificationCount++;
            }
            profiles.add(certification.getProfile());
        }
        Assert.isTrue(defaultCertificationCount > 0, "At least one default Ollama certification is required.");
        Assert.isTrue(defaultCertificationCount == 1, "Only one default Ollama certification is allowed.");
        int certificationSize = certifications.size();
        Assert.isTrue(certificationSize <= 1 || profiles.size() == certificationSize, "All Ollama certification profiles must be unique.");
    }

    /**
     * Configuration properties for Ollama chat provider.
     * <p>
     * Contains the base URL, chatCompletionEndpoint, and certification details required
     * to connect to the Ollama chat API.
     * </p>
     */
    @Getter
    @Setter
    public static class ChatProvider {

        /**
         * The base URL for the Ollama API. Defaults to "http://localhost:11434".
         */
        private String baseUrl = "http://localhost:11434";
        /**
         * The chatCompletionEndpoint for the chat completions API. Defaults to "/api/chat".
         */
        private String chatCompletionEndpoint = "/api/chat";
        /**
         * A flag indicating if this chat provider is the default one. Defaults to true.
         */
        private boolean isDefault = true;
        /**
         * A list of authentication credentials for accessing the API.
         */
        private List<OllamaCertification> certifications = List.of();

        /**
         * A set of specific models that are allowed for chat completions. If not specified, all models are allowed.
         */
        private Set<String> limitedModels;
    }

    /**
     * Ollama certification configuration.
     * <p>
     * Represents authentication credentials and organizational information
     * for accessing Ollama's API. Each certification can be associated with
     * a specific profile and can be marked as the default certification.
     * </p>
     */
    @Getter
    @Setter
    public static class OllamaCertification {

        /**
         * A unique name for this certification profile. Defaults to "default-profile".
         */
        private String profile = "default-profile";
        /**
         * The API token for authentication with Ollama. This is a required field.
         */
        private String token;
        /**
         * A flag indicating if this is the default certification to use.
         * Exactly one certification must be marked as default. Defaults to false.
         */
        private boolean isDefault = false;
    }
}
