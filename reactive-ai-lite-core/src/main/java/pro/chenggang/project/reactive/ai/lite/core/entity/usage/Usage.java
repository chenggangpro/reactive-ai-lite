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
package pro.chenggang.project.reactive.ai.lite.core.entity.usage;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * Defines the contract for accessing token usage information from an AI model response.
 * <p>
 * This interface provides methods to retrieve the number of tokens consumed
 * by the prompt (input), the completion (output), and any other provider-specific
 * tokens. It also provides access to the raw usage data returned by the API.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface Usage {

    /**
     * Returns the number of tokens used in the prompt (input).
     *
     * @return the count of prompt tokens
     */
    Integer getPromptTokens();

    /**
     * Returns the number of tokens used in the completion (output).
     *
     * @return the count of completion tokens
     */
    Integer getCompletionTokens();

    /**
     * Returns the number of other tokens used that are not categorized as prompt or completion tokens.
     * <p>
     * This might include reasoning tokens, caching tokens, or other provider-specific metrics.
     * </p>
     *
     * @return the count of other tokens
     */
    Integer getOtherTokens();

    /**
     * Returns the total number of tokens used.
     * <p>
     * The total is typically calculated as: promptTokens + completionTokens + otherTokens.
     * </p>
     *
     * @return the total count of all tokens
     */
    Integer getTotalTokens();

    /**
     * Returns the raw usage data as an ObjectNode.
     * <p>
     * This provides access to the original, unparsed usage information returned by the model API,
     * which may contain additional fields beyond the standard token counts.
     * </p>
     *
     * @return the raw usage data as an {@link ObjectNode}, or {@code null} if not available
     */
    @Nullable
    ObjectNode getRawUsage();

    /**
     * Checks whether the usage information is valid by verifying if the raw usage data is present.
     * <p>
     * A valid usage indicates that the underlying model API response actually contained usage data.
     * </p>
     *
     * @return {@code true} if the raw usage data is not null, {@code false} otherwise
     */
    default boolean isValidUsage() {
        return Objects.nonNull(getRawUsage());
    }

    /**
     * Creates a new builder instance for constructing a {@link Usage} object from raw data.
     *
     * @param rawUsage the raw usage data as an {@link ObjectNode}
     * @return a new {@link UsageBuilder} instance
     */
    static UsageBuilder newUsageBuilder(@NonNull ObjectNode rawUsage) {
        return new UsageBuilder(rawUsage);
    }

    /**
     * Builder class for constructing {@link Usage} instances from raw usage data.
     * <p>
     * This builder allows customization of token extraction logic through configurable
     * extractor functions. It processes raw usage data (represented as an {@link ObjectNode})
     * and applies the specified extractors to derive prompt tokens, completion tokens, and
     * other tokens. The total tokens are automatically calculated as the sum of these values.
     * </p>
     *
     * @author Gang Cheng
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    class UsageBuilder {

        @Nullable
        private final ObjectNode rawUsage;
        private Function<ObjectNode, Integer> promptTokensExtractor;
        private Function<ObjectNode, Integer> completionTokensExtractor;
        private Function<ObjectNode, Integer> otherTokensExtractor;

        /**
         * Sets the extractor function for prompt tokens.
         *
         * @param extractor the function to extract prompt tokens from the raw usage data
         * @return this builder instance for method chaining
         */
        public UsageBuilder promptTokensExtractor(@NonNull Function<ObjectNode, Integer> extractor) {
            this.promptTokensExtractor = extractor;
            return this;
        }

        /**
         * Sets the extractor function for completion tokens.
         *
         * @param extractor the function to extract completion tokens from the raw usage data
         * @return this builder instance for method chaining
         */
        public UsageBuilder completionTokensExtractor(@NonNull Function<ObjectNode, Integer> extractor) {
            this.completionTokensExtractor = extractor;
            return this;
        }

        /**
         * Sets the extractor function for other tokens.
         *
         * @param extractor the function to extract other tokens from the raw usage data
         * @return this builder instance for method chaining
         */
        public UsageBuilder otherTokensExtractor(@NonNull Function<ObjectNode, Integer> extractor) {
            this.otherTokensExtractor = extractor;
            return this;
        }

        /**
         * Builds and returns a {@link Usage} instance based on the configured extractors.
         * <p>
         * If the raw usage data is null, it returns a default empty Usage instance.
         * Otherwise, it applies the extractors. If an extractor is not set, it defaults to -1.
         * If an extractor returns null, it defaults to 0.
         * </p>
         *
         * @return a new {@link Usage} instance
         */
        public Usage build() {
            if (Objects.isNull(rawUsage)) {
                return DefaultUsage.builder().build();
            }
            Integer promptTokens = this.extractTokens(rawUsage, promptTokensExtractor);
            Integer completionTokens = this.extractTokens(rawUsage, completionTokensExtractor);
            Integer otherTokens = this.extractTokens(rawUsage, otherTokensExtractor);
            Integer totalTokens = promptTokens + completionTokens + otherTokens;
            return DefaultUsage.builder()
                    .rawUsage(rawUsage)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .otherTokens(otherTokens)
                    .totalTokens(totalTokens)
                    .build();
        }

        /**
         * Extracts token count from raw usage data using the provided extractor function.
         *
         * @param rawUsage  the raw usage data to extract tokens from
         * @param extractor the function to apply for token extraction
         * @return the extracted token count, or -1 if the extractor is null, or 0 if the extractor returns null
         */
        private Integer extractTokens(@NonNull ObjectNode rawUsage, @Nullable Function<ObjectNode, Integer> extractor) {
            if (Objects.isNull(extractor)) {
                return -1;
            }
            Integer tokens = extractor.apply(rawUsage);
            return Objects.isNull(tokens) ? 0 : tokens;
        }
    }
}
