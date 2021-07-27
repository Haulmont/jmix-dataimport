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

package io.jmix.dataimport.extractor.data;

import io.jmix.dataimport.InputDataFormat;
import io.jmix.dataimport.extractor.data.impl.CsvDataExtractor;
import io.jmix.dataimport.extractor.data.impl.ExcelDataExtractor;
import io.jmix.dataimport.extractor.data.impl.JsonDataExtractor;
import io.jmix.dataimport.extractor.data.impl.XmlDataExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component("datimp_ImportedDataExtractors")
public class ImportedDataExtractors {
    @Autowired
    protected ApplicationContext applicationContext;

    protected Map<String, ImportedDataExtractor> extractorsByInputFormats = new HashMap<>();

    @PostConstruct
    protected void init() {
        extractorsByInputFormats.put(InputDataFormat.CSV, applicationContext.getBean(CsvDataExtractor.class));
        extractorsByInputFormats.put(InputDataFormat.XLSX, applicationContext.getBean(ExcelDataExtractor.class));
        extractorsByInputFormats.put(InputDataFormat.JSON, applicationContext.getBean(JsonDataExtractor.class));
        extractorsByInputFormats.put(InputDataFormat.XML, applicationContext.getBean(XmlDataExtractor.class));
        addCustomExtractors();
    }

    public ImportedDataExtractor getExtractor(String inputDataFormat) {
        ImportedDataExtractor dataExtractor = extractorsByInputFormats.get(inputDataFormat);
        if (dataExtractor == null) {
            throw new IllegalArgumentException(String.format("Input data format [%s] is not supported for import", inputDataFormat));
        }
        return dataExtractor;
    }

    protected void addCustomExtractors() {

    }
}
