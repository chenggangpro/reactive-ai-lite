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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.util.StringUtils;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.util.context.Context;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * A {@link BaseSubscriber} implementation that parses a stream of JSON string chunks from an AI model's response.
 * <p>
 * This class is responsible for transforming a raw {@code Flux<String>} into a structured {@code Flux<RawStreamResponse>}.
 * It handles backpressure, aggregates related message parts (like tool calls), determines the data type of each chunk,
 * and emits them as discrete {@link RawStreamResponse} objects.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Slf4j
public class StreamResponseParser extends BaseSubscriber<String> {

    protected static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;
    private final Deque<ObjectNode> toolCallData = new ConcurrentLinkedDeque<>();
    private final AtomicReference<StreamDataType> currentDataType = new AtomicReference<>();
    private final AtomicBoolean inParsing = new AtomicBoolean(false);
    private final AtomicBoolean cancelSignal = new AtomicBoolean(false);
    private final AtomicLong requestDataCount = new AtomicLong(0);
    private final Object monitor = new Object();

    private final ExecutionContextView executionContextView;
    private final FluxSink<RawStreamResponse> sink;
    private final Function<JsonChunkParsingData, JsonStreamChunkSlide[]> streamChunkParser;
    private final Function<List<ObjectNode>, ObjectNode> rawToolCallMerger;
    private final Map<String, Object> parsingAttributes = new ConcurrentHashMap<>();

    /**
     * Constructs a new StreamResponseParser.
     *
     * @param executionContextView A view of the current execution context.
     * @param sink                 The {@link FluxSink} to push parsed {@link RawStreamResponse} objects to.
     * @param streamChunkParser    A function to parse stream chunks from a JSON object.
     * @param rawToolCallMerger    A function to merge multiple tool call chunks into a single representative JSON object.
     */
    protected StreamResponseParser(@NonNull ExecutionContextView executionContextView,
                                   @NonNull FluxSink<RawStreamResponse> sink,
                                   @NonNull Function<JsonChunkParsingData, JsonStreamChunkSlide[]> streamChunkParser,
                                   @NonNull Function<List<ObjectNode>, ObjectNode> rawToolCallMerger) {
        this.executionContextView = executionContextView;
        this.sink = sink;
        this.streamChunkParser = streamChunkParser;
        this.rawToolCallMerger = rawToolCallMerger;
    }

    /**
     * A static factory method to create a {@link Flux} of {@link RawStreamResponse} from a raw stream of JSON strings.
     * <p>
     * It sets up the reactive pipeline by creating a {@link StreamResponseParser} and connecting it to the upstream
     * and downstream fluxes.
     * </p>
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
        return Flux.create(fluxSink -> {
            StreamResponseParser streamResponseParser = new StreamResponseParser(executionContextView, fluxSink, streamChunkParser, rawToolCallMerger);
            fluxSink.onRequest(streamResponseParser::onSinkRequestData);
            fluxSink.onCancel(streamResponseParser::onSinkCancel);
            fluxSink.onDispose(streamResponseParser::onSinkDispose);
            rawStreamResponse.subscribe(streamResponseParser);
        });
    }

    @Override
    public Context currentContext() {
        return Context.of(this.sink.contextView());
    }

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        if (log.isTraceEnabled()) {
            log.trace("[RawStreamResponseFlux]Hook on subscribe triggered");
        }
        if (!this.sink.isCancelled() && this.sink.requestedFromDownstream() > 0) {
            request(1);
        }
    }

    @Override
    protected void hookOnNext(String value) {
        synchronized (monitor) {
            while (inParsing.get()) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    this.sink.error(e);
                    Thread.currentThread().interrupt();
                }
            }
            inParsing.set(true);
        }
        try {
            if (cancelSignal.get()) {
                log.trace("[RawStreamResponseFlux]Hook on next triggered and cancel signal is set");
                return;
            }
            this.parsingRawStreamResponse(value);
        } finally {
            synchronized (monitor) {
                inParsing.set(false);
                monitor.notifyAll();
            }
        }
    }

    private void parsingRawStreamResponse(String value) {
        if (!StringUtils.hasText(value)) {
            this.requestNext();
            return;
        }
        boolean isDone = SSE_DONE_PREDICATE.test(value);
        if (isDone) {
            this.requestNext();
            return;
        }
        ObjectNode objectNode;
        try {
            JsonNode treeNode = OBJECT_MAPPER.readTree(value);
            if (Objects.isNull(treeNode) || treeNode.isMissingNode() || treeNode.isNull() || !treeNode.isObject()) {
                this.sink.error(new IllegalStateException("Invalid JSON chunk in stream response: " + value));
                onSinkCancel();
                return;
            }
            objectNode = (ObjectNode) treeNode;
        } catch (JsonProcessingException e) {
            this.sink.error(e);
            onSinkCancel();
            return;
        }
        JsonStreamChunkSlide[] jsonStreamChunkSlides;
        try {
            jsonStreamChunkSlides = streamChunkParser.apply(JsonChunkParsingData.builder()
                    .data(objectNode)
                    .parsingAttributes(this.parsingAttributes)
                    .build()
            );
        } catch (Exception e) {
            this.sink.error(e);
            onSinkCancel();
            return;
        }
        if (jsonStreamChunkSlides.length == 0) {
            this.requestNext();
            return;
        }
        int maxIndex = jsonStreamChunkSlides.length - 1;
        for (int i = 0; i < jsonStreamChunkSlides.length; i++) {
            JsonStreamChunkSlide jsonStreamChunkSlide = jsonStreamChunkSlides[i];
            StreamDataType streamDataType = jsonStreamChunkSlide.getStreamDataType();
            // if value is role content then request next without checking remain count
            if (StreamDataType.ROLE.equals(streamDataType)) {
                RawStreamResponse rawStreamResponse = RawStreamResponse.builder()
                        .contextView(this.executionContextView)
                        .dataType(StreamDataType.ROLE)
                        .dataContent(objectNode)
                        .build();
                this.sink.next(rawStreamResponse);
                this.requestDataCount.decrementAndGet();
                toolCallData.add(objectNode);
                if (i == maxIndex) {
                    request(1);
                }
                continue;
            }
            if (StreamDataType.TOOL_CALL.equals(currentDataType.get()) && !StreamDataType.TOOL_CALL.equals(streamDataType)) {
                List<ObjectNode> rawToolCalls = new ArrayList<>();
                while (!toolCallData.isEmpty()) {
                    rawToolCalls.add(toolCallData.poll());
                }
                ObjectNode allToolCalls = rawToolCallMerger.apply(rawToolCalls);
                RawStreamResponse rawStreamResponse = RawStreamResponse.builder()
                        .contextView(this.executionContextView)
                        .dataType(StreamDataType.TOOL_CALL)
                        .dataContent(allToolCalls)
                        .build();
                this.sink.next(rawStreamResponse);
                this.requestDataCount.decrementAndGet();
                if (i == maxIndex) {
                    this.requestNext();
                }
            }
            currentDataType.set(streamDataType);
            // answer content or reasoning content
            if (StreamDataType.ANSWER_CONTENT.equals(streamDataType) || StreamDataType.REASONING_CONTENT.equals(streamDataType)) {
                RawStreamResponse rawStreamResponse = RawStreamResponse.builder()
                        .contextView(this.executionContextView)
                        .dataType(streamDataType)
                        .dataContent(objectNode)
                        .build();
                this.sink.next(rawStreamResponse);
                this.requestDataCount.decrementAndGet();
                if (i == maxIndex) {
                    this.requestNext();
                }
                continue;
            }
            // tool call content
            if (StreamDataType.TOOL_CALL.equals(streamDataType)) {
                toolCallData.add(objectNode);
                if (i == maxIndex) {
                    request(1);
                }
                continue;
            }
            // others content
            RawStreamResponse rawStreamResponse = RawStreamResponse.builder()
                    .contextView(this.executionContextView)
                    .dataType(streamDataType)
                    .dataContent(objectNode)
                    .build();
            this.sink.next(rawStreamResponse);
            this.requestDataCount.decrementAndGet();
            if (i == maxIndex) {
                this.requestNext();
            }
        }
    }

    @Override
    protected void hookOnComplete() {
        if (log.isTraceEnabled()) {
            log.trace("[RawStreamResponseFlux]Hook on complete triggered");
        }
        synchronized (monitor) {
            while (inParsing.get()) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    this.sink.error(e);
                    Thread.currentThread().interrupt();
                }
            }
        }
        // if there is tool call content left
        if (StreamDataType.TOOL_CALL.equals(currentDataType.get()) && !toolCallData.isEmpty()) {
            List<ObjectNode> rawToolCalls = new ArrayList<>();
            while (!toolCallData.isEmpty()) {
                rawToolCalls.add(toolCallData.poll());
            }
            ObjectNode allToolCalls = rawToolCallMerger.apply(rawToolCalls);
            RawStreamResponse rawStreamResponse = RawStreamResponse.builder()
                    .contextView(this.executionContextView)
                    .dataType(StreamDataType.TOOL_CALL)
                    .dataContent(allToolCalls)
                    .build();
            this.sink.next(rawStreamResponse);
            this.requestDataCount.decrementAndGet();
        }
        this.sink.complete();
        this.cleanup();
    }

    @Override
    protected void hookOnCancel() {
        if (log.isTraceEnabled()) {
            log.trace("[RawStreamResponseFlux]Hook on cancel triggered");
        }
        this.cancelSignal.set(true);
        this.sink.complete();
        this.cleanup();
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        log.debug("Error occurred during processing: {}", throwable.getMessage());
        this.cancelSignal.set(true);
        this.sink.error(throwable);
        this.cleanup();
    }

    private void onSinkRequestData(long ln) {
        log.trace("Requesting {} data from downstream", ln);
        requestDataCount.accumulateAndGet(ln, Long::sum);
        if (requestDataCount.get() > 0 && !this.inParsing.get()) {
            requestNext();
        }
    }

    private void onSinkCancel() {
        if (log.isTraceEnabled()) {
            log.trace("[FluxSink]On sink cancel");
        }
        this.cancelSignal.set(true);
        this.cleanup();
        cancel();
    }

    private void onSinkDispose() {
        if (log.isTraceEnabled()) {
            log.trace("[FluxSink]On sink dispose");
        }
        this.cancelSignal.set(true);
        this.inParsing.set(false);
        cleanup();
        dispose();
    }

    private void cleanup() {
        if (!this.toolCallData.isEmpty()) {
            this.toolCallData.clear();
        }
        if (!this.parsingAttributes.isEmpty()) {
            this.parsingAttributes.clear();
        }
    }

    private void requestNext() {
        if (!this.sink.isCancelled()
                && !this.cancelSignal.get()
                && this.requestDataCount.get() > 0) {
            request(1);
        }
    }

    /**
     * Represents a parsed data containing JSON data.
     *
     * @author Cheng Gang
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
     * @author Cheng Gang
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
