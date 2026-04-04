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
 * Represents the different capabilities or functionalities that an AI model or provider can support.
 * <p>
 * This enum is used to categorize and identify the type of AI service being used, allowing
 * the system to route requests to the appropriate handlers and ensure that providers are
 * capable of fulfilling specific requests.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public enum Capability {

    /**
     * Represents chat or conversational AI capabilities, typically involving text-based interactions.
     */
    CHAT,

    /**
     * Represents audio processing capabilities, such as speech-to-text or text-to-speech.
     */
    AUDIO,

    /**
     * Represents image generation or analysis capabilities.
     */
    IMAGE,

    /**
     * Represents the capability to generate embeddings or vector representations of text or other data.
     */
    EMBEDDING,

    ;

}
