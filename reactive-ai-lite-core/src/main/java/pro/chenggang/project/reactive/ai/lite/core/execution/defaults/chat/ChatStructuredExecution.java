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
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.message.AssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonSchemaUtil;
import reactor.core.publisher.Mono;

import static pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER;

/**
 * The standard implementation of {@link StructuredExecution} for LLM chat operations.
 * <p>
 * This class orchestrates a request that mandates a structured, typed JSON response
 * from the AI model. It uses the {@link LlmProviderExecutor} to resolve the appropriate
 * provider and delegates the schema generation, request execution, and JSON deserialization
 * to the provider implementation.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class ChatStructuredExecution implements StructuredExecution {

    /**
     * The executor responsible for resolving the provider and executing the request.
     */
    private final LlmProviderExecutor llmProviderExecutor;

    /**
     * Constructs a new {@link ChatStructuredExecution}.
     *
     * @param llmProviderRegistry the registry for looking up providers
     * @param executionSpec       the execution specification
     */
    private ChatStructuredExecution(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ExecutionSpec executionSpec) {
        this.llmProviderExecutor = LlmProviderExecutor.builder()
                .llmProviderRegistry(llmProviderRegistry)
                .executionSpec(executionSpec)
                .build();
    }

    /**
     * Factory method for creating a new {@link ChatStructuredExecution}.
     *
     * @param llmProviderRegistry the registry for looking up providers
     * @param executionSpec       the execution specification
     * @return a new {@link ChatStructuredExecution} instance
     */
    public static ChatStructuredExecution of(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ExecutionSpec executionSpec) {
        return new ChatStructuredExecution(llmProviderRegistry, executionSpec);
    }

    /**
     * Retrieves the underlying execution specification.
     *
     * @return the execution spec
     */
    @Override
    public ExecutionSpec executionSpec() {
        return this.llmProviderExecutor.getExecutionSpec();
    }

    /**
     * Executes the structured request using a class type for deserialization.
     */
    @Override
    public <R> Mono<StructuredResponse<R>> execute(@NonNull Class<R> resultType) {
        return llmProviderExecutor.<StructuredResponse<R>>executeChat((llmChatProvider, executionInfo) -> {
                    String schema = JsonSchemaUtil.generateForType(resultType);
                    ExecutionInfo modifiedInfo = executionInfo.toBuilder()
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
                                R structuredContent;
                                try {
                                    structuredContent = OBJECT_MAPPER.readValue(content, resultType);
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
                    ExecutionSpec executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Executes the structured request using a parameterized type for deserialization.
     */
    @Override
    public <R> Mono<StructuredResponse<R>> execute(@NonNull ParameterizedTypeReference<R> resultType) {
        return llmProviderExecutor.<StructuredResponse<R>>executeChat((llmChatProvider, executionInfo) -> {
                    String schema = JsonSchemaUtil.generateForType(resultType.getType());
                    ExecutionInfo modifiedInfo = executionInfo.toBuilder()
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
                                R structuredContent;
                                try {
                                    structuredContent = OBJECT_MAPPER.readValue(content, OBJECT_MAPPER.getTypeFactory().constructType(resultType.getType()));
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
                    ExecutionSpec executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Executes the structured request using a raw JSON schema string.
     */
    @Override
    public Mono<RawResponse> executeRaw(@NonNull String responseJsonSchema) {
        return llmProviderExecutor.executeChat((llmChatProvider, executionInfo) -> {
                    ExecutionInfo modifiedInfo = executionInfo.toBuilder()
                            .responseJsonSchema(responseJsonSchema)
                            .build();
                    return llmChatProvider.executeGeneralRaw(modifiedInfo);
                })
                .contextWrite(context -> {
                    ExecutionSpec executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Executes the structured request using a class type to generate the schema, returning raw JSON.
     */
    @Override
    public <R> Mono<RawResponse> executeRaw(@NonNull Class<R> resultType) {
        return llmProviderExecutor.executeChat((llmChatProvider, executionInfo) -> {
                    String schema = JsonSchemaUtil.generateForType(resultType);
                    ExecutionInfo modifiedInfo = executionInfo.toBuilder()
                            .structuredOutputType(resultType)
                            .responseJsonSchema(schema)
                            .build();
                    return llmChatProvider.executeGeneralRaw(modifiedInfo);
                })
                .contextWrite(context -> {
                    ExecutionSpec executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Executes the structured request using a parameterized type to generate the schema, returning raw JSON.
     */
    @Override
    public <R> Mono<RawResponse> executeRaw(@NonNull ParameterizedTypeReference<R> resultType) {
        return llmProviderExecutor.executeChat((llmChatProvider, executionInfo) -> {
                    String schema = JsonSchemaUtil.generateForType(resultType.getType());
                    ExecutionInfo modifiedInfo = executionInfo.toBuilder()
                            .structuredOutputType(resultType.getType())
                            .responseJsonSchema(schema)
                            .build();
                    return llmChatProvider.executeGeneralRaw(modifiedInfo);
                })
                .contextWrite(context -> {
                    ExecutionSpec executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

}
