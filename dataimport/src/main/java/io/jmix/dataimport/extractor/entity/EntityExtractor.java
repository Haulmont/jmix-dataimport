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

package io.jmix.dataimport.extractor.entity;

import io.jmix.dataimport.extractor.data.ImportedData;
import io.jmix.dataimport.extractor.data.ImportedDataItem;
import io.jmix.dataimport.model.configuration.ImportConfiguration;

import java.util.List;

/**
 * Provides methods to extract entities and populate their properties by values from the imported data
 */
public interface EntityExtractor {
    /**
     * Creates an entity using {@link ImportConfiguration#entityMetaClass} and populates the properties for which mappings are set in import configuration.
     * Values for the properties are got by processing the raw values from {@link ImportedDataItem}.
     *
     * @param importConfiguration import configuration
     * @param dataItem source of raw values for the entity
     * @return result with created entity filled by property values and source data item
     */
    EntityExtractionResult extract(ImportConfiguration importConfiguration, ImportedDataItem dataItem);

    /**
     * Each {@link ImportedDataItem} in specified imported data is processed. For each data item it is checked whether
     * the entity with the same values of simple properties are extracted. If such entity exists, it will be populated by values from current imported data item.
     *
     * @param importConfiguration import configuration
     * @param importedData imported data
     * @return extraction results for each unique extracted entity
     */
    List<EntityExtractionResult> extract(ImportConfiguration importConfiguration, ImportedData importedData);
}
