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
import pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawResponseConverter;
import pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawStreamResponseConverter;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExecutionInterfaceTest {

    @Test
    void testGeneralExecutionDefault() {
        GeneralExecution execution = mock(GeneralExecution.class);
        RawResponse rawResponse = mock(RawResponse.class);
        when(execution.executeRaw()).thenReturn(Mono.just(rawResponse));
        
        RawResponseConverter<String> converter = resp -> "converted";
        when(execution.execute(any())).thenCallRealMethod();
        
        StepVerifier.create(execution.execute(converter))
                .expectNext("converted")
                .verifyComplete();
    }

    @Test
    void testStreamExecutionDefault() {
        StreamExecution execution = mock(StreamExecution.class);
        RawStreamResponse chunk = mock(RawStreamResponse.class);
        when(execution.executeRaw()).thenReturn(Flux.just(chunk));
        
        RawStreamResponseConverter<String> converter = resp -> "converted";
        when(execution.execute(any())).thenCallRealMethod();
        
        StepVerifier.create(execution.execute(converter))
                .expectNext("converted")
                .verifyComplete();
    }
}
