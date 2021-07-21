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

import io.jmix.dataimport.configuration.mapping.PropertyMapping;
import io.jmix.dataimport.extractor.entity.EntityExtractionResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

/**
 * An object that allows configuring import process of entities from JSON, XML, CSV, XLSX.
 *
 * For that, there are the following options in the import configuration:
 * <ol>
 *     <li>Entity class (required): class of the entity that should be imported.</li>
 *     <li>Code (required): code of import configuration.</li>
 *     <li>Input data format (required): xlsx, csv, json or xml.</li>
 *     <li>Name: name of import configuration.</li>
 *     <li>Property mappings: list of {@link PropertyMapping}.</li>
 *     <li>Transaction strategy: {@link ImportTransactionStrategy}. By default, each entity is imported in the separate transaction.</li>
 *     <li>Date format: date format used in the input data.</li>
 *     <li>Custom formats of boolean true and false values.</li>
 *     <li>Pre-import predicate: a predicate that is executed for each extracted entity before import. If the predicate returns false, the entity won't be imported.
 *         Also it is possible to make additional changes with a extracted entity in the predicate. In this case, the entity will be imported with applied changes.
 *     </li>
 *     <li>Input data charset: this parameter is required if CSV is input data format. Default value: UTF-8.</li>
 *     <li>Unique entity configurations: list of {@link UniqueEntityConfiguration}.</li>
 * </ol>
 *
 * <br/>
 * Import configuration can be created by constructor or by {@link ImportConfigurationBuilder}.
 *
 * @see ImportConfigurationBuilder
 */
public class ImportConfiguration {
    protected Class entityClass;

    protected String name;
    protected String code;
    protected List<PropertyMapping> propertyMappings;

    protected ImportTransactionStrategy transactionStrategy;

    protected String inputDataFormat;

    protected String dateFormat;
    protected String booleanTrueValue;
    protected String booleanFalseValue;

    protected String inputDataCharset = StandardCharsets.UTF_8.name();

    protected List<UniqueEntityConfiguration> uniqueEntityConfigurations;

    protected Predicate<EntityExtractionResult> preImportPredicate;

    public ImportConfiguration(Class entityClass, String code) {
        this.entityClass = entityClass;
        this.code = code;
    }

    public List<PropertyMapping> getPropertyMappings() {
        return propertyMappings;
    }

    public ImportConfiguration setPropertyMappings(List<PropertyMapping> propertyMappings) {
        this.propertyMappings = propertyMappings;
        return this;
    }

    public ImportConfiguration addPropertyMapping(PropertyMapping propertyMapping) {
        this.propertyMappings.add(propertyMapping);
        return this;
    }

    public String getCode() {
        return code;
    }

    public ImportConfiguration setCode(String code) {
        this.code = code;
        return this;
    }

    public String getName() {
        return name;
    }

    public ImportConfiguration setName(String name) {
        this.name = name;
        return this;
    }

    public ImportTransactionStrategy getTransactionStrategy() {
        return transactionStrategy;
    }

    public ImportConfiguration setTransactionStrategy(ImportTransactionStrategy transactionStrategy) {
        this.transactionStrategy = transactionStrategy;
        return this;
    }

    public Class getEntityClass() {
        return entityClass;
    }

    public String getInputDataFormat() {
        return inputDataFormat;
    }

    public ImportConfiguration setInputDataFormat(String inputDataFormat) {
        this.inputDataFormat = inputDataFormat;
        return this;
    }

    public String getInputDataCharset() {
        return inputDataCharset;
    }

    public ImportConfiguration setInputDataCharset(String inputDataCharset) {
        this.inputDataCharset = inputDataCharset;
        return this;
    }

    public List<UniqueEntityConfiguration> getUniqueEntityConfigurations() {
        return uniqueEntityConfigurations;
    }

    public ImportConfiguration setUniqueEntityConfigurations(List<UniqueEntityConfiguration> uniqueEntityConfigurations) {
        this.uniqueEntityConfigurations = uniqueEntityConfigurations;
        return this;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public ImportConfiguration setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        return this;
    }

    public String getBooleanTrueValue() {
        return booleanTrueValue;
    }

    public ImportConfiguration setBooleanTrueValue(String booleanTrueValue) {
        this.booleanTrueValue = booleanTrueValue;
        return this;
    }

    public String getBooleanFalseValue() {
        return booleanFalseValue;
    }

    public ImportConfiguration setBooleanFalseValue(String booleanFalseValue) {
        this.booleanFalseValue = booleanFalseValue;
        return this;
    }

    public Predicate<EntityExtractionResult> getPreImportPredicate() {
        return preImportPredicate;
    }

    public ImportConfiguration setPreImportPredicate(Predicate<EntityExtractionResult> preImportPredicate) {
        this.preImportPredicate = preImportPredicate;
        return this;
    }
}