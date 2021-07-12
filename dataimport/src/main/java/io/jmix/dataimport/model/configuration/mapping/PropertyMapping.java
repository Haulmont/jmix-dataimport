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

package io.jmix.dataimport.model.configuration.mapping;

import io.jmix.dataimport.property.binding.CustomValueContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class PropertyMapping {
    protected String propertyName;
    protected String fieldName;

    protected boolean isAssociation;

    protected Object defaultValue;

    protected Function<CustomValueContext, Object> customValueFunction;

    protected ReferenceEntityPolicy referenceEntityPolicy;
    protected List<PropertyMapping> propertyMappings = new ArrayList<>();

    public PropertyMapping(String propertyName, String fieldName) {
        this.propertyName = propertyName;
        this.fieldName = fieldName;
    }

    public PropertyMapping(String propertyName, @Nullable String fieldName, ReferenceEntityPolicy referenceEntityPolicy) {
        this.propertyName = propertyName;
        this.isAssociation = true;
        this.referenceEntityPolicy = referenceEntityPolicy;
        this.fieldName = fieldName;
    }

    public PropertyMapping() {
    }

    public String getFieldName() {
        return fieldName;
    }

    public PropertyMapping setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public PropertyMapping setPropertyName(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    @Nullable
    public Object getDefaultValue() {
        return defaultValue;
    }

    public PropertyMapping setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    @Nullable
    public Function<CustomValueContext, Object> getCustomValueFunction() {
        return customValueFunction;
    }

    public PropertyMapping setCustomValueFunction(Function<CustomValueContext, Object> customValueFunction) {
        this.customValueFunction = customValueFunction;
        return this;
    }

    public PropertyMapping addSimplePropertyMapping(String propertyName, String fieldName) {
        this.propertyMappings.add(new PropertyMapping(propertyName, fieldName));
        return this;
    }

    public PropertyMapping addPropertyMapping(PropertyMapping propertyMapping) {
        this.propertyMappings.add(propertyMapping);
        return this;
    }

    public boolean isAssociation() {
        return isAssociation;
    }

    public void setAssociation(boolean association) {
        isAssociation = association;
    }

    public ReferenceEntityPolicy getReferenceEntityPolicy() {
        return referenceEntityPolicy;
    }

    public PropertyMapping setReferenceEntityPolicy(ReferenceEntityPolicy referenceEntityPolicy) {
        this.referenceEntityPolicy = referenceEntityPolicy;
        return this;
    }

    public List<PropertyMapping> getPropertyMappings() {
        return propertyMappings;
    }

    public PropertyMapping setPropertyMappings(List<PropertyMapping> propertyMappings) {
        this.propertyMappings = propertyMappings;
        return this;
    }
}