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
package pro.chenggang.project.reactive.ai.lite.core.message.defaults;

import lombok.Getter;
import lombok.NonNull;
import org.springframework.util.Assert;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;
import pro.chenggang.project.reactive.ai.lite.core.tool.LlmToolCallResponse;

import java.util.List;

/**
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
public class ToolResponseMessage implements Message {

    private final List<LlmToolCallResponse> llmToolCallResponses;

    private ToolResponseMessage(@NonNull List<LlmToolCallResponse> llmToolCallResponses) {
        Assert.notEmpty(llmToolCallResponses, () -> "The tool call responses list cannot be empty. Please provide at least one response.");
        this.llmToolCallResponses = llmToolCallResponses;
    }

    @Override
    public String text() {
        throw new UnsupportedOperationException("ToolMessage does not have a text content.");
    }

    public static ToolResponseMessage of(@NonNull List<LlmToolCallResponse> llmToolCallResponses) {
        return new ToolResponseMessage(llmToolCallResponses);
    }

}
