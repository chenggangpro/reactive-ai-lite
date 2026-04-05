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
package pro.chenggang.project.reactive.ai.lite.core.certification.defaults;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class UriTokenCertificationTest {

    @Test
    void testUriTokenCertification() {
        UriTokenCertification certification = UriTokenCertification.builder()
                .profile("test-profile")
                .isDefault(true)
                .name("api_key")
                .token("test-token")
                .build();
        
        assertThat(certification.profile()).isEqualTo("test-profile");
        assertThat(certification.isDefault()).isTrue();
        assertThat(certification.name()).isEqualTo("api_key");
        assertThat(certification.token()).isEqualTo("test-token");

        UriBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://localhost");
        certification.applyTo(uriBuilder);
        assertThat(uriBuilder.build().getQuery()).contains("api_key=test-token");
    }
}
