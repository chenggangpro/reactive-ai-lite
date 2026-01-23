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
package pro.chenggang.project.reactive.ai.lite.core.option;

/**
 * Defines the different modes of execution for an AI request.
 * This enum specifies how the response from the AI service should be handled,
 * whether as a single complete response, a stream of data, or a structured object.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public enum ExecutionType {

    /**
     * Represents a standard request-response execution, where a single, complete response is expected.
     */
    GENERAL,

    /**
     * Represents a streaming execution, where the response is received as a stream of events or data chunks.
     * This is typically used for real-time updates, like in a chat application.
     */
    STREAM,

    /**
     * Represents an execution that is expected to return a structured response, such as a JSON object
     * that can be mapped to a specific class.
     */
    STRUCTURED,

    ;
}
