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

import io.jmix.dataimport.property.populator.CustomMappingContext;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mapping for reference property that is mapped by multiple data fields from the input data.
 * This mapping allows to set:
 * <ul>
 *     <li>Entity property name: name of the reference property</li>
 *     <li>Data field name (optional): name of the data field that contains a raw values of properties for the reference entity.
 *         <b>Note:</b> this field is helpful for JSON (if there is a separate object for reference property)
 *         or XML (if there is a separate tag for reference property).
 *     </li>
 *     <li>Reference import policy: {@link ReferenceImportPolicy}</li>
 *     <li>Lookup property names: property names by which existing entity should be searched.</li>
 *     <li>Reference property mappings: list of {@link PropertyMapping} for reference entity properties.</li>
 * </ul>
 */
public class ReferenceMultiFieldPropertyMapping implements PropertyMapping {
    protected String entityPropertyName;
    protected String dataFieldName; //optional
    protected List<PropertyMapping> referencePropertyMappings = new ArrayList<>();
    protected List<String> lookupPropertyNames;
    protected ReferenceImportPolicy referenceImportPolicy;

    private ReferenceMultiFieldPropertyMapping(Builder builder) {
        this.entityPropertyName = builder.entityPropertyName;
        this.referencePropertyMappings = builder.referencePropertyMappings;
        this.lookupPropertyNames = builder.lookupPropertyNames;
        this.referenceImportPolicy = builder.referenceImportPolicy;
        this.dataFieldName = builder.dataFieldName;
    }

    public String getDataFieldName() {
        return dataFieldName;
    }

    @Override
    public boolean isReference() {
        return true;
    }

    public ReferenceMultiFieldPropertyMapping setDataFieldName(String dataFieldName) {
        this.dataFieldName = dataFieldName;
        return this;
    }

    public List<PropertyMapping> getReferencePropertyMappings() {
        return referencePropertyMappings;
    }

    public ReferenceMultiFieldPropertyMapping setReferencePropertyMappings(List<PropertyMapping> referencePropertyMappings) {
        this.referencePropertyMappings = referencePropertyMappings;
        return this;
    }

    public List<String> getLookupPropertyNames() {
        return lookupPropertyNames;
    }

    public ReferenceMultiFieldPropertyMapping setLookupPropertyNames(List<String> lookupPropertyNames) {
        this.lookupPropertyNames = lookupPropertyNames;
        return this;
    }

    public ReferenceImportPolicy getReferenceImportPolicy() {
        return referenceImportPolicy;
    }

    public String getEntityPropertyName() {
        return entityPropertyName;
    }

    public static Builder builder(String entityPropertyName, ReferenceImportPolicy policy) {
        return new Builder(entityPropertyName, policy);
    }

    public static class Builder {
        private String entityPropertyName;
        private List<PropertyMapping> referencePropertyMappings = new ArrayList<>();
        private List<String> lookupPropertyNames = new ArrayList<>();
        private ReferenceImportPolicy referenceImportPolicy;
        private String dataFieldName; //optional - makes sense for XML and JSON only
        private boolean lookupByAllSimpleProperties;

        public Builder(String entityPropertyName, ReferenceImportPolicy referenceImportPolicy) {
            this.entityPropertyName = entityPropertyName;
            this.referenceImportPolicy = referenceImportPolicy;
        }

        public Builder addSimplePropertyMapping(String entityPropertyName, String dataFieldName) {
            this.referencePropertyMappings.add(new SimplePropertyMapping(entityPropertyName, dataFieldName));
            return this;
        }

        public Builder addCustomPropertyMapping(String entityPropertyName,
                                                String dataFieldName,
                                                Function<CustomMappingContext, Object> customValueFunction) {
            this.referencePropertyMappings.add(new CustomPropertyMapping(entityPropertyName, dataFieldName, customValueFunction));
            return this;
        }

        public Builder addCustomPropertyMapping(String entityPropertyName,
                                                Function<CustomMappingContext, Object> customValueFunction) {
            this.referencePropertyMappings.add(new CustomPropertyMapping(entityPropertyName)
                    .setCustomValueFunction(customValueFunction));
            return this;
        }

        public Builder addReferencePropertyMapping(String entityPropertyName,
                                                   String dataFieldName,
                                                   String lookupPropertyName,
                                                   ReferenceImportPolicy policy) {
            this.referencePropertyMappings.add(new ReferencePropertyMapping(entityPropertyName)
                    .setDataFieldName(dataFieldName)
                    .setLookupPropertyName(lookupPropertyName)
                    .setReferenceImportPolicy(policy));
            return this;
        }

        public Builder addPropertyMapping(PropertyMapping propertyMapping) {
            this.referencePropertyMappings.add(propertyMapping);
            return this;
        }

        public Builder withLookupPropertyNames(List<String> lookupPropertyNames) {
            this.lookupPropertyNames = lookupPropertyNames;
            return this;
        }

        /**
         * All simple properties for which mappings are set will be used to lookup existing entity.
         *
         * @return current instance of builder
         */
        public Builder lookupByAllSimpleProperties() {
            this.lookupByAllSimpleProperties = true;
            return this;
        }

        public Builder withLookupPropertyNames(String... lookupPropertyNames) {
            this.lookupPropertyNames = Arrays.asList(lookupPropertyNames);
            return this;
        }

        public Builder withDataFieldName(String dataFieldName) {
            this.dataFieldName = dataFieldName;
            return this;
        }

        public ReferenceMultiFieldPropertyMapping build() {
            if (lookupByAllSimpleProperties) {
                this.lookupPropertyNames = referencePropertyMappings.stream().map(PropertyMapping::getEntityPropertyName).collect(Collectors.toList());
            }
            validate();
            return new ReferenceMultiFieldPropertyMapping(this);
        }

        protected void validate() {
            if (referenceImportPolicy == null) {
                throw new IllegalArgumentException(String.format("Reference import policy is not set for property [%s]", entityPropertyName));
            }
            if (referenceImportPolicy != ReferenceImportPolicy.CREATE && (CollectionUtils.isEmpty(lookupPropertyNames))) {
                throw new IllegalArgumentException(String.format("Lookup properties are not set for property [%s]", entityPropertyName));
            }
        }
    }
}
