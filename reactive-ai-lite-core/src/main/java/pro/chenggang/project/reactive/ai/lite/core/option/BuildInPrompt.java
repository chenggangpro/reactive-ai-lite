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
 * Provides built-in default prompts and system instructions for AI tools.
 * <p>
 * This abstract class serves as a central repository for predefined string constants
 * that configure the AI assistant's persona and behavioral guidelines. By abstracting
 * these defaults, the framework ensures a consistent baseline across all interactions
 * while allowing library users to reference or override them as needed.
 * </p>
 * <p>
 * The class is intentionally declared abstract to prevent instantiation, as it
 * exists solely to hold static constants. Its design follows the "constant holder"
 * pattern, keeping default prompts decoupled from business logic and easily
 * discoverable.
 * </p>
 * <p>
 * The included prompts are carefully crafted to balance helpfulness, moderation,
 * and multilingual support. They are typically used as fallback system messages
 * when no explicit instructions are provided by the calling code.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public abstract class BuildInPrompt {

    /**
     * Default system prompt: a general-purpose instruction that shapes the AI's
     * behavior into a multilingual, safe, and helpful assistant.
     * <p>
     * This prompt is designed to work out-of-the-box for a wide range of AI‑powered
     * tasks. It explicitly instructs the model to:
     * <ul>
     *   <li>Act as an intelligent and helpful assistant.</li>
     *   <li>Identify the user's language from the first message and respond
     *       in that same language for the entire conversation, adapting automatically
     *       if the user switches languages later. This behavior ensures a seamless
     *       multilingual experience without requiring the user to specify a language
     *       preference.</li>
     *   <li>Maintain a polite, concise, yet thorough communication style, balancing
     *       brevity with completeness.</li>
     *   <li>Admit lack of knowledge rather than fabricating answers, promoting
     *       trustworthiness.</li>
     *   <li>Prioritize safety and accuracy, mitigating the risk of harmful or
     *       misleading responses.</li>
     * </ul>
     * </p>
     * <p>
     * The instruction to "detect language and stick to it" is grounded in the need
     * for natural, user-friendly interaction in a global context. The emphasis on
     * politeness and safety follows common AI ethics guidelines. This prompt is
     * often used as the default {@code system} message when no custom system
     * prompt is supplied to the AI agent.
     * </p>
     *
     * @see <a href="https://platform.openai.com/docs/guides/prompt-engineering">
     *      OpenAI Prompt Engineering Guide</a> (for related best practices)
     */
    public static final String SYSTEM_PROMPT = """
            You are an intelligent, helpful AI assistant. Your goal is to assist the user with tasks, information, and problem-solving. 
            Identify the language the user types in their initial message. You must respond in that same language for the entire conversation, even if the user switches languages later (in which case, adapt to the new language). 
            Maintain a polite and concise, yet thorough, communication style. 
            If you do not know the answer, state that you do not know. Always prioritize safety and accuracy in your responses.
            """;
}