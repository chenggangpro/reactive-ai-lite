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
 * <p>
 * The total token count is typically the sum of prompt, completion, and other tokens.
 * Implementations may derive these values from the raw usage data of different
 * model providers, which is why a configurable builder exists.
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
     * <p>
     * The builder allows custom extraction logic for token counts from provider‑specific
     * raw usage structures.
     * </p>
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
     * <p>
     * If no extractor is provided for a token type, the corresponding count defaults to 0.
     * When the raw usage data itself is {@code null}, a default empty {@link Usage} is returned.
     * </p>
     *
     * @author Gang Cheng
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    class UsageBuilder {

        /**
         * The raw usage data node from which token counts are extracted.
         * <p>
         * May be {@code null} if no usage data is available, in which case {@link #build()}
         * will return a default empty {@link Usage} instance.
         * </p>
         */
        @Nullable
        private final ObjectNode rawUsage;

        /**
         * Function to extract the prompt tokens count from the raw usage data.
         * <p>
         * If not explicitly set, the token count for prompts will be treated as 0.
         * </p>
         */
        private Function<ObjectNode, Integer> promptTokensExtractor;

        /**
         * Function to extract the completion tokens count from the raw usage data.
         * <p>
         * If not explicitly set, the token count for completions will be treated as 0.
         * </p>
         */
        private Function<ObjectNode, Integer> completionTokensExtractor;

        /**
         * Function to extract the other tokens count from the raw usage data.
         * <p>
         * If not explicitly set, the token count for other categories will be treated as 0.
         * </p>
         */
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
         * If the raw usage data is null, it returns a default empty {@link Usage} instance.
         * Otherwise, it applies the extractor functions. For any token type whose extractor
         * has not been set, the count is treated as 0. If an extractor returns {@code null},
         * that is also treated as 0.
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
         * Extracts a token count from the raw usage data using the provided extractor function.
         * <p>
         * If the extractor is {@code null} or the extractor returns {@code null}, the result
         * is treated as 0 – indicating that the token type is not present or not configured.
         * </p>
         *
         * @param rawUsage  the raw usage data node (never {@code null})
         * @param extractor the function to apply for token extraction, may be {@code null}
         * @return the extracted token count, or 0 if the extractor is null or returns null
         */
        private Integer extractTokens(@NonNull ObjectNode rawUsage, @Nullable Function<ObjectNode, Integer> extractor) {
            if (Objects.isNull(extractor)) {
                return 0;
            }
            Integer tokens = extractor.apply(rawUsage);
            return Objects.isNull(tokens) ? 0 : tokens;
        }
    }
}