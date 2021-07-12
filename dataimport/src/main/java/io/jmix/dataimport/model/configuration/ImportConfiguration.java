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

package io.jmix.dataimport.model.configuration;

import io.jmix.dataimport.extractor.entity.EntityExtractionResult;
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping;
import org.apache.commons.compress.utils.CharsetNames;

import java.util.List;
import java.util.function.Predicate;

public class ImportConfiguration {
    private String name;
    private String code;
    private List<PropertyMapping> propertyMappings;

    private String transactionStrategy;

    private String entityMetaClass;

    private String inputDataFormat;

    private String dateFormat;
    private String booleanTrueValue;
    private String booleanFalseValue;

    private String fileCharset = CharsetNames.UTF_8;

    private List<UniqueEntityConfiguration> uniqueEntityConfigurations;

    private Predicate<EntityExtractionResult> preImportPredicate;

    public ImportConfiguration(String entityMetaClass, String code) {
        this.entityMetaClass = entityMetaClass;
        this.code = code;
    }

    public List<PropertyMapping> getPropertyMappings() {
        return propertyMappings;
    }

    public ImportConfiguration setPropertyMappings(List<PropertyMapping> propertyMappings) {
        this.propertyMappings = propertyMappings;
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
        return transactionStrategy == null ? null : ImportTransactionStrategy.fromId(transactionStrategy);
    }

    public ImportConfiguration setTransactionStrategy(ImportTransactionStrategy transactionStrategy) {
        this.transactionStrategy = transactionStrategy == null ? null : transactionStrategy.getId();
        return this;
    }

    public String getEntityMetaClass() {
        return entityMetaClass;
    }

    public ImportConfiguration setEntityMetaClass(String entityMetaClass) {
        this.entityMetaClass = entityMetaClass;
        return this;
    }

    public String getInputDataFormat() {
        return inputDataFormat;
    }

    public ImportConfiguration setInputDataFormat(String inputDataFormat) {
        this.inputDataFormat = inputDataFormat;
        return this;
    }

    public String getFileCharset() {
        return fileCharset;
    }

    public ImportConfiguration setFileCharset(String fileCharset) {
        this.fileCharset = fileCharset;
        return this;
    }

    public List<UniqueEntityConfiguration> getUniqueEntityConfigurations() {
        return uniqueEntityConfigurations;
    }

    public ImportConfiguration setUniqueEntityConfigurations(List<UniqueEntityConfiguration> uniqueEntityConfigurations) {
        this.uniqueEntityConfigurations = uniqueEntityConfigurations;
        return this;
    }

    public ImportConfiguration setTransactionStrategy(String transactionStrategy) {
        this.transactionStrategy = transactionStrategy;
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