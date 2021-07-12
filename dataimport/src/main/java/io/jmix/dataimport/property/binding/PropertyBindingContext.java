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

import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.dataimport.extractor.data.ImportedObject;
import io.jmix.dataimport.model.configuration.ImportConfiguration;
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping;

import javax.annotation.Nullable;
import java.util.List;

public class PropertyBindingContext {
    protected ImportConfiguration importConfiguration;
    protected PropertyMapping propertyMapping;
    protected ImportedObject sourceObject;

    protected MetaClass ownerEntityMetaClass;

    protected List<Object> createdAssociations;

    public PropertyBindingContext(ImportConfiguration importConfiguration,
                                  PropertyMapping propertyMapping,
                                  ImportedObject sourceObject) {
        this.importConfiguration = importConfiguration;
        this.propertyMapping = propertyMapping;
        Object rawValue = sourceObject.getRawValue(propertyMapping.getFieldName());
        if (rawValue instanceof ImportedObject) {
            this.sourceObject = (ImportedObject) rawValue;
        } else {
            this.sourceObject = sourceObject;
        }
    }

    public ImportConfiguration getImportConfiguration() {
        return importConfiguration;
    }

    public PropertyBindingContext setImportConfiguration(ImportConfiguration importConfiguration) {
        this.importConfiguration = importConfiguration;
        return this;
    }

    public PropertyMapping getPropertyMapping() {
        return propertyMapping;
    }

    public PropertyBindingContext setPropertyMapping(PropertyMapping propertyMapping) {
        this.propertyMapping = propertyMapping;
        return this;
    }

    public ImportedObject getSourceObject() {
        return sourceObject;
    }

    public PropertyBindingContext setSourceObject(ImportedObject sourceObject) {
        this.sourceObject = sourceObject;
        return this;
    }

    public MetaClass getOwnerEntityMetaClass() {
        return ownerEntityMetaClass;
    }

    public PropertyBindingContext setOwnerEntityMetaClass(MetaClass ownerEntityMetaClass) {
        this.ownerEntityMetaClass = ownerEntityMetaClass;
        return this;
    }

    @Nullable
    public List<Object> getCreatedAssociations() {
        return createdAssociations;
    }

    public PropertyBindingContext setCreatedAssociations(@Nullable List<Object> createdAssociations) {
        this.createdAssociations = createdAssociations;
        return this;
    }

    public Object getRawValue() {
        Object rawValue = sourceObject.getRawValue(propertyMapping.getFieldName()); //get raw value for object list/simple properties
        if (rawValue != null) {
            return rawValue;
        }
        return sourceObject;
    }

    public MetaProperty getAssociationMetaProperty() {
        return ownerEntityMetaClass.getProperty(propertyMapping.getPropertyName());
    }

}
