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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.option.ResponseDataType;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.util.context.Context;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * A {@link BaseSubscriber} implementation that parses a stream of JSON string chunks from an AI model's response.
 * This class is responsible for transforming a raw {@code Flux<String>} into a structured {@code Flux<RawStreamResponse>}.
 * It handles backpressure, aggregates related message parts (like tool calls), determines the data type of each chunk,
 * and emits them as discrete {@link RawStreamResponse} objects.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Slf4j
public class StreamResponseParser extends BaseSubscriber<String> {

    protected static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;
    private final Deque<ObjectNode> toolCallData = new ConcurrentLinkedDeque<>();
    private final AtomicReference<ResponseDataType> currentDataType = new AtomicReference<>();
    private final AtomicBoolean inParsing = new AtomicBoolean(false);
    private final AtomicBoolean cancelSignal = new AtomicBoolean(false);
    private final AtomicLong requestDataCount = new AtomicLong(0);
    private final Object monitor = new Object();

    private final ArrayNode requestMessages;
    private final ExecutionContextView executionContextView;
    private final FluxSink<RawStreamResponse> sink;
    private final Function<ObjectNode, StreamChunk[]> streamChunkParser;
    private final Function<List<ObjectNode>, ObjectNode> rawToolCallMerger;

    /**
     * Constructs a new StreamResponseParser.
     *
     * @param requestMessages      The initial request messages sent to the AI model.
     * @param executionContextView A view of the current execution context.
     * @param sink                 The {@link FluxSink} to push parsed {@link RawStreamResponse} objects to.
     * @param streamChunkParser    A function to parse stream chunks from a JSON object.
     * @param rawToolCallMerger    A function to merge multiple tool call chunks into a single representative JSON object.
     */
    public StreamResponseParser(@NonNull ArrayNode requestMessages,
                                @NonNull ExecutionContextView executionContextView,
                                @NonNull FluxSink<RawStreamResponse> sink,
                                @NonNull Function<ObjectNode, StreamChunk[]> streamChunkParser,
                                @NonNull Function<List<ObjectNode>, ObjectNode> rawToolCallMerger) {
        this.requestMessages = requestMessages;
        this.executionContextView = executionContextView;
        this.sink = sink;
        this.streamChunkParser = streamChunkParser;
        this.rawToolCallMerger = rawToolCallMerger;
    }

    /**
     * A static factory method to create a {@link Flux} of {@link RawStreamResponse} from a raw stream of JSON strings.
     * It sets up the reactive pipeline by creating a {@link StreamResponseParser} and connecting it to the upstream
     * and downstream fluxes.
     *
     * @param requestMessages      The initial request messages sent to the AI model.
     * @param executionContextView A view of the current execution context.
     * @param streamChunkParser    A function to parse stream chunks from a JSON object.
     * @param rawToolCallMerger    A function to merge multiple tool call chunks into a single representative JSON object.
     * @return A new {@link Flux} that emits parsed {@link RawStreamResponse} objects.
     */
    public static Flux<RawStreamResponse> parseStreamResponse(@NonNull ArrayNode requestMessages,
                                                              @NonNull ExecutionContextView executionContextView,
                                                              @NonNull Flux<String> rawStreamResponse,
                                                              @NonNull Function<ObjectNode, StreamChunk[]> streamChunkParser,
                                                              @NonNull Function<List<ObjectNode>, ObjectNode> rawToolCallMerger) {
        return Flux.create(fluxSink -> {
            StreamResponseParser streamResponseParser = new StreamResponseParser(requestMessages, executionContextView, fluxSink, streamChunkParser, rawToolCallMerger);
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
        if (!this.sink.isCancelled()) {
            if (this.sink.requestedFromDownstream() > 0) {
                request(1);
            }
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
            inParsing.set(false);
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }
    }

    private void parsingRawStreamResponse(String value) {
        boolean isDone = SSE_DONE_PREDICATE.test(value);
        if (isDone) {
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
        if (currentDataType.compareAndSet(null, ResponseDataType.REQUEST_MESSAGE)) {
            this.sink.next(RawStreamResponse.builder()
                    .contextView(this.executionContextView)
                    .dataType(ResponseDataType.REQUEST_MESSAGE)
                    .dataContent(requestMessages)
                    .build()
            );
            this.requestDataCount.decrementAndGet();
        }
        StreamChunk[] streamChunks = streamChunkParser.apply(objectNode);
        if (streamChunks.length == 0) {
            return;
        }
        int maxIndex = streamChunks.length - 1;
        for (int i = 0; i < streamChunks.length; i++) {
            StreamChunk streamChunk = streamChunks[i];
            ResponseDataType responseDataType = streamChunk.getResponseDataType();
            // if value is role content then request next without checking remain count
            if (ResponseDataType.ROLE.equals(responseDataType)) {
                toolCallData.add(objectNode);
                if (i == maxIndex) {
                    request(1);
                }
                continue;
            }
            if (ResponseDataType.TOOL_CALL.equals(currentDataType.get()) && !ResponseDataType.TOOL_CALL.equals(responseDataType)) {
                List<ObjectNode> rawToolCalls = new ArrayList<>();
                while (!toolCallData.isEmpty()) {
                    rawToolCalls.add(toolCallData.poll());
                }
                ObjectNode allToolCalls = rawToolCallMerger.apply(rawToolCalls);
                RawStreamResponse rawStreamResponse = RawStreamResponse.builder()
                        .contextView(this.executionContextView)
                        .dataType(ResponseDataType.TOOL_CALL)
                        .dataContent(allToolCalls)
                        .build();
                this.sink.next(rawStreamResponse);
                this.requestDataCount.decrementAndGet();
                if (i == maxIndex) {
                    this.requestNext();
                }
                continue;
            }
            currentDataType.set(responseDataType);
            // answer content or reasoning content
            if (ResponseDataType.ANSWER_CONTENT.equals(responseDataType) || ResponseDataType.REASONING_CONTENT.equals(responseDataType)) {
                RawStreamResponse rawStreamResponse = RawStreamResponse.builder()
                        .contextView(this.executionContextView)
                        .dataType(responseDataType)
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
            if (ResponseDataType.TOOL_CALL.equals(responseDataType)) {
                toolCallData.add(objectNode);
                if (i == maxIndex) {
                    request(1);
                }
                continue;
            }
            // others content
            RawStreamResponse rawStreamResponse = RawStreamResponse.builder()
                    .contextView(this.executionContextView)
                    .dataType(responseDataType)
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
        if (ResponseDataType.TOOL_CALL.equals(currentDataType.get()) && !toolCallData.isEmpty()) {
            List<ObjectNode> rawToolCalls = new ArrayList<>();
            while (!toolCallData.isEmpty()) {
                rawToolCalls.add(toolCallData.poll());
            }
            ObjectNode allToolCalls = rawToolCallMerger.apply(rawToolCalls);
            RawStreamResponse rawStreamResponse = RawStreamResponse.builder()
                    .contextView(this.executionContextView)
                    .dataType(ResponseDataType.TOOL_CALL)
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
    }

    private void requestNext() {
        if (!this.sink.isCancelled()
                && !this.cancelSignal.get()
                && this.requestDataCount.get() > 0) {
            request(1);
        }
    }

    @Getter
    @Builder
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class StreamChunk {
        /**
         * The response data type.
         */
        private final ResponseDataType responseDataType;
        /**
         * The response data content.
         */
        private final ObjectNode dataContent;
    }

}
