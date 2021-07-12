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

import io.jmix.dataimport.extractor.data.ImportedDataItem;
import io.jmix.dataimport.extractor.data.ImportedObject;
import io.jmix.dataimport.model.configuration.ImportConfiguration;
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping;

import java.util.List;
import java.util.Map;

public interface EntityPopulator {
    EntityInfo populateProperties(Object entity, ImportConfiguration importConfiguration, ImportedDataItem dataItem);

    EntityInfo populateProperties(Object entity, ImportConfiguration importConfiguration, ImportedDataItem dataItem, Map<PropertyMapping, List<Object>> createdAssociations);

    Object populateAssociation(Object entity, PropertyMapping associationMapping, ImportConfiguration importConfiguration, ImportedObject sourceObject);
}
