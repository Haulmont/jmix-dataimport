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

import io.jmix.dataimport.extractor.data.ImportedData;
import io.jmix.dataimport.configuration.ImportConfiguration;
import io.jmix.dataimport.result.ImportResult;

import java.io.InputStream;

/**
 * API to import entities from the input data (XLSX, CSV, JSON, XML) using specified import configuration.
 *
 * @see ImportConfiguration
 */
public interface DataImporter {
    ImportResult importData(ImportConfiguration configuration, byte[] content);

    ImportResult importData(ImportConfiguration configuration, InputStream inputStream);

    ImportResult importData(ImportConfiguration configuration, ImportedData importedData);
}
