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

package io.jmix.dataimport.property.association;

import io.jmix.core.Metadata;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.dataimport.DuplicateEntityManager;
import io.jmix.dataimport.EntityPopulator;
import io.jmix.dataimport.extractor.data.ImportedObject;
import io.jmix.dataimport.extractor.data.ImportedObjectList;
import io.jmix.dataimport.property.binding.PropertyBindingContext;
import io.jmix.dataimport.property.binding.PropertyBindingUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.*;


@Component("datimp_AssociationCreator")
public class AssociationCreator {
    public static final Logger log = LoggerFactory.getLogger(AssociationCreator.class);

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected PropertyBindingUtils propertyBindingUtils;

    @Autowired
    protected EntityPopulator entityPopulator;

    @Autowired
    protected DuplicateEntityManager duplicateEntityManager;


    @Nullable
    public Collection<Object> createOneToManyAssociation(Object entityToPopulate, PropertyBindingContext context) {
        Object rawValue = context.getRawValue();

        String propertyName = context.getPropertyMapping().getPropertyName();
        Collection<Object> currentValue = EntityValues.getValue(entityToPopulate, propertyName);

        Collection<Object> resultCollection = currentValue == null ? createEmptyCollection(context.getAssociationMetaProperty()) : currentValue;
        if (resultCollection == null) {
            log.warn(String.format("Not supported type of collection for property [%s] in entity [%s]", propertyName, metadata.getClass(entityToPopulate).getName()));
            return null;
        }
        if (rawValue instanceof ImportedObject) {
            Object createdEntity = createAssociation(context, currentValue, null);
            resultCollection.add(createdEntity);
        } else if (rawValue instanceof ImportedObjectList) {
            Collection<Object> createdEntities = createEntityCollection(entityToPopulate, context, (ImportedObjectList) rawValue);
            resultCollection.addAll(createdEntities);
        }
        return resultCollection;
    }

    @Nullable
    public Object createManyToOneAssociation(PropertyBindingContext context) {
        return createAssociation(context, context.getCreatedAssociations(), null);
    }

    @Nullable
    public Object createOneToOneAssociation(PropertyBindingContext context) {
        return createAssociation(context, null, null);
    }

    @Nullable
    protected Collection<Object> createEmptyCollection(MetaProperty associationMetaProperty) {
        Class<?> javaType = associationMetaProperty.getJavaType();
        if (List.class.isAssignableFrom(javaType)) {
            return new ArrayList<>();
        } else if (Set.class.isAssignableFrom(javaType)) {
            return new LinkedHashSet<>();
        }
        return null;
    }

    protected Object createAssociation(PropertyBindingContext context,
                                     @Nullable Collection<Object> existingEntities,
                                     @Nullable ImportedObject newValueSource) {

        if (newValueSource != null) {
            context.setSourceObject(newValueSource);
        }

        Object entityToPopulate = null;
        if (CollectionUtils.isNotEmpty(existingEntities)) {
            Map<String, Object> propertyValues = propertyBindingUtils.getPropertyValues(context);
            entityToPopulate = duplicateEntityManager.find(existingEntities, propertyValues);
        }

        if (entityToPopulate == null) {
            entityToPopulate = createEntity(context);
        }

        return entityPopulator.populateAssociation(entityToPopulate, context.getPropertyMapping(), context.getImportConfiguration(),
                context.getSourceObject());
    }

    protected Object createEntity(PropertyBindingContext context) {
        MetaClass associationMetaClass = context.getAssociationMetaProperty().getRange().asClass();
        return metadata.create(associationMetaClass);
    }

    protected Collection<Object> createEntityCollection(Object entityToPopulate,
                                                        PropertyBindingContext context,
                                                        ImportedObjectList objectList) {
        Collection<Object> createdEntities = new ArrayList<>();
        objectList.getImportedObjects().forEach(importedObject -> {
            Object createdAssociation = createAssociation(context, createdEntities, importedObject);
            if (!createdEntities.contains(createdAssociation)) {
                createdEntities.add(createdAssociation);
            }
        });
        MetaProperty inverseProperty = context.getAssociationMetaProperty().getInverse();
        if (inverseProperty != null) {
            createdEntities.forEach(entity -> EntityValues.setValue(entity, inverseProperty.getName(), entityToPopulate));
        }
        return createdEntities;
    }
}
