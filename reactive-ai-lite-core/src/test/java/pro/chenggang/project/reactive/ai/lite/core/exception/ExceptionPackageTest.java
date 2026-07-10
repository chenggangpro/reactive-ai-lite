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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionPackageTest {

    @Test
    void testClientResponseErrorException() {
        RestClientResponseException cause = new RestClientResponseException("Error 500", 500, "Internal Server Error", null, null, null);
        ClientResponseErrorException exception = new ClientResponseErrorException(cause);
        assertThat(exception.getMessage()).contains("Llm client response error: Error 500");
        assertThat(exception.getCause()).isEqualTo(cause);

        Assertions.assertThatThrownBy(() -> new ClientResponseErrorException(null))
                .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
    }

    @Test
    void testErrorServerSentEventException() {
        ObjectNode node = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        node.put("error", "details");
        ErrorServerSentEventException exception = new ErrorServerSentEventException("test_type", "test_message", node);
        assertThat(exception.getType()).isEqualTo("test_type");
        assertThat(exception.getMessage()).isEqualTo("test_message");
        assertThat(exception.getErrorJsonContent()).isEqualTo(node);

        ErrorServerSentEventException nullNodeEx = new ErrorServerSentEventException(null, null, null);
        assertThat(nullNodeEx.getType()).isNull();
        assertThat(nullNodeEx.getMessage()).isNull();
        assertThat(nullNodeEx.getErrorJsonContent()).isNull();
    }

    @Test
    void testExecutionContextLossException() {
        ExecutionContextLossException exception = new ExecutionContextLossException();
        assertThat(exception.getMessage()).contains("Missing running execution context of type");
    }

    @Test
    void testLlmClientException() {
        LlmClientException exception1 = new LlmClientException("message");
        assertThat(exception1.getMessage()).isEqualTo("message");

        LlmClientException exception2 = new LlmClientException("message", new RuntimeException());
        assertThat(exception2.getMessage()).isEqualTo("message");
        assertThat(exception2.getCause()).isInstanceOf(RuntimeException.class);

        LlmClientException exception3 = new LlmClientException(new RuntimeException());
        assertThat(exception3.getCause()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void testNoProfileFoundLlmClientException() {
        LlmProviderInfo providerInfo = new LlmProviderInfo() {
            @Override
            public String name() { return "test_provider"; }
            @Override
            public String baseUrl() { return ""; }
            @Override
            public String endpoint() { return ""; }
            @Override
            public Set<String> profiles() { return Set.of("profile1", "profile2"); }
        };

        NoProfileFoundLlmClientException exception1 = new NoProfileFoundLlmClientException(providerInfo);
        assertThat(exception1.getLlmProviderInfo()).isEqualTo(providerInfo);
        assertThat(exception1.getPickedProfile()).isNull();
        assertThat(exception1.getMessage()).contains("test_provider", "profile1", "profile2", "the picked profile is null");

        NoProfileFoundLlmClientException exception2 = new NoProfileFoundLlmClientException(providerInfo, "missing_profile");
        assertThat(exception2.getLlmProviderInfo()).isEqualTo(providerInfo);
        assertThat(exception2.getPickedProfile()).isEqualTo("missing_profile");
        assertThat(exception2.getMessage()).contains("test_provider", "profile1", "profile2", "missing_profile");

        Assertions.assertThatThrownBy(() -> new NoProfileFoundLlmClientException(null))
                .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);

        Assertions.assertThatThrownBy(() -> new NoProfileFoundLlmClientException(null, "missing_profile"))
                .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);

        Assertions.assertThatThrownBy(() -> new NoProfileFoundLlmClientException(providerInfo, null))
                .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
    }

    @Test
    void testResponseMessageExtractFailedException() {
        ObjectNode node = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        node.put("foo", "bar");

        ResponseMessageExtractFailedException exception1 = new ResponseMessageExtractFailedException(node);
        assertThat(exception1.getResponseBody()).isEqualTo(node);
        assertThat(exception1.getMessage()).contains("Failed to extract response message");

        ResponseMessageExtractFailedException exception2 = new ResponseMessageExtractFailedException(node, new RuntimeException());
        assertThat(exception2.getResponseBody()).isEqualTo(node);
        assertThat(exception2.getCause()).isInstanceOf(RuntimeException.class);

        Assertions.assertThatThrownBy(() -> new ResponseMessageExtractFailedException(null))
                .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);

        Assertions.assertThatThrownBy(() -> new ResponseMessageExtractFailedException(null, new RuntimeException()))
                .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);

        Assertions.assertThatThrownBy(() -> new ResponseMessageExtractFailedException(node, null))
                .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
    }

    @Test
    void testStructuredMessageExtractFailedException() {
        ObjectNode node = JsonRelatedUtil.OBJECT_MAPPER.createObjectNode();
        node.put("foo", "bar");

        StructuredMessageExtractFailedException exception1 = new StructuredMessageExtractFailedException(node, "raw content");
        assertThat(exception1.getResponseBody()).isEqualTo(node);
        assertThat(exception1.getContent()).isEqualTo("raw content");
        assertThat(exception1.getMessage()).contains("Failed to deserialize structured content: raw content");

        StructuredMessageExtractFailedException exception2 = new StructuredMessageExtractFailedException(node, "raw content", new RuntimeException());
        assertThat(exception2.getResponseBody()).isEqualTo(node);
        assertThat(exception2.getContent()).isEqualTo("raw content");
        assertThat(exception2.getCause()).isInstanceOf(RuntimeException.class);
        assertThat(exception2.getMessage()).contains("Failed to extract response message");

        Assertions.assertThatThrownBy(() -> new StructuredMessageExtractFailedException(null, "raw content"))
                .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);

        Assertions.assertThatThrownBy(() -> new StructuredMessageExtractFailedException(null, "raw content", new RuntimeException()))
                .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);

        Assertions.assertThatThrownBy(() -> new StructuredMessageExtractFailedException(node, "raw content", null))
                .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
    }
}
