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
import io.jmix.dataimport.*;
import io.jmix.dataimport.extractor.data.ImportedData;
import io.jmix.dataimport.extractor.data.ImportedDataItem;
import io.jmix.dataimport.extractor.entity.EntityExtractionResult;
import io.jmix.dataimport.extractor.entity.EntityExtractor;
import io.jmix.dataimport.model.configuration.ImportConfiguration;
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping;
import io.jmix.dataimport.property.binding.impl.SimplePropertyValueProvider;
import io.jmix.dataimport.property.association.CreatedAssociation;
import io.jmix.dataimport.property.binding.PropertyBindingUtils;
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
    protected EntityPopulator entityPopulator;
    @Autowired
    protected SimplePropertyValueProvider simplePropertyValueProvider;
    @Autowired
    protected PropertyBindingUtils propertyBindingUtils;
    @Autowired
    protected DuplicateEntityManager duplicateEntityManager;

    @Override
    public EntityExtractionResult extract(ImportConfiguration importConfiguration, ImportedDataItem dataItem) {
        Object entity = metadata.create(importConfiguration.getEntityMetaClass());
        EntityInfo entityInfo = entityPopulator.populateProperties(entity, importConfiguration, dataItem);
        return new EntityExtractionResult(entityInfo.getEntity(), dataItem);
    }

    @Override
    public List<EntityExtractionResult> extract(ImportConfiguration importConfiguration, ImportedData importedData) {
        List<EntityExtractionResult> results = new ArrayList<>();
        Map<PropertyMapping, List<Object>> createdAssociations = new HashMap<>();

        importedData.getItems().forEach(importedDataItem -> {
            EntityExtractionResult alreadyExtractedEntity = getExistingEntity(importedDataItem, importConfiguration, results);
            Object entityToPopulate = alreadyExtractedEntity == null ? metadata.create(importConfiguration.getEntityMetaClass()) : alreadyExtractedEntity.getEntity();

            EntityInfo entityInfo = entityPopulator.populateProperties(entityToPopulate, importConfiguration, importedDataItem, createdAssociations);

            if (alreadyExtractedEntity == null) {
                results.add(new EntityExtractionResult(entityToPopulate, importedDataItem));
            } else {
                alreadyExtractedEntity.setImportedDataItem(importedDataItem);
            }

            fillCreatedAssociations(entityInfo, createdAssociations);
        });

        return results;
    }

    public void fillCreatedAssociations(EntityInfo entityInfo, Map<PropertyMapping, List<Object>> createdAssociationsByMapping) {
        List<CreatedAssociation> createdAssociations = entityInfo.getCreatedAssociations();
        if (CollectionUtils.isNotEmpty(createdAssociations)) {
            createdAssociations.forEach(createdAssociation -> {
                PropertyMapping propertyMapping = createdAssociation.getPropertyMapping();
                Object createdObject = createdAssociation.getCreatedObject();
                List<Object> createdObjects = createdAssociationsByMapping.getOrDefault(propertyMapping, new ArrayList<>());
                if (!(createdObject instanceof Collection) && !createdObjects.contains(createdObject)) {
                    createdObjects.add(createdObject);
                }
                createdAssociationsByMapping.put(propertyMapping, createdObjects);
            });
        }
    }

    @Nullable
    protected EntityExtractionResult getExistingEntity(ImportedDataItem dataItem, ImportConfiguration importConfiguration, List<EntityExtractionResult> extractedEntities) {
        if (!extractedEntities.isEmpty()) {
            Map<String, Object> propertyValues = propertyBindingUtils.getPropertyValues(importConfiguration, dataItem);

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
