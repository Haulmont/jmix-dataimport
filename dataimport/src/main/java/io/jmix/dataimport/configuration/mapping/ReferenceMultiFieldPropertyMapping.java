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
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    protected List<PropertyMapping> referencePropertyMappings;
    protected List<String> lookupPropertyNames;
    protected ReferenceImportPolicy referenceImportPolicy;

    public ReferenceMultiFieldPropertyMapping(String entityPropertyName,
                                              ReferenceImportPolicy importPolicy) {
        this.entityPropertyName = entityPropertyName;
        this.referenceImportPolicy = importPolicy;
    }

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

    public ReferenceMultiFieldPropertyMapping addReferencePropertyMapping(PropertyMapping referencePropertyMapping) {
        this.referencePropertyMappings.add(referencePropertyMapping);
        return this;
    }

    public ReferenceMultiFieldPropertyMapping addSimplePropertyMapping(String entityProperty, String dataFieldName) {
        this.referencePropertyMappings.add(new SimplePropertyMapping(entityProperty, dataFieldName));
        return this;
    }

    public List<String> getLookupPropertyNames() {
        return lookupPropertyNames;
    }

    public ReferenceMultiFieldPropertyMapping setLookupPropertyNames(List<String> lookupPropertyNames) {
        this.lookupPropertyNames = lookupPropertyNames;
        return this;
    }

    public ReferenceMultiFieldPropertyMapping setLookupPropertyNames(String... lookupPropertyNames) {
        this.lookupPropertyNames = Arrays.asList(lookupPropertyNames);
        return this;
    }

    public ReferenceImportPolicy getReferenceImportPolicy() {
        return referenceImportPolicy;
    }

    public ReferenceMultiFieldPropertyMapping setReferenceImportPolicy(ReferenceImportPolicy referenceImportPolicy) {
        this.referenceImportPolicy = referenceImportPolicy;
        return this;
    }

    public String getEntityPropertyName() {
        return entityPropertyName;
    }

    public ReferenceMultiFieldPropertyMapping setEntityPropertyName(String entityPropertyName) {
        this.entityPropertyName = entityPropertyName;
        return this;
    }

    public static Builder builder(String entityPropertyName) {
        return new Builder(entityPropertyName);
    }

    public static class Builder {
        private String entityPropertyName;
        private List<PropertyMapping> referencePropertyMappings = new ArrayList<>();
        private List<String> lookupPropertyNames = new ArrayList<>();
        private ReferenceImportPolicy referenceImportPolicy;
        private String dataFieldName; //optional - makes sense for XML and JSON only
        private boolean lookupByAllSimpleProperties;

        public Builder(String entityPropertyName) {
            this.entityPropertyName = entityPropertyName;
        }

        public Builder addSimplePropertyMapping(String entityPropertyName, String dataFieldName) {
            this.referencePropertyMappings.add(new SimplePropertyMapping(entityPropertyName, dataFieldName));
            return this;
        }

        public Builder addCustomPropertyMapping(String entityPropertyName,
                                                String dataFieldName,
                                                Function<PropertyMappingContext, Object> customValueFunction) {
            this.referencePropertyMappings.add(new CustomPropertyMapping(entityPropertyName, dataFieldName, customValueFunction));
            return this;
        }

        public Builder addReferencePropertyMapping(String entityPropertyName,
                                                   String lookupPropertyName,
                                                   String dataFieldName,
                                                   ReferenceImportPolicy policy) {
            this.referencePropertyMappings.add(ReferencePropertyMapping.byEntityPropertyName(entityPropertyName)
                    .withDataFieldName(dataFieldName)
                    .withLookupPropertyName(lookupPropertyName)
                    .withReferenceImportPolicy(policy)
                    .build());
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

        public Builder withSimplePropertyMappings(String entityPropertyName1, String dataFieldName1,
                                                  String entityPropertyName2, String dataFieldName2) {
            this.referencePropertyMappings.add(new SimplePropertyMapping(entityPropertyName1, dataFieldName1));
            this.referencePropertyMappings.add(new SimplePropertyMapping(entityPropertyName2, dataFieldName2));
            return this;
        }

        /**
         * Creates simple property mappings for each key-value pair in map.
         * <br/>
         * <b>Note:</b> Key is interpreted as entity property name, value - data field name.
         *
         * @param simplePropertyMappings map that contains data for simple property mapping where key - entity property name, value - data field name
         * @return current instance of builder
         */
        public Builder withSimplePropertyMappings(Map<String, String> simplePropertyMappings) {
            simplePropertyMappings.forEach(this::addSimplePropertyMapping);
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

        public Builder withReferenceImportPolicy(ReferenceImportPolicy referenceImportPolicy) {
            this.referenceImportPolicy = referenceImportPolicy;
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
