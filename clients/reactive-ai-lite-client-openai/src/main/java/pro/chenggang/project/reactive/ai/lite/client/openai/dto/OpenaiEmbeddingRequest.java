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
package pro.chenggang.project.reactive.ai.lite.client.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Represents a request payload for the OpenAI Embeddings API.
 * <p>
 * This class encapsulates all the parameters required to generate vector embeddings
 * from text inputs. These embeddings can be used for various natural language
 * processing tasks such as search, clustering, recommendations, and anomaly detection.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenaiEmbeddingRequest {

    /**
     * The input text to be embedded.
     * <p>
     * This can be encoded as a string or an array of tokens. To generate embeddings
     * for multiple inputs within a single API request, provide a list of strings
     * or a list of token arrays.
     * </p>
     */
    @JsonProperty("input")
    private List<String> input;

    /**
     * The ID of the specific embedding model to use for the request.
     * <p>
     * Examples include {@code text-embedding-3-small}, {@code text-embedding-3-large},
     * or {@code text-embedding-ada-002}.
     * </p>
     */
    @JsonProperty("model")
    private String model;

    /**
     * The format in which the resulting embeddings should be returned.
     * <p>
     * Supported values are typically {@code float} (default) or {@code base64}.
     * </p>
     */
    @JsonProperty("encoding_format")
    private String encodingFormat;

    /**
     * The number of dimensions the resulting output embeddings should have.
     * <p>
     * This parameter is supported only by certain models (e.g., {@code text-embedding-3} series)
     * and allows for reducing the size of the embeddings at the cost of some precision.
     * </p>
     */
    @JsonProperty("dimensions")
    private Integer dimensions;

    /**
     * A unique identifier representing the end-user.
     * <p>
     * Providing this parameter helps OpenAI to monitor usage and detect potential abuse
     * or prohibited content generation on a per-user basis.
     * </p>
     */
    @JsonProperty("user")
    private String user;

}
