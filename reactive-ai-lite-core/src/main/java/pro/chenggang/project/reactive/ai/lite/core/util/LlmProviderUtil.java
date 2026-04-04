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

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.springframework.web.reactive.function.client.ClientResponse;
import pro.chenggang.project.reactive.ai.lite.core.exception.ClientResponseErrorException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Utility class for common tasks related to LLM provider interactions.
 * <p>
 * This class primarily provides standardized mechanisms for handling HTTP errors
 * returned by AI provider APIs during reactive WebClient operations.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public abstract class LlmProviderUtil {


    /**
     * Extracts and handles error responses from a WebClient {@link ClientResponse}.
     * <p>
     * This method reads the error response body, formats an error message including
     * the HTTP status and body content, and maps the error to an appropriate
     * {@link RestClientResponseException} (e.g., HttpClientErrorException for 4xx,
     * HttpServerErrorException for 5xx). The resulting exception is then wrapped
     * into a custom {@link ClientResponseErrorException} within the reactive stream.
     * </p>
     *
     * @param clientResponse the HTTP client response representing the error
     * @return a {@link Mono} emitting the mapped error exception
     */
    public static Mono<Throwable> handleClientResponseError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(new ParameterizedTypeReference<byte[]>() {})
                .defaultIfEmpty(new byte[0])
                .<Throwable>flatMap(bodyBytes -> {
                    HttpStatusCode httpStatusCode = clientResponse.statusCode();
                    HttpStatus httpStatus = HttpStatus.resolve(httpStatusCode.value());
                    HttpHeaders httpHeaders = clientResponse.headers()
                            .asHttpHeaders();
                    String reasonPhrase = httpStatus == null ? "UNKNOWN" : httpStatus.getReasonPhrase();
                    int httpStatusValue = httpStatus == null ? -1 : httpStatus.value();
                    Charset charset = getCharset(httpHeaders);
                    String errorMessage = getErrorMessage(httpStatusValue, reasonPhrase, bodyBytes, charset);
                    if (Objects.nonNull(httpStatus)) {
                        switch (httpStatus.series()) {
                            case CLIENT_ERROR -> {
                                return Mono.error(HttpClientErrorException.create(errorMessage,
                                        httpStatus,
                                        reasonPhrase,
                                        httpHeaders,
                                        bodyBytes,
                                        charset
                                ));
                            }
                            case SERVER_ERROR -> {
                                return Mono.error(HttpServerErrorException.create(errorMessage,
                                        httpStatus,
                                        reasonPhrase,
                                        httpHeaders,
                                        bodyBytes,
                                        charset
                                ));
                            }
                        }
                    }
                    return Mono.error(new UnknownHttpStatusCodeException(errorMessage,
                            httpStatusValue,
                            reasonPhrase,
                            httpHeaders,
                            bodyBytes,
                            charset
                    ));
                })
                .onErrorMap(RestClientResponseException.class, ClientResponseErrorException::new);
    }

    /**
     * Formats a human-readable error message combining the status code, status text, and response body.
     *
     * @param rawStatusCode the raw HTTP status code
     * @param statusText    the HTTP status reason phrase
     * @param responseBody  the raw byte array of the response body
     * @param charset       the character set to use for decoding the body
     * @return a formatted error message string
     */
    protected static String getErrorMessage(int rawStatusCode, String statusText, byte[] responseBody, Charset charset) {
        String preface = rawStatusCode + " " + statusText + ": ";
        if (ObjectUtils.isEmpty(responseBody)) {
            return preface + "[no body]";
        }
        charset = (charset != null ? charset : StandardCharsets.UTF_8);
        String bodyText = new String(responseBody, charset);
        bodyText = LogFormatUtils.formatValue(bodyText, -1, true);
        return preface + bodyText;
    }

    /**
     * Extracts the character set from the HTTP headers, if present.
     *
     * @param headers the HTTP headers to inspect
     * @return the {@link Charset}, or null if not explicitly defined
     */
    protected static Charset getCharset(HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        return (contentType != null ? contentType.getCharset() : null);
    }

}
