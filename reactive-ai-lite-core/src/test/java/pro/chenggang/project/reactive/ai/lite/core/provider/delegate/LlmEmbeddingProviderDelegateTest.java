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
package pro.chenggang.project.reactive.ai.lite.core.provider.delegate;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.HttpHeaderTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmEmbeddingRequestData;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LlmEmbeddingProviderDelegateTest {

    @Test
    void testCheckTokenCertification() {
        LlmEmbeddingProviderDelegate delegate = mock(LlmEmbeddingProviderDelegate.class);
        org.mockito.Mockito.doCallRealMethod().when(delegate).checkTokenCertification(org.mockito.ArgumentMatchers.any());

        LlmEmbeddingRequestData dataWithToken = LlmEmbeddingRequestData.builder()
                .executionContext(mock(pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext.class))
                .modelName("test-model")
                .input(java.util.List.of("test-input"))
                .tokenCertification(mock(TokenCertification.class))
                .build();

        assertThatNoException().isThrownBy(() -> delegate.checkTokenCertification(dataWithToken));

        LlmEmbeddingRequestData dataWithoutToken = LlmEmbeddingRequestData.builder()
                .executionContext(mock(pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext.class))
                .modelName("test-model")
                .input(java.util.List.of("test-input"))
                .build();

        assertThatIllegalStateException().isThrownBy(() -> delegate.checkTokenCertification(dataWithoutToken))
                .withMessageContaining("At least one token certification is required");
    }

    @Test
    void testApplyStandardTokenCertification() {
        LlmEmbeddingProviderDelegate delegate = mock(LlmEmbeddingProviderDelegate.class);
        org.mockito.Mockito.doCallRealMethod().when(delegate).applyStandardTokenCertification(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        WebClient.RequestBodySpec spec = mock(WebClient.RequestBodySpec.class);
        BearerTokenCertification bearer = BearerTokenCertification.builder()
                .profile("test")
                .token("test-token")
                .build();
        
        delegate.applyStandardTokenCertification(spec, bearer);
        verify(spec).headers(org.mockito.ArgumentMatchers.any());

        WebClient.RequestBodySpec spec2 = mock(WebClient.RequestBodySpec.class);
        HttpHeaderTokenCertification header = HttpHeaderTokenCertification.builder()
                .profile("test")
                .headerName("X-Api-Key")
                .token("test-key")
                .build();
        
        delegate.applyStandardTokenCertification(spec2, header);
        verify(spec2).headers(org.mockito.ArgumentMatchers.any());
    }
}
