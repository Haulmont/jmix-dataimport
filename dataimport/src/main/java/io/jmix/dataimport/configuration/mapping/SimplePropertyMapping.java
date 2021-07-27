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
 * Mapping for the simple property of entity.
 * The following types of simple properties are supported:
 * <ul>
 *     <li>String</li>
 *     <li>Integer</li>
 *     <li>Double</li>
 *     <li>Long</li>
 *     <li>BigDecimal</li>
 *     <li>Boolean</li>
 *     <li>Date</li>
 *     <li>Local date</li>
 *     <li>Enumeration</li>
 * </ul>
 */
public class SimplePropertyMapping implements PropertyMapping {
    protected String entityPropertyName;
    protected String dataFieldName;

    public SimplePropertyMapping(String entityPropertyName, String dataFieldName) {
        this.entityPropertyName = entityPropertyName;
        this.dataFieldName = dataFieldName;
    }

    public String getDataFieldName() {
        return dataFieldName;
    }

    @Override
    public boolean isReference() {
        return false;
    }

    public String getEntityPropertyName() {
        return entityPropertyName;
    }
}