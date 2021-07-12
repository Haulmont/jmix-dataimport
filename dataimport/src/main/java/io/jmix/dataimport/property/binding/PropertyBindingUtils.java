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

package io.jmix.dataimport.property.binding;

import io.jmix.core.Metadata;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.dataimport.property.binding.impl.SimplePropertyValueProvider;
import io.jmix.dataimport.extractor.data.ImportedDataItem;
import io.jmix.dataimport.extractor.data.ImportedObject;
import io.jmix.dataimport.model.configuration.ImportConfiguration;
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("datimp_PropertyBindingUtils")
public class PropertyBindingUtils {
    @Autowired
    protected SimplePropertyValueProvider simplePropertyValueProvider;

    @Autowired
    protected Metadata metadata;

    public PropertyBindingContext createContext(Object entityToPopulate,
                                                ImportConfiguration importConfiguration,
                                                ImportedObject importedObject,
                                                PropertyMapping propertyMapping) {
        return new PropertyBindingContext(importConfiguration, propertyMapping, importedObject)
                .setOwnerEntityMetaClass(metadata.getClass(entityToPopulate));
    }

    public Map<String, Object> getPropertyValues(PropertyBindingContext context) {
        PropertyMapping associationMapping = context.getPropertyMapping();
        MetaClass associationMetaClass = context.getAssociationMetaProperty().getRange().asClass();
        return getPropertyValues(context.getImportConfiguration(), associationMetaClass, associationMapping.getPropertyMappings(), context.getSourceObject());
    }

    public Map<String, Object> getPropertyValues(ImportConfiguration importConfiguration, ImportedDataItem dataItem) {
        return getPropertyValues(importConfiguration, metadata.getClass(importConfiguration.getEntityMetaClass()), importConfiguration.getPropertyMappings(), dataItem);
    }

    protected Map<String, Object> getPropertyValues(ImportConfiguration importConfiguration,
                                                    MetaClass sourceEntityMetaClass,
                                                    List<PropertyMapping> propertyMappings,
                                                    ImportedObject valuesSource) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyMappings.stream()
                .filter(propertyMapping -> !propertyMapping.isAssociation())
                .forEach(propertyMapping -> {
                    Object propertyRawValue = valuesSource.getRawValue(propertyMapping.getFieldName());
                    Object value = simplePropertyValueProvider.getValue(propertyRawValue, sourceEntityMetaClass,
                            propertyMapping, importConfiguration);
                    propertyValues.put(propertyMapping.getPropertyName(), value);
                });
        return propertyValues;
    }
}
