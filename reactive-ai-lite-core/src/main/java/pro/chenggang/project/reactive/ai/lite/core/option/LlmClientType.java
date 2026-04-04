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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the types of Large Language Model (LLM) clients, each corresponding to a specific AI capability.
 * <p>
 * This enum is used to categorize clients based on their primary function, such as chat, audio processing, etc.
 * It provides a strong typing mechanism to ensure that appropriate capabilities are requested from providers.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum LlmClientType {

    /**
     * A client for chat and conversational AI, associated with {@link Capability#CHAT}.
     */
    CHAT(Capability.CHAT),

    /**
     * A client for audio processing tasks, associated with {@link Capability#AUDIO}.
     */
    AUDIO(Capability.AUDIO),

    /**
     * A client for image generation or analysis, associated with {@link Capability#IMAGE}.
     */
    IMAGE(Capability.IMAGE),

    /**
     * A client for creating embeddings or vector representations, associated with {@link Capability#EMBEDDING}.
     */
    EMBEDDING(Capability.EMBEDDING),

    ;

    /**
     * The underlying capability associated with the LLM client type.
     */
    private final Capability capability;
}
