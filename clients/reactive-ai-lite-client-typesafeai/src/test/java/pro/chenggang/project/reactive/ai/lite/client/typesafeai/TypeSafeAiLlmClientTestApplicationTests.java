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
package pro.chenggang.project.reactive.ai.lite.client.typesafeai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import pro.chenggang.project.reactive.ai.lite.client.typesafeai.configuration.TypeSafeAiLlmClientProviderConfiguration;
import pro.chenggang.project.reactive.ai.lite.client.typesafeai.properties.TypeSafeAiClientProperties;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.DefaultLlmSystemOneProvider;
import reactor.core.publisher.Hooks;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base Spring Boot test verifying context loading for the TypeSafe AI client.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@SpringBootTest(classes = TypeSafeAiLlmClientTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import({WebClientAutoConfiguration.class, TypeSafeAiLlmClientProviderConfiguration.class})
public class TypeSafeAiLlmClientTestApplicationTests {

    static {
        Hooks.onOperatorDebug();
        String envToken = System.getenv("TYPESAFE_AI_TOKEN");
        if (envToken == null || envToken.isBlank() || envToken.startsWith("${")) {
            String propToken = System.getProperty("TYPESAFE_AI_TOKEN");
            if (propToken == null || propToken.isBlank()) {
                try {
                    java.nio.file.Path zshrc = java.nio.file.Path.of(System.getProperty("user.home"), ".zshrc");
                    if (java.nio.file.Files.exists(zshrc)) {
                        for (String line : java.nio.file.Files.readAllLines(zshrc)) {
                            if (line.contains("TYPESAFE_AI_TOKEN")) {
                                int eq = line.indexOf('=');
                                if (eq > 0) {
                                    String token = line.substring(eq + 1).trim();
                                    token = token.replaceAll("^['\"]|['\"]$", "");
                                    if (!token.isBlank()) {
                                        System.setProperty("TYPESAFE_AI_TOKEN", token);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TypeSafeAiClientProperties properties;

    @Autowired
    private DefaultLlmSystemOneProvider systemOneProvider;

    /**
     * Verifies that the Spring Boot context loads properly with TypeSafe AI beans configured.
     */
    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(properties).isNotNull();
        assertThat(properties.getBaseUrl()).isEqualTo("https://api.typesafe.ai");
        assertThat(systemOneProvider).isNotNull();
        assertThat(systemOneProvider.info().name()).isEqualTo("TypeSafeAI");
    }
}
