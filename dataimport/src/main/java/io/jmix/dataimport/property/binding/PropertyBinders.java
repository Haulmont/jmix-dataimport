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

import io.jmix.dataimport.model.configuration.mapping.PropertyMapping;
import io.jmix.dataimport.property.binding.impl.AssociationPropertyBinder;
import io.jmix.dataimport.property.binding.impl.CustomValuePropertyBinder;
import io.jmix.dataimport.property.binding.impl.SimplePropertyBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component("datimp_PropertyBinders")
public class PropertyBinders {

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected CustomValuePropertyBinder customValuePropertyBinder;

    @Autowired
    protected SimplePropertyBinder simplePropertyBinder;

    @Autowired
    protected AssociationPropertyBinder associationPropertyBinder;

    public PropertyBinder getPropertyBinder(PropertyMapping propertyMapping) {
        if (propertyMapping.getCustomValueFunction() != null) {
            return customValuePropertyBinder;
        }
        if (!propertyMapping.isAssociation()) {
            return simplePropertyBinder;
        } else {
            return associationPropertyBinder;
        }
    }
}
