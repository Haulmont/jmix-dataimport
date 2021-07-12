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

package io.jmix.dataimport.impl;

import io.jmix.core.*;
import io.jmix.core.entity.EntityValues;
import io.jmix.dataimport.*;
import io.jmix.dataimport.extractor.data.ImportedDataItem;
import io.jmix.dataimport.extractor.data.ImportedObject;
import io.jmix.dataimport.model.configuration.ImportConfiguration;
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping;

import io.jmix.dataimport.property.association.CreatedAssociation;
import io.jmix.dataimport.property.binding.PropertyBinder;
import io.jmix.dataimport.property.binding.PropertyBinders;
import io.jmix.dataimport.property.binding.PropertyBindingContext;
import io.jmix.dataimport.property.binding.PropertyBindingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component("datimp_EntityPopulator")
public class EntityPopulatorImpl implements EntityPopulator {
    public static final Logger log = LoggerFactory.getLogger(EntityPopulator.class);

    @Autowired
    protected Metadata metadata;
    @Autowired
    protected EntityStates entityStates;
    @Autowired
    protected PropertyBinders propertyBinders;
    @Autowired
    protected PropertyBindingUtils propertyBindingUtils;

    public EntityInfo populateProperties(Object entity, ImportConfiguration importConfiguration, ImportedDataItem dataItem) {
        importConfiguration.getPropertyMappings().forEach(propertyMapping -> {
            PropertyBindingContext context = propertyBindingUtils.createContext(entity, importConfiguration, dataItem, propertyMapping);
            populateProperty(entity, context);

        });
        return new EntityInfo(entity)
                .setCreatedAssociations(getCreateAssociations(entity, importConfiguration));
    }

    @Override
    public EntityInfo populateProperties(Object entity,
                                         ImportConfiguration importConfiguration,
                                         ImportedDataItem dataItem,
                                         Map<PropertyMapping, List<Object>> createdAssociations) {
        importConfiguration.getPropertyMappings().forEach(propertyMapping -> {
            PropertyBindingContext context = propertyBindingUtils.createContext(entity, importConfiguration, dataItem, propertyMapping);
            if (propertyMapping.isAssociation()) {
                context.setCreatedAssociations(createdAssociations.get(propertyMapping));
            }
            populateProperty(entity, context);

        });
        return new EntityInfo(entity)
                .setCreatedAssociations(getCreateAssociations(entity, importConfiguration));
    }

    protected void populateProperty(Object entity, PropertyBindingContext context) {
        PropertyBinder propertyBinder = propertyBinders.getPropertyBinder(context.getPropertyMapping());
        propertyBinder.bindProperty(entity, context);
    }

    @Override
    public Object populateAssociation(Object entity, PropertyMapping associationMapping,
                                      ImportConfiguration importConfiguration, ImportedObject sourceObject) {
        associationMapping.getPropertyMappings().forEach(propertyMapping ->
                populateProperty(entity, propertyBindingUtils.createContext(entity, importConfiguration,
                        sourceObject, propertyMapping)));

        return entity;
    }

    protected List<CreatedAssociation> getCreateAssociations(Object entity, ImportConfiguration configuration) {
        List<CreatedAssociation> createdAssociations = new ArrayList<>();
        configuration.getPropertyMappings().stream().filter(PropertyMapping::isAssociation)
                .forEach(propertyMapping -> addCreatedAssociation(entity, createdAssociations, propertyMapping));
        return createdAssociations;
    }

    protected void fillCreatedAssociations(Object entity, PropertyMapping associationPropertyMapping, List<CreatedAssociation> createdAssociations) {
        associationPropertyMapping.getPropertyMappings().stream().filter(PropertyMapping::isAssociation).forEach(propertyMapping -> {
            addCreatedAssociation(entity, createdAssociations, propertyMapping);
        });
    }

    protected void addCreatedAssociation(Object entity, List<CreatedAssociation> createdAssociations, PropertyMapping propertyMapping) {
        Object value = EntityValues.getValue(entity, propertyMapping.getPropertyName());
        if (value != null) {
            if (value instanceof Collection) {
                ((Collection<?>) value).forEach(o -> {
                    if (entityStates.isNew(o)) {
                        createdAssociations.add(getCreatedAssociation(entity, o, propertyMapping));
                        fillCreatedAssociations(o, propertyMapping, createdAssociations);
                    }
                });
            } else {
                if (entityStates.isNew(value)) {
                    createdAssociations.add(getCreatedAssociation(entity, value, propertyMapping));
                    fillCreatedAssociations(value, propertyMapping, createdAssociations);
                }
            }
        }

    }

    protected CreatedAssociation getCreatedAssociation(Object entityToPopulate, Object associationValue, PropertyMapping propertyMapping) {
        return new CreatedAssociation()
                .setOwnerEntity(entityToPopulate)
                .setCreatedObject(associationValue)
                .setPropertyMapping(propertyMapping);
    }
}