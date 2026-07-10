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
package pro.chenggang.project.reactive.ai.lite.client.openai.embedding;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pro.chenggang.project.reactive.ai.lite.client.openai.OpenaiLlmClientTestApplicationTests;
import pro.chenggang.project.reactive.ai.lite.core.api.ReactiveLlmClient;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;
import reactor.test.StepVerifier;

/**
 * @author Gang Cheng
 * @version 0.1.0
 */
public class OpenaiEmbeddingClientTests extends OpenaiLlmClientTestApplicationTests {

    @Autowired
    ReactiveLlmClient reactiveLlmClient;

    String model = "Qwen3-Embedding-8B-mxfp8";  // using oMLX instead

    @Test
    void testEmbeddingGeneral() {
        reactiveLlmClient.embedding()
                .model(contextView -> model)
                .inputText("你现在是一名运维工程师，你负责保障系统和服务的正常运行。你熟悉各种监控工具，能够高效地处理故障和进行系统优化。你还懂得如何进行数据备份和恢复，以保证数据安全。请在这个角色下为我解答以下问题。")
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
    void testEmbeddingGeneralRaw() {
        reactiveLlmClient.embedding()
                .model(contextView -> model)
                .inputText("你现在是一名运维工程师，你负责保障系统和服务的正常运行。你熟悉各种监控工具，能够高效地处理故障和进行系统优化。你还懂得如何进行数据备份和恢复，以保证数据安全。请在这个角色下为我解答以下问题。")
                .general()
                .executeRaw()
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
}
