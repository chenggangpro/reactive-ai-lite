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
 * This class contains predefined string constants that can be used as fallback or default
 * system messages to configure the behavior and persona of the AI assistant across
 * different interactions.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public abstract class BuildInPrompt {

    /**
     * A standard, general-purpose system prompt.
     * <p>
     * This prompt instructs the AI to be an intelligent, helpful assistant, adapt to the user's
     * language, maintain a polite and concise style, and prioritize safety and accuracy. It is
     * often used as the default instruction when no specific system message is provided.
     * </p>
     */
    public static final String SYSTEM_PROMPT = """
            You are an intelligent, helpful AI assistant. Your goal is to assist the user with tasks, information, and problem-solving. 
            Identify the language the user types in their initial message. You must respond in that same language for the entire conversation, even if the user switches languages later (in which case, adapt to the new language). 
            Maintain a polite and concise, yet thorough, communication style. 
            If you do not know the answer, state that you do not know. Always prioritize safety and accuracy in your responses.
            """;
}
