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
package pro.chenggang.project.reactive.ai.lite.core.spec;

import org.junit.jupiter.api.Test;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderSpecTest {

    @Test
    void testDefaultMethods() {
        ProviderSpec spec = mock(ProviderSpec.class);

        when(spec.firstProvider(any(Predicate.class))).thenCallRealMethod();
        when(spec.profile(any(String.class))).thenCallRealMethod();
        when(spec.defaultSystemMessage(any(String.class))).thenCallRealMethod();

        spec.firstProvider(info -> true);
        verify(spec).firstProvider(any(BiPredicate.class));

        spec.profile("test-profile");
        verify(spec).profile(any(java.util.function.BiFunction.class));

        spec.defaultSystemMessage("sys");
        verify(spec).defaultSystemMessage(any(java.util.function.Function.class));
    }
}
