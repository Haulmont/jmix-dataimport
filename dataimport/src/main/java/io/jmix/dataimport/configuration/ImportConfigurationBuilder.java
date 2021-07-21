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

package io.jmix.dataimport.configuration;

import io.jmix.dataimport.InputDataFormat;
import io.jmix.dataimport.configuration.mapping.*;
import io.jmix.dataimport.extractor.entity.EntityExtractionResult;
import io.jmix.dataimport.property.populator.PropertyMappingContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Builds an instance of {@link ImportConfiguration} using the following parameters:
 * <ol>
 *     <li>Entity class (required): class of entity that will be imported using created import configuration</li>
 *     <li>Configuration code (required)</li>
 *     <li>Configuration name</li>
 *     <li>Property mappings: list of {@link PropertyMapping}</li>
 *     <li>Transaction strategy: {@link ImportTransactionStrategy}. By default, each entity is imported in the separate transaction.</li>
 *     <li>Input data format: xlsx, csv, json or xml.</li>
 *     <li>Date format</li>
 *     <li>Custom formats of boolean true and false values</li>
 *     <li>Pre-import predicate: a predicate that is executed for each extracted entity before import. If the predicate returns false, the entity won't be imported.
 *         Also it is possible to make additional changes with a extracted entity in the predicate. In this case, the entity will be imported with applied changes.
 *     </li>
 *     <li>Input data charset: this parameter is required if CSV is input data format. Default value: UTF-8</li>
 *     <li>Unique entity configurations: list of {@link UniqueEntityConfiguration}.</li>
 * </ol>
 */
public class ImportConfigurationBuilder {
    private Class entityClass;
    private String name;
    private String code;
    private List<PropertyMapping> propertyMappings = new ArrayList<>();

    private ImportTransactionStrategy transactionStrategy;

    private String inputDataFormat;

    private String dateFormat;
    private String booleanTrueValue;
    private String booleanFalseValue;

    private String inputDataCharset = StandardCharsets.UTF_8.name();

    private Predicate<EntityExtractionResult> preImportPredicate;

    private List<UniqueEntityConfiguration> uniqueEntityConfigurations = new ArrayList<>();

    public ImportConfigurationBuilder(Class entityClass, String configurationCode) {
        this.entityClass = entityClass;
        this.code = configurationCode;
    }

    /**
     * Creates an instance of {@link ImportConfigurationBuilder} for the specified entity class and import configuration code.
     *
     * @param entityClass       entity class
     * @param configurationCode import configuration code
     * @return new instance of {@link ImportConfigurationBuilder}
     */
    public static ImportConfigurationBuilder of(Class entityClass, String configurationCode) {
        return new ImportConfigurationBuilder(entityClass, configurationCode);
    }

    /**
     * Sets a import configuration name.
     *
     * @param name import configuration name
     * @return current instance of builder
     */
    public ImportConfigurationBuilder withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets a charset of input data.
     *
     * @param inputDataCharset charset of input data.
     * @return current instance of builder
     */
    public ImportConfigurationBuilder withFileCharset(String inputDataCharset) {
        this.inputDataCharset = inputDataCharset;
        return this;
    }

    /**
     * Adds a mapping for specified simple property.
     *
     * @param entityPropertyName entity property name
     * @param dataFieldName      name of the field from input data that contains a raw value of property
     * @return current instance of builder
     */
    public ImportConfigurationBuilder addSimplePropertyMapping(String entityPropertyName, String dataFieldName) {
        this.propertyMappings.add(new SimplePropertyMapping(entityPropertyName, dataFieldName));
        return this;
    }

    /**
     * Adds a custom mapping for property.
     *
     * @param entityPropertyName  entity property name
     * @param dataFieldName       name of the field from input data that contains a raw value of property
     * @param customValueFunction function to get value for the
     * @return current instance of builder
     */
    public ImportConfigurationBuilder addCustomPropertyMapping(String entityPropertyName,
                                                               String dataFieldName,
                                                               Function<PropertyMappingContext, Object> customValueFunction) {
        this.propertyMappings.add(new CustomPropertyMapping(entityPropertyName, dataFieldName, customValueFunction));
        return this;
    }

    public ImportConfigurationBuilder addUniqueEntityConfiguration(DuplicateEntityPolicy policy, String... entityPropertyNames) {
        this.uniqueEntityConfigurations.add(new UniqueEntityConfiguration(Arrays.asList(entityPropertyNames), policy));
        return this;
    }

    public ImportConfigurationBuilder addUniqueEntityConfiguration(DuplicateEntityPolicy policy, List<String> entityPropertyNames) {
        this.uniqueEntityConfigurations.add(new UniqueEntityConfiguration(entityPropertyNames, policy));
        return this;
    }

    public ImportConfigurationBuilder withTransactionStrategy(ImportTransactionStrategy transactionStrategy) {
        this.transactionStrategy = transactionStrategy;
        return this;
    }

    /**
     * Creates and adds a property mapping for the reference property mapped by one data field.
     *
     * @param entityPropertyName reference property name
     * @param lookupPropertyName property name from the reference entity
     * @param dataFieldName      name of the field from input data that contains a raw value of lookup property
     * @param policy             reference import policy
     * @return current instance of builder
     *
     * @see ReferencePropertyMapping
     */
    public ImportConfigurationBuilder addReferencePropertyMapping(String entityPropertyName,
                                                                  String lookupPropertyName,
                                                                  String dataFieldName,
                                                                  ReferenceImportPolicy policy) {
        this.propertyMappings.add(ReferencePropertyMapping.byEntityPropertyName(entityPropertyName)
                .withDataFieldName(dataFieldName)
                .withReferenceImportPolicy(policy)
                .withLookupPropertyName(lookupPropertyName)
                .build());
        return this;
    }

    /**
     * Adds specified property mapping. Property mapping can be:
     * <ul>
     *     <li>{@link SimplePropertyMapping}</li>
     *     <li>{@link CustomPropertyMapping}</li>
     *     <li>{@link ReferencePropertyMapping}</li>
     *     <li>{@link ReferenceMultiFieldPropertyMapping}</li>
     * </ul>
     *
     * @param propertyMapping property mapping
     * @return current instance of builder
     *
     * @see ReferenceMultiFieldPropertyMapping.Builder
     * @see ReferencePropertyMapping.Builder
     */
    public ImportConfigurationBuilder addPropertyMapping(PropertyMapping propertyMapping) {
        this.propertyMappings.add(propertyMapping);
        return this;
    }

    /**
     * Sets a date format.
     *
     * @param dateFormat date format used in the input data
     * @return current instance of builder
     */
    public ImportConfigurationBuilder withDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        return this;
    }

    /**
     * Sets the strings for boolean true and false values.
     *
     * @param booleanTrueValue  string that represents boolean true value
     * @param booleanFalseValue string that represents boolean false value
     * @return current instance of builder
     */
    public ImportConfigurationBuilder withBooleanFormats(String booleanTrueValue, String booleanFalseValue) {
        this.booleanTrueValue = booleanTrueValue;
        this.booleanFalseValue = booleanFalseValue;
        return this;
    }

    /**
     * Sets a format of input data. For example, xlsx or json.
     *
     * @param inputDataFormat format of input data
     * @return current instance of builder
     *
     * @see InputDataFormat
     */

    public ImportConfigurationBuilder withInputDataFormat(String inputDataFormat) {
        this.inputDataFormat = inputDataFormat;
        return this;
    }

    public ImportConfigurationBuilder withPreImportPredicate(Predicate<EntityExtractionResult> preImportPredicate) {
        this.preImportPredicate = preImportPredicate;
        return this;
    }

    public ImportConfiguration build() {
        return new ImportConfiguration(entityClass, this.code)
                .setDateFormat(dateFormat)
                .setBooleanTrueValue(booleanTrueValue)
                .setBooleanFalseValue(booleanFalseValue)
                .setName(this.name)
                .setTransactionStrategy(this.transactionStrategy)
                .setInputDataFormat(this.inputDataFormat)
                .setPropertyMappings(propertyMappings)
                .setInputDataCharset(this.inputDataCharset)
                .setPreImportPredicate(this.preImportPredicate)
                .setUniqueEntityConfigurations(this.uniqueEntityConfigurations);
    }
}
