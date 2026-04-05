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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.function.Function;

import static org.mockito.Mockito.mock;
import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

class StreamResponseParserTest {

    @Test
    void testParseStreamResponse() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<String> rawStream = Flux.just("{\"text\": \"Hello\"}", "{\"text\": \" World\"}");

        Function<ObjectNode, StreamResponseParser.JsonStreamChunkSlide[]> parser = node -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.ANSWER_CONTENT)
                        .dataContent(node)
                        .build()
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> nodes.get(0);

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.ANSWER_CONTENT)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.ANSWER_CONTENT)
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseWithDone() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<String> rawStream = Flux.just("{\"text\": \"Hello\"}", "[DONE]");

        Function<ObjectNode, StreamResponseParser.JsonStreamChunkSlide[]> parser = node -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.ANSWER_CONTENT)
                        .dataContent(node)
                        .build()
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> nodes.get(0);

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.ANSWER_CONTENT)
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseInvalidJson() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<String> rawStream = Flux.just("invalid json");

        Function<ObjectNode, StreamResponseParser.JsonStreamChunkSlide[]> parser = node -> new StreamResponseParser.JsonStreamChunkSlide[0];
        Function<List<ObjectNode>, ObjectNode> merger = nodes -> nodes.get(0);

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectError()
                .verify();
    }

    @Test
    void testParseStreamResponseWithRole() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<String> rawStream = Flux.just("{\"role\": \"assistant\"}");

        Function<ObjectNode, StreamResponseParser.JsonStreamChunkSlide[]> parser = node -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.ROLE)
                        .dataContent(node)
                        .build()
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> nodes.get(0);

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.ROLE)
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseWithToolCall() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<String> rawStream = Flux.just("{\"tool\": \"part1\"}", "{\"tool\": \"part2\"}", "{\"text\": \"end\"}");

        Function<ObjectNode, StreamResponseParser.JsonStreamChunkSlide[]> parser = node -> {
            if (node.has("tool")) {
                return new StreamResponseParser.JsonStreamChunkSlide[]{
                        StreamResponseParser.JsonStreamChunkSlide.builder()
                                .streamDataType(StreamDataType.TOOL_CALL)
                                .dataContent(node)
                                .build()
                };
            } else {
                return new StreamResponseParser.JsonStreamChunkSlide[]{
                        StreamResponseParser.JsonStreamChunkSlide.builder()
                                .streamDataType(StreamDataType.ANSWER_CONTENT)
                                .dataContent(node)
                                .build()
                };
            }
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> {
            ObjectNode merged = OBJECT_MAPPER.createObjectNode();
            merged.put("merged", true);
            return merged;
        };

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.TOOL_CALL && resp.getDataContent().has("merged"))
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.ANSWER_CONTENT)
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseWithToolCallAtEnd() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<String> rawStream = Flux.just("{\"tool\": \"part1\"}", "{\"tool\": \"part2\"}");

        Function<ObjectNode, StreamResponseParser.JsonStreamChunkSlide[]> parser = node -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.TOOL_CALL)
                        .dataContent(node)
                        .build()
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> {
            ObjectNode merged = OBJECT_MAPPER.createObjectNode();
            merged.put("merged", true);
            return merged;
        };

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.TOOL_CALL && resp.getDataContent().has("merged"))
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseWithUsage() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<String> rawStream = Flux.just("{\"usage\": {}}");

        Function<ObjectNode, StreamResponseParser.JsonStreamChunkSlide[]> parser = node -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.USAGE)
                        .dataContent(node)
                        .build()
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> nodes.get(0);

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.USAGE)
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseWithMultipleSlidesInOneChunk() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        // One raw string containing multiple slides logic is handled by the parser function
        Flux<String> rawStream = Flux.just("{\"data\": \"multiple\"}");

        Function<ObjectNode, StreamResponseParser.JsonStreamChunkSlide[]> parser = node -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.REASONING_CONTENT)
                        .dataContent(JsonNodeFactory.instance.objectNode().put("reason", 1))
                        .build(),
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.ANSWER_CONTENT)
                        .dataContent(JsonNodeFactory.instance.objectNode().put("answer", 1))
                        .build()
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> nodes.get(0);

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.REASONING_CONTENT)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.ANSWER_CONTENT)
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseWithEmptySlides() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<String> rawStream = Flux.just("{}");

        Function<ObjectNode, StreamResponseParser.JsonStreamChunkSlide[]> parser = node -> new StreamResponseParser.JsonStreamChunkSlide[0];
        Function<List<ObjectNode>, ObjectNode> merger = nodes -> nodes.get(0);

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseWithReasoning() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<String> rawStream = Flux.just("{\"reasoning\": \"thinking\"}");

        Function<ObjectNode, StreamResponseParser.JsonStreamChunkSlide[]> parser = node -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.REASONING_CONTENT)
                        .dataContent(node)
                        .build()
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> nodes.get(0);

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.REASONING_CONTENT)
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseWithToolCallTransition() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        // First slide is TOOL_CALL, second slide in SAME chunk is ANSWER_CONTENT
        Flux<String> rawStream = Flux.just("{\"data\": \"transition\"}");

        Function<ObjectNode, StreamResponseParser.JsonStreamChunkSlide[]> parser = node -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.TOOL_CALL)
                        .dataContent(JsonNodeFactory.instance.objectNode().put("tool", 1))
                        .build(),
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.ANSWER_CONTENT)
                        .dataContent(JsonNodeFactory.instance.objectNode().put("answer", 1))
                        .build()
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> {
            ObjectNode merged = JsonNodeFactory.instance.objectNode();
            merged.put("merged", true);
            return merged;
        };

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.TOOL_CALL && resp.getDataContent().has("merged"))
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.ANSWER_CONTENT)
                .verifyComplete();
    }
}
