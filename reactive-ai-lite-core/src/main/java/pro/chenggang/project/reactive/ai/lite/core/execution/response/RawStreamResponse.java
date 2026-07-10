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
package pro.chenggang.project.reactive.ai.lite.core.execution.response;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;

/**
 * Represents a raw, partially processed JSON object emitted from a stream response.
 * <p>
 * This class wraps a single JSON node received from the provider's Server-Sent Events (SSE)
 * stream. While the framework has identified the category of the data (e.g., TOOL_CALL,
 * ANSWER_CONTENT), the content itself is left as a raw {@link ObjectNode}. This allows
 * interceptors and converters to inspect or manipulate the raw structure before it
 * is formally parsed into a {@link StreamResponse}.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@SuperBuilder
public class RawStreamResponse extends LlmResponse {

    /**
     * The categorized type of the streaming data chunk.
     * <p>
     * The {@link StreamDataType} is determined by a classifier that examines the raw SSE
     * event structure (e.g., based on event name, type field, or specific JSON keys).
     * This type serves as a routing key: downstream converters use it to decide how to
     * deserialize {@link #dataContent} into the appropriate concrete {@link StreamResponse}
     * subclass (such as {@code ToolCallStreamResponse} or {@code AnswerStreamResponse}).
     * <p>
     * Because the classification occurs before full parsing, it enables the framework to
     * apply type-specific preprocessing (e.g., filtering out empty chunks, accumulating
     * tool call arguments) while still allowing low-level JSON manipulation if needed.
     * </p>
     */
    private final StreamDataType dataType;

    /**
     * The raw JSON content associated with this streaming data slide.
     * <p>
     * This {@link ObjectNode} holds the complete JSON payload as delivered by the SSE
     * event, before any domain-specific deserialization. Keeping the content in its raw
     * form gives interceptors the opportunity to:
     * <ul>
     *   <li>log or monitor sensitive fields before they are mapped to Java objects,</li>
     *   <li>apply transformations (like adding/removing fields) without depending on
     *       class-specific serialization logic,</li>
     *   <li>decide dynamically which converter to use based on the JSON structure rather
     *       than the already-computed {@link #dataType}.</li>
     * </ul>
     * Once all interceptors have run, a matching {@code StreamResponseConverter} will
     * interpret this node into a typed {@link StreamResponse} and publish it further
     * downstream.
     * </p>
     */
    private final ObjectNode dataContent;
}