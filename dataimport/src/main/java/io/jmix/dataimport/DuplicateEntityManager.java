/*
 * Copyright 2021 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.dataimport;

import io.jmix.core.FetchPlan;
import io.jmix.dataimport.configuration.UniqueEntityConfiguration;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public interface DuplicateEntityManager {
    @Nullable
    Object load(Object entity, UniqueEntityConfiguration configuration, FetchPlan fetchPlan);

    boolean isDuplicated(Object firstEntity, Object secondEntity, UniqueEntityConfiguration configuration);

    @Nullable
    Object find(Collection<Object> existingEntities, Map<String, Object> propertyValues);

    @Nullable
    Object load(Class entityClass, Map<String, Object> propertyValues, @Nullable FetchPlan fetchPlan);
}
