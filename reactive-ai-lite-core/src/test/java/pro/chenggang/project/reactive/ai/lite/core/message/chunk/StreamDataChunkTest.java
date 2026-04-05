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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.usage.Usage;
import pro.chenggang.project.reactive.ai.lite.core.option.StreamDataType;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StreamDataChunkTest {

    @Test
    void testUsageStreamDataChunk() {
        Usage usage = mock(Usage.class);
        UsageStreamDataChunk chunk = UsageStreamDataChunk.builder()
                .usage(usage)
                .build();
        
        assertThat(chunk.getDataType()).isEqualTo(StreamDataType.USAGE);
        assertThat(chunk.getUsage()).isEqualTo(usage);
    }

    @Test
    void testRawStreamDataChunk() {
        RawStreamDataChunk chunk = RawStreamDataChunk.builder()
                .value(JsonNodeFactory.instance.objectNode())
                .build();
        
        assertThat(chunk.getDataType()).isEqualTo(StreamDataType.UNKNOWN);
        assertThat(chunk.getValue()).isNotNull();
    }

    @Test
    void testToolCallStreamDataChunk() {
        ToolCallStreamDataChunk chunk = ToolCallStreamDataChunk.builder()
                .toolCalls(Collections.emptyList())
                .build();
        
        assertThat(chunk.getDataType()).isEqualTo(StreamDataType.TOOL_CALL);
        assertThat(chunk.getToolCalls()).isEmpty();
    }
}
