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
import io.jmix.dataimport.configuration.*;
import io.jmix.dataimport.configuration.mapping.*;
import io.jmix.dataimport.extractor.data.RawValuesSource;
import io.jmix.dataimport.property.populator.PropertyMappingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Component("datimp_PropertyValueProvider")
public class PropertyValueProvider {

    @Autowired
    protected ReferenceValueProvider referenceValueProvider;

    @Autowired
    protected CustomValueProvider customValueProvider;

    @Autowired
    protected SimplePropertyValueProvider simplePropertyValueProvider;

    @Autowired
    protected Metadata metadata;

    @Nullable
    public Object getSimpleValue(Object entity,
                                 ImportConfiguration importConfiguration,
                                 PropertyMapping propertyMapping,
                                 RawValuesSource rawValuesSource) {
        PropertyMappingContext propertyMappingContext = createContext(entity, importConfiguration, propertyMapping, rawValuesSource);
        if (propertyMapping instanceof CustomPropertyMapping) {
            return customValueProvider.getValue(propertyMappingContext);
        } else if (propertyMapping instanceof SimplePropertyMapping) {
            return simplePropertyValueProvider.getValue(propertyMappingContext);
        }

        return null;
    }

    @Nullable
    public Object getReferenceValue(Object entity,
                                    ImportConfiguration importConfiguration,
                                    PropertyMapping propertyMapping,
                                    RawValuesSource rawValuesSource,
                                    @Nullable Map<PropertyMapping, List<Object>> createdReferences) {

        if (propertyMapping instanceof ReferenceMultiFieldPropertyMapping) {
            return processMultiFieldMapping(entity, importConfiguration, propertyMapping, rawValuesSource, createdReferences);
        } else if (propertyMapping instanceof ReferencePropertyMapping) {
            PropertyMappingContext propertyMappingContext = createContext(entity, importConfiguration, propertyMapping, rawValuesSource);
            return referenceValueProvider.getSingleEntity(propertyMappingContext, getCreatedReferences(propertyMapping, createdReferences));
        }
        return null;
    }

    @Nullable
    protected Object processMultiFieldMapping(Object entity,
                                              ImportConfiguration importConfiguration,
                                              PropertyMapping propertyMapping,
                                              RawValuesSource rawValuesSource,
                                              @Nullable Map<PropertyMapping, List<Object>> createdReferences) {
        PropertyMappingContext propertyMappingContext = createContext(entity, importConfiguration, propertyMapping, getRawValueSource(rawValuesSource, propertyMapping));
        boolean isCollection = propertyMappingContext.getMetaProperty().getRange().getCardinality().isMany();
        if (!isCollection) {
            return referenceValueProvider.getSingleEntity(propertyMappingContext, getCreatedReferences(propertyMapping, createdReferences));
        } else {
            return referenceValueProvider.getEntityCollection(entity, propertyMappingContext);
        }
    }


    protected PropertyMappingContext createContext(Object entity, ImportConfiguration importConfiguration,
                                                   PropertyMapping propertyMapping,
                                                   RawValuesSource rawValuesSource) {
        return new PropertyMappingContext(propertyMapping)
                .setImportConfiguration(importConfiguration)
                .setRawValuesSource(rawValuesSource)
                .setOwnerEntityMetaClass(metadata.getClass(entity));
    }

    protected RawValuesSource getRawValueSource(RawValuesSource rawValuesSource, PropertyMapping propertyMapping) {
        if (propertyMapping.getDataFieldName() == null) {
            return rawValuesSource;
        }
        Object propertyRawValuesSource = rawValuesSource.getRawValue(propertyMapping.getDataFieldName());
        if (propertyRawValuesSource instanceof RawValuesSource) {
            return (RawValuesSource) propertyRawValuesSource;
        }
        return rawValuesSource;
    }

    @Nullable
    protected List<Object> getCreatedReferences(PropertyMapping propertyMapping, @Nullable Map<PropertyMapping, List<Object>> createdReferences) {
        if (createdReferences != null) {
            return createdReferences.get(propertyMapping);
        }
        return null;
    }
}
