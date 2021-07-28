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

package io.jmix.dataimport.configuration;

import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.dataimport.configuration.mapping.PropertyMapping;
import io.jmix.dataimport.configuration.mapping.ReferenceImportPolicy;
import io.jmix.dataimport.configuration.mapping.ReferenceMultiFieldPropertyMapping;
import io.jmix.dataimport.exception.ImportException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("datimp_ImportConfigurationValidator")
public class ImportConfigurationValidator {
    @Autowired
    protected MetadataTools metadataTools;
    @Autowired
    protected Metadata metadata;

    public void validate(ImportConfiguration importConfiguration) throws ImportException {
        MetaClass entityMetaClass = metadata.getClass(importConfiguration.getEntityClass());
        validatePropertyMappings(entityMetaClass, importConfiguration.getPropertyMappings());
    }

    protected void validatePropertyMappings(MetaClass ownerEntity, List<PropertyMapping> propertyMappings) {
        propertyMappings.forEach(propertyMapping -> {
            if (propertyMapping instanceof ReferenceMultiFieldPropertyMapping) {
                validateMultiFieldMapping(ownerEntity, (ReferenceMultiFieldPropertyMapping) propertyMapping);
            }
        });
    }

    protected void validateMultiFieldMapping(MetaClass ownerEntity, ReferenceMultiFieldPropertyMapping propertyMapping) {
        MetaProperty referenceProperty = ownerEntity.getProperty(propertyMapping.getEntityPropertyName());
        if (metadataTools.isEmbedded(referenceProperty) && propertyMapping.getReferenceImportPolicy() != ReferenceImportPolicy.CREATE) {
            throw new ImportException(String.format("Incorrect policy [%s] for embedded reference [%s]. Only CREATE policy supported.", propertyMapping.getReferenceImportPolicy(),
                    propertyMapping.getEntityPropertyName()));
        }
        validatePropertyMappings(referenceProperty.getRange().asClass(), propertyMapping.getReferencePropertyMappings());
    }
}
