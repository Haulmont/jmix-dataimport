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

package io.jmix.dataimport.property.binding.impl;

import io.jmix.core.entity.EntityValues;
import io.jmix.dataimport.property.binding.CustomValueContext;
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping;
import io.jmix.dataimport.property.binding.PropertyBindingContext;
import io.jmix.dataimport.property.binding.PropertyBinder;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component("datimp_CustomValuePropertyBinder")
public class CustomValuePropertyBinder implements PropertyBinder {
    @Override
    public void bindProperty(Object entityToPopulate, PropertyBindingContext context) {
        Object rawValue = context.getRawValue();
        PropertyMapping propertyMapping = context.getPropertyMapping();
        Function<CustomValueContext, Object> customValueFunction = propertyMapping.getCustomValueFunction();
        Object customValue = customValueFunction.apply(new CustomValueContext().setRawValue(rawValue).setImportedObject(context.getSourceObject()).setPropertyMapping(propertyMapping));
        EntityValues.setValue(entityToPopulate, propertyMapping.getPropertyName(), customValue);
    }
}
