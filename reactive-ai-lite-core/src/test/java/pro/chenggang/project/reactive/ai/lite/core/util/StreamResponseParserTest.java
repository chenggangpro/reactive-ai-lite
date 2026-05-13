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

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.ANSWER_CONTENT)
                        .dataContent(data.getData())
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

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.ANSWER_CONTENT)
                        .dataContent(data.getData())
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

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> new StreamResponseParser.JsonStreamChunkSlide[0];
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

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.ROLE)
                        .dataContent(data.getData())
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

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> {
            ObjectNode node = data.getData();
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

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.TOOL_CALL)
                        .dataContent(data.getData())
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

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.USAGE)
                        .dataContent(data.getData())
                        .build()
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> nodes.get(0);

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.USAGE)
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseWithMultipleChunks() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<String> rawStream = Flux.just("{\"data\": \"multiple\"}");

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.ROLE)
                        .dataContent(data.getData())
                        .build(),
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.ANSWER_CONTENT)
                        .dataContent(data.getData())
                        .build()
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> nodes.get(0);

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.ROLE)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.ANSWER_CONTENT)
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseEmptyJson() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<String> rawStream = Flux.just("{}");

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> new StreamResponseParser.JsonStreamChunkSlide[0];
        Function<List<ObjectNode>, ObjectNode> merger = nodes -> nodes.get(0);

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseWithReasoning() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<String> rawStream = Flux.just("{\"reasoning\": \"thinking\"}");

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.REASONING_CONTENT)
                        .dataContent(data.getData())
                        .build()
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> nodes.get(0);

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.REASONING_CONTENT)
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseDataTransition() {
        ExecutionContextView contextView = mock(ExecutionContextView.class);
        Flux<String> rawStream = Flux.just("{\"data\": \"transition\"}");

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> new StreamResponseParser.JsonStreamChunkSlide[]{
                StreamResponseParser.JsonStreamChunkSlide.builder()
                        .streamDataType(StreamDataType.ROLE)
                        .dataContent(data.getData())
                        .build()
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> nodes.get(0);

        Flux<RawStreamResponse> parsedStream = StreamResponseParser.parseStreamResponse(contextView, rawStream, parser, merger);

        StepVerifier.create(parsedStream)
                .expectNextMatches(resp -> resp.getDataType() == StreamDataType.ROLE)
                .verifyComplete();
    }
}
