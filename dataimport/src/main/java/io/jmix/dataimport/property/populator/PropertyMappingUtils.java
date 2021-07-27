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

package io.jmix.dataimport.property.populator;

import io.jmix.core.Metadata;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.dataimport.configuration.ImportConfiguration;
import io.jmix.dataimport.configuration.mapping.CustomPropertyMapping;
import io.jmix.dataimport.configuration.mapping.PropertyMapping;
import io.jmix.dataimport.configuration.mapping.ReferenceMultiFieldPropertyMapping;
import io.jmix.dataimport.configuration.mapping.ReferencePropertyMapping;
import io.jmix.dataimport.extractor.data.ImportedDataItem;
import io.jmix.dataimport.extractor.data.RawValuesSource;
import io.jmix.dataimport.property.populator.impl.CustomValueProvider;
import io.jmix.dataimport.property.populator.impl.SimplePropertyValueProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("datimp_PropertyMappingUtils")
public class PropertyMappingUtils {
    @Autowired
    protected SimplePropertyValueProvider simplePropertyValueProvider;

    @Autowired
    protected CustomValueProvider customValueProvider;

    @Autowired
    protected Metadata metadata;

    public Map<String, Object> getPropertyValues(PropertyMappingContext context) {
        PropertyMapping mapping = context.getPropertyMapping();
        if (mapping instanceof ReferenceMultiFieldPropertyMapping) {
            ReferenceMultiFieldPropertyMapping multiFieldMapping = (ReferenceMultiFieldPropertyMapping) mapping;
            List<String> lookupPropertyNames = multiFieldMapping.getLookupPropertyNames();
            List<PropertyMapping> propertyMappings = multiFieldMapping.getReferencePropertyMappings()
                    .stream()
                    .filter(propertyMapping -> lookupPropertyNames.contains(propertyMapping.getEntityPropertyName()))
                    .collect(Collectors.toList());
            MetaClass referenceMetaClass = context.getMetaProperty().getRange().asClass();
            return getPropertyValues(context.getImportConfiguration(), referenceMetaClass, propertyMappings, context.getRawValuesSource());
        } else if (mapping instanceof ReferencePropertyMapping) {
            return Collections.singletonMap(((ReferencePropertyMapping) mapping).getLookupPropertyName(),
                    simplePropertyValueProvider.getValue(context));
        }
        return Collections.emptyMap();
    }

    public Map<String, Object> getPropertyValues(ImportConfiguration importConfiguration, ImportedDataItem dataItem) {
        return getPropertyValues(importConfiguration, metadata.getClass(importConfiguration.getEntityClass()), importConfiguration.getPropertyMappings(), dataItem);
    }

    protected Map<String, Object> getPropertyValues(ImportConfiguration importConfiguration,
                                                    MetaClass sourceEntityMetaClass,
                                                    List<PropertyMapping> propertyMappings,
                                                    RawValuesSource valuesSource) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyMappings.stream()
                .filter(propertyMapping -> !propertyMapping.isReference())
                .forEach(propertyMapping -> {
                    Object value;
                    if (propertyMapping instanceof CustomPropertyMapping) {
                        value = customValueProvider.getValue((CustomPropertyMapping) propertyMapping, importConfiguration, valuesSource);
                    } else {
                        PropertyMappingContext mappingContext = new PropertyMappingContext(propertyMapping)
                                .setRawValuesSource(valuesSource)
                                .setImportConfiguration(importConfiguration)
                                .setOwnerEntityMetaClass(sourceEntityMetaClass);
                        value = simplePropertyValueProvider.getValue(mappingContext);
                    }
                    propertyValues.put(propertyMapping.getEntityPropertyName(), value);
                });
        return propertyValues;
    }
}
