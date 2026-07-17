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
package pro.chenggang.project.reactive.ai.lite.client.openai.provider.speech;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import pro.chenggang.project.reactive.ai.lite.client.openai.certification.OrganizationTokenCertification;
import pro.chenggang.project.reactive.ai.lite.client.openai.dto.OpenaiSpeechRequest;
import pro.chenggang.project.reactive.ai.lite.client.openai.provider.OpenaiLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.UriTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmSpeechRequestData;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.delegate.LlmSpeechProviderDelegate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * Default implementation of the {@link LlmSpeechProviderDelegate} for OpenAI speech models.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class OpenaiSpeechProviderDelegate implements LlmSpeechProviderDelegate {

    /**
     * The LLM provider info for the speech provider.
     */
    private final LlmProviderInfo llmProviderInfo;

    /**
     * The base URL for the OpenAI API.
     */
    private final String baseUrL;

    /**
     * The speech endpoint for the OpenAI API.
     */
    private final String speechEndpoint;

    /**
     * The WebClient used to make HTTP requests.
     */
    private final WebClient webClient;

    /**
     * Instantiates a new Openai speech provider delegate.
     *
     * @param webClientBuilder the web client builder
     * @param baseUrL          the base URL
     * @param speechEndpoint   the speech endpoint
     * @param isDefault        whether this is the default provider
     * @param name             the provider name
     * @param supportedModels  the supported models
     * @param certifications   the token certifications
     */
    @Builder
    private OpenaiSpeechProviderDelegate(@NonNull WebClient.Builder webClientBuilder,
                                         @NonNull String baseUrL,
                                         @NonNull String speechEndpoint,
                                         boolean isDefault,
                                         @NonNull String name,
                                         Set<String> supportedModels,
                                         @NonNull List<TokenCertification> certifications) {
        this.baseUrL = baseUrL;
        this.speechEndpoint = speechEndpoint;
        this.webClient = webClientBuilder.baseUrl(baseUrL).build();
        this.llmProviderInfo = OpenaiLlmProviderInfo.builder()
                .isDefault(isDefault)
                .name(name)
                .supportedModels(supportedModels)
                .profiles(certifications.stream().map(TokenCertification::profile).collect(Collectors.toSet()))
                .baseUrl(baseUrL)
                .endpoint(speechEndpoint)
                .build();
    }

    /**
     * Retrieves the LLM provider info.
     *
     * @return the LLM provider info
     */
    @Override
    public LlmProviderInfo providerInfo() {
        return this.llmProviderInfo;
    }

    /**
     * Initializes the request body for the speech generation request.
     *
     * @param llmSpeechRequestData the LLM speech request data
     * @return the ObjectNode representing the request body
     */
    @Override
    public ObjectNode initializeRequestBody(@NonNull LlmSpeechRequestData llmSpeechRequestData) {
        return OBJECT_MAPPER.valueToTree(this.buildRequest(llmSpeechRequestData));
    }

    /**
     * Extracts the general response (non-streaming) from the WebClient response.
     *
     * @param responseSpec     the WebClient response specification
     * @param executionContext the execution context
     * @return a Mono emitting the combined DataBuffer of the audio response
     */
    @Override
    public Mono<DataBuffer> extractGeneralResponse(WebClient.ResponseSpec responseSpec, ExecutionContext executionContext) {
        return DataBufferUtils.join(responseSpec.bodyToFlux(DataBuffer.class));
    }

    /**
     * Extracts the stream response from the WebClient response.
     *
     * @param responseSpec     the WebClient response specification
     * @param executionContext the execution context
     * @return a Flux emitting the streaming DataBuffers of the audio response
     */
    @Override
    public Flux<DataBuffer> extractStreamResponse(WebClient.ResponseSpec responseSpec, ExecutionContext executionContext) {
        return responseSpec.bodyToFlux(DataBuffer.class);
    }

    /**
     * Loads and configures the RequestBodySpec for the speech request, applying headers and certifications.
     *
     * @param llmSpeechRequestData the LLM speech request data
     * @return the configured RequestBodySpec
     * @throws IllegalStateException if no token certification is provided
     */
    @Override
    public RequestBodySpec loadRequestBodySpec(@NonNull LlmSpeechRequestData llmSpeechRequestData) {
        AtomicBoolean certificationSet = new AtomicBoolean(false);
        RequestBodyUriSpec requestBodyUriSpec = this.webClient.post();
        Optional<TokenCertification> optionalTokenCertification = llmSpeechRequestData.getTokenCertification();
        if (optionalTokenCertification.isEmpty()) {
            throw new IllegalStateException("At least one token certification is required for the speech request.");
        }
        TokenCertification tokenCertification = optionalTokenCertification.get();
        RequestBodySpec requestBodySpec = requestBodyUriSpec.uri(uriBuilder -> {
            uriBuilder.path(this.speechEndpoint);
            if (tokenCertification instanceof UriTokenCertification uriTokenCertification) {
                uriTokenCertification.applyTo(uriBuilder);
                certificationSet.set(true);
            }
            return uriBuilder.build();
        });
        requestBodySpec.contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, "reactive-ai-lite")
                .acceptCharset(StandardCharsets.UTF_8)
                .accept(MediaType.APPLICATION_OCTET_STREAM);

        if (tokenCertification instanceof BearerTokenCertification bearerTokenCertification) {
            if (!certificationSet.get()) {
                requestBodySpec.headers(bearerTokenCertification::applyTo);
                certificationSet.set(true);
            }
        } else if (tokenCertification instanceof OrganizationTokenCertification organizationTokenCertification) {
            if (!certificationSet.get()) {
                requestBodySpec.headers(organizationTokenCertification::applyTo);
                certificationSet.set(true);
            }
        }
        if (!certificationSet.get()) {
            log.warn("No token certification be applied, cause of the unknown TokenCertification : {}", tokenCertification);
        }
        return requestBodySpec;
    }

    /**
     * Returns a string representation of the delegate.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "OpenaiSpeechProviderDelegate{" +
                "llmProviderInfo=" + llmProviderInfo +
                ", baseUrL='" + baseUrL + '\'' +
                ", speechEndpoint='" + speechEndpoint + '\'' +
                '}';
    }

    /**
     * Builds the OpenaiSpeechRequest from the generalized LlmSpeechRequestData.
     *
     * @param llmSpeechRequestData the LLM speech request data
     * @return the specialized OpenaiSpeechRequest
     */
    protected OpenaiSpeechRequest buildRequest(LlmSpeechRequestData llmSpeechRequestData) {
        return OpenaiSpeechRequest.builder()
                .model(llmSpeechRequestData.getModelName())
                .input(llmSpeechRequestData.getInput())
                .voice(llmSpeechRequestData.getVoice())
                .responseFormat(llmSpeechRequestData.getResponseFormat())
                .speed(llmSpeechRequestData.getSpeed())
                .build();
    }
}
