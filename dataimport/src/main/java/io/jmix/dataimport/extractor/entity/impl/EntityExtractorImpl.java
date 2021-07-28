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

package io.jmix.dataimport.extractor.entity.impl;

import io.jmix.core.Metadata;
import io.jmix.core.entity.EntityValues;
import io.jmix.dataimport.DuplicateEntityManager;
import io.jmix.dataimport.configuration.ImportConfiguration;
import io.jmix.dataimport.configuration.mapping.PropertyMapping;
import io.jmix.dataimport.extractor.data.ImportedData;
import io.jmix.dataimport.extractor.data.ImportedDataItem;
import io.jmix.dataimport.extractor.entity.EntityExtractionResult;
import io.jmix.dataimport.extractor.entity.EntityExtractor;
import io.jmix.dataimport.property.populator.EntityInfo;
import io.jmix.dataimport.property.populator.EntityPropertiesPopulator;
import io.jmix.dataimport.property.populator.PropertyMappingUtils;
import io.jmix.dataimport.property.populator.impl.CreatedReference;
import io.jmix.dataimport.property.populator.impl.SimplePropertyValueProvider;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.*;

@Component("datimp_EntityExtractor")
public class EntityExtractorImpl implements EntityExtractor {
    @Autowired
    protected Metadata metadata;
    @Autowired
    protected EntityPropertiesPopulator entityPropertiesPopulator;
    @Autowired
    protected SimplePropertyValueProvider simplePropertyValueProvider;
    @Autowired
    protected PropertyMappingUtils propertyMappingUtils;
    @Autowired
    protected DuplicateEntityManager duplicateEntityManager;

    @Override
    public Object extractEntity(ImportConfiguration importConfiguration, ImportedDataItem dataItem) {
        Object entity = metadata.create(importConfiguration.getEntityClass());
        EntityInfo entityInfo = entityPropertiesPopulator.populateProperties(entity, importConfiguration, dataItem);
        return entityInfo.getEntity();
    }

    @Override
    public List<EntityExtractionResult> extractEntities(ImportConfiguration importConfiguration, ImportedData importedData) {
        List<EntityExtractionResult> results = new ArrayList<>();
        Map<PropertyMapping, List<Object>> createdReferences = new HashMap<>();

        importedData.getItems().forEach(importedDataItem -> {
            EntityExtractionResult alreadyExtractedEntity = getExistingEntity(importedDataItem, importConfiguration, results);
            Object entityToPopulate = alreadyExtractedEntity == null ? metadata.create(importConfiguration.getEntityClass()) : alreadyExtractedEntity.getEntity();

            EntityInfo entityInfo = entityPropertiesPopulator.populateProperties(entityToPopulate, importConfiguration, importedDataItem, createdReferences);

            if (alreadyExtractedEntity == null) {
                results.add(new EntityExtractionResult(entityToPopulate, importedDataItem));
            } else {
                alreadyExtractedEntity.setImportedDataItem(importedDataItem);
            }

            fillCreatedReferences(entityInfo, createdReferences);
        });

        return results;
    }

    public void fillCreatedReferences(EntityInfo entityInfo, Map<PropertyMapping, List<Object>> createdReferencesByMapping) {
        List<CreatedReference> createdReferences = entityInfo.getCreatedReferences();
        if (CollectionUtils.isNotEmpty(createdReferences)) {
            createdReferences.forEach(createdReference -> {
                PropertyMapping propertyMapping = createdReference.getPropertyMapping();
                Object createdObject = createdReference.getCreatedObject();
                List<Object> createdObjects = createdReferencesByMapping.getOrDefault(propertyMapping, new ArrayList<>());
                if (!(createdObject instanceof Collection) && !createdObjects.contains(createdObject)) {
                    createdObjects.add(createdObject);
                }
                createdReferencesByMapping.put(propertyMapping, createdObjects);
            });
        }
    }

    @Nullable
    protected EntityExtractionResult getExistingEntity(ImportedDataItem dataItem, ImportConfiguration importConfiguration, List<EntityExtractionResult> extractedEntities) {
        if (!extractedEntities.isEmpty()) {
            Map<String, Object> propertyValues = propertyMappingUtils.getPropertyValues(importConfiguration, dataItem);

            return extractedEntities.stream()
                    .filter(extractionResult -> !findNotEqualValue(propertyValues, extractionResult.getEntity()))
                    .findFirst().orElse(null);
        }
        return null;
    }

    protected boolean findNotEqualValue(Map<String, Object> propertyValues, Object entity) {
        return propertyValues.entrySet().stream().anyMatch(entry -> {
            String propertyName = entry.getKey();
            Object propertyValue = entry.getValue();
            Object propertyValueInEntity = EntityValues.getValue(entity, propertyName);
            return !EntityValues.propertyValueEquals(propertyValue, propertyValueInEntity);
        });
    }
}
