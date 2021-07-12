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

package io.jmix.dataimport.extractor.data.impl;

import com.opencsv.CSVReader;
import io.jmix.dataimport.exception.ImportException;
import io.jmix.dataimport.extractor.data.DataExtractor;
import io.jmix.dataimport.extractor.data.ImportedData;
import io.jmix.dataimport.extractor.data.ImportedDataItem;
import io.jmix.dataimport.model.configuration.ImportConfiguration;
import io.jmix.dataimport.model.result.ImportErrorType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Arrays;
import java.util.List;

@Component("datimp_CsvDataExtractor")
public class CsvDataExtractor implements DataExtractor {
    @Override
    public ImportedData extract(String content) {
        CSVReader csvReader = new CSVReader(new StringReader(content));
        return getImportedData(csvReader);
    }

    @Override
    public ImportedData extract(InputStream inputStream, ImportConfiguration importConfiguration) {
        CSVReader csvReader;
        try {
            csvReader = new CSVReader(new InputStreamReader(inputStream, importConfiguration.getFileCharset()));
        } catch (UnsupportedEncodingException e) {
            throw new ImportException(e, ImportErrorType.GENERAL, "Unable to read lines from CSV: " + e.getMessage());
        }
        return getImportedData(csvReader);
    }

    @Override
    public ImportedData extract(byte[] inputData, ImportConfiguration importConfiguration) {
        CSVReader csvReader;
        try {
            csvReader = new CSVReader(new InputStreamReader(new ByteArrayInputStream(inputData), importConfiguration.getFileCharset()));
        } catch (UnsupportedEncodingException e) {
            throw new ImportException(e, ImportErrorType.GENERAL, "Unable to read lines from CSV: " + e.getMessage());
        }
        return getImportedData(csvReader);
    }

    protected ImportedData getImportedData(CSVReader csvReader) {
        List<String[]> strings;
        try {
            strings = csvReader.readAll();
        } catch (IOException e) {
            throw new ImportException(e, ImportErrorType.GENERAL, "Unable to read lines from CSV: " + e.getMessage());
        }
        ImportedData importedData = new ImportedData();
        if (CollectionUtils.isNotEmpty(strings)) {
            List<String> columnNames = Arrays.asList(strings.get(0));
            importedData.setFieldNames(columnNames);
            for (int i = 1; i < strings.size(); i++) {
                ImportedDataItem importedDataItem = new ImportedDataItem();
                importedDataItem.setItemIndex(i);
                String[] values = strings.get(i);
                for (int j = 0; j < values.length; j++) {
                    importedDataItem.addRawValue(columnNames.get(j), values[j]);
                }
                importedData.addItem(importedDataItem);
            }

        }
        return importedData;
    }
}
