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
package pro.chenggang.project.reactive.ai.lite.core.message.chunk;

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.usage.Usage;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UsageStreamDataChunkTest {

    @Test
    void testUsageStreamDataChunk() {
        Usage usage = mock(Usage.class);
        UsageStreamDataChunk chunk = UsageStreamDataChunk.builder()
                .usage(usage)
                .build();
                
        assertThat(chunk.getUsage()).isEqualTo(usage);
        assertThat(chunk.getDataType()).isEqualTo(StreamDataType.USAGE);
    }
}
