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
package pro.chenggang.project.reactive.ai.lite.core.certification;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.BearerTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.HttpHeaderTokenCertification;
import pro.chenggang.project.reactive.ai.lite.core.certification.defaults.UriTokenCertification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CertificationPackageTest {

    @Test
    void testBearerTokenCertification() {
        BearerTokenCertification cert = BearerTokenCertification.builder()
                .profile("default")
                .token("my-bearer-token")
                .isDefault(true)
                .build();

        assertThat(cert.profile()).isEqualTo("default");
        assertThat(cert.token()).isEqualTo("my-bearer-token");
        assertThat(cert.name()).isEqualTo(HttpHeaders.AUTHORIZATION);
        assertThat(cert.isDefault()).isTrue();

        HttpHeaders headers = new HttpHeaders();
        cert.applyTo(headers);
        assertThat(headers.get(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer my-bearer-token");
        
        assertThrows(IllegalArgumentException.class, () -> BearerTokenCertification.builder().token("").build());
    }

    @Test
    void testHttpHeaderTokenCertification() {
        HttpHeaderTokenCertification cert = HttpHeaderTokenCertification.builder()
                .profile("custom")
                .headerName("X-API-Key")
                .token("my-api-key")
                .isDefault(false)
                .build();

        assertThat(cert.profile()).isEqualTo("custom");
        assertThat(cert.name()).isEqualTo("X-API-Key");
        assertThat(cert.token()).isEqualTo("my-api-key");
        assertThat(cert.isDefault()).isFalse();

        HttpHeaders headers = new HttpHeaders();
        cert.applyTo(headers);
        assertThat(headers.get("X-API-Key")).containsExactly("my-api-key");
    }

    @Test
    void testUriTokenCertification() {
        UriTokenCertification cert = UriTokenCertification.builder()
                .profile("uri-profile")
                .name("api_key")
                .token("my-uri-token")
                .isDefault(true)
                .build();

        assertThat(cert.profile()).isEqualTo("uri-profile");
        assertThat(cert.name()).isEqualTo("api_key");
        assertThat(cert.token()).isEqualTo("my-uri-token");
        assertThat(cert.isDefault()).isTrue();

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://localhost");
        cert.applyTo(builder);
        assertThat(builder.build().getQuery()).isEqualTo("api_key=my-uri-token");
    }
}
