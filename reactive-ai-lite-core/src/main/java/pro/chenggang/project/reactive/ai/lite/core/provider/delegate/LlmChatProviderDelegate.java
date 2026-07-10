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
 * Delegate interface that encapsulates all provider-specific logic needed to integrate an LLM chat model
 * into the reactive AI lite framework. Each implementation tailors the generic request/response flow
 * to the concrete HTTP API of a particular LLM provider (e.g., OpenAI, Ollama, Groq).
 * <p>
 * By isolating these concerns behind a single interface, the core execution pipeline remains agnostic
 * of provider differences. A delegate is responsible for:
 * <ul>
 *     <li>Providing metadata about the provider (identity, default settings, etc.)</li>
 *     <li>Constructing the provider‑specific HTTP request (base URL, headers, body)</li>
 *     <li>Parsing and normalising both blocking and streaming responses into unified domain models</li>
 *     <li>Handling any peculiarities of the provider’s SSE stream structure, such as tool‑call deltas</li>
 * </ul>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmChatProviderDelegate {

    /**
     * Supplies metadata that uniquely identifies the provider and declares its default configuration
     * values. This information is used by the framework to adjust its behaviour (e.g., default model
     * selection, feature flags, cost‑tracking hints) without requiring runtime type introspection of
     * the delegate itself.
     *
     * @return a non‑null {@link LlmProviderInfo} carrying the provider’s identity and defaults
     */
    LlmProviderInfo providerInfo();

    /**
     * Prepares the {@link RequestBodySpec} that will be used to issue the HTTP call to the LLM provider.
     * The implementation typically sets the base URL (e.g., {@code https://api.openai.com/v1/chat/completions}),
     * any required authentication headers (via {@link #applyStandardTokenCertification(RequestBodySpec, TokenCertification)}
     * or custom logic), content‑type negotiation, and any provider‑specific static headers.
     * <p>
     * This method is invoked once per request; the returned spec is later augmented with the actual
     * request body by the client layer.
     *
     * @param llmChatRequestData the fully‑prepared, framework‑agnostic request context (model, messages, tools, etc.)
     * @return a mutable {@link RequestBodySpec} representing the start of the HTTP request
     */
    RequestBodySpec loadRequestBodySpec(LlmChatRequestData llmChatRequestData);

    /**
     * Translates the framework’s generic request representation into the provider‑specific JSON payload
     * that must be sent as the HTTP request body. This is where field mapping, formatting differences,
     * and non‑standard parameters are applied.
     * <p>
     * Example: OpenAI expects a JSON object with a {@code messages} array of role‑content pairs and a
     * {@code tools} array; Ollama uses a flat JSON with a {@code prompt} field and stream control
     * attributes. Each delegate’s implementation ensures the correct shape.
     *
     * @param llmChatRequestData the canonical request data to be converted
     * @return a JSON {@link ObjectNode} ready to be serialised into the HTTP request body
     */
    ObjectNode initializeRequestBody(LlmChatRequestData llmChatRequestData);

    /**
     * Breaks down a single parsed Server‑Sent Event (SSE) line into one or more stream‑chunk slides.
     * Many LLM APIs bundle multiple deltas (e.g., choices for different choices, or tool‑call fragments)
     * inside a single SSE message; this method extracts each individually addressable piece so that the
     * streaming pipeline can process them uniformly.
     * <p>
     * The returned array may be empty (e.g., for keep‑alive comments), or contain several slides when
     * the SSE carries compound updates.
     *
     * @param jsonChunkParsingData the raw event data already decoded into a JSON tree, plus metadata
     * @return an array of {@link JsonStreamChunkSlide}, never {@code null} but possibly empty
     */
    JsonStreamChunkSlide[] extractStreamChunks(JsonChunkParsingData jsonChunkParsingData);

    /**
     * Reassembles a complete tool‑call definition from potentially fragmented raw JSON chunks that
     * arrived during streaming. Providers streaming tool calls often deliver function name, arguments
     * (which themselves may be spread across multiple events), and other metadata in separate SSE
     * payloads. This method merges those fragments into a single JSON object that mirrors the expected
     * non‑streaming tool‑call shape.
     * <p>
     * If {@code distinctToolCalls} is {@code true}, the implementation should de‑duplicate by
     * {@code tool‑call index} or id, keeping only the latest merged copy for each distinct call.
     *
     * @param rawToolCallMessages an ordered list of raw JSON nodes, each representing one fragment of a tool call
     * @param distinctToolCalls   whether to keep only distinct (usually the latest) tool calls
     * @return a single {@link ObjectNode} containing the merged and possibly de‑duplicated tool call array
     */
    ObjectNode mergeRawToolCallMessages(List<ObjectNode> rawToolCallMessages, boolean distinctToolCalls);

    /**
     * Converts a raw provider response (the entire HTTP body of a non‑streaming completion) into a
     * standardised {@link GeneralResponse}. The extracted response includes:
     * <ul>
     *     <li>The assistant’s text reply (if present)</li>
     *     <li>Any tool‑call requests issued by the model</li>
     *     <li>Metadata such as finish reason, token usage, and model identification</li>
     * </ul>
     * The conversion relies on the provided {@link ToolDefinition} list to correctly reconstruct
     * tool calls whose argument structure may be partially embedded in the provider’s JSON.
     *
     * @param toolDefinitions the tools that were advertised to the model; used for argument schema reconstruction
     * @param rawResponse     the raw HTTP response body, pre‑parsed into a structure the delegate understands
     * @return a Mono emitting the fully parsed, framework‑compatible {@link GeneralResponse}
     */
    Mono<GeneralResponse> extractGeneralResponse(List<ToolDefinition> toolDefinitions, RawResponse rawResponse);

    /**
     * Maps a single raw stream chunk (a just‑in‑time fragment of a streaming completion) into a
     * standardised {@link StreamResponse}. The publisher returned by this method emits zero or more
     * stream responses; each one corresponds to a meaningful unit of information deliverable to the
     * caller (a text delta, a tool‑call delta, a final message marker, etc.).
     * <p>
     * The implementation must handle provider idiosyncrasies like streaming tool‑call argument
     * accumulation and partial finish signals, leveraging the supplied tool definitions as needed.
     *
     * @param toolDefinitions   the tools available for the request; may be empty
     * @param rawStreamResponse the raw chunk data as received from the HTTP stream
     * @return a Publisher of {@link StreamResponse} items representing parsed deltas
     */
    Publisher<StreamResponse> extractStreamResponseContent(List<ToolDefinition> toolDefinitions, RawStreamResponse rawStreamResponse);

    /**
     * Verifies that the request carries at least one valid token certification. Most cloud‑based
     * providers require an API key (Bearer token or custom header). Providers that are accessed
     * locally without authentication (e.g., Ollama on localhost) may override this method with
     * an empty implementation to skip the check.
     * <p>
     * The default implementation throws an {@link IllegalStateException} if no certification is present.
     * Override with caution; skipping validation for a provider that actually needs it will result
     * in runtime authorization failures.
     *
     * @param llmChatRequestData the request data carrying the token certifications (if any)
     * @throws IllegalStateException if no certification is found (default behaviour)
     */
    default void checkTokenCertification(LlmChatRequestData llmChatRequestData) {
        if (llmChatRequestData.getTokenCertification().isEmpty()) {
            throw new IllegalStateException("At least one token certification is required for the chat completion request.");
        }
    }

    /**
     * Applies a standard {@link TokenCertification} to a {@link RequestBodySpec} by setting the
     * appropriate HTTP headers. This convenience method is intended to be called from within
     * {@link #loadRequestBodySpec(LlmChatRequestData)} to avoid repeating header‑setting logic.
     * <p>
     * It currently handles {@link BearerTokenCertification} (sets the {@code Authorization: Bearer …} header)
     * and {@link HttpHeaderTokenCertification} (custom header name/value). Other certification types,
     * such as URI‑path tokens, must be handled at the URI construction level and are not covered here.
     *
     * @param requestBodySpec    the mutable request spec where headers will be added
     * @param tokenCertification the token certification instance to apply
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