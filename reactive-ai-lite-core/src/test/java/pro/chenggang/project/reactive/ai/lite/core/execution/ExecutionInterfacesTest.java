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
package pro.chenggang.project.reactive.ai.lite.core.execution;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawResponseConverter;
import pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawStreamResponseConverter;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.EmbeddingResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StructuredResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;

class ExecutionInterfacesTest {

    @Test
    void testEmbeddingExecutionDefaultMethod() {
        EmbeddingExecution execution = new EmbeddingExecution() {
            @Override
            public Mono<EmbeddingResponse> execute() {
                return Mono.empty();
            }

            @Override
            public Mono<RawResponse> executeRaw() {
                RawResponse rawResponse = mock(RawResponse.class);
                return Mono.just(rawResponse);
            }
        };

        RawResponseConverter<String> converter = raw -> "converted";

        StepVerifier.create(execution.execute(converter))
                .expectNext("converted")
                .verifyComplete();
    }

    @Test
    void testGeneralExecutionDefaultMethod() {
        GeneralExecution execution = new GeneralExecution() {
            @Override
            public Mono<GeneralResponse> execute() {
                return Mono.empty();
            }

            @Override
            public Mono<RawResponse> executeRaw() {
                RawResponse rawResponse = mock(RawResponse.class);
                return Mono.just(rawResponse);
            }
        };

        RawResponseConverter<String> converter = raw -> "converted";

        StepVerifier.create(execution.execute(converter))
                .expectNext("converted")
                .verifyComplete();
    }

    @Test
    void testStreamExecutionDefaultMethod() {
        StreamExecution execution = new StreamExecution() {
            @Override
            public Flux<StreamResponse> execute() {
                return Flux.empty();
            }

            @Override
            public Flux<RawStreamResponse> executeRaw() {
                RawStreamResponse rawResponse = mock(RawStreamResponse.class);
                return Flux.just(rawResponse);
            }
        };

        RawStreamResponseConverter<String> converter = raw -> "converted";

        StepVerifier.create(execution.execute(converter))
                .expectNext("converted")
                .verifyComplete();
    }

    @Test
    void testStructuredExecutionDefaultMethods() {
        StructuredExecution execution = new StructuredExecution() {
            @Override
            public <R> Mono<StructuredResponse<R>> execute(Class<R> resultType) {
                return Mono.empty();
            }

            @Override
            public <R> Mono<StructuredResponse<R>> execute(ParameterizedTypeReference<R> resultType) {
                return Mono.empty();
            }

            @Override
            public Mono<RawResponse> executeRaw(String responseJsonSchema) {
                return Mono.just(mock(RawResponse.class));
            }

            @Override
            public <R> Mono<RawResponse> executeRaw(Class<R> resultType) {
                return Mono.just(mock(RawResponse.class));
            }

            @Override
            public <R> Mono<RawResponse> executeRaw(ParameterizedTypeReference<R> resultType) {
                return Mono.just(mock(RawResponse.class));
            }
        };

        RawResponseConverter<String> converter = raw -> "converted";

        StepVerifier.create(execution.execute("schema", converter))
                .expectNext("converted")
                .verifyComplete();

        StepVerifier.create(execution.execute(String.class, converter))
                .expectNext("converted")
                .verifyComplete();

        StepVerifier.create(execution.execute(new ParameterizedTypeReference<String>() {}, converter))
                .expectNext("converted")
                .verifyComplete();
    }
}
