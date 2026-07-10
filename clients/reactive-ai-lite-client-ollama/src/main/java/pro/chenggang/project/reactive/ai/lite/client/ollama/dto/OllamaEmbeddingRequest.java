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
package pro.chenggang.project.reactive.ai.lite.client.ollama.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Represents a request to the Ollama embedding API.
 * <p>
 * This immutable data class encapsulates all parameters needed to generate
 * vector embeddings for one or more input texts using a specified model.
 * It leverages Lombok annotations for automatic generation of getters and
 * a builder pattern, and serializes only non-null fields due to
 * {@link JsonInclude.Include#NON_NULL}.
 * </p>
 * <p>
 * Usage typically involves building a request with a model name and input
 * (either a single string or a list of strings), optionally setting truncation
 * behavior, model parameters (such as temperature), and a keep-alive duration
 * for the model in memory.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see <a href="https://github.com/ollama/ollama/blob/main/docs/api.md#generate-embeddings">Ollama API – Generate Embeddings</a>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaEmbeddingRequest {

    /**
     * The name of the model to generate embeddings from.
     * <p>
     * This must be a valid model identifier known to the Ollama server.
     * The model's embedding capabilities (e.g., dimensions, pooling strategy)
     * are determined by the underlying model file.
     * </p>
     */
    @JsonProperty("model")
    private String model;

    /**
     * The input text(s) for which embeddings should be generated.
     * <p>
     * This field accepts two types:
     * <ul>
     *   <li>A single {@link String} – embedding of that text is returned.</li>
     *   <li>A {@link java.util.List} of strings – embeddings for all texts are returned
     *       as a list of vectors, preserving order.</li>
     * </ul>
     * The server processes the input accordingly. Ensure that the total token count
     * does not exceed the model's context length unless truncation is enabled.
     * </p>
     */
    @JsonProperty("input")
    private Object input;

    /**
     * Controls truncation of the input when it exceeds the model's context length.
     * <p>
     * If {@code true} (the default server behavior), the end of each input string is
     * silently truncated to fit within the context window. If {@code false}, the request
     * fails with an error when the context length is exceeded. Set to {@code null} to let
     * the server apply its own default (which is effectively {@code true}).
     * </p>
     */
    @JsonProperty("truncate")
    private Boolean truncate;

    /**
     * Additional model parameters as documented for the Modelfile.
     * <p>
     * This map can contain key-value pairs such as {@code "temperature"}, {@code "top_p"},
     * {@code "num_predict"}, and any other options that influence the model's behavior.
     * The exact set of supported options depends on the specific model and its configuration.
     * If left as {@code null} or empty, the model's built-in defaults are used.
     * </p>
     *
     * @see <a href="https://github.com/ollama/ollama/blob/main/docs/modelfile.md#parameter">Modelfile Parameters</a>
     */
    @JsonProperty("options")
    private Map<String, Object> options;

    /**
     * Controls how long the model stays loaded in memory after the request completes.
     * <p>
     * The value is a duration string, e.g., {@code "5m"} for 5 minutes, {@code "1h"}
     * for one hour, or {@code "-1"} to keep the model loaded indefinitely (until the
     * server is stopped). If not provided ({@code null}), the server uses its default
     * keep‐alive setting (typically {@code "5m"}). Setting a longer duration can reduce
     * latency for subsequent requests at the cost of increased memory consumption.
     * </p>
     */
    @JsonProperty("keep_alive")
    private String keepAlive;
}