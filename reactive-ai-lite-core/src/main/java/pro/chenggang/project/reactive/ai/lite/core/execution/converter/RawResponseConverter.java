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

import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;

/**
 * A functional interface for converting a {@link RawResponse} into a specific streamable response type.
 *
 * @param <STREAM_RESPONSE> The target type of the stream response.
 * @author Cheng Gang
 * @version 0.1.0
 */
@FunctionalInterface
public interface RawResponseConverter<STREAM_RESPONSE> {

    /**
     * Converts the given raw response into the target stream response type.
     *
     * @param rawResponse The raw response to convert.
     * @return The converted stream response.
     */
    STREAM_RESPONSE convert(RawResponse rawResponse);

}