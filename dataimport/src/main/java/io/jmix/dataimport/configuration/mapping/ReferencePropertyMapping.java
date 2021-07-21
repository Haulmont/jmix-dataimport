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

/**
 * Mapping for reference property by one lookup property that has a raw value in the input data.
 * <br/>
 * This mapping allows to set:
 * <ul>
 *     <li>Entity property name: name of the reference property</li>
 *     <li>Lookup property name: name of the property from reference entity</li>
 *     <li>Data field name: name of the data field that contains a raw value of lookup property</li>
 *     <li>Reference import policy. Default value: {@link ReferenceImportPolicy#IGNORE_IF_MISSING}</li>
 * </ul>
 *
 * @see ReferenceImportPolicy
 */
public class ReferencePropertyMapping implements PropertyMapping {
    private String entityPropertyName;
    private String dataFieldName;
    private String lookupPropertyName;
    private ReferenceImportPolicy referenceImportPolicy;

    public ReferencePropertyMapping(String entityPropertyName, String dataFieldName, String lookupPropertyName) {
        this.entityPropertyName = entityPropertyName;
        this.dataFieldName = dataFieldName;
        this.lookupPropertyName = lookupPropertyName;
        this.referenceImportPolicy = ReferenceImportPolicy.IGNORE_IF_MISSING;
    }

    public ReferencePropertyMapping(String entityPropertyName, String lookupPropertyName, String dataFieldName, ReferenceImportPolicy referenceImportPolicy) {
        this.entityPropertyName = entityPropertyName;
        this.dataFieldName = dataFieldName;
        this.lookupPropertyName = lookupPropertyName;
        this.referenceImportPolicy = referenceImportPolicy;
    }

    private ReferencePropertyMapping(Builder builder) {
        this.entityPropertyName = builder.entityPropertyName;
        this.lookupPropertyName = builder.lookupPropertyName;
        this.dataFieldName = builder.dataFieldName;
        this.referenceImportPolicy = builder.referenceImportPolicy;
    }

    public String getEntityPropertyName() {
        return entityPropertyName;
    }

    public void setEntityPropertyName(String entityPropertyName) {
        this.entityPropertyName = entityPropertyName;
    }

    public String getDataFieldName() {
        return dataFieldName;
    }

    @Override
    public boolean isReference() {
        return true;
    }

    public void setDataFieldName(String dataFieldName) {
        this.dataFieldName = dataFieldName;
    }

    public String getLookupPropertyName() {
        return lookupPropertyName;
    }

    public void setLookupPropertyName(String lookupPropertyName) {
        this.lookupPropertyName = lookupPropertyName;
    }

    public ReferenceImportPolicy getReferenceImportPolicy() {
        return referenceImportPolicy;
    }

    public void setReferenceImportPolicy(ReferenceImportPolicy referenceImportPolicy) {
        this.referenceImportPolicy = referenceImportPolicy;
    }

    public static Builder byEntityPropertyName(String entityPropertyName) {
        return new Builder(entityPropertyName);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String entityPropertyName;
        private String lookupPropertyName;
        private ReferenceImportPolicy referenceImportPolicy = ReferenceImportPolicy.IGNORE_IF_MISSING;
        private String dataFieldName;

        public Builder(String entityPropertyName) {
            this.entityPropertyName = entityPropertyName;
        }

        public Builder() {
        }

        public Builder withLookupPropertyName(String lookupPropertyName) {
            this.lookupPropertyName = lookupPropertyName;
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

        public ReferencePropertyMapping build() {
            validate();
            return new ReferencePropertyMapping(this);
        }

        protected void validate() {
            if (entityPropertyName == null) {
                throw new IllegalArgumentException("Entity property name is not set for reference property mapping");
            }
            if (lookupPropertyName == null) {
                throw new IllegalArgumentException(String.format("Lookup property is not set for reference property [%s]", entityPropertyName));
            }

            if (dataFieldName == null) {
                throw new IllegalArgumentException(String.format("Data field is not set for reference property [%s]", entityPropertyName));
            }
        }
    }
}
