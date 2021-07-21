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

package io.jmix.dataimport.configuration.mapping;

import io.jmix.dataimport.property.populator.PropertyMappingContext;

import java.util.function.Function;

/**
 * Mapping for entity property that has a custom value calculated by specified function.
 */
public class CustomPropertyMapping implements PropertyMapping {
    protected String entityPropertyName;
    protected String dataFieldName;
    protected Function<PropertyMappingContext, Object> customValueFunction;

    public CustomPropertyMapping(String entityPropertyName, String dataFieldName, Function<PropertyMappingContext, Object> customValueFunction) {
        this.entityPropertyName = entityPropertyName;
        this.dataFieldName = dataFieldName;
        this.customValueFunction = customValueFunction;
    }

    private CustomPropertyMapping(Builder builder) {
        this.entityPropertyName = builder.entityPropertyName;
        this.dataFieldName = builder.dataFieldName;
        this.customValueFunction = builder.customValueFunction;
    }

    public String getDataFieldName() {
        return dataFieldName;
    }

    @Override
    public boolean isReference() {
        return false;
    }

    public CustomPropertyMapping setDataFieldName(String dataFieldName) {
        this.dataFieldName = dataFieldName;
        return this;
    }

    public Function<PropertyMappingContext, Object> getCustomValueFunction() {
        return customValueFunction;
    }

    public CustomPropertyMapping setCustomValueFunction(Function<PropertyMappingContext, Object> customValueFunction) {
        this.customValueFunction = customValueFunction;
        return this;
    }

    public String getEntityPropertyName() {
        return entityPropertyName;
    }

    public void setEntityPropertyName(String entityPropertyName) {
        this.entityPropertyName = entityPropertyName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String entityPropertyName;
        private String dataFieldName;
        private Function<PropertyMappingContext, Object> customValueFunction;

        public Builder withEntityPropertyName(String entityPropertyName) {
            this.entityPropertyName = entityPropertyName;
            return this;
        }

        public Builder withDataFieldName(String dataFieldName) {
            this.dataFieldName = dataFieldName;
            return this;
        }

        public Builder withCustomValueFunction(Function<PropertyMappingContext, Object> customValueFunction) {
            this.customValueFunction = customValueFunction;
            return this;
        }

        public CustomPropertyMapping build() {
            return new CustomPropertyMapping(this);
        }
    }
}
