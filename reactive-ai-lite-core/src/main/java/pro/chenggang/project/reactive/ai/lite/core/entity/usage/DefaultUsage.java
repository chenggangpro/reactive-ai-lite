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
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import org.springframework.lang.Nullable;

/**
 * The default, immutable implementation of the {@link Usage} interface.
 * <p>
 * This class serves as the standard representation of token usage metrics across the framework.
 * It uses Lombok to generate a builder for easy construction and is annotated with
 * {@code @Jacksonized} for seamless JSON serialization and deserialization. Default values for
 * token counts are initialized to zero.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultUsage implements Usage {

    /**
     * The number of tokens used in the prompt. Defaults to 0.
     */
    @NonNull
    @Builder.Default
    private final Integer promptTokens = 0;

    /**
     * The number of tokens used in the completion. Defaults to 0.
     */
    @NonNull
    @Builder.Default
    private final Integer completionTokens = 0;

    /**
     * The number of other tokens used. Defaults to 0.
     */
    @NonNull
    @Builder.Default
    private final Integer otherTokens = 0;

    /**
     * The total number of tokens used. Defaults to 0.
     */
    @NonNull
    @Builder.Default
    private final Integer totalTokens = 0;

    /**
     * The raw, unparsed usage data returned by the underlying API. May be null.
     */
    @Nullable
    private final ObjectNode rawUsage;

    /**
     * Returns the number of tokens used in the prompt.
     *
     * @return the count of prompt tokens
     */
    @Override
    public Integer getPromptTokens() {
        return this.promptTokens;
    }

    /**
     * Returns the number of tokens used in the completion.
     *
     * @return the count of completion tokens
     */
    @Override
    public Integer getCompletionTokens() {
        return this.completionTokens;
    }

    /**
     * Returns the number of other tokens used.
     *
     * @return the count of other tokens
     */
    @Override
    public Integer getOtherTokens() {
        return this.otherTokens;
    }

    /**
     * Returns the total number of tokens used.
     *
     * @return the total count of all tokens
     */
    @Override
    public Integer getTotalTokens() {
        return this.totalTokens;
    }

    /**
     * Returns the raw usage data.
     *
     * @return the raw usage data as an {@link ObjectNode}, or {@code null} if not available
     */
    @Nullable
    @Override
    public ObjectNode getRawUsage() {
        return this.rawUsage;
    }

}
