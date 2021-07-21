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

package io.jmix.dataimport.impl;

import io.jmix.core.DataManager;
import io.jmix.dataimport.DataImportExecutor;
import io.jmix.dataimport.DataImporter;
import io.jmix.dataimport.exception.ImportException;
import io.jmix.dataimport.extractor.data.ImportedDataExtractor;
import io.jmix.dataimport.extractor.data.ImportedDataExtractors;
import io.jmix.dataimport.extractor.data.ImportedData;
import io.jmix.dataimport.configuration.ImportConfiguration;
import io.jmix.dataimport.result.ImportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component("datimp_DataImporter")
public class DataImporterImpl implements DataImporter {
    protected static final Logger log = LoggerFactory.getLogger(DataImporter.class);
    @Autowired
    protected ImportedDataExtractors importedDataExtractors;
    @Autowired
    protected DataManager dataManager;
    @Autowired
    protected ObjectProvider<DataImportExecutor> dataImportExecutors;

    @Override
    public ImportResult importData(ImportConfiguration configuration, byte[]  content) {
        try {
            ImportedDataExtractor dataExtractor = importedDataExtractors.getExtractor(configuration.getInputDataFormat());
            ImportedData importedData = dataExtractor.extract(content, configuration);
            return importData(configuration, importedData);
        } catch (ImportException e) {
            log.error("Import failed: ", e);
            return new ImportResult()
                    .setSuccess(false)
                    .setErrorMessage(e.getErrorMessage());
        }
    }


    @Override
    public ImportResult importData(ImportConfiguration configuration, InputStream inputStream) {
        try {
            ImportedDataExtractor dataExtractor = importedDataExtractors.getExtractor(configuration.getInputDataFormat());
            ImportedData importedData = dataExtractor.extract(inputStream, configuration);
            return importData(configuration, importedData);
        } catch (ImportException e) {
            log.error("Import failed: ", e);
            return new ImportResult()
                    .setSuccess(false)
                    .setErrorMessage(e.getErrorMessage());
        }
    }

    @Override
    public ImportResult importData(ImportConfiguration configuration, ImportedData importedData) {
        DataImportExecutor dataImportExecutor = dataImportExecutors.getObject(configuration, importedData);
        return dataImportExecutor.importData();
    }

}
