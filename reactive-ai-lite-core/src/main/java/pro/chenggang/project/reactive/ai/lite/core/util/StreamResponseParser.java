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
package pro.chenggang.project.reactive.ai.lite.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * A thread-safe, non-blocking utility that transforms a reactive stream of raw JSON string chunks
 * (typically from an AI provider's Server-Sent Events (SSE) response) into a structured
 * {@link Flux} of {@link RawStreamResponse} objects.
 * <p>
 * The parser follows these steps:
 * <ol>
 *   <li><strong>Filtering:</strong> Skips empty or blank chunks.</li>
 *   <li><strong>Termination:</strong> Stops upon receiving the special {@code "[DONE]"} marker.</li>
 *   <li><strong>Parsing:</strong> Deserializes each non‑terminal chunk into an {@link ObjectNode}.</li>
 *   <li><strong>Sliding:</strong> Passes the JSON node along with a mutable {@code parsingAttributes} map
 *       to a user‑supplied {@code streamChunkParser} function, which returns zero or more typed
 *       {@link JsonStreamChunkSlide} fragments. This allows complex parsing logic (e.g., chunking of
 *       a single event into multiple typed pieces) while preserving cross‑chunk state.</li>
 *   <li><strong>Buffering:</strong> Consecutive slides of the same {@link StreamDataType} are grouped
 *       together using {@link Flux#bufferUntilChanged}.</li>
 *   <li><strong>Merging:</strong> For {@link StreamDataType#TOOL_CALL} groups, the separate
 *       {@link ObjectNode}s are merged into a single representative node via the
 *       {@code rawToolCallMerger} function (e.g., combining tool call id, name, and accumulated
 *       arguments).</li>
 *   <li><strong>Mapping:</strong> Each final slide is converted to a {@link RawStreamResponse} that
 *       links back to the originating {@link ExecutionContext}.</li>
 * </ol>
 * </p>
 * <p>
 * The {@code parsingAttributes} map is a {@link ConcurrentHashMap}, making the entire pipeline
 * safe for parallel processing of multiple streams. The class is designed as a utility; it is
 * not meant to be instantiated.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ExecutionContext
 * @see RawStreamResponse
 */
@Slf4j
public class StreamResponseParser {

    /**
     * A predicate that detects the SSE terminal marker {@code "[DONE]"}.
     * When this predicate matches, the upstream source is considered complete and
     * no further chunks are processed.
     */
    protected static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private StreamResponseParser() {
        // Utility class
    }

    /**
     * Creates a {@link Flux} that processes a raw JSON string stream into discrete
     * {@link RawStreamResponse} objects, using the provided chunk parser and tool‑call merger.
     *
     * <p><strong>Detailed pipeline logic:</strong></p>
     * <ol>
     *   <li>Wraps the entire processing in {@link Flux#defer} to ensure each subscriber gets a fresh
     *       {@link ConcurrentHashMap} for {@code parsingAttributes}.</li>
     *   <li>Removes empty and blank strings with {@link Flux#filter}.</li>
     *   <li>Stops emitting when the {@code "[DONE]"} marker is encountered via
     *       {@link Flux#takeUntil}.</li>
     *   <li>For each non‑terminal chunk, attempts to parse it as JSON. If the JSON is invalid,
     *       missing, or not an object, the stream errors immediately.</li>
     *   <li>Invokes the {@code streamChunkParser} function, which may inspect the JSON and
     *       the mutable {@code parsingAttributes} map to produce an array of
     *       {@link JsonStreamChunkSlide} objects. A single JSON event can therefore be transformed
     *       into multiple typed slides (e.g., model thinking content vs. final text).</li>
     *   <li>Flattens the array of slides into a sequential flux of slides.</li>
     *   <li>Groups slides that share the same {@link StreamDataType} via
     *       {@code Flux#bufferUntilChanged(JsonStreamChunkSlide::getStreamDataType)}. This is essential
     *       because AI streams often interleave different data types (text, tool calls) and each
     *       type must be assembled separately.</li>
     *   <li>For buffered groups of type {@link StreamDataType#TOOL_CALL}, applies the
     *       {@code rawToolCallMerger} function to combine multiple {@link ObjectNode} chunks into a
     *       single node. This is necessary because a tool call’s id, name, and arguments typically
     *       arrive in separate chunks.</li>
     *   <li>Maps each finalised {@link JsonStreamChunkSlide} to a {@link RawStreamResponse},
     *       attaching the originating {@link ExecutionContext}.</li>
     * </ol>
     *
     * @param executionContext  the execution context that generated this stream; never null
     * @param rawStreamResponse the raw stream of JSON string chunks from the AI provider
     * @param streamChunkParser a function that converts a single parsed JSON node (and any parsing
     *                          state) into zero or more typed slides; the parsing attributes map
     *                          is mutable and shared across all chunks of a single stream
     * @param rawToolCallMerger a function that merges a list of {@link ObjectNode}s representing
     *                          a tool call into a single consolidated node; never null
     * @return a reactive {@link Flux} that emits structured {@link RawStreamResponse} objects;
     *         the flux is cold and can be subscribed to repeatedly (each subscription triggers a
     *         fresh processing pipeline)
     */
    public static Flux<RawStreamResponse> parseStreamResponse(@NonNull ExecutionContext executionContext,
                                                              @NonNull Flux<String> rawStreamResponse,
                                                              @NonNull Function<JsonChunkParsingData, JsonStreamChunkSlide[]> streamChunkParser,
                                                              @NonNull Function<List<ObjectNode>, ObjectNode> rawToolCallMerger) {
        return Flux.defer(() -> {
            Map<String, Object> parsingAttributes = new ConcurrentHashMap<>();
            return rawStreamResponse
                    .filter(StringUtils::hasText)
                    .takeUntil(SSE_DONE_PREDICATE)
                    .concatMap(value -> {
                        if (SSE_DONE_PREDICATE.test(value)) {
                            return Flux.empty();
                        }
                        try {
                            JsonNode treeNode = OBJECT_MAPPER.readTree(value);
                            if (Objects.isNull(treeNode) || treeNode.isMissingNode() || treeNode.isNull() || !treeNode.isObject()) {
                                return Flux.error(new IllegalStateException("Invalid JSON chunk in stream response: " + value));
                            }
                            ObjectNode objectNode = (ObjectNode) treeNode;
                            JsonStreamChunkSlide[] slides = streamChunkParser.apply(JsonChunkParsingData.builder()
                                    .data(objectNode)
                                    .parsingAttributes(parsingAttributes)
                                    .build());
                            return Flux.fromArray(slides);
                        } catch (Exception e) {
                            return Flux.error(Exceptions.propagate(e));
                        }
                    })
                    .bufferUntilChanged(JsonStreamChunkSlide::getStreamDataType)
                    .flatMapIterable(bufferedSlides -> {
                        if (bufferedSlides.isEmpty()) {
                            return List.of();
                        }
                        StreamDataType type = bufferedSlides.getFirst().getStreamDataType();
                        if (StreamDataType.TOOL_CALL.equals(type)) {
                            List<ObjectNode> nodes = bufferedSlides.stream()
                                    .map(JsonStreamChunkSlide::getDataContent)
                                    .toList();
                            return List.of(JsonStreamChunkSlide.builder()
                                    .streamDataType(StreamDataType.TOOL_CALL)
                                    .dataContent(rawToolCallMerger.apply(nodes))
                                    .build());
                        }
                        return bufferedSlides;
                    })
                    .map(slide -> RawStreamResponse.builder()
                            .executionContext(executionContext)
                            .dataType(slide.getStreamDataType())
                            .dataContent(slide.getDataContent())
                            .build()
                    );
        });
    }

    /**
     * Holds the data and mutable state needed during the parsing of a single JSON chunk
     * from the stream.
     * <p>
     * The {@code data} field is the parsed JSON node for the current event. The
     * {@code parsingAttributes} map is a {@link ConcurrentHashMap} shared across all
     * chunks of one stream, allowing parsers to accumulate or correlate information
     * (for example, tracking the current tool call id and accumulating arguments
     * across multiple chunks).
     * </p>
     *
     * @author Gang Cheng
     */
    @Getter
    @Builder
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class JsonChunkParsingData {

        /**
         * The raw JSON content of the current chunk, never null.
         */
        private final ObjectNode data;

        /**
         * A mutable map shared across all chunks of the same stream, used to persist
         * parsing state between individual chunk invocations. The map is thread-safe.
         */
        private final Map<String, Object> parsingAttributes;

        /**
         * Retrieves a parsing attribute by name, or {@code null} if not present.
         *
         * @param <T>  the expected type of the attribute
         * @param name the attribute name (never null)
         * @return the attribute value, or {@code null} if it does not exist
         */
        @SuppressWarnings("unchecked")
        public <T> T getParsingAttribute(@NonNull String name) {
            return (T) getParsingAttributes().get(name);
        }

        /**
         * Retrieves a parsing attribute by name, returning the given default value if the
         * attribute is not present.
         *
         * @param <T>          the expected type of the attribute
         * @param name         the attribute name (never null)
         * @param defaultValue the default value to return if the attribute is absent (never null)
         * @return the attribute value, or the default value
         */
        @SuppressWarnings("unchecked")
        public <T> T getParsingAttributeOrDefault(@NonNull String name, @NonNull T defaultValue) {
            return (T) getParsingAttributes().getOrDefault(name, defaultValue);
        }

    }

    /**
     * Represents a single typed fragment of parsed stream data, possibly one of many
     * produced from a single raw JSON event.
     * <p>
     * Each slide carries a {@link StreamDataType} that classifies the content, the
     * actual data as an {@link ObjectNode}, and an optional attributes map for any
     * extra metadata (e.g., tool call ids, roles). Slides are later buffered by type and,
     * for tool calls, merged into a unified representation.
     * </p>
     *
     * @author Gang Cheng
     */
    @Getter
    @Builder
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class JsonStreamChunkSlide {
        /**
         * The data type of this slide, determined by the chunk parser.
         * Used for buffering and downstream classification.
         */
        private final StreamDataType streamDataType;

        /**
         * The content of this slide, never null.
         */
        private final ObjectNode dataContent;

        /**
         * Optional extra attributes associated with this slide, such as tool call
         * metadata or role information. May be empty but never null.
         */
        private final Map<String, Object> attributes;
    }

}