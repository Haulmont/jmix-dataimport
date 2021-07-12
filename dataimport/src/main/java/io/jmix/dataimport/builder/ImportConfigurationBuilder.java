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

package io.jmix.dataimport.builder;

import io.jmix.core.Metadata;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.utils.ObjectPathUtils;
import io.jmix.dataimport.property.binding.CustomValueContext;
import io.jmix.dataimport.extractor.entity.EntityExtractionResult;
import io.jmix.dataimport.model.configuration.DuplicateEntityPolicy;
import io.jmix.dataimport.model.configuration.ImportConfiguration;
import io.jmix.dataimport.model.configuration.ImportTransactionStrategy;
import io.jmix.dataimport.model.configuration.UniqueEntityConfiguration;
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping;
import io.jmix.dataimport.model.configuration.mapping.ReferenceEntityPolicy;
import org.apache.commons.compress.utils.CharsetNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@Component("datimp_ImportConfigurationBuilder")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ImportConfigurationBuilder {
    @Autowired
    private Metadata metadata;

    private String name;
    private String code;
    private Map<String, PropertyMapping> propertyMappings = new HashMap<>();

    private ImportTransactionStrategy transactionStrategy;

    private String entityMetaClass;
    private Class entityClass;

    private String inputDataFormat;

    private String dateFormat;
    private String booleanTrueValue;
    private String booleanFalseValue;

    private String fileCharset = CharsetNames.UTF_8;

    private Predicate<EntityExtractionResult> preImportPredicate;

    private List<UniqueEntityConfiguration> uniqueEntityConfigurations = new ArrayList<>();

    public ImportConfigurationBuilder(String entityMetaClass, String configurationCode) {
        this.entityMetaClass = entityMetaClass;
        this.code = configurationCode;
    }

    public ImportConfigurationBuilder(Class entityClass, String configurationCode) {
        this.entityClass = entityClass;
        this.code = configurationCode;
    }

    public ImportConfigurationBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ImportConfigurationBuilder withFileCharset(String fileCharset) {
        this.fileCharset = fileCharset;
        return this;
    }

    public ImportConfigurationBuilder addSimplePropertyMapping(String propertyName, String fieldName) {
        this.propertyMappings.put(propertyName, new PropertyMapping(propertyName, fieldName));
        return this;
    }

    public ImportConfigurationBuilder addCustomPropertyMapping(String propertyName,
                                                               String fieldName,
                                                               Function<CustomValueContext, Object> customValueFunction) {
        this.propertyMappings.put(propertyName, new PropertyMapping(propertyName, fieldName)
                .setCustomValueFunction(customValueFunction));
        return this;
    }

    public ImportConfigurationBuilder addUniqueEntityConfiguration(DuplicateEntityPolicy policy, String... properties) {
        this.uniqueEntityConfigurations.add(new UniqueEntityConfiguration(Arrays.asList(properties), policy));
        return this;
    }

    public ImportConfigurationBuilder withTransactionStrategy(ImportTransactionStrategy transactionStrategy) {
        this.transactionStrategy = transactionStrategy;
        return this;
    }

    public ImportConfigurationBuilder addAssociationPropertyMapping(String propertyPath,
                                                                    String fieldName,
                                                                    ReferenceEntityPolicy policy) {
        String[] properties = ObjectPathUtils.parseValuePath(propertyPath);
        if (properties.length == 2) {
            String baseProperty = properties[0];
            PropertyMapping associationPropertyMapping = propertyMappings.getOrDefault(baseProperty, new PropertyMapping(properties[0], null, policy));
            associationPropertyMapping.addSimplePropertyMapping(properties[1], fieldName);
            propertyMappings.put(baseProperty, associationPropertyMapping);
        }

        return this;
    }

    public ImportConfigurationBuilder addPropertyMapping(PropertyMapping propertyMapping) {
        this.propertyMappings.put(propertyMapping.getPropertyName(), propertyMapping);
        return this;
    }


    public ImportConfigurationBuilder withDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        return this;
    }

    public ImportConfigurationBuilder withBooleanFormats(String booleanTrueValue, String booleanFalseValue) {
        this.booleanTrueValue = booleanTrueValue;
        this.booleanFalseValue = booleanFalseValue;
        return this;
    }


    public ImportConfigurationBuilder withInputDataFormat(String inputDataFormat) {
        this.inputDataFormat = inputDataFormat;
        return this;
    }

    public ImportConfigurationBuilder withPreImportPredicate(Predicate<EntityExtractionResult> preImportPredicate) {
        this.preImportPredicate = preImportPredicate;
        return this;
    }

    public ImportConfiguration build() {
        String entityMetaClassName = getEntityMetaClassName();
        return new ImportConfiguration(entityMetaClassName, this.code)
                .setDateFormat(dateFormat)
                .setBooleanTrueValue(booleanTrueValue)
                .setBooleanFalseValue(booleanFalseValue)
                .setName(this.name)
                .setTransactionStrategy(this.transactionStrategy)
                .setInputDataFormat(this.inputDataFormat)
                .setPropertyMappings(new ArrayList<>(propertyMappings.values()))
                .setFileCharset(this.fileCharset)
                .setPreImportPredicate(this.preImportPredicate)
                .setUniqueEntityConfigurations(this.uniqueEntityConfigurations);
    }

    @Nullable
    protected String getEntityMetaClassName() {
        if (this.entityClass != null) {
            MetaClass entityMetaClass = metadata.findClass(entityClass);
            if (entityMetaClass != null) {
                return entityMetaClass.getName();
            }
        }
        return this.entityMetaClass;
    }
}
