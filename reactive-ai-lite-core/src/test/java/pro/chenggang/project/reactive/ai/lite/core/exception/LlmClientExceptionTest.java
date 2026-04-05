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
package pro.chenggang.project.reactive.ai.lite.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmClientExceptionTest {

    @Test
    void testMessageConstructor() {
        String message = "Test exception";
        LlmClientException exception = new LlmClientException(message);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    void testMessageAndCauseConstructor() {
        String message = "Test exception";
        RuntimeException cause = new RuntimeException("cause");
        LlmClientException exception = new LlmClientException(message, cause);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void testCauseConstructor() {
        RuntimeException cause = new RuntimeException("cause");
        LlmClientException exception = new LlmClientException(cause);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
