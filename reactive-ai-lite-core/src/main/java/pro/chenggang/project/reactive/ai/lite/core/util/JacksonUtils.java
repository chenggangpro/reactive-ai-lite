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
 * Utility class providing dynamic Jackson module discovery and instantiation.
 * <p>
 * In reactive AI lite, {@link com.fasterxml.jackson.databind.ObjectMapper} instances are
 * configured with a set of Jackson modules that enhance serialization/deserialization
 * capabilities. Instead of declaring hard dependencies on every possible module (which
 * would force users to include them), this utility discovers and instantiates modules
 * that are actually present on the classpath at runtime. This keeps optional features
 * (like Java 8 date/time or Kotlin support) truly optional.
 * </p>
 * <p>
 * The class is abstract and contains only static methods; it is not meant to be instantiated.
 * All module loading is done reflectively, with missing modules silently ignored so that
 * the application can run without them.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public abstract class JacksonUtils {

    /**
     * Discovers and instantiates commonly used Jackson modules that are available on the classpath.
     * <p>
     * This method attempts to load the following modules, which are often needed to handle
     * Java 8+ language and API features:
     * <ul>
     *   <li>{@code Jdk8Module} – for correct handling of {@code Optional}, {@code Stream}, etc.</li>
     *   <li>{@code JavaTimeModule} – for JSR-310 Java Time API types (e.g., {@code LocalDate},
     *       {@code Instant}).</li>
     *   <li>{@code ParameterNamesModule} – to preserve method/constructor parameter names
     *       when reading JSON (requires {@code -parameters} compiler flag).</li>
     *   <li>{@code KotlinModule} – if the Kotlin runtime is detected on the classpath, it
     *       is loaded to support Kotlin data classes and null-safety annotations.</li>
     * </ul>
     * If a module’s class is not found (because the corresponding library is not a dependency),
     * the exception is caught and the module is simply omitted from the returned list. This
     * makes the {@code ObjectMapper} configuration resilient to changes in the dependency set.
     * </p>
     * <p>
     * The instantiation is done via {@link BeanUtils#instantiateClass(Class)}, which uses
     * the default (public no-arg) constructor. All discovered modules are returned as a
     * flat {@link List} that can be registered in one go.
     * </p>
     *
     * @return a list of instantiated Jackson {@link Module} objects; never {@code null}
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