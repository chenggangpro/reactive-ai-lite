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
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * A utility class that parses a stream of JSON string chunks from an AI model's response.
 * <p>
 * This class is responsible for transforming a raw {@code Flux<String>} into a structured {@code Flux<RawStreamResponse>}.
 * It aggregates related message parts (like tool calls), determines the data type of each chunk,
 * and emits them as discrete {@link RawStreamResponse} objects using high-level non-blocking reactive operators.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class StreamResponseParser {

    protected static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

    private StreamResponseParser() {
        // Utility class
    }

    /**
     * A static factory method to create a {@link Flux} of {@link RawStreamResponse} from a raw stream of JSON strings.
     *
     * @param executionContextView A view of the current execution context.
     * @param rawStreamResponse    The raw stream of JSON strings from the provider.
     * @param streamChunkParser    A function to parse stream chunks from a JSON object.
     * @param rawToolCallMerger    A function to merge multiple tool call chunks into a single representative JSON object.
     * @return A new {@link Flux} that emits parsed {@link RawStreamResponse} objects.
     */
    public static Flux<RawStreamResponse> parseStreamResponse(@NonNull ExecutionContextView executionContextView,
                                                              @NonNull Flux<String> rawStreamResponse,
                                                              @NonNull Function<JsonChunkParsingData, JsonStreamChunkSlide[]> streamChunkParser,
                                                              @NonNull Function<List<ObjectNode>, ObjectNode> rawToolCallMerger) {
        return Flux.defer(() -> {
            Map<String, Object> parsingAttributes = new ConcurrentHashMap<>();
            List<ObjectNode> toolCallData = new ArrayList<>();
            StreamDataType[] currentDataType = new StreamDataType[1];
            return rawStreamResponse
                    .filter(StringUtils::hasText)
                    .takeUntil(SSE_DONE_PREDICATE)
                    .flatMapIterable(value -> {
                        if (SSE_DONE_PREDICATE.test(value)) {
                            return List.of();
                        }
                        try {
                            JsonNode treeNode = OBJECT_MAPPER.readTree(value);
                            if (Objects.isNull(treeNode) || treeNode.isMissingNode() || treeNode.isNull() || !treeNode.isObject()) {
                                throw new IllegalStateException("Invalid JSON chunk in stream response: " + value);
                            }
                            ObjectNode objectNode = (ObjectNode) treeNode;
                            JsonStreamChunkSlide[] slides = streamChunkParser.apply(JsonChunkParsingData.builder()
                                    .data(objectNode)
                                    .parsingAttributes(parsingAttributes)
                                    .build());
                            return List.of(slides);
                        } catch (Exception e) {
                            throw reactor.core.Exceptions.propagate(e);
                        }
                    })
                    .concatMapIterable(slide -> {
                        StreamDataType streamDataType = slide.getStreamDataType();
                        if (StreamDataType.ROLE.equals(streamDataType)) {
                            return List.of(slide);
                        }
                        List<JsonStreamChunkSlide> toEmit = new ArrayList<>();
                        if (StreamDataType.TOOL_CALL.equals(currentDataType[0]) && !StreamDataType.TOOL_CALL.equals(streamDataType)) {
                            if (!toolCallData.isEmpty()) {
                                toEmit.add(JsonStreamChunkSlide.builder()
                                        .streamDataType(StreamDataType.TOOL_CALL)
                                        .dataContent(rawToolCallMerger.apply(new ArrayList<>(toolCallData)))
                                        .build());
                                toolCallData.clear();
                            }
                        }
                        currentDataType[0] = streamDataType;
                        if (StreamDataType.TOOL_CALL.equals(streamDataType)) {
                            toolCallData.add(slide.getDataContent());
                        } else {
                            toEmit.add(slide);
                        }
                        return toEmit;
                    })
                    .concatWith(Flux.defer(() -> {
                        // Flush remaining tool calls on complete
                        if (!toolCallData.isEmpty()) {
                            JsonStreamChunkSlide merged = JsonStreamChunkSlide.builder()
                                    .streamDataType(StreamDataType.TOOL_CALL)
                                    .dataContent(rawToolCallMerger.apply(new ArrayList<>(toolCallData)))
                                    .build();
                            toolCallData.clear();
                            return Flux.just(merged);
                        }
                        return Flux.empty();
                    }))
                    .map(slide -> RawStreamResponse.builder()
                            .contextView(executionContextView)
                            .dataType(slide.getStreamDataType())
                            .dataContent(slide.getDataContent())
                            .build()
                    );
        });
    }

    /**
     * Represents a parsed data containing JSON data.
     *
     * @author Gang Cheng
     */
    @Getter
    @Builder
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class JsonChunkParsingData {

        /**
         * The parsed JSON data content.
         */
        private final ObjectNode data;

        /**
         * The parsingAttributes associated while the whole parsing process.
         */
        private final Map<String, Object> parsingAttributes;

        /**
         * Return the attribute value if present or {@code null} if not present.
         *
         * @param <T>  the attribute type
         * @param name the attribute name
         * @return the attribute value, or {@code null} if it does not exist
         */
        @SuppressWarnings("unchecked")
        public <T> T getParsingAttribute(@NonNull String name) {
            return (T) getParsingAttributes().get(name);
        }

        /**
         * Return the attribute value or a default value if the attribute is not present.
         *
         * @param <T>          the attribute type
         * @param name         the attribute name
         * @param defaultValue a default value to return instead if the attribute is missing
         * @return the attribute value, or the default value
         */
        @SuppressWarnings("unchecked")
        public <T> T getParsingAttributeOrDefault(@NonNull String name, @NonNull T defaultValue) {
            return (T) getParsingAttributes().getOrDefault(name, defaultValue);
        }

    }

    /**
     * Represents a single slide or fragment of parsed JSON stream data.
     *
     * @author Gang Cheng
     */
    @Getter
    @Builder
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class JsonStreamChunkSlide {
        /**
         * The response data type.
         */
        private final StreamDataType streamDataType;

        /**
         * The response data content.
         */
        private final ObjectNode dataContent;

        /**
         * The parsingAttributes associated with the Server-Sent Event.
         */
        private final Map<String, Object> attributes;
    }

}
