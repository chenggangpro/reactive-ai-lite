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
package pro.chenggang.project.reactive.ai.lite.client.anthropic.chat;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import pro.chenggang.project.reactive.ai.lite.client.anthropic.AnthropicLlmClientTestApplicationTests;
import pro.chenggang.project.reactive.ai.lite.core.api.ReactiveLlmClient;
import pro.chenggang.project.reactive.ai.lite.core.message.AssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.TextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.ToolResultMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.Role;
import pro.chenggang.project.reactive.ai.lite.core.tool.DefaultToolDefinition;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonSchemaUtil;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Gang Cheng
 * @version 0.1.0
 */
public class AnthropicChatClientTests extends AnthropicLlmClientTestApplicationTests {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ReactiveLlmClient reactiveLlmClient;

    String model = "Qwen3-30B-A3B-Thinking-2507-Claude-4.5-Sonnet-High-Reasoning-Distill-qx86x-hi-mlx";  // using oMLX instead

    @Test
    void testChatGeneralExecute() {
        reactiveLlmClient.chat()
                .newChat()
                .providerSpec()
                .defaultProvider()
                .defaultProfile()
                .chatSpec()
                .model(contextView -> model)
                .systemMessage((contextView -> "你现在是一名运维工程师，你负责保障系统和服务的正常运行。你熟悉各种监控工具，能够高效地处理故障和进行系统优化。你还懂得如何进行数据备份和恢复，以保证数据安全。请在这个角色下为我解答以下问题。"))
                .historicalMessage(List.of(
                        TextMessage.newTextMessage(Role.USER)
                                .content("你能做什么")
                                .build(),
                        TextMessage.newTextMessage(Role.ASSISTANT)
                                .content("我是一名运维工程师，负责保障系统和服务的正常运行。熟悉各种监控工具，能够高效地处理故障和进行系统优化。还懂得如何进行数据备份和恢复，以保证数据安全。")
                                .build()
                ))
                .textMessage((contextView -> "192.168.64.1/24 网段范围?"))
                .general()
                .execute()
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    try {
                        System.out.println(JsonRelatedUtil.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(response));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    void testChatStreamExecuteRaw() {
        reactiveLlmClient.chat()
                .newChat()
                .providerSpec()
                .defaultProvider()
                .defaultProfile()
                .chatSpec()
                .model(contextView -> model)
                .systemMessage((contextView -> "你现在是一名运维工程师，你负责保障系统和服务的正常运行。你熟悉各种监控工具，能够高效地处理故障和进行系统优化。你还懂得如何进行数据备份和恢复，以保证数据安全。请在这个角色下为我解答以下问题。"))
                .historicalMessage(List.of(
                        TextMessage.newTextMessage(Role.USER)
                                .content("你能做什么")
                                .build(),
                        TextMessage.newTextMessage(Role.ASSISTANT)
                                .content("我是一名运维工程师，负责保障系统和服务的正常运行。熟悉各种监控工具，能够高效地处理故障和进行系统优化。还懂得如何进行数据备份和恢复，以保证数据安全。")
                                .build()
                ))
                .textMessage((contextView -> "192.168.64.1/24 网段范围?"))
                .stream()
                .execute()
                .collectList()
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    try {
                        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    void testChatStructuredExecuteRaw() {
        reactiveLlmClient.chat()
                .newChat()
                .providerSpec()
                .defaultProvider()
                .defaultProfile()
                .chatSpec()
                .model(contextView -> model)
                .systemMessage((contextView -> "你现在是一名运维工程师，你负责保障系统和服务的正常运行。你熟悉各种监控工具，能够高效地处理故障和进行系统优化。你还懂得如何进行数据备份和恢复，以保证数据安全。请在这个角色下为我解答以下问题。\n"
                        + "你的结果数据必须满足 JSON SCHEMA：" + JsonSchemaUtil.generateForType(ResultClass.class) + "  \n\n"
                        + "示例：{\"min_range\": \"192.168.0.1\", \"max_range\": \"192.168.0.255\"}"
                ))
                .textMessage((contextView -> "192.168.64.1/24 网段范围?"))
                .maxCompletionTokens(contextView -> 4000)
                .structured()
                .execute(new ParameterizedTypeReference<ResultClass>() {})
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    try {
                        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    void testChatGeneralExecuteWithToolCalls() {
        reactiveLlmClient.chat()
                .newChat()
                .contextConfigure((executionContext, parentAttributes) -> {
                    if (Objects.nonNull(parentAttributes) && !parentAttributes.isEmpty()) {
                        executionContext.getAttributes().putAll(parentAttributes);
                    }
                    executionContext.getAttributes().put("sequence", 0);
                })
                .providerSpec()
                .defaultProvider()
                .defaultProfile()
                .chatSpec()
                .model(contextView -> model)
                .systemMessage((contextView -> "You are a helpful assistant"))
                .textMessage((contextView -> "帮我分析销售数据：1.读取sales.csv 2.计算月度增长 3.生成图表 4.写报告"))
                .tools(List.of(
                        ToolDefinition.newToolDefinition()
                                .name("read_csv")
                                .description("读取CSV文件内容")
                                .inputSchema(
                                        "{\"type\":\"object\",\"properties\":{\"filepath\":{\"type\":\"string\",\"description\":\"文件路径\"},\"encoding\":{\"type\":\"string\",\"description\":\"文件编码\",\"default\":\"utf-8\"}},\"required\":[\"filepath\"]}")
                                .build()

                ))
                .general()
                .execute()
                .flatMap(generalResponse -> {
                    AssistantTextMessage assistantTextMessage = generalResponse.getAssistantTextMessage();
                    List<ToolResultMessage> toolResultMessages = generalResponse.getToolCalls()
                            .map(assistantToolCalls -> {
                                return assistantToolCalls.stream()
                                        .<ToolResultMessage>map(assistantToolCall -> ToolResultMessage.newToolResultMessage(assistantToolCall.getId())
                                                .content(
                                                        """
                                                                name,month,amount
                                                                A,2025-01,323.44
                                                                B,2025-01,123.44
                                                                C,2025-01,423.44
                                                                D,2025-01,523.44
                                                                A,2025-02,723.44
                                                                B,2025-02,23.24
                                                                C,2025-02,23.34
                                                                D,2025-02,33.14
                                                                A,2025-03,43.44
                                                                B,2025-03,53.44
                                                                C,2025-03,43.44
                                                                D,2025-03,23.34
                                                                A,2025-04,33.44
                                                                B,2025-04,23.44
                                                                C,2025-04,23.44
                                                                D,2025-04,23.44
                                                                A,2025-05,23.44
                                                                B,2025-05,23.44
                                                                C,2025-05,23.44
                                                                D,2025-05,23.44
                                                                """
                                                )
                                                .build())
                                        .toList();
                            })
                            .orElse(List.of());
                    return reactiveLlmClient.chat()
                            .newChat()
                            .parentAttributes(generalResponse.getExecutionContext().getAttributes())
                            .contextConfigure((executionContext, parentAttributes) -> {
                                Map<String, Object> attributes = executionContext.getAttributes();
                                if (Objects.nonNull(parentAttributes) && !parentAttributes.isEmpty()) {
                                    attributes.putAll(parentAttributes);
                                }
                                int sequence = executionContext.getAttribute("sequence");
                                attributes.put("sequence", ++sequence);
                            })
                            .providerSpec()
                            .defaultProvider()
                            .defaultProfile()
                            .chatSpec()
                            .model(contextView -> model)
                            .systemMessage((contextView -> "You are a helpful assistant"))
                            .historicalMessage(
                                    List.of(
                                            TextMessage.newTextMessage(Role.USER)
                                                    .content("帮我分析销售数据：1.读取sales.csv 2.计算月度增长 3.生成图表 4.写报告")
                                                    .build(),
                                            assistantTextMessage
                                    )
                            )
                            .toolsResponse(toolResultMessages)
                            .general()
                            .execute();
                })
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    try {
                        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    void testChatGeneralExecuteRawWithToolCalls() {
        reactiveLlmClient.chat()
                .newChat()
                .providerSpec()
                .defaultProvider()
                .defaultProfile()
                .chatSpec()
                .model(contextView -> model)
                .systemMessage((contextView -> "You are a helpful assistant"))
                .textMessage((contextView -> "帮我分析销售数据：1.读取sales.csv 2.计算月度增长 3.生成图表 4.写报告"))
                .tools(List.of(
                        DefaultToolDefinition.builder()
                                .name("read_csv")
                                .description("读取CSV文件内容")
                                .inputSchema(
                                        "{\"type\":\"object\",\"properties\":{\"filepath\":{\"type\":\"string\",\"description\":\"文件路径\"},\"encoding\":{\"type\":\"string\",\"description\":\"文件编码\",\"default\":\"utf-8\"}},\"required\":[\"filepath\"]}")
                                .build()

                ))
                .general()
                .executeRaw()
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    try {
                        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    void testChatStreamExecuteWithToolCalls() {
        reactiveLlmClient.chat()
                .newChat()
                .providerSpec()
                .defaultProvider()
                .defaultProfile()
                .chatSpec()
                .model(contextView -> model)
                .systemMessage((contextView -> "You are a helpful assistant"))
                .textMessage((contextView -> "帮我分析销售数据：1.读取sales.csv 2.计算月度增长 3.生成图表 4.写报告"))
                .tools(List.of(
                        DefaultToolDefinition.builder()
                                .name("read_csv")
                                .description("读取CSV文件内容")
                                .inputSchema(
                                        "{\"type\":\"object\",\"properties\":{\"filepath\":{\"type\":\"string\",\"description\":\"文件路径\"},\"encoding\":{\"type\":\"string\",\"description\":\"文件编码\",\"default\":\"utf-8\"}},\"required\":[\"filepath\"]}")
                                .build()

                ))
                .stream()
                .execute()
                .collectList()
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    try {
                        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    void testChatStreamExecuteRawWithToolCalls() {
        reactiveLlmClient.chat()
                .newChat()
                .providerSpec()
                .defaultProvider()
                .defaultProfile()
                .chatSpec()
                .model(contextView -> model)
                .systemMessage((contextView -> "You are a helpful assistant"))
                .textMessage((contextView -> "帮我分析销售数据：1.读取sales.csv 2.计算月度增长 3.生成图表 4.写报告"))
                .tools(List.of(
                        DefaultToolDefinition.builder()
                                .name("read_csv")
                                .description("读取CSV文件内容")
                                .inputSchema(
                                        "{\"type\":\"object\",\"properties\":{\"filepath\":{\"type\":\"string\",\"description\":\"文件路径\"},\"encoding\":{\"type\":\"string\",\"description\":\"文件编码\",\"default\":\"utf-8\"}},\"required\":[\"filepath\"]}")
                                .build()

                ))
                .stream()
                .executeRaw()
                .collectList()
                .as(StepVerifier::create)
                .consumeNextWith(response -> {
                    try {
                        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @JsonClassDescription("The result is represented as a structured output which contains the minimum ip range and maximum ip range.")
    public static class ResultClass {

        @JsonProperty(value = "min_range", required = true)
        private final String minRange;

        @JsonProperty(value = "max_range", required = true)
        private final String maxRange;
    }
}
