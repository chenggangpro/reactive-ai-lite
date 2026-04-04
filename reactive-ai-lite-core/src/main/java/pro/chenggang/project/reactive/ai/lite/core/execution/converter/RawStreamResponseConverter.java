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
package pro.chenggang.project.reactive.ai.lite.core.execution.converter;

import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;

/**
 * A functional interface for converting a generic, unparsed {@link RawStreamResponse}
 * into a specific, application-defined response type.
 * <p>
 * This is typically used within the fluent streaming execution API (like
 * {@link pro.chenggang.project.reactive.ai.lite.core.execution.StreamExecution#execute(RawStreamResponseConverter)})
 * to allow developers to supply custom parsing logic for individual JSON chunks emitted
 * by the provider's Server-Sent Events (SSE) stream.
 * </p>
 *
 * @param <STREAM_RESPONSE> the target type of the converted stream response
 * @author Cheng Gang
 * @version 0.1.0
 */
@FunctionalInterface
public interface RawStreamResponseConverter<STREAM_RESPONSE> {

    /**
     * Converts the given raw stream response chunk into the target type.
     *
     * @param rawStreamResponse the raw stream chunk containing the unparsed JSON body
     * @return the converted response of type {@code STREAM_RESPONSE}
     */
    STREAM_RESPONSE convert(RawStreamResponse rawStreamResponse);

}
