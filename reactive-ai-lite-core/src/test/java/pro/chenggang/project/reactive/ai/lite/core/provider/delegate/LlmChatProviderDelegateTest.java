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
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.HttpHeaderTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmChatRequestData;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LlmChatProviderDelegateTest {

    @Test
    void testCheckTokenCertification() {
        LlmChatProviderDelegate delegate = mock(LlmChatProviderDelegate.class);
        // Call real method for checkTokenCertification
        Mockito.doCallRealMethod().when(delegate).checkTokenCertification(ArgumentMatchers.any());

        LlmChatRequestData dataWithToken = LlmChatRequestData.builder()
                .executionContext(mock(ExecutionContext.class))
                .tokenCertification(mock(TokenCertification.class))
                .build();

        assertThatNoException().isThrownBy(() -> delegate.checkTokenCertification(dataWithToken));

        LlmChatRequestData dataWithoutToken = LlmChatRequestData.builder()
                .executionContext(mock(ExecutionContext.class))
                .build();

        assertThatIllegalStateException().isThrownBy(() -> delegate.checkTokenCertification(dataWithoutToken))
                .withMessageContaining("At least one token certification is required");
    }

    @Test
    void testApplyStandardTokenCertification() {
        LlmChatProviderDelegate delegate = mock(LlmChatProviderDelegate.class);
        Mockito.doCallRealMethod().when(delegate).applyStandardTokenCertification(ArgumentMatchers.any(), ArgumentMatchers.any());

        WebClient.RequestBodySpec spec = mock(WebClient.RequestBodySpec.class);
        BearerTokenCertification bearer = BearerTokenCertification.builder()
                .profile("test")
                .token("test-token")
                .build();
        
        delegate.applyStandardTokenCertification(spec, bearer);
        verify(spec).headers(ArgumentMatchers.any());

        WebClient.RequestBodySpec spec2 = mock(WebClient.RequestBodySpec.class);
        HttpHeaderTokenCertification header = HttpHeaderTokenCertification.builder()
                .profile("test")
                .headerName("X-Api-Key")
                .token("test-key")
                .build();
        
        delegate.applyStandardTokenCertification(spec2, header);
        verify(spec2).headers(ArgumentMatchers.any());
    }
}
