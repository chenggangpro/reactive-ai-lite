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
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmSystemOneRequestData;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestions;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for default methods in {@link LlmSystemOneProviderDelegate}.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
class LlmSystemOneProviderDelegateTest {

    @Test
    void testCheckTokenCertification() {
        LlmSystemOneProviderDelegate delegate = mock(LlmSystemOneProviderDelegate.class);
        doCallRealMethod().when(delegate).checkTokenCertification(any());

        SystemOneQuestions questions = SystemOneQuestions.of("q1", SystemOneQuestion.newNoulBuilder("test").build());

        LlmSystemOneRequestData dataWithToken = LlmSystemOneRequestData.builder()
                .executionContext(mock(ExecutionContext.class))
                .modelName("test-model")
                .state(SystemOneContent.text("test-state"))
                .questions(questions)
                .tokenCertification(mock(TokenCertification.class))
                .build();

        assertThatNoException().isThrownBy(() -> delegate.checkTokenCertification(dataWithToken));

        LlmSystemOneRequestData dataWithoutToken = LlmSystemOneRequestData.builder()
                .executionContext(mock(ExecutionContext.class))
                .modelName("test-model")
                .state(SystemOneContent.text("test-state"))
                .questions(questions)
                .build();

        assertThatIllegalStateException().isThrownBy(() -> delegate.checkTokenCertification(dataWithoutToken))
                .withMessageContaining("At least one token certification is required");
    }

    @Test
    void testApplyStandardTokenCertification() {
        LlmSystemOneProviderDelegate delegate = mock(LlmSystemOneProviderDelegate.class);
        doCallRealMethod().when(delegate).applyStandardTokenCertification(any(), any());

        WebClient.RequestBodySpec specBearer = mock(WebClient.RequestBodySpec.class);
        BearerTokenCertification bearer = BearerTokenCertification.builder()
                .profile("test")
                .token("test-token")
                .build();

        delegate.applyStandardTokenCertification(specBearer, bearer);
        verify(specBearer).headers(any());

        WebClient.RequestBodySpec specHeader = mock(WebClient.RequestBodySpec.class);
        HttpHeaderTokenCertification header = HttpHeaderTokenCertification.builder()
                .profile("test")
                .headerName("X-Api-Key")
                .token("test-key")
                .build();

        delegate.applyStandardTokenCertification(specHeader, header);
        verify(specHeader).headers(any());

        WebClient.RequestBodySpec specOther = mock(WebClient.RequestBodySpec.class);
        TokenCertification otherCertification = mock(TokenCertification.class);
        delegate.applyStandardTokenCertification(specOther, otherCertification);
        verify(specOther, never()).headers(any());
    }
}
