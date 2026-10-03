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
package pro.chenggang.project.reactive.ai.lite.client.ollama.provider.systemone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaSystemOneQuestion;
import pro.chenggang.project.reactive.ai.lite.client.ollama.dto.OllamaSystemOneRequest;
import pro.chenggang.project.reactive.ai.lite.client.ollama.provider.OllamaLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.UriTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.usage.Usage;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmSystemOneRequestData;
import pro.chenggang.project.reactive.ai.lite.core.exception.ResponseMessageExtractFailedException;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneResponse;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Base64Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.ChoiceQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.NoulQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.NoulQuestion.NoulCriteria;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion.ScoreQuestion;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.delegate.LlmSystemOneProviderDelegate;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * Implementation of {@link LlmSystemOneProviderDelegate} for communicating with
 * Ollama's SystemOne decision endpoint ({@code POST /v1/systemone}).
 * <p>
 * This delegate prepares HTTP requests matching the Ollama SystemOne wire format,
 * transforms generic {@link LlmSystemOneRequestData} into JSON payloads including optional
 * Base64 image attachments, handles optional authentication, and parses raw responses into
 * standardized {@link SystemOneResponse} objects.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmSystemOneProviderDelegate
 * @see OllamaSystemOneRequest
 * @since 0.1.0
 */
@Slf4j
public class OllamaSystemOneProviderDelegate implements LlmSystemOneProviderDelegate {

    /**
     * Provider metadata descriptor holding provider identity, base URL, and profile names.
     */
    private final LlmProviderInfo llmProviderInfo;

    /**
     * The base URL for the Ollama API (e.g. {@code http://localhost:11434}).
     */
    private final String baseUrl;

    /**
     * The endpoint path for SystemOne evaluation operations (e.g. {@code /v1/systemone}).
     */
    private final String systemOneEndpoint;

    /**
     * Pre-configured {@link WebClient} targeting the specified {@link #baseUrl}.
     */
    private final WebClient webClient;

    /**
     * Constructs a new {@link OllamaSystemOneProviderDelegate} instance.
     *
     * @param webClientBuilder  the reactive {@link WebClient.Builder}; must not be null
     * @param baseUrl           the API base URL; must not be null
     * @param systemOneEndpoint the SystemOne endpoint path; must not be null
     * @param isDefault         whether this provider is marked as default
     * @param name              the logical provider name; must not be null
     * @param supportedModels   optional set of supported model names
     * @param certifications    list of configured token certifications; must not be null
     */
    @Builder
    private OllamaSystemOneProviderDelegate(@NonNull WebClient.Builder webClientBuilder,
                                            @NonNull String baseUrl,
                                            @NonNull String systemOneEndpoint,
                                            boolean isDefault,
                                            @NonNull String name,
                                            Set<String> supportedModels,
                                            @NonNull List<TokenCertification> certifications) {
        this.baseUrl = baseUrl;
        this.systemOneEndpoint = systemOneEndpoint;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.llmProviderInfo = OllamaLlmProviderInfo.builder()
                .isDefault(isDefault)
                .name(name)
                .supportedModels(supportedModels)
                .profiles(certifications.stream().map(TokenCertification::profile).collect(Collectors.toSet()))
                .baseUrl(baseUrl)
                .endpoint(systemOneEndpoint)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LlmProviderInfo providerInfo() {
        return this.llmProviderInfo;
    }

    /**
     * {@inheritDoc}
     * <p>
     * For local Ollama instances, authentication is optional. This override bypasses mandatory
     * token verification so that requests without an API token succeed against local servers.
     * </p>
     */
    @Override
    public void checkTokenCertification(LlmSystemOneRequestData llmSystemOneRequestData) {
        // No-op: Local Ollama calls do not require token certifications
    }

    /**
     * {@inheritDoc}
     * <p>
     * Constructs a POST request targeting Ollama's SystemOne endpoint with standard headers
     * ({@code Content-Type: application/json}, {@code User-Agent: reactive-ai-lite},
     * {@code Accept: application/json}, {@code Accept-Charset: UTF-8}) and applies any
     * configured token certification (URI-based or Bearer header).
     * </p>
     */
    @Override
    public RequestBodySpec loadRequestBodySpec(@NonNull LlmSystemOneRequestData llmSystemOneRequestData) {
        this.checkTokenCertification(llmSystemOneRequestData);
        RequestBodyUriSpec requestBodyUriSpec = this.webClient.post();
        RequestBodySpec requestBodySpec = requestBodyUriSpec.uri(uriBuilder -> {
            uriBuilder.path(this.systemOneEndpoint);
            llmSystemOneRequestData.getTokenCertification()
                    .ifPresent(tokenCertification -> {
                        if (tokenCertification instanceof UriTokenCertification uriTokenCertification) {
                            uriTokenCertification.applyTo(uriBuilder);
                        }
                    });
            return uriBuilder.build();
        });
        requestBodySpec.contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, "reactive-ai-lite")
                .acceptCharset(StandardCharsets.UTF_8)
                .accept(MediaType.APPLICATION_JSON);
        llmSystemOneRequestData.getTokenCertification()
                .ifPresent(token -> this.applyStandardTokenCertification(requestBodySpec, token));
        return requestBodySpec;
    }

    /**
     * Extracts the number of prompt (input) tokens used from the raw usage JSON data.
     *
     * @param rawUsage the raw JSON object representing usage statistics; must not be null
     * @return the number of prompt tokens consumed, or 0 if missing or invalid
     */
    protected Integer extractPromptTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/input_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        jsonNode = rawUsage.at("/prompt_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    /**
     * Extracts the number of completion (output) tokens used from the raw usage JSON data.
     *
     * @param rawUsage the raw JSON object representing usage statistics; must not be null
     * @return the number of completion tokens consumed, or 0 if missing or invalid
     */
    protected Integer extractCompletionTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/output_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        jsonNode = rawUsage.at("/completion_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            return jsonNode.intValue();
        }
        return 0;
    }

    /**
     * Extracts the number of other tokens used from the raw usage JSON data.
     *
     * @param rawUsage the raw JSON object representing usage statistics; must not be null
     * @return the number of other tokens consumed, or 0 if missing or invalid
     */
    protected Integer extractOtherTokenUsage(ObjectNode rawUsage) {
        JsonNode jsonNode = rawUsage.at("/total_tokens");
        if (!jsonNode.isMissingNode() && (jsonNode.isIntegralNumber() || jsonNode.isInt())) {
            int totalTokens = jsonNode.intValue();
            int promptTokens = this.extractPromptTokenUsage(rawUsage);
            int completionTokens = this.extractCompletionTokenUsage(rawUsage);
            return Math.max(0, totalTokens - promptTokens - completionTokens);
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Converts the structured {@link LlmSystemOneRequestData} into an Ollama SystemOne
     * JSON payload containing {@code model}, {@code state}, optional {@code images}, and
     * {@code questions}. Validates image MIME types via {@link #checkImagesMimeType(LlmSystemOneRequestData)}.
     * </p>
     */
    @Override
    public ObjectNode initializeRequestBody(@NonNull LlmSystemOneRequestData llmSystemOneRequestData) {
        this.checkImagesMimeType(llmSystemOneRequestData);
        OllamaSystemOneRequest request = this.buildRequest(llmSystemOneRequestData);
        return OBJECT_MAPPER.valueToTree(request);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Parses the raw JSON response from Ollama into a standardized {@link SystemOneResponse}.
     * Verifies that the {@code answers} node exists and is an object; emits a
     * {@link ResponseMessageExtractFailedException} if missing or malformed.
     * Maps evaluated answers for {@code noul}, {@code choice}, and {@code score} question types,
     * and extracts token usage information.
     * </p>
     *
     * @param rawResponse the raw HTTP response wrapper containing status, headers, and the JSON body; must not be null
     * @return a {@link Mono} that emits the parsed {@link SystemOneResponse} or an error if extraction fails
     */
    @Override
    public Mono<SystemOneResponse> extractGeneralResponse(@NonNull RawResponse rawResponse) {
        return Mono.fromCallable(rawResponse::getResponseBody)
                .handle((rawResponseBody, syncSink) -> {
                    var systemOneResponseBuilder = SystemOneResponse.builder()
                            .executionContext(rawResponse.getExecutionContext())
                            .rawResponseBody(rawResponseBody);
                    JsonNode answersNode = rawResponseBody.at("/answers");
                    if (answersNode.isMissingNode() || !answersNode.isObject()) {
                        log.error("Failed to extract answers from response body. Response body: {}", rawResponseBody.toPrettyString());
                        syncSink.error(new ResponseMessageExtractFailedException(rawResponseBody));
                        return;
                    }
                    JsonNode usageNode = rawResponseBody.at("/usage");
                    if (!usageNode.isMissingNode() && usageNode.isObject() && !usageNode.isNull()) {
                        Usage usage = Usage.newUsageBuilder((ObjectNode) usageNode)
                                .promptTokensExtractor(this::extractPromptTokenUsage)
                                .completionTokensExtractor(this::extractCompletionTokenUsage)
                                .otherTokensExtractor(this::extractOtherTokenUsage)
                                .build();
                        systemOneResponseBuilder.usage(usage);
                    }
                    Map<String, SystemOneAnswer> answers = new LinkedHashMap<>();
                    for (Entry<String, JsonNode> entry : answersNode.properties()) {
                        String questionId = entry.getKey();
                        JsonNode answerNode = entry.getValue();
                        if (Objects.isNull(answerNode) || !answerNode.isObject()) {
                            continue;
                        }
                        JsonNode typeNode = answerNode.at("/type");
                        if (typeNode.isMissingNode() || !typeNode.isTextual()) {
                            continue;
                        }
                        String type = typeNode.asText();
                        switch (type.toLowerCase()) {
                            case "noul" -> {
                                JsonNode valueNode = answerNode.at("/noul");
                                if (valueNode.isMissingNode() || valueNode.isNull() || !valueNode.isNumber()) {
                                    valueNode = answerNode.at("/scale");
                                }
                                if (!valueNode.isMissingNode() && !valueNode.isNull() && valueNode.isNumber()) {
                                    Number number = valueNode.numberValue();
                                    double scale = number.doubleValue();
                                    answers.put(questionId, SystemOneAnswer.NoulAnswer.builder()
                                            .scale(scale)
                                            .build()
                                    );
                                }
                            }
                            case "choice" -> {
                                String chosen = "";
                                JsonNode choiceNode = answerNode.at("/choice");
                                if (!choiceNode.isMissingNode() && !choiceNode.isNull() && choiceNode.isTextual()) {
                                    chosen = choiceNode.asText();
                                }
                                double confidence = 0.0;
                                JsonNode confidenceNode = answerNode.at("/confidence");
                                if (!confidenceNode.isMissingNode() && !confidenceNode.isNull() && confidenceNode.isNumber()) {
                                    confidence = confidenceNode.numberValue().doubleValue();
                                }
                                List<SystemOneAnswer.ChoiceAnswer.Probability> probabilities = new ArrayList<>();
                                JsonNode probsNode = answerNode.at("/probabilities");
                                if (!probsNode.isMissingNode() && !probsNode.isNull() && probsNode.isObject()) {
                                    for (Entry<String, JsonNode> probEntry : probsNode.properties()) {
                                        JsonNode probValueNode = probEntry.getValue();
                                        double probability = 0.0;
                                        if (Objects.nonNull(probValueNode) && !probValueNode.isMissingNode() && !probValueNode.isNull() && probValueNode.isNumber()) {
                                            probability = probValueNode.numberValue().doubleValue();
                                        }
                                        probabilities.add(SystemOneAnswer.ChoiceAnswer.Probability.builder()
                                                .key(probEntry.getKey())
                                                .probability(probability)
                                                .build());
                                    }
                                }
                                answers.put(questionId, SystemOneAnswer.ChoiceAnswer.builder()
                                        .choice(chosen)
                                        .confidence(confidence)
                                        .probabilities(probabilities)
                                        .build()
                                );
                            }
                            case "score" -> {
                                double score = 0.0;
                                JsonNode scoreNode = answerNode.at("/score");
                                if (!scoreNode.isMissingNode() && !scoreNode.isNull() && scoreNode.isNumber()) {
                                    score = scoreNode.numberValue().doubleValue();
                                }
                                double confidence = 0.0;
                                JsonNode confidenceNode = answerNode.at("/confidence");
                                if (!confidenceNode.isMissingNode() && !confidenceNode.isNull() && confidenceNode.isNumber()) {
                                    confidence = confidenceNode.numberValue().doubleValue();
                                }
                                List<SystemOneAnswer.ScoreAnswer.LegendProbability> legendProbs = new ArrayList<>();
                                JsonNode scoreProbsNode = answerNode.at("/probabilities");
                                JsonNode legendNode = answerNode.at("/legend");
                                int size = 0;
                                if (!legendNode.isMissingNode() && !legendNode.isNull()) {
                                    size = Math.max(size, legendNode.size());
                                }
                                if (!scoreProbsNode.isMissingNode() && !scoreProbsNode.isNull()) {
                                    size = Math.max(size, scoreProbsNode.size());
                                }
                                if (size > 0) {
                                    boolean hasLegend = !legendNode.isMissingNode() && !legendNode.isNull();
                                    boolean hasScoreProbs = !scoreProbsNode.isMissingNode() && !scoreProbsNode.isNull();
                                    for (int i = 0; i < size; i++) {
                                        String indexKey = String.valueOf(i);
                                        String levelName = indexKey;
                                        if (hasLegend) {
                                            JsonNode levelNode = legendNode.at("/" + indexKey);
                                            if (!levelNode.isMissingNode() && !levelNode.isNull() && levelNode.isTextual()) {
                                                levelName = levelNode.asText();
                                            }
                                        }
                                        double probability = 0.0;
                                        if (hasScoreProbs) {
                                            JsonNode probValueNode = scoreProbsNode.at("/" + indexKey);
                                            if (!probValueNode.isMissingNode() && !probValueNode.isNull() && probValueNode.isNumber()) {
                                                probability = probValueNode.numberValue().doubleValue();
                                            }
                                        }
                                        legendProbs.add(SystemOneAnswer.ScoreAnswer.LegendProbability.builder()
                                                .level(levelName)
                                                .probability(probability)
                                                .build());
                                    }
                                }
                                answers.put(questionId, SystemOneAnswer.ScoreAnswer.builder()
                                        .score(score)
                                        .confidence(confidence)
                                        .legendProbabilities(legendProbs)
                                        .build()
                                );
                            }
                            default -> log.warn("Unknown question answer type encountered: {}", type);
                        }
                    }
                    systemOneResponseBuilder.answers(answers);
                    SystemOneResponse systemOneResponse = systemOneResponseBuilder.build();
                    syncSink.next(systemOneResponse);
                });
    }

    /**
     * Converts generic {@link LlmSystemOneRequestData} into a strongly typed
     * {@link OllamaSystemOneRequest} payload object.
     *
     * @param requestData the generic SystemOne request data; must not be null
     * @return a populated {@link OllamaSystemOneRequest}
     */
    protected OllamaSystemOneRequest buildRequest(@NonNull LlmSystemOneRequestData requestData) {
        Object stateValue = this.convertContent(requestData.getState());
        if (Objects.isNull(stateValue)) {
            throw new IllegalArgumentException("State value cannot be null for Ollama SystemOne request");
        }
        List<String> images = null;
        if (!requestData.getImages().isEmpty()) {
            images = requestData.getImages()
                    .stream()
                    .map(Base64Attachment::base64Content)
                    .toList();
        }
        Map<String, OllamaSystemOneQuestion> questionsMap = new LinkedHashMap<>();
        requestData.getQuestions().getAllQuestions().forEach((key, question) -> {
            OllamaSystemOneQuestion convertedQuestion = this.convertQuestion(question);
            questionsMap.put(key, convertedQuestion);
        });
        return OllamaSystemOneRequest.builder()
                .model(requestData.getModelName())
                .state(stateValue)
                .images(images)
                .questions(questionsMap)
                .build();
    }

    /**
     * Converts a {@link SystemOneQuestion} into an {@link OllamaSystemOneQuestion}.
     *
     * @param question the domain question model; must not be null
     * @return the corresponding {@link OllamaSystemOneQuestion}
     */
    protected OllamaSystemOneQuestion convertQuestion(@NonNull SystemOneQuestion question) {
        String type = question.type().getValue();
        Object instructions = this.convertContent(question.instructions());
        Object criteria = switch (question) {
            case NoulQuestion noulQuestion -> {
                NoulCriteria noulCriteria = noulQuestion.criteria();
                if (Objects.isNull(noulCriteria)) {
                    yield null;
                }
                Map<String, Object> criteriaMap = new LinkedHashMap<>();
                if (Objects.nonNull(noulCriteria.getTrueOption())) {
                    criteriaMap.put("true", this.convertContent(noulCriteria.getTrueOption()));
                }
                if (Objects.nonNull(noulCriteria.getFalseOption())) {
                    criteriaMap.put("false", this.convertContent(noulCriteria.getFalseOption()));
                }
                yield criteriaMap.isEmpty() ? null : criteriaMap;
            }
            case ChoiceQuestion choiceQuestion -> {
                Map<String, SystemOneContent<?>> choiceCriteria = choiceQuestion.criteria();
                if (Objects.isNull(choiceCriteria) || choiceCriteria.isEmpty()) {
                    yield null;
                }
                Map<String, Object> criteriaMap = new LinkedHashMap<>();
                choiceCriteria.forEach((optKey, optVal) -> criteriaMap.put(optKey, this.convertContent(optVal)));
                yield criteriaMap;
            }
            case ScoreQuestion scoreQuestion -> {
                List<SystemOneContent<?>> scoreCriteria = scoreQuestion.criteria();
                if (Objects.isNull(scoreCriteria) || scoreCriteria.isEmpty()) {
                    yield null;
                }
                yield scoreCriteria.stream()
                        .map(this::convertContent)
                        .toList();
            }
        };
        return OllamaSystemOneQuestion.builder()
                .type(type)
                .instructions(instructions)
                .criteria(criteria)
                .build();
    }

    /**
     * Converts {@link SystemOneContent} into a plain Java representation (String, Map, List, or null)
     * suitable for Jackson serialization.
     *
     * @param content the content container; may be null
     * @return the unwrapped Java content value, or null if empty
     */
    protected Object convertContent(SystemOneContent<?> content) {
        if (Objects.isNull(content) || content instanceof SystemOneContent.NullContent) {
            return null;
        }
        return content.getValue();
    }

    /**
     * Returns a string representation of this delegate.
     *
     * @return a descriptive string representation
     */
    @Override
    public String toString() {
        return "OllamaSystemOneProviderDelegate{" +
                "llmProviderInfo=" + llmProviderInfo +
                ", baseUrl='" + baseUrl + '\'' +
                ", systemOneEndpoint='" + systemOneEndpoint + '\'' +
                '}';
    }
}
