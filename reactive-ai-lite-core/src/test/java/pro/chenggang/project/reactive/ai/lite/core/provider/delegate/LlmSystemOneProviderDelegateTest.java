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
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.function.client.WebClient;
import pro.chenggang.project.reactive.ai.lite.core.certification.TokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.HttpHeaderTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.values.LlmSystemOneRequestData;
import pro.chenggang.project.reactive.ai.lite.core.message.attachment.Base64Attachment;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneContent;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestion;
import pro.chenggang.project.reactive.ai.lite.core.message.systemone.SystemOneQuestions;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void testCheckImagesMimeType() {
        LlmSystemOneProviderDelegate delegate = mock(LlmSystemOneProviderDelegate.class);
        doCallRealMethod().when(delegate).checkImagesMimeType(any());

        SystemOneQuestions questions = SystemOneQuestions.of("q1", SystemOneQuestion.newNoulBuilder("test").build());

        // Empty images: no exception
        LlmSystemOneRequestData dataWithoutImages = LlmSystemOneRequestData.builder()
                .executionContext(mock(ExecutionContext.class))
                .modelName("test-model")
                .state(SystemOneContent.text("test-state"))
                .questions(questions)
                .build();
        assertThatNoException().isThrownBy(() -> delegate.checkImagesMimeType(dataWithoutImages));

        // Valid image types: png, jpeg, jpg, webp
        Base64Attachment png = Base64Attachment.builder()
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .name("test.png")
                .base64Content("cG5n")
                .build();
        Base64Attachment jpeg = Base64Attachment.builder()
                .mimeType(MimeTypeUtils.IMAGE_JPEG)
                .name("test.jpeg")
                .base64Content("anBlZw==")
                .build();
        Base64Attachment jpg = Base64Attachment.builder()
                .mimeType(MimeType.valueOf("image/jpg"))
                .name("test.jpg")
                .base64Content("anBn")
                .build();
        Base64Attachment webp = Base64Attachment.builder()
                .mimeType(MimeType.valueOf("image/webp"))
                .name("test.webp")
                .base64Content("d2VicA==")
                .build();

        LlmSystemOneRequestData dataWithValidImages = LlmSystemOneRequestData.builder()
                .executionContext(mock(ExecutionContext.class))
                .modelName("test-model")
                .state(SystemOneContent.text("test-state"))
                .questions(questions)
                .images(List.of(png, jpeg, jpg, webp))
                .build();
        assertThatNoException().isThrownBy(() -> delegate.checkImagesMimeType(dataWithValidImages));

        // Invalid MIME type: gif
        Base64Attachment gif = Base64Attachment.builder()
                .mimeType(MimeTypeUtils.IMAGE_GIF)
                .name("test.gif")
                .base64Content("Z2lm")
                .build();
        LlmSystemOneRequestData dataWithGif = LlmSystemOneRequestData.builder()
                .executionContext(mock(ExecutionContext.class))
                .modelName("test-model")
                .state(SystemOneContent.text("test-state"))
                .questions(questions)
                .images(List.of(gif))
                .build();
        assertThatIllegalArgumentException().isThrownBy(() -> delegate.checkImagesMimeType(dataWithGif))
                .withMessageContaining("Unsupported image MIME type");

        // Invalid MIME type: application/json
        Base64Attachment json = Base64Attachment.builder()
                .mimeType(MimeTypeUtils.APPLICATION_JSON)
                .name("test.json")
                .base64Content("e30=")
                .build();
        LlmSystemOneRequestData dataWithJson = LlmSystemOneRequestData.builder()
                .executionContext(mock(ExecutionContext.class))
                .modelName("test-model")
                .state(SystemOneContent.text("test-state"))
                .questions(questions)
                .images(List.of(json))
                .build();
        assertThatIllegalArgumentException().isThrownBy(() -> delegate.checkImagesMimeType(dataWithJson))
                .withMessageContaining("Unsupported image MIME type");

        // Image attachment with null mimeType: mock Base64Attachment returning null
        Base64Attachment nullMimeTypeImage = mock(Base64Attachment.class);
        when(nullMimeTypeImage.mimeType()).thenReturn(null);
        LlmSystemOneRequestData dataWithNullMimeType = LlmSystemOneRequestData.builder()
                .executionContext(mock(ExecutionContext.class))
                .modelName("test-model")
                .state(SystemOneContent.text("test-state"))
                .questions(questions)
                .images(List.of(nullMimeTypeImage))
                .build();
        assertThatIllegalArgumentException().isThrownBy(() -> delegate.checkImagesMimeType(dataWithNullMimeType))
                .withMessageContaining("Unsupported image MIME type");

        // Null image attachment in list
        List<Base64Attachment> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        LlmSystemOneRequestData mockDataWithNull = mock(LlmSystemOneRequestData.class);
        when(mockDataWithNull.getImages()).thenReturn(listWithNull);
        assertThatIllegalArgumentException().isThrownBy(() -> delegate.checkImagesMimeType(mockDataWithNull))
                .withMessageContaining("Image attachment must not be null");
    }
}
