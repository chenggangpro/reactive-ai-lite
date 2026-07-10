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
package pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.exception.StructuredMessageExtractFailedException;
import pro.chenggang.project.reactive.ai.lite.core.execution.StructuredExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.LlmProviderExecutor;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StructuredResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.message.AssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonSchemaUtil;
import reactor.core.publisher.Mono;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * The standard implementation of {@link StructuredExecution} that orchestrates chat-based interactions
 * requiring a structured JSON response from the language model.
 *
 * <p>This execution enforces a typed contract by generating a JSON Schema from the desired Java type
 * (or parameterized type) and passing it to the LLM provider. The provider uses the schema as part
 * of its structured output capabilities (e.g., via function calling or dedicated JSON mode) to ensure
 * the response conforms to the expected structure. After receiving the raw response, this class
 * extracts the JSON content, strips potential Markdown code fences, deserializes it, and packages
 * everything into a {@link StructuredResponse}.
 *
 * <p>Usage is thread‑safe and intended for reactive streams via Project Reactor. The actual provider
 * selection and lifecycle management is delegated to {@link LlmProviderExecutor}, which ensures that
 * the appropriate {@link LlmChatProvider} is used based on the execution spec.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see StructuredResponse
 * @see RawResponse
 * @see LlmProviderExecutor
 */
@Slf4j
public class ChatStructuredExecution implements StructuredExecution {

    /**
     * The generic executor that resolves the {@link LlmChatProvider} from the registry,
     * applies execution metadata, and invokes the provider's execution method.
     *
     * <p>This field is initialized once in the private constructor and remains immutable.
     * It centralises provider lookup, context preparation, and error handling so that the
     * various {@code execute} methods can focus solely on the structured‑output specifics.
     */
    private final LlmProviderExecutor<ChatExecutionInfo> llmProviderExecutor;

    /**
     * Constructs a new {@link ChatStructuredExecution} for the given provider registry and execution spec.
     *
     * <p>The constructor is kept private to force creation through the static factory method
     * {@link #of(LlmProviderRegistry, ChatExecutionSpec)}. This ensures that every instance
     * is fully configured with both the registry (for dynamic provider lookup) and the chat‑specific
     * execution information (model, temperature, messages, etc.).
     *
     * @param llmProviderRegistry the registry of available LLM providers; must not be {@code null}
     * @param executionSpec       the specification detailing how the chat request should be performed
     */
    private ChatStructuredExecution(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ChatExecutionSpec executionSpec) {
        this.llmProviderExecutor = LlmProviderExecutor.<ChatExecutionInfo>builder()
                .llmProviderRegistry(llmProviderRegistry)
                .executionSpec(executionSpec)
                .build();
    }

    /**
     * Static factory that creates a new {@link ChatStructuredExecution} tailored to the supplied provider
     * registry and chat execution specification.
     *
     * <p>The returned instance is ready to execute structured requests with typed output. The factory
     * pattern allows the caller to share a single registry and spec across multiple executions
     * without worrying about internal state.
     *
     * @param llmProviderRegistry the registry for locating the appropriate LLM chat provider; must not be {@code null}
     * @param executionSpec       the specification carrying model, parameters, and context; must not be {@code null}
     * @return a fully configured {@link ChatStructuredExecution}
     */
    public static ChatStructuredExecution of(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ChatExecutionSpec executionSpec) {
        return new ChatStructuredExecution(llmProviderRegistry, executionSpec);
    }

    /**
     * Executes the structured request, generating a JSON schema from the given {@link Class} type and
     * deserializing the model's JSON response into an instance of that type.
     *
     * <p>The flow:
     * <ol>
     *   <li>A JSON schema is produced from {@code resultType} using Jackson's JSON Schema generator.</li>
     *   <li>The original {@link ChatExecutionInfo} is updated with the schema and the target type;
     *       the provider will use this metadata to request structured output.</li>
     *   <li>The provider's {@code executeGeneral} method is called; it returns a {@code RawResponse}
     *       containing the assistant's message (potentially in JSON form).</li>
     *   <li>The assistant message text is extracted. If it is empty or {@code null}, a
     *       {@link StructuredMessageExtractFailedException} is signaled.</li>
     *   <li>The text is cleaned of any Markdown code fences (```json...```) by
     *       {@link #extractJsonContent(String)}.</li>
     *   <li>The cleaned JSON is deserialized using Jackson's {@code ObjectMapper} and the provided
     *       {@code resultType}.</li>
     *   <li>A {@link StructuredResponse} containing the original response metadata, the raw body,
     *       the assistant message, and the deserialized structured content is emitted.</li>
     * </ol>
     *
     * @param <R>        the type of the structured content the caller expects
     * @param resultType the Java class representing the expected JSON structure; must not be {@code null}
     * @return a cold {@link Mono} emitting the {@link StructuredResponse} on success,
     *         or an error if any extraction or deserialization step fails
     */
    @Override
    public <R> Mono<StructuredResponse<R>> execute(@NonNull Class<R> resultType) {
        return llmProviderExecutor.<LlmChatProvider, StructuredResponse<R>>execute(LlmChatProvider.class, (llmChatProvider, executionInfo) -> {
                    String schema = JsonSchemaUtil.generateForType(resultType);
                    ChatExecutionInfo modifiedInfo = executionInfo.toBuilder()
                            .structuredOutputType(resultType)
                            .responseJsonSchema(schema)
                            .build();
                    return llmChatProvider.executeGeneral(modifiedInfo)
                            .handle((generalResponse, sink) -> {
                                AssistantTextMessage assistantTextMessage = generalResponse.getAssistantTextMessage();
                                if (assistantTextMessage == null) {
                                    sink.error(new StructuredMessageExtractFailedException(generalResponse.getRawResponseBody(), null, new IllegalArgumentException("AssistantTextMessage is null")));
                                    return;
                                }
                                String content = assistantTextMessage.getContent();
                                if (content == null || content.isBlank()) {
                                    sink.error(new StructuredMessageExtractFailedException(generalResponse.getRawResponseBody(), content, new IllegalArgumentException("Structured content is empty or null")));
                                    return;
                                }
                                String jsonContent = extractJsonContent(content);
                                if (jsonContent.isBlank()) {
                                    sink.error(new StructuredMessageExtractFailedException(generalResponse.getRawResponseBody(), content, new IllegalArgumentException("Structured content is empty or null after markdown extraction")));
                                    return;
                                }
                                R structuredContent;
                                try {
                                    structuredContent = OBJECT_MAPPER.readValue(jsonContent, resultType);
                                } catch (Exception e) {
                                    sink.error(new StructuredMessageExtractFailedException(generalResponse.getRawResponseBody(), content, e));
                                    return;
                                }
                                StructuredResponse<R> structuredResponse = StructuredResponse.<R>builder()
                                        .executionContext(generalResponse.getExecutionContext())
                                        .rawResponseBody(generalResponse.getRawResponseBody())
                                        .usage(generalResponse.getUsage())
                                        .assistantTextMessage(generalResponse.getAssistantTextMessage())
                                        .structuredContent(structuredContent)
                                        .build();
                                sink.next(structuredResponse);
                            });
                })
                .contextWrite(context -> {
                    ExecutionSpec<ChatExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Executes the structured request for a parameterized type (e.g., {@code List<MyPojo>}), generating
     * a JSON schema from the generic type information and deserializing accordingly.
     *
     * <p>This method follows the same pipeline as {@link #execute(Class)} but uses
     * {@link ParameterizedTypeReference} to capture generic type information at runtime. The JSON schema
     * is produced from {@link ParameterizedTypeReference#getType()}, and the Jackson deserialization
     * is performed using a {@code JavaType} derived from that same type.
     *
     * @param <R>        the type of the structured content (may include generic parameters)
     * @param resultType a {@link ParameterizedTypeReference} capturing the full generic type; must not be {@code null}
     * @return a cold {@link Mono} of {@link StructuredResponse} with the deserialized content
     */
    @Override
    public <R> Mono<StructuredResponse<R>> execute(@NonNull ParameterizedTypeReference<R> resultType) {
        return llmProviderExecutor.<LlmChatProvider, StructuredResponse<R>>execute(LlmChatProvider.class, (llmChatProvider, executionInfo) -> {
                    String schema = JsonSchemaUtil.generateForType(resultType.getType());
                    ChatExecutionInfo modifiedInfo = executionInfo.toBuilder()
                            .structuredOutputType(resultType.getType())
                            .responseJsonSchema(schema)
                            .build();
                    return llmChatProvider.executeGeneral(modifiedInfo)
                            .handle((generalResponse, sink) -> {
                                AssistantTextMessage assistantTextMessage = generalResponse.getAssistantTextMessage();
                                if (assistantTextMessage == null) {
                                    sink.error(new StructuredMessageExtractFailedException(generalResponse.getRawResponseBody(), null, new IllegalArgumentException("AssistantTextMessage is null")));
                                    return;
                                }
                                String content = assistantTextMessage.getContent();
                                if (content == null || content.isBlank()) {
                                    sink.error(new StructuredMessageExtractFailedException(generalResponse.getRawResponseBody(), content, new IllegalArgumentException("Structured content is empty or null")));
                                    return;
                                }
                                String jsonContent = extractJsonContent(content);
                                if (jsonContent.isBlank()) {
                                    sink.error(new StructuredMessageExtractFailedException(generalResponse.getRawResponseBody(), content, new IllegalArgumentException("Structured content is empty or null after markdown extraction")));
                                    return;
                                }
                                R structuredContent;
                                try {
                                    structuredContent = OBJECT_MAPPER.readValue(jsonContent, OBJECT_MAPPER.getTypeFactory().constructType(resultType.getType()));
                                } catch (Exception e) {
                                    sink.error(new StructuredMessageExtractFailedException(generalResponse.getRawResponseBody(), content, e));
                                    return;
                                }
                                StructuredResponse<R> structuredResponse = StructuredResponse.<R>builder()
                                        .executionContext(generalResponse.getExecutionContext())
                                        .rawResponseBody(generalResponse.getRawResponseBody())
                                        .usage(generalResponse.getUsage())
                                        .assistantTextMessage(generalResponse.getAssistantTextMessage())
                                        .structuredContent(structuredContent)
                                        .build();
                                sink.next(structuredResponse);
                            });
                })
                .contextWrite(context -> {
                    ExecutionSpec<ChatExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Executes the structured request using a manually provided JSON schema string, returning the raw
     * response from the LLM provider without deserialization.
     *
     * <p>This low‑level method is useful when the schema is constructed outside the Jackson Jackson
     * schema generation, or when the caller needs direct access to the provider's raw JSON.
     * The {@code executeRaw} delegation ensures that the schema is embedded into the
     * {@link ChatExecutionInfo} and the provider's raw execution path is used.
     *
     * @param responseJsonSchema the JSON schema describing the expected output structure; must not be {@code null}
     * @return a {@link Mono} of {@link RawResponse} containing the provider's raw output
     */
    @Override
    public Mono<RawResponse> executeRaw(@NonNull String responseJsonSchema) {
        return llmProviderExecutor.execute(LlmChatProvider.class, (llmChatProvider, executionInfo) -> {
                    ChatExecutionInfo modifiedInfo = executionInfo.toBuilder()
                            .responseJsonSchema(responseJsonSchema)
                            .build();
                    return llmChatProvider.executeGeneralRaw(modifiedInfo);
                })
                .contextWrite(context -> {
                    ExecutionSpec<ChatExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Generates a JSON schema from the given {@link Class} and executes the request in raw mode,
     * returning the raw provider response without deserializing into a typed Java object.
     *
     * <p>This is a convenience variant of {@link #executeRaw(String)} that uses Jackson to produce the
     * schema from the {@code resultType} and passes it along. Useful when you want the structured‑output
     * contract enforced by the model but prefer to parse the JSON yourself.
     *
     * @param <R>        the Java type used solely for schema generation
     * @param resultType the target class from which a JSON schema will be derived; must not be {@code null}
     * @return a {@link Mono} of {@link RawResponse}
     */
    @Override
    public <R> Mono<RawResponse> executeRaw(@NonNull Class<R> resultType) {
        return llmProviderExecutor.execute(LlmChatProvider.class, (llmChatProvider, executionInfo) -> {
                    String schema = JsonSchemaUtil.generateForType(resultType);
                    ChatExecutionInfo modifiedInfo = executionInfo.toBuilder()
                            .structuredOutputType(resultType)
                            .responseJsonSchema(schema)
                            .build();
                    return llmChatProvider.executeGeneralRaw(modifiedInfo);
                })
                .contextWrite(context -> {
                    ExecutionSpec<ChatExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Generates a JSON schema from the given {@link ParameterizedTypeReference} and executes the request
     * in raw mode, returning the raw provider response.
     *
     * <p>This method mirrors {@link #executeRaw(Class)} but supports generic types. The schema is
     * derived from the full generic type information captured by the parameterized type reference,
     * enabling accurate JSON Schema generation for complex types (e.g., {@code List<Foo>}).
     *
     * @param <R>        the expected generic type
     * @param resultType a {@link ParameterizedTypeReference} capturing the generic type; must not be {@code null}
     * @return a {@link Mono} of {@link RawResponse}
     */
    @Override
    public <R> Mono<RawResponse> executeRaw(@NonNull ParameterizedTypeReference<R> resultType) {
        return llmProviderExecutor.execute(LlmChatProvider.class, (llmChatProvider, executionInfo) -> {
                    String schema = JsonSchemaUtil.generateForType(resultType.getType());
                    ChatExecutionInfo modifiedInfo = executionInfo.toBuilder()
                            .structuredOutputType(resultType.getType())
                            .responseJsonSchema(schema)
                            .build();
                    return llmChatProvider.executeGeneralRaw(modifiedInfo);
                })
                .contextWrite(context -> {
                    ExecutionSpec<ChatExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Strips optional Markdown code fences (```json) from the assistant's message content.
     *
     * <p>Many LLM providers return JSON output wrapped in triple backticks, often with a language
     * identifier like "json" or "javascript". This method removes the opening fence (with or without
     * the language tag) and the closing ``` to obtain the plain JSON string. It handles leading and
     * trailing whitespace and gracefully returns {@code null} if the input is {@code null}.
     *
     * <p>The extraction is idempotent: if no fences are present the original trimmed content is
     * returned unchanged.
     *
     * @param content the raw text content returned by the assistant, possibly wrapped in Markdown code fences
     * @return the cleaned JSON string, or {@code null} if the input was {@code null}
     */
    private String extractJsonContent(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring("```json".length());
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring("```".length());
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - "```".length());
        }
        return trimmed.trim();
    }

}