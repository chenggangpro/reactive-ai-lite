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
package pro.chenggang.project.reactive.ai.lite.core.message.defaults;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolResultMessage;

/**
 * The default, immutable implementation of the {@link ToolResultMessage} interface.
 * <p>
 * This class provides a concrete representation of the response or output generated
 * by a tool execution. It contains the original tool call ID to allow correlation
 * with the request and the actual content produced by the tool. It uses Lombok's
 * {@code @Builder} for construction and is annotated with {@code @Jacksonized}
 * to support JSON serialization and deserialization.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Jacksonized
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultToolResultMessage implements ToolResultMessage {

    /**
     * The unique identifier of the tool call that generated this result.
     */
    @NonNull
    private final String toolCallId;

    /**
     * The output or content produced by the tool execution.
     */
    @NonNull
    private final String content;

    /**
     * Retrieves the unique identifier of the tool call.
     *
     * @return the tool call identifier
     */
    @Override
    public String toolCallId() {
        return this.toolCallId;
    }

    /**
     * Retrieves the actual output or result data from the tool execution.
     *
     * @return the content of the tool result
     */
    @Override
    public String content() {
        return this.content;
    }

    /**
     * Retrieves the actual concrete type of this message.
     *
     * @return the {@link DefaultToolResultMessage} class type
     */
    @Override
    public Class<? extends Message> getActualType() {
        return DefaultToolResultMessage.class;
    }
}
