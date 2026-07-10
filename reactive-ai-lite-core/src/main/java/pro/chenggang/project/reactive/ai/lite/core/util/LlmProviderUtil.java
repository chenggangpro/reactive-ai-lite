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
 * Common helper for standardizing error handling when interacting with AI provider APIs.
 * <p>
 * Provides a reactive pipeline that converts non‑success WebClient {@link ClientResponse}s into
 * meaningful, typed exceptions. This ensures that downstream error processors (e.g., global
 * exception handlers) receive consistent, diagnostic‑rich failure information regardless of
 * the particular provider’s error format.
 * </p>
 * <p>
 * The utility reads the raw response body, extracts the charset from the content‑type header,
 * constructs a human‑readable error message, and maps HTTP 4xx/5xx statuses to
 * {@link RestClientResponseException} subclasses (or a fallback for unknown status codes).
 * All resulting exceptions are then wrapped in a {@link ClientResponseErrorException}, which
 * carries the original response details and can be handled uniformly throughout the framework.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public abstract class LlmProviderUtil {

    /**
     * Transforms a non‑successful {@link ClientResponse} into a typed exception wrapped in a
     * {@link Mono} error signal.
     * <p>
     * This method is typically used inside a reactive WebClient chain via
     * {@code .onStatus(status, response -> handleClientResponseError(response))}. It first
     * reads the error body as a byte array (defaulting to an empty array if no body is present),
     * then extracts the HTTP status code, status text, response headers, and character set.
     * Using those, it builds a descriptive error message that includes both the status line and
     * the body content (sanitised for logging).
     * </p>
     * <p>
     * The mapping logic distinguishes between client errors (4xx) and server errors (5xx),
     * creating an {@link HttpClientErrorException} or {@link HttpServerErrorException}
     * respectively. For any other status code that cannot be resolved to a known {@link HttpStatus},
     * an {@link UnknownHttpStatusCodeException} is used. This ensures that the error type conveys
     * semantic meaning beyond a generic HTTP failure.
     * </p>
     * <p>
     * Finally, any {@link RestClientResponseException} that results from the mapping is caught
     * and wrapped in a {@link ClientResponseErrorException} via {@code .onErrorMap}. This
     * provides a single custom exception type that can be conveniently handled by application‑level
     * error handlers while still preserving the original response data.
     * </p>
     *
     * @param clientResponse the reactive HTTP client response representing the error (status not in 2xx family)
     * @return a {@link Mono} that emits only an error signal; the emitted {@link Throwable} is a
     *         {@link ClientResponseErrorException} wrapping the appropriate Spring exception
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
                    int httpStatusValue = httpStatusCode.value();
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
     * Constructs a human‑readable error message from the HTTP status components and the response body.
     * <p>
     * The message is designed to be useful for both logging and debugging. It prepends the numeric
     * status code and status text, followed by the decoded body content. If the body is empty, a
     * placeholder {@code [no body]} is appended. The body text is formatted using
     * {@code LogFormatUtils#formatValue(String, int, boolean)} to truncate extremely long bodies
     * when necessary (max length is unlimited here, indicated by -1).
     * </p>
     *
     * @param rawStatusCode the raw HTTP status code (e.g., 404)
     * @param statusText    the reason phrase associated with the status (e.g., "Not Found")
     * @param responseBody  the raw byte array of the response body; may be empty or null
     * @param charset       the character set to decode the body; if {@code null}, UTF-8 is used as fallback
     * @return a formatted string beginning with "{@code 404 Not Found: }" followed by body content or "[no body]"
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
     * Retrieves the character set from the HTTP headers' Content‑Type field.
     * <p>
     * Proper charset detection is crucial for correctly decoding error response bodies,
     * especially when the provider returns non‑UTF‑8 content (e.g., legacy APIs). This
     * method parses the {@code Content-Type} header and returns the associated charset,
     * or {@code null} if the header is missing or does not specify a charset.
     * </p>
     *
     * @param headers the HTTP headers from the response
     * @return the {@link Charset} defined in the Content‑Type, or {@code null} if not present
     */
    protected static Charset getCharset(HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        return (contentType != null ? contentType.getCharset() : null);
    }
}