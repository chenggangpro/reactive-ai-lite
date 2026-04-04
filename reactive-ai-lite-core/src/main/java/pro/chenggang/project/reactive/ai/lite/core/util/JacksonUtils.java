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
package pro.chenggang.project.reactive.ai.lite.core.util;

import com.fasterxml.jackson.databind.Module;
import org.springframework.beans.BeanUtils;
import org.springframework.core.KotlinDetector;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for Jackson-related operations.
 * <p>
 * Provides helper methods for configuring Jackson ObjectMapper instances, such as
 * dynamically discovering and instantiating available Jackson modules in the classpath.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public abstract class JacksonUtils {

    /**
     * Discovers and instantiates common Jackson modules available in the classpath.
     * <p>
     * It attempts to load and instantiate the following modules:
     * <ul>
     *     <li>Jdk8Module (for Java 8 Optionals, etc.)</li>
     *     <li>JavaTimeModule (for JSR-310 Java 8 Date/Time API)</li>
     *     <li>ParameterNamesModule (for preserving parameter names)</li>
     *     <li>KotlinModule (if Kotlin is detected on the classpath)</li>
     * </ul>
     * If a module's class is not found, it is silently ignored.
     * </p>
     *
     * @return a list of instantiated Jackson {@link Module}s
     */
    @SuppressWarnings("unchecked")
    public static List<Module> instantiateAvailableModules() {
        List<Module> modules = new ArrayList<>();
        try {
            Class<? extends Module> jdk8ModuleClass = (Class<? extends Module>) Class
                    .forName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module");
            Module jdk8Module = BeanUtils.instantiateClass(jdk8ModuleClass);
            modules.add(jdk8Module);
        } catch (ClassNotFoundException ex) {
            // jackson-datatype-jdk8 not available
        }

        try {
            Class<? extends Module> javaTimeModuleClass = (Class<? extends Module>) Class
                    .forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule");
            Module javaTimeModule = BeanUtils.instantiateClass(javaTimeModuleClass);
            modules.add(javaTimeModule);
        } catch (ClassNotFoundException ex) {
            // jackson-datatype-jsr310 not available
        }

        try {
            Class<? extends Module> parameterNamesModuleClass = (Class<? extends Module>) Class
                    .forName("com.fasterxml.jackson.module.paramnames.ParameterNamesModule");
            Module parameterNamesModule = BeanUtils
                    .instantiateClass(parameterNamesModuleClass);
            modules.add(parameterNamesModule);
        } catch (ClassNotFoundException ex) {
            // jackson-module-parameter-names not available
        }

        // Kotlin present?
        if (KotlinDetector.isKotlinPresent()) {
            try {
                Class<? extends Module> kotlinModuleClass = (Class<? extends Module>) Class
                        .forName("com.fasterxml.jackson.module.kotlin.KotlinModule");
                Module kotlinModule = BeanUtils.instantiateClass(kotlinModuleClass);
                modules.add(kotlinModule);
            } catch (ClassNotFoundException ex) {
                // jackson-module-kotlin not available
            }
        }
        return modules;
    }

}
