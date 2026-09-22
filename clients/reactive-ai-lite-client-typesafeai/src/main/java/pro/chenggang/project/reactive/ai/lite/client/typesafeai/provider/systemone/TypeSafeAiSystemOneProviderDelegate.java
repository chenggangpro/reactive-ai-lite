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
package pro.chenggang.project.reactive.ai.lite.client.typesafeai.provider.systemone;

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
import pro.chenggang.project.reactive.ai.lite.client.typesafeai.dto.TypeSafeAiQuestion;
import pro.chenggang.project.reactive.ai.lite.client.typesafeai.dto.TypeSafeAiSystemOneRequest;
import pro.chenggang.project.reactive.ai.lite.client.typesafeai.provider.TypeSafeAiLlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.usage.Usage;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmSystemOneRequestData;
import pro.chenggang.project.reactive.ai.lite.core.exception.ResponseMessageExtractFailedException;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneAnswer;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneResponse;
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
 * TypeSafe AI's SystemOne evaluation API ({@code POST https://api.typesafe.ai/v1/systemone}).
 * <p>
 * This delegate prepares HTTP requests matching the TypeSafe AI SystemOne wire format,
 * transforms generic {@link LlmSystemOneRequestData} into JSON payloads, applies Bearer
 * token authentication, and provides the framework for parsing raw responses into
 * strongly typed {@link SystemOneResponse} objects.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmSystemOneProviderDelegate
 * @see TypeSafeAiSystemOneRequest
 * @since 0.1.0
 */
@Slf4j
public class TypeSafeAiSystemOneProviderDelegate implements LlmSystemOneProviderDelegate {

    /**
     * Provider metadata descriptor holding provider identity, base URL, and profile names.
     */
    private final LlmProviderInfo llmProviderInfo;

    /**
     * The base URL for the TypeSafe AI API (e.g. {@code https://api.typesafe.ai}).
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
     * Constructs a new {@link TypeSafeAiSystemOneProviderDelegate} instance.
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
    private TypeSafeAiSystemOneProviderDelegate(@NonNull WebClient.Builder webClientBuilder,
                                                @NonNull String baseUrl,
                                                @NonNull String systemOneEndpoint,
                                                boolean isDefault,
                                                @NonNull String name,
                                                Set<String> supportedModels,
                                                @NonNull List<TokenCertification> certifications) {
        this.baseUrl = baseUrl;
        this.systemOneEndpoint = systemOneEndpoint;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.llmProviderInfo = TypeSafeAiLlmProviderInfo.builder()
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
     * Constructs a POST request targeting the SystemOne endpoint with standard headers
     * ({@code Content-Type: application/json}, {@code User-Agent: reactive-ai-lite},
     * {@code Accept: application/json}, {@code Accept-Charset: UTF-8}) and injects the
     * Bearer token certification from the request data.
     * </p>
     */
    @Override
    public RequestBodySpec loadRequestBodySpec(@NonNull LlmSystemOneRequestData llmSystemOneRequestData) {
        this.checkTokenCertification(llmSystemOneRequestData);
        RequestBodyUriSpec requestBodyUriSpec = this.webClient.post();
        RequestBodySpec requestBodySpec = requestBodyUriSpec.uri(uriBuilder -> {
            uriBuilder.path(this.systemOneEndpoint);
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
     * <p>
     * Looks for {@code /input_tokens} first, and falls back to {@code /prompt_tokens}.
     * If the field is missing or not an integral number, defaults to 0.
     * </p>
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
     * <p>
     * Looks for {@code /output_tokens} first, and falls back to {@code /completion_tokens}.
     * If the field is missing or not an integral number, defaults to 0.
     * </p>
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
     * <p>
     * If {@code /total_tokens} is present, calculates {@code total - prompt - completion}.
     * If missing or not an integral number, defaults to 0.
     * </p>
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
     * Converts the structured {@link LlmSystemOneRequestData} into a TypeSafe AI
     * JSON payload containing {@code model}, {@code state}, and {@code questions}.
     * </p>
     */
    @Override
    public ObjectNode initializeRequestBody(@NonNull LlmSystemOneRequestData llmSystemOneRequestData) {
        TypeSafeAiSystemOneRequest request = this.buildRequest(llmSystemOneRequestData);
        return OBJECT_MAPPER.valueToTree(request);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Parses the raw JSON response from TypeSafe AI into a standardized {@link SystemOneResponse}.
     * Verifies that the {@code answers} node exists and is an object; emits a
     * {@link ResponseMessageExtractFailedException} if missing or malformed.
     * Extracts token usage using {@link Usage#newUsageBuilder(ObjectNode)} and maps evaluated
     * answers for {@code noul}, {@code choice}, and {@code score} question types.
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
                        if(typeNode.isMissingNode() || !typeNode.isTextual()){
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
     * {@link TypeSafeAiSystemOneRequest} payload object.
     *
     * @param requestData the generic SystemOne request data; must not be null
     * @return a populated {@link TypeSafeAiSystemOneRequest}
     */
    protected TypeSafeAiSystemOneRequest buildRequest(@NonNull LlmSystemOneRequestData requestData) {
        Object stateValue = this.convertContent(requestData.getState());
        Map<String, TypeSafeAiQuestion> questionsMap = new LinkedHashMap<>();
        requestData.getQuestions().getAllQuestions().forEach((key, question) -> {
            TypeSafeAiQuestion convertedQuestion = this.convertQuestion(question);
            questionsMap.put(key, convertedQuestion);
        });
        return TypeSafeAiSystemOneRequest.builder()
                .model(requestData.getModelName())
                .state(stateValue)
                .questions(questionsMap)
                .build();
    }

    /**
     * Converts a {@link SystemOneQuestion} into a {@link TypeSafeAiQuestion}.
     *
     * @param question the domain question model; must not be null
     * @return the corresponding {@link TypeSafeAiQuestion}
     */
    protected TypeSafeAiQuestion convertQuestion(@NonNull SystemOneQuestion question) {
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
        return TypeSafeAiQuestion.builder()
                .type(type)
                .instructions(instructions)
                .criteria(criteria)
                .build();
    }

    /**
     * Converts {@link SystemOneContent} into plain Java representation (String, Map, List, or null)
     * suitable for Jackson serialization.
     *
     * @param content the content container; may be null
     * @return unwrapped Java content value, or null if empty
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
        return "TypeSafeAiSystemOneProviderDelegate{" +
                "llmProviderInfo=" + llmProviderInfo +
                ", baseUrl='" + baseUrl + '\'' +
                ", systemOneEndpoint='" + systemOneEndpoint + '\'' +
                '}';
    }
}
