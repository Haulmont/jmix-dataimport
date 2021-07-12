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

import io.jmix.dataimport.extractor.data.ImportedObject;
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping;

public class CustomValueContext {
    protected Object rawValue;
    protected ImportedObject importedObject;
    protected PropertyMapping propertyMapping;

    public Object getRawValue() {
        return rawValue;
    }

    public CustomValueContext setRawValue(Object rawValue) {
        this.rawValue = rawValue;
        return this;
    }

    public ImportedObject getImportedObject() {
        return importedObject;
    }

    public CustomValueContext setImportedObject(ImportedObject importedObject) {
        this.importedObject = importedObject;
        return this;
    }

    public PropertyMapping getPropertyMapping() {
        return propertyMapping;
    }

    public CustomValueContext setPropertyMapping(PropertyMapping propertyMapping) {
        this.propertyMapping = propertyMapping;
        return this;
    }
}
