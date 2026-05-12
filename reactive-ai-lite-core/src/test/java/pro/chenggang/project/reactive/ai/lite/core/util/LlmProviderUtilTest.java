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

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import pro.chenggang.project.reactive.ai.lite.core.exception.ClientResponseErrorException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmProviderUtilTest {

    @Test
    void testHandleClientResponseError4xx() {
        ClientResponse response = mock(ClientResponse.class);
        when(response.statusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(response.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(response.headers().asHttpHeaders()).thenReturn(new HttpHeaders());

        byte[] body = "{\"error\": \"bad request\"}".getBytes(StandardCharsets.UTF_8);
        when(response.bodyToMono(any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(body));

        Mono<Throwable> result = LlmProviderUtil.handleClientResponseError(response);

        StepVerifier.create(result)
                .expectError(ClientResponseErrorException.class)
                .verify();
    }

    @Test
    void testHandleClientResponseError5xx() {
        ClientResponse response = mock(ClientResponse.class);
        when(response.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(response.headers()).thenReturn(mock(ClientResponse.Headers.class));
        when(response.headers().asHttpHeaders()).thenReturn(new HttpHeaders());

        byte[] body = "Internal Server Error".getBytes(StandardCharsets.UTF_8);
        when(response.bodyToMono(any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(body));

        Mono<Throwable> result = LlmProviderUtil.handleClientResponseError(response);

        StepVerifier.create(result)
                .expectError(ClientResponseErrorException.class)
                .verify();
    }
}
