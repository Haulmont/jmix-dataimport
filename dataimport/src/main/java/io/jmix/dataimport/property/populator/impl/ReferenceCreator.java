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

import io.jmix.core.Metadata;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.metamodel.model.Range;
import io.jmix.dataimport.DuplicateEntityManager;
import io.jmix.dataimport.extractor.data.RawValuesSource;
import io.jmix.dataimport.configuration.mapping.ReferenceMultiFieldPropertyMapping;
import io.jmix.dataimport.configuration.mapping.ReferencePropertyMapping;
import io.jmix.dataimport.configuration.mapping.PropertyMapping;
import io.jmix.dataimport.property.populator.EntityPropertiesPopulator;
import io.jmix.dataimport.extractor.data.ImportedObject;
import io.jmix.dataimport.extractor.data.ImportedObjectList;
import io.jmix.dataimport.property.populator.PropertyMappingUtils;
import io.jmix.dataimport.property.populator.PropertyMappingContext;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.*;


@Component("datimp_ReferenceCreator")
public class ReferenceCreator {
    public static final Logger log = LoggerFactory.getLogger(ReferenceCreator.class);

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected PropertyMappingUtils propertyMappingUtils;

    @Autowired
    protected EntityPropertiesPopulator entityPropertiesPopulator;

    @Autowired
    protected DuplicateEntityManager duplicateEntityManager;

    @Autowired
    protected SimplePropertyValueProvider simplePropertyValueProvider;

    @Nullable
    public Object createEntity(PropertyMappingContext context, @Nullable List<Object> createdReferences) {
        Range.Cardinality cardinality = context.getMetaProperty().getRange().getCardinality();
        if (cardinality == Range.Cardinality.MANY_TO_ONE) {
            return populateReferenceEntity(context, createdReferences, null);
        } else if (cardinality == Range.Cardinality.ONE_TO_ONE) {
            return populateReferenceEntity(context, null, null);
        }
        return null;
    }

    @Nullable
    public Collection<Object> createOneToManyCollection(Object entityToPopulate, PropertyMappingContext context) {
        Object rawValue = context.getRawValue();

        String propertyName = context.getPropertyMapping().getEntityPropertyName();
        Collection<Object> currentValue = EntityValues.getValue(entityToPopulate, propertyName);

        Collection<Object> resultCollection = currentValue == null ? createEmptyCollection(context.getMetaProperty()) : currentValue;
        if (resultCollection == null) {
            log.warn(String.format("Not supported type of collection for property [%s] in entity [%s]", propertyName, metadata.getClass(entityToPopulate).getName()));
            return null;
        }

        if (rawValue == null || rawValue instanceof RawValuesSource) {
            Object createdEntity = populateReferenceEntity(context, currentValue, null);
            resultCollection.add(createdEntity);
        } else if (rawValue instanceof ImportedObjectList) {
            Collection<Object> createdEntities = createEntityCollection(entityToPopulate, context, (ImportedObjectList) rawValue);
            resultCollection.addAll(createdEntities);
        }
        return resultCollection;
    }

    protected Object populateReferenceEntity(PropertyMappingContext context,
                                             @Nullable Collection<Object> existingEntities,
                                             @Nullable ImportedObject newValueSource) {
        if (newValueSource != null) {
            context.setRawValuesSource(newValueSource);
        }

        Object entityToPopulate = null;
        if (CollectionUtils.isNotEmpty(existingEntities)) {
            Map<String, Object> propertyValues = propertyMappingUtils.getPropertyValues(context);
            entityToPopulate = duplicateEntityManager.find(existingEntities, propertyValues);
        }

        if (entityToPopulate == null) {
            entityToPopulate = createEntity(context);
        }

        PropertyMapping propertyMapping = context.getPropertyMapping();
        if (propertyMapping instanceof ReferenceMultiFieldPropertyMapping) {
            return entityPropertiesPopulator.populateReference(entityToPopulate, (ReferenceMultiFieldPropertyMapping) propertyMapping,
                    context.getImportConfiguration(),
                    context.getRawValuesSource());
        } else if (propertyMapping instanceof ReferencePropertyMapping) {
            EntityValues.setValue(entityToPopulate, ((ReferencePropertyMapping) propertyMapping).getLookupPropertyName(),
                    simplePropertyValueProvider.getValue(context));
        }
        return entityToPopulate;
    }

    protected Object createEntity(PropertyMappingContext context) {
        MetaClass referenceMetaClass = context.getMetaProperty().getRange().asClass();
        return metadata.create(referenceMetaClass);
    }

    protected Collection<Object> createEntityCollection(Object entityToPopulate,
                                                        PropertyMappingContext context,
                                                        ImportedObjectList objectList) {
        Collection<Object> createdEntities = new ArrayList<>();
        objectList.getImportedObjects().forEach(importedObject -> {
            Object createdReference = populateReferenceEntity(context, createdEntities, importedObject);
            if (!createdEntities.contains(createdReference)) {
                createdEntities.add(createdReference);
            }
        });
        MetaProperty inverseProperty = context.getMetaProperty().getInverse();
        if (inverseProperty != null) {
            createdEntities.forEach(entity -> EntityValues.setValue(entity, inverseProperty.getName(), entityToPopulate));
        }
        return createdEntities;
    }

    @Nullable
    protected Collection<Object> createEmptyCollection(MetaProperty referenceMetaProperty) {
        Class<?> javaType = referenceMetaProperty.getJavaType();
        if (List.class.isAssignableFrom(javaType)) {
            return new ArrayList<>();
        } else if (Set.class.isAssignableFrom(javaType)) {
            return new LinkedHashSet<>();
        }
        return null;
    }
}
