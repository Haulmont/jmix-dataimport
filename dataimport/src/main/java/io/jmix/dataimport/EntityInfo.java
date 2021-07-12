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

package io.jmix.dataimport;

import io.jmix.dataimport.property.association.CreatedAssociation;

import java.util.ArrayList;
import java.util.List;

public class EntityInfo {
    protected Object entity;
    protected List<CreatedAssociation> createdAssociations = new ArrayList<>();

    public EntityInfo(Object entity) {
        this.entity = entity;
    }

    public Object getEntity() {
        return entity;
    }

    public EntityInfo setEntity(Object entity) {
        this.entity = entity;
        return this;
    }

    public List<CreatedAssociation> getCreatedAssociations() {
        return createdAssociations;
    }

    public EntityInfo setCreatedAssociations(List<CreatedAssociation> createdAssociations) {
        this.createdAssociations = createdAssociations;
        return this;
    }
}