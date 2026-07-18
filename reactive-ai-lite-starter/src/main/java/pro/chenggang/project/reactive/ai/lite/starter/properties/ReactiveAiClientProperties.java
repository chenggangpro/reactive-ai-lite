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
package pro.chenggang.project.reactive.ai.lite.starter.properties;

import lombok.Getter;
import lombok.Setter;

/**
 * Central configuration properties holder for the Reactive AI Lite starter.
 * <p>
 * This class acts as the unified entry point for all reactive AI client configurations within
 * the framework. Instead of spreading logging-related settings across individual client implementations, 
 * it consolidates them into a single, easily manageable property set. When the application starts,
 * Spring Boot's configuration binding mechanism maps properties from {@code application.yml}
 * (or {@code application.properties}) prefixed with {@value #PREFIX} to an instance of this class,
 * which is then injected into the reactive client builders.
 * <p>
 * The primary motivation is to provide a rapid and consistent way to toggle detailed operational
 * logging for all supported AI providers (e.g., OpenAI, Azure, or custom providers) without 
 * interfering with each provider’s complex configuration. This aligns with the "Lite" philosophy 
 * of reducing boilerplate and simplifying developer experience in reactive programming.
 * <p>
 * Example usage in YAML:
 * <pre>{@code
 * reactive:
 *   ai:
 *     lite:
 *       client:
 *         enable-logging: false
 * }</pre>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Getter
@Setter
public class ReactiveAiClientProperties {

    /**
     * The configuration prefix that binds all properties inside this class.
     * <p>
     * When Spring Boot scans for {@code @ConfigurationProperties} beans, it uses this constant
     * to deduce the full property path. For example, any property starting with 
     * {@code reactive.ai.lite.client} will automatically map to a field in this class. 
     * This prefix follows the <em>relaxed binding</em> rules of Spring Boot, making it 
     * compatible with various property naming conventions (camelCase, kebab-case, etc.).
     */
    public static final String PREFIX = "reactive.ai.lite.client";

    /**
     * Toggles verbose logging for all reactive AI client operations.
     * <p>
     * Under the hood, every reactive client implementation in the Lite framework checks this flag
     * before emitting log messages. When enabled (the default), the clients log request payloads, 
     * response payloads, execution timings, and any detected anomalies during the reactive stream 
     * lifecycle. This is essential during development and debugging to understand how the AI 
     * provider is responding and to troubleshoot integration issues.
     * <p>
     * In production, however, such extensive logging can severely impact performance due to the 
     * sheer volume of data and the overhead of reactive context capturing. Setting this property 
     * to {@code false} suppresses those logs globally, instantly improving throughput and 
     * reducing log storage costs without changing any code.
     * <p>
     * Because the reactive clients are managed by the framework and share this centralized 
     * configuration, a single toggle affects all provider instances. This design prevents 
     * accidental inconsistencies where one provider logs while another does not.
     */
    private boolean loggingEnabled = true;
}