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
package pro.chenggang.project.reactive.ai.lite.core.entity.context;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.entity.AttributesAbility;

import java.util.Map;

/**
 * A read-only or restricted view of an {@link ExecutionContext}.
 * <p>
 * This class implements {@link AttributesAbility} to expose the attributes map
 * contained within an ExecutionContext. It is commonly used when executing dynamic
 * configuration functions (e.g., determining which model or profile to use) where
 * the context data needs to be read but should not necessarily be modified or where
 * the full capabilities of the context are not needed.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ExecutionContextView implements AttributesAbility {

    /**
     * The underlying attributes map from the ExecutionContext.
     */
    @NonNull
    private final Map<String, Object> attributes;

    /**
     * Retrieves the map of attributes.
     *
     * @return the map of attributes
     */
    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

}
