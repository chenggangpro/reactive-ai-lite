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
 * Represents the type of a Large Language Model (LLM) client, providing a strongly typed mapping to a specific {@link Capability}.
 * 
 * <p>This enum ensures that each LLM client type is associated with a designated AI capability, enabling type‑safe
 * configuration and selection of suitable providers for different tasks. The mapping is central to the framework's
 * pluggable architecture, where multiple client implementations can be registered for the same capability,
 * and the appropriate client is resolved based on the requested {@code LlmClientType}.
 * 
 * <p>Each constant corresponds directly to a distinct capability domain:
 * <ul>
 *   <li>{@link #CHAT} for conversational AI tasks, such as question answering, dialogue, and instruction following.</li>
 *   <li>{@link #EMBEDDING} for generating dense vector representations of text used in similarity search, clustering, and retrieval‑augmented generation.</li>
 *   <li>Future extensions may include AUDIO and IMAGE types.</li>
 * </ul>
 * 
 * <p>The enum is immutable and its constants are shared across the application, guaranteeing consistency in capability
 * references.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum LlmClientType {

    /**
     * A client type for chat/completions, corresponding to the {@link Capability#CHAT} capability.
     * 
     * <p>Clients of this type are expected to process natural language prompts and generate coherent,
     * context-aware responses. They can be used for single‑turn or multi‑turn conversations, instruction
     * processing, and any task that involves generating text from a given prompt.
     * 
     * <p>When a service requests a {@code LlmClientType.CHAT} client, the framework will resolve a provider
     * that advertises support for the {@code CHAT} capability.
     */
    CHAT(Capability.CHAT),

    /**
     * A client type for generating text embeddings, corresponding to the {@link Capability#EMBEDDING} capability.
     * 
     * <p>Embedding clients transform text into fixed‑length vector representations that capture semantic meaning.
     * These vectors are the foundation for many downstream tasks such as similarity measurement, semantic search,
     * clustering, and retrieval‑augmented generation pipelines.
     * 
     * <p>Requesting a client of this type triggers resolution of a provider that supports the {@code EMBEDDING} capability,
     * ensuring that the returned client can produce high‑quality embeddings.
     */
    EMBEDDING(Capability.EMBEDDING),

//    /**
//     * A client for audio processing tasks, associated with {@link Capability#AUDIO}.
//     */
//    AUDIO(Capability.AUDIO),
//
//    /**
//     * A client for image generation or analysis, associated with {@link Capability#IMAGE}.
//     */
//    IMAGE(Capability.IMAGE),

    ;

    /**
     * The concrete {@link Capability} that this client type represents.
     * 
     * <p>This mapping is the cornerstone of the type‑safe architecture. It is set at construction time
     * once for each enum constant and cannot be altered, guaranteeing that every instance consistently
     * identifies its intended AI function.
     */
    private final Capability capability;
}