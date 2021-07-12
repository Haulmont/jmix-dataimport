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

package io.jmix.dataimport.property.binding.impl;

import io.jmix.core.DataManager;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.metamodel.model.Range;
import io.jmix.dataimport.DuplicateEntityManager;
import io.jmix.dataimport.EntityPopulator;
import io.jmix.dataimport.property.binding.PropertyBindingUtils;
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping;
import io.jmix.dataimport.model.configuration.mapping.ReferenceEntityPolicy;
import io.jmix.dataimport.property.association.AssociationCreator;
import io.jmix.dataimport.property.binding.PropertyBindingContext;
import io.jmix.dataimport.property.binding.PropertyBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

@Component("datimp_AssociationPropertyBinder")
public class AssociationPropertyBinder implements PropertyBinder {
    public static final Logger log = LoggerFactory.getLogger(AssociationPropertyBinder.class);
    @Autowired
    protected SimplePropertyValueProvider simplePropertyValueProvider;
    @Autowired
    protected MetadataTools metadataTools;
    @Autowired
    protected DataManager dataManager;
    @Autowired
    protected AssociationCreator associationCreator;
    @Autowired
    protected DuplicateEntityManager duplicateEntityManager;
    @Autowired
    protected EntityPopulator entityPopulator;
    @Autowired
    protected Metadata metadata;
    @Autowired
    protected PropertyBindingUtils propertyBindingUtils;

    @Override
    public void bindProperty(Object entityToPopulate, PropertyBindingContext context) {
        if (metadataTools.isEmbedded(context.getAssociationMetaProperty())) {
            bindOneToOneAssociation(entityToPopulate, context);
            return;
        }

        Range.Cardinality cardinality = context.getAssociationMetaProperty().getRange().getCardinality();
        switch (cardinality) {
            case MANY_TO_ONE:
                bindManyToOneAssociation(entityToPopulate, context);
                break;
            case ONE_TO_ONE:
                bindOneToOneAssociation(entityToPopulate, context);
                break;
            case ONE_TO_MANY:
                bindOneToManyAssociation(entityToPopulate, context);
                break;
            default:
                break;
        }
    }

    protected void bindOneToOneAssociation(Object entityToPopulate, PropertyBindingContext context) {
        PropertyMapping associationMapping = context.getPropertyMapping();
        Object resultValue = loadEntity(context);
        if (resultValue == null) {
            ReferenceEntityPolicy referenceEntityPolicy = associationMapping.getReferenceEntityPolicy();
            if (referenceEntityPolicy == ReferenceEntityPolicy.CREATE) {
                resultValue = associationCreator.createOneToOneAssociation(context);
            } else if (referenceEntityPolicy == ReferenceEntityPolicy.IGNORE) {
                logIgnoredAssociation(entityToPopulate, context);
            }
        }

        if (resultValue != null) {
            EntityValues.setValue(entityToPopulate, associationMapping.getPropertyName(), resultValue);
        }
    }

    protected void bindOneToManyAssociation(Object entityToPopulate, PropertyBindingContext context) {
        PropertyMapping associationMapping = context.getPropertyMapping();
        ReferenceEntityPolicy referenceEntityPolicy = associationMapping.getReferenceEntityPolicy();

        if (referenceEntityPolicy == ReferenceEntityPolicy.CREATE) {
            Collection<Object> oneToManyAssociation = associationCreator.createOneToManyAssociation(entityToPopulate, context);
            EntityValues.setValue(entityToPopulate, associationMapping.getPropertyName(), oneToManyAssociation);
        } else if (referenceEntityPolicy == ReferenceEntityPolicy.IGNORE) {
            logIgnoredAssociation(entityToPopulate, context);
        }
    }


    protected void bindManyToOneAssociation(Object entityToPopulate, PropertyBindingContext context) {
        PropertyMapping associationMapping = context.getPropertyMapping();
        Object resultValue = loadEntity(context);
        if (resultValue == null) {
            ReferenceEntityPolicy referenceEntityPolicy = associationMapping.getReferenceEntityPolicy();
            if (referenceEntityPolicy == ReferenceEntityPolicy.CREATE) {
                resultValue = associationCreator.createManyToOneAssociation(context);
            } else if (referenceEntityPolicy == ReferenceEntityPolicy.IGNORE) {
                logIgnoredAssociation(entityToPopulate, context);
            }
        }
        if (resultValue != null) {
            EntityValues.setValue(entityToPopulate, associationMapping.getPropertyName(), resultValue);
        }
    }

    @Nullable
    public Object loadEntity(PropertyBindingContext context) {
        MetaProperty associationMetaProperty = context.getAssociationMetaProperty();
        MetaClass associationMetaClass = associationMetaProperty.getRange().asClass();

        if (metadataTools.isEmbedded(associationMetaProperty)) {
            return null;
        }

        Map<String, Object> propertyValues = propertyBindingUtils.getPropertyValues(context);

        if (!propertyValues.isEmpty()) {
            return duplicateEntityManager.load(associationMetaClass.getJavaClass(), propertyValues, null);
        }

        return null;
    }

    private void logIgnoredAssociation(Object entityToPopulate, PropertyBindingContext context) {
        log.info(String.format("Existing value not found for property [%s] in entity [%s], but new one is not created by policy. Values source: %s",
                context.getPropertyMapping().getPropertyName(),
                metadata.getClass(entityToPopulate).getName(),
                context.getSourceObject()));
    }
}
