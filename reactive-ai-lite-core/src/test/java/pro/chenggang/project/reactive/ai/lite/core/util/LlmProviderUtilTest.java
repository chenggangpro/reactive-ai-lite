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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import pro.chenggang.project.reactive.ai.lite.core.exception.ClientResponseErrorException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmProviderUtilTest {

    @Test
    void testHandleClientResponseErrorWithClientError() {
        ClientResponse response = mockClientResponse(HttpStatus.BAD_REQUEST, "{\"error\":\"bad\"}");

        Mono<Throwable> errorMono = LlmProviderUtil.handleClientResponseError(response);

        StepVerifier.create(errorMono)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ClientResponseErrorException.class);
                    assertThat(throwable.getMessage()).contains("400 Bad Request");
                    assertThat(throwable.getMessage()).contains("\"error\":\"bad\"");
                })
                .verify();
    }

    @Test
    void testHandleClientResponseErrorWithServerError() {
        ClientResponse response = mockClientResponse(HttpStatus.INTERNAL_SERVER_ERROR, "server down");

        Mono<Throwable> errorMono = LlmProviderUtil.handleClientResponseError(response);

        StepVerifier.create(errorMono)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ClientResponseErrorException.class);
                    assertThat(throwable.getMessage()).contains("500 Internal Server Error");
                    assertThat(throwable.getMessage()).contains("server down");
                })
                .verify();
    }

    @Test
    void testHandleClientResponseErrorWithUnknownError() {
        ClientResponse response = mockClientResponse(HttpStatusCode.valueOf(600), "unknown");

        Mono<Throwable> errorMono = LlmProviderUtil.handleClientResponseError(response);

        StepVerifier.create(errorMono)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ClientResponseErrorException.class);
                    assertThat(throwable.getMessage()).contains("600 UNKNOWN");
                    assertThat(throwable.getMessage()).contains("unknown");
                })
                .verify();
    }

    @Test
    void testHandleClientResponseErrorWithEmptyBody() {
        ClientResponse response = mockClientResponse(HttpStatus.NOT_FOUND, null);

        Mono<Throwable> errorMono = LlmProviderUtil.handleClientResponseError(response);

        StepVerifier.create(errorMono)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ClientResponseErrorException.class);
                    assertThat(throwable.getMessage()).contains("404 Not Found");
                    assertThat(throwable.getMessage()).contains("[no body]");
                })
                .verify();
    }

    private ClientResponse mockClientResponse(HttpStatusCode statusCode, String body) {
        ClientResponse response = mock(ClientResponse.class);
        ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        when(response.statusCode()).thenReturn(statusCode);
        when(response.headers()).thenReturn(headers);
        when(headers.asHttpHeaders()).thenReturn(httpHeaders);

        if (body != null) {
            when(response.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(body.getBytes(StandardCharsets.UTF_8)));
        } else {
            when(response.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.empty());
        }

        return response;
    }
}
