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
package pro.chenggang.project.reactive.ai.lite.core.provider.delegate;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.reactivestreams.Publisher;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.HttpHeaderTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmChatRequestData;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.tool.ToolDefinition;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser.JsonChunkParsingData;
import pro.chenggang.project.reactive.ai.lite.core.util.StreamResponseParser.JsonStreamChunkSlide;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Delegate interface representing the specific capabilities and API integration logic of an LLM Chat Provider.
 * This interface encapsulates the provider-specific logic that was formerly tightly coupled through inheritance.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmChatProviderDelegate {

    /**
     * Returns the metadata and configuration information for this provider.
     *
     * @return the {@link LlmProviderInfo}
     */
    LlmProviderInfo providerInfo();

    /**
     * Initializes the WebClient request specification for this specific provider.
     *
     * @param llmChatRequestData the structured request data
     * @return a configured {@link RequestBodySpec}
     */
    RequestBodySpec loadRequestBodySpec(LlmChatRequestData llmChatRequestData);

    /**
     * Transforms the generic chat request data into the provider-specific JSON payload.
     *
     * @param llmChatRequestData the structured request data
     * @return a JSON {@link ObjectNode} representing the request body
     */
    ObjectNode initializeRequestBody(LlmChatRequestData llmChatRequestData);

    /**
     * Extracts individual stream chunks from a parsed Server-Sent Event (SSE).
     *
     * @param jsonChunkParsingData the parsed JSON SSE event
     * @return an array of {@link JsonStreamChunkSlide} containing the extracted data
     */
    JsonStreamChunkSlide[] extractStreamChunks(JsonChunkParsingData jsonChunkParsingData);

    /**
     * Merges multiple raw tool call message chunks into a single, cohesive JSON object.
     *
     * @param rawToolCallMessages a list of raw JSON tool call fragments
     * @param distinctToolCalls   whether to filter for distinct tool calls
     * @return the merged {@link ObjectNode}
     */
    ObjectNode mergeRawToolCallMessages(List<ObjectNode> rawToolCallMessages, boolean distinctToolCalls);

    /**
     * Extracts a standardized {@link GeneralResponse} from the raw provider response.
     *
     * @param toolDefinitions the tools that were available during the request
     * @param rawResponse     the raw response data
     * @return a Mono emitting the parsed {@link GeneralResponse}
     */
    Mono<GeneralResponse> extractGeneralResponse(List<ToolDefinition> toolDefinitions, RawResponse rawResponse);

    /**
     * Extracts a standardized {@link StreamResponse} from a raw stream chunk.
     *
     * @param toolDefinitions   the tools that were available during the request
     * @param rawStreamResponse the raw stream chunk
     * @return a Publisher emitting the parsed {@link StreamResponse}
     */
    Publisher<StreamResponse> extractStreamResponseContent(List<ToolDefinition> toolDefinitions, RawStreamResponse rawStreamResponse);

    /**
     * Checks if the required token certification is present in the request data.
     * Providers that do not require token certification (like Ollama) can override this to do nothing.
     *
     * @param llmChatRequestData the structured request data
     */
    default void checkTokenCertification(LlmChatRequestData llmChatRequestData) {
        if (llmChatRequestData.getTokenCertification().isEmpty()) {
            throw new IllegalStateException("At least one token certification is required for the chat completion request.");
        }
    }

    /**
     * Default utility method to apply standard TokenCertification to a WebClient RequestBodySpec.
     * Delegates can invoke this internally within `loadRequestBodySpec` to avoid logic duplication.
     * <p/>
     * UriTokenCertification needs to be applied at the URI construction level, which is usually
     * handled directly by the delegate when creating the URI, not on the RequestBodySpec directly.
     * Therefore, it's typically ignored here or handled manually by the provider.
     *
     * @param requestBodySpec    the request spec to modify
     * @param tokenCertification the token certification to apply
     */
    default void applyStandardTokenCertification(RequestBodySpec requestBodySpec, TokenCertification tokenCertification) {
        if (tokenCertification instanceof BearerTokenCertification bearerTokenCertification) {
            requestBodySpec.headers(bearerTokenCertification::applyTo);
            return;
        }
        if (tokenCertification instanceof HttpHeaderTokenCertification httpHeaderTokenCertification) {
            requestBodySpec.headers(httpHeaderTokenCertification::applyTo);
            return;
        }
    }
}
