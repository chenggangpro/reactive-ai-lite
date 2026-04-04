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
 * A functional interface for converting a generic, unparsed {@link RawResponse}
 * into a specific, application-defined response type.
 * <p>
 * This interface is typically used within the fluent execution API (like
 * {@link pro.chenggang.project.reactive.ai.lite.core.execution.GeneralExecution#execute(RawResponseConverter)})
 * to allow developers to supply custom parsing logic when the standard framework
 * extractions are insufficient or when dealing with experimental provider APIs.
 * </p>
 *
 * @param <RESPONSE> the target type of the converted response
 * @author Cheng Gang
 * @version 0.1.0
 */
@FunctionalInterface
public interface RawResponseConverter<RESPONSE> {

    /**
     * Converts the given raw JSON response into the target type.
     *
     * @param rawResponse the raw response containing the unparsed JSON body
     * @return the converted response of type {@code RESPONSE}
     */
    RESPONSE convert(RawResponse rawResponse);

}
