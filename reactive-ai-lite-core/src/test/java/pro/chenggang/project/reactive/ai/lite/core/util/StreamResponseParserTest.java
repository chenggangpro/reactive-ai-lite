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
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

class StreamResponseParserTest {

    @Test
    void testParseStreamResponseStandard() {
        ExecutionContext mockContext = mock(ExecutionContext.class);
        
        Flux<String> rawStream = Flux.just(
                "{\"type\":\"ANSWER_CONTENT\",\"text\":\"Hello\"}",
                "{\"type\":\"ANSWER_CONTENT\",\"text\":\" World\"}",
                "[DONE]"
        );

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> {
            ObjectNode node = data.getData();
            StreamDataType type = StreamDataType.valueOf(node.get("type").asText());
            return new StreamResponseParser.JsonStreamChunkSlide[]{
                    StreamResponseParser.JsonStreamChunkSlide.builder()
                            .streamDataType(type)
                            .dataContent(node)
                            .build()
            };
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> {
            ObjectNode merged = OBJECT_MAPPER.createObjectNode();
            nodes.forEach(n -> JsonChunkMerger.merge(merged, n));
            return merged;
        };

        Flux<RawStreamResponse> result = StreamResponseParser.parseStreamResponse(mockContext, rawStream, parser, merger);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getDataType()).isEqualTo(StreamDataType.ANSWER_CONTENT);
                    assertThat(response.getDataContent().get("text").asText()).isEqualTo("Hello");
                })
                .assertNext(response -> {
                    assertThat(response.getDataType()).isEqualTo(StreamDataType.ANSWER_CONTENT);
                    assertThat(response.getDataContent().get("text").asText()).isEqualTo(" World");
                })
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseToolCallMerging() {
        ExecutionContext mockContext = mock(ExecutionContext.class);
        
        Flux<String> rawStream = Flux.just(
                "{\"type\":\"TOOL_CALL\",\"text\":\"call1\"}",
                "{\"type\":\"TOOL_CALL\",\"text\":\"call2\"}",
                "[DONE]"
        );

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> {
            ObjectNode node = data.getData();
            StreamDataType type = StreamDataType.valueOf(node.get("type").asText());
            return new StreamResponseParser.JsonStreamChunkSlide[]{
                    StreamResponseParser.JsonStreamChunkSlide.builder()
                            .streamDataType(type)
                            .dataContent(node)
                            .build()
            };
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> {
            ObjectNode merged = OBJECT_MAPPER.createObjectNode();
            nodes.forEach(n -> JsonChunkMerger.merge(merged, n));
            return merged;
        };

        Flux<RawStreamResponse> result = StreamResponseParser.parseStreamResponse(mockContext, rawStream, parser, merger);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getDataType()).isEqualTo(StreamDataType.TOOL_CALL);
                    assertThat(response.getDataContent().get("text").asText()).isEqualTo("call1call2");
                })
                .verifyComplete();
    }

    @Test
    void testParseStreamResponseNotObjectJson() {
        ExecutionContext mockContext = mock(ExecutionContext.class);
        
        Flux<String> rawStream = Flux.just(
                "{\"type\":\"ANSWER_CONTENT\",\"text\":\"Hello\"}",
                "[]"
        );

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> {
            ObjectNode node = data.getData();
            return new StreamResponseParser.JsonStreamChunkSlide[]{
                    StreamResponseParser.JsonStreamChunkSlide.builder()
                            .streamDataType(StreamDataType.ANSWER_CONTENT)
                            .dataContent(node)
                            .build()
            };
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> OBJECT_MAPPER.createObjectNode();

        Flux<RawStreamResponse> result = StreamResponseParser.parseStreamResponse(mockContext, rawStream, parser, merger);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalStateException && throwable.getMessage().contains("Invalid JSON chunk"))
                .verify();
    }

    @Test
    void testParseStreamResponseMalformedJson() {
        ExecutionContext mockContext = mock(ExecutionContext.class);
        
        Flux<String> rawStream = Flux.just(
                "{\"type\":\"ANSWER_CONTENT\",\"text\":\"Hello\"}",
                "invalid_json"
        );

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> {
            ObjectNode node = data.getData();
            return new StreamResponseParser.JsonStreamChunkSlide[]{
                    StreamResponseParser.JsonStreamChunkSlide.builder()
                            .streamDataType(StreamDataType.ANSWER_CONTENT)
                            .dataContent(node)
                            .build()
            };
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> OBJECT_MAPPER.createObjectNode();

        Flux<RawStreamResponse> result = StreamResponseParser.parseStreamResponse(mockContext, rawStream, parser, merger);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> {
                    Throwable root = reactor.core.Exceptions.unwrap(throwable);
                    return root instanceof com.fasterxml.jackson.core.JsonParseException;
                })
                .verify();
    }

    @Test
    void testParseStreamResponseExceptionInParser() {
        ExecutionContext mockContext = mock(ExecutionContext.class);
        
        Flux<String> rawStream = Flux.just(
                "{\"type\":\"ANSWER_CONTENT\",\"text\":\"Hello\"}"
        );

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> {
            throw new RuntimeException("Parser error");
        };

        Function<List<ObjectNode>, ObjectNode> merger = nodes -> OBJECT_MAPPER.createObjectNode();

        Flux<RawStreamResponse> result = StreamResponseParser.parseStreamResponse(mockContext, rawStream, parser, merger);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Parser error"))
                .verify();
    }

    @Test
    void testJsonChunkParsingDataAttributes() {
        StreamResponseParser.JsonChunkParsingData data = StreamResponseParser.JsonChunkParsingData.builder()
                .data(OBJECT_MAPPER.createObjectNode())
                .parsingAttributes(Map.of("key1", "val1"))
                .build();

        assertThat((String) data.getParsingAttribute("key1")).isEqualTo("val1");
        assertThat((String) data.getParsingAttribute("key2")).isNull();
        assertThat(data.getParsingAttributeOrDefault("key2", "defaultVal")).isEqualTo("defaultVal");
    }
    @Test
    void testParseStreamResponseNullJson() {
        ExecutionContext mockContext = mock(ExecutionContext.class);
        
        Flux<String> rawStream = Flux.just("null");

        Function<StreamResponseParser.JsonChunkParsingData, StreamResponseParser.JsonStreamChunkSlide[]> parser = data -> new StreamResponseParser.JsonStreamChunkSlide[0];
        Function<List<ObjectNode>, ObjectNode> merger = nodes -> OBJECT_MAPPER.createObjectNode();

        Flux<RawStreamResponse> result = StreamResponseParser.parseStreamResponse(mockContext, rawStream, parser, merger);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalStateException && throwable.getMessage().contains("Invalid JSON chunk"))
                .verify();
    }
}
