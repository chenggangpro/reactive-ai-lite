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
package pro.chenggang.project.reactive.ai.lite.core.execution.values;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;

import java.util.List;
import java.util.function.Function;

/**
 * Provides the specification for configuring and building {@link EmbeddingExecutionInfo} instances
 * used to invoke embedding model services. This spec extends the base {@link ExecutionSpec} and adds
 * embedding-specific configuration capabilities.
 * <p>
 * The specification is designed to allow dynamic resolution of the input texts and the target 
 * embedding dimensions at runtime via {@link Function}s that accept the current 
 * {@link ExecutionContext}. This enables flexible integration with various embedding models 
 * that may support variable dimensions (e.g., OpenAI's text-embedding-3 models) and multiple 
 * input sources.
 * <p>
 * Instances are typically created using the {@code SuperBuilder} pattern provided by Lombok, 
 * which automatically generates a builder to set the properties defined here and those inherited 
 * from {@link ExecutionSpec}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ExecutionSpec
 * @see EmbeddingExecutionInfo
 */
@Getter
@SuperBuilder
public class EmbeddingExecutionSpec extends ExecutionSpec<EmbeddingExecutionInfo> {

    /**
     * A function that dynamically extracts the list of texts to be embedded from the 
     * {@link ExecutionContext} at the time of execution.
     * <p>
     * The function is invoked when the {@link EmbeddingExecutionInfo} resolves its input 
     * texts, allowing the actual content to depend on runtime state such as user queries, 
     * documents, or other context information. The returned list of strings will be sent 
     * to the embedding model.
     */
    private final Function<ExecutionContext, List<String>> inputTextConfigure;

    /**
     * A function that determines the desired embedding dimensions from the 
     * {@link ExecutionContext} at execution time.
     * <p>
     * Many modern embedding models support configurable output dimensions to trade off 
     * between accuracy and performance. This function enables dynamic selection of the 
     * dimension count based on the execution context, allowing the same specification 
     * to be used with different configurations for different calls.
     */
    private final Function<ExecutionContext, Integer> dimensionsConfigure;

    /**
     * Constructs a new {@link EmbeddingExecutionInfo} by transferring all configured 
     * functions and profile settings from this specification. The returned object 
     * is a concrete builder that will lazily resolve the input texts and dimensions 
     * when the embedding request is prepared.
     * <p>
     * This method is called internally by the execution framework when an embedding 
     * operation is requested. It ensures that all configuration from the spec is 
     * properly passed to the execution info instance.
     *
     * @param executionContext the current execution context, which will be passed to 
     *                         the configuration functions when their values are needed; 
     *                         must not be null.
     * @return a fully configured {@link EmbeddingExecutionInfo} that is ready to 
     *         generate the embedding request.
     */
    @Override
    public EmbeddingExecutionInfo newExecutionInfo(@NonNull ExecutionContext executionContext) {
        return EmbeddingExecutionInfo.builder()
                .profilePicker(this.getProfilePicker())
                .defaultProfile(this.isDefaultProfile())
                .modelNameConfigure(this.getModelNameConfigure())
                .rawRequestCustomizerConfigure(this.getRawRequestCustomizerConfigure())
                .inputTextConfigure(this.inputTextConfigure)
                .dimensionsConfigure(this.dimensionsConfigure)
                .build();
    }
}