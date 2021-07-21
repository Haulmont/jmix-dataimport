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

package io.jmix.dataimport.property.populator.impl;

import io.jmix.core.*;
import io.jmix.core.entity.EntityValues;
import io.jmix.dataimport.extractor.data.RawValuesSource;
import io.jmix.dataimport.extractor.data.ImportedDataItem;
import io.jmix.dataimport.configuration.ImportConfiguration;
import io.jmix.dataimport.configuration.mapping.ReferenceMultiFieldPropertyMapping;
import io.jmix.dataimport.configuration.mapping.PropertyMapping;

import io.jmix.dataimport.property.populator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component("datimp_EntityPropertiesPopulator")
public class EntityPropertiesPopulatorImpl implements EntityPropertiesPopulator {
    public static final Logger log = LoggerFactory.getLogger(EntityPropertiesPopulator.class);

    @Autowired
    protected Metadata metadata;
    @Autowired
    protected EntityStates entityStates;
    @Autowired
    protected PropertyValueProvider propertyValueProvider;

    public EntityInfo populateProperties(Object entity, ImportConfiguration importConfiguration, ImportedDataItem dataItem) {
        return populateProperties(entity, importConfiguration, dataItem, null);
    }

    @Override
    public EntityInfo populateProperties(Object entity,
                                         ImportConfiguration importConfiguration,
                                         ImportedDataItem dataItem,
                                         @Nullable Map<PropertyMapping, List<Object>> createdReferences) {
        importConfiguration.getPropertyMappings().forEach(propertyMapping -> populateProperty(entity, propertyMapping, importConfiguration,
                dataItem, createdReferences));
        return new EntityInfo(entity)
                .setCreatedReferences(getCreatedReferences(entity, importConfiguration));
    }

    protected void populateProperty(Object entity, PropertyMapping propertyMapping,
                                    ImportConfiguration importConfiguration,
                                    RawValuesSource rawValuesSource,
                                    @Nullable Map<PropertyMapping, List<Object>> createdReferences) {
        Object value;
        if (propertyMapping.isReference()) {
            value = propertyValueProvider.getReferenceValue(entity, importConfiguration, propertyMapping, rawValuesSource, createdReferences);
        } else {
            value = propertyValueProvider.getSimpleValue(entity, importConfiguration, propertyMapping, rawValuesSource);
        }
        EntityValues.setValueEx(entity, propertyMapping.getEntityPropertyName(), value);
    }

    @Override
    public Object populateReference(Object entity, ReferenceMultiFieldPropertyMapping referenceMapping,
                                    ImportConfiguration importConfiguration, RawValuesSource rawValuesSource) {
        referenceMapping.getReferencePropertyMappings().forEach(propertyMapping ->
                populateProperty(entity, propertyMapping, importConfiguration, rawValuesSource, null));

        return entity;
    }

    protected List<CreatedReference> getCreatedReferences(Object entity, ImportConfiguration configuration) {
        List<CreatedReference> createdReferences = new ArrayList<>();
        configuration.getPropertyMappings().stream().filter(propertyMapping -> propertyMapping instanceof ReferenceMultiFieldPropertyMapping)
                .forEach(propertyMapping -> addCreatedReference(entity, createdReferences, (ReferenceMultiFieldPropertyMapping) propertyMapping));
        return createdReferences;
    }

    protected void fillCreatedReferences(Object entity, ReferenceMultiFieldPropertyMapping referenceMultiFieldPropertyMapping, List<CreatedReference> createdReferences) {
        referenceMultiFieldPropertyMapping.getReferencePropertyMappings()
                .stream()
                .filter(propertyMapping -> propertyMapping instanceof ReferenceMultiFieldPropertyMapping)
                .forEach(propertyMapping -> addCreatedReference(entity, createdReferences, (ReferenceMultiFieldPropertyMapping) propertyMapping));
    }

    protected void addCreatedReference(Object entity, List<CreatedReference> createdReferences, ReferenceMultiFieldPropertyMapping propertyMapping) {
        Object value = EntityValues.getValue(entity, propertyMapping.getEntityPropertyName());
        if (value != null) {
            if (value instanceof Collection) {
                ((Collection<?>) value).forEach(o -> {
                    if (entityStates.isNew(o)) {
                        createdReferences.add(getCreatedReferences(entity, o, propertyMapping));
                        fillCreatedReferences(o, propertyMapping, createdReferences);
                    }
                });
            } else {
                if (entityStates.isNew(value)) {
                    createdReferences.add(getCreatedReferences(entity, value, propertyMapping));
                    fillCreatedReferences(value, propertyMapping, createdReferences);
                }
            }
        }

    }

    protected CreatedReference getCreatedReferences(Object entityToPopulate, Object referenceValue, ReferenceMultiFieldPropertyMapping propertyMapping) {
        return new CreatedReference()
                .setOwnerEntity(entityToPopulate)
                .setCreatedObject(referenceValue)
                .setPropertyMapping(propertyMapping);
    }
}