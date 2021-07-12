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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.dataimport.exception.ImportException;
import io.jmix.dataimport.extractor.data.*;
import io.jmix.dataimport.model.configuration.ImportConfiguration;
import io.jmix.dataimport.model.result.ImportErrorType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component("datimp_JsonDataExtractor")
public class JsonDataExtractor implements DataExtractor {
    @Override
    public ImportedData extract(String content) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(content);
            return getImportedData(rootNode);
        } catch (JsonProcessingException e) {
            throw new ImportException(e, ImportErrorType.GENERAL, "Error while parsing JSON: " + e.getMessage());
        }
    }

    protected ImportedData getImportedData(JsonNode rootNode) {
        ImportedData importedData = new ImportedData();
        if (rootNode.isArray()) {
            Iterator<JsonNode> iterator = rootNode.elements();
            int itemIndex = 1;
            List<String> fieldNames = new ArrayList<>();

            while (iterator.hasNext()) {
                JsonNode entityJsonNode = iterator.next();
                ImportedDataItem importedDataItem = createImportedDataItem(entityJsonNode, itemIndex);
                importedData.addItem(importedDataItem);
                itemIndex++;

                Iterator<String> fieldNamesIterator = entityJsonNode.fieldNames();
                while (fieldNamesIterator.hasNext()) {
                    String fieldName = fieldNamesIterator.next();
                    if (!fieldNames.contains(fieldName)) {
                        fieldNames.add(fieldName);
                    }
                }
            }
            importedData.setFieldNames(fieldNames);
        } else if (rootNode.isObject()) {
            Iterator<String> fieldNamesIterator = rootNode.fieldNames();
            while (fieldNamesIterator.hasNext()) {
                importedData.addFieldName(fieldNamesIterator.next());
            }
            ImportedDataItem importedDataItem = createImportedDataItem(rootNode, 1);
            importedData.addItem(importedDataItem);

        }
        return importedData;
    }

    protected ImportedDataItem createImportedDataItem(JsonNode jsonNode, int itemIndex) {
        ImportedDataItem item = new ImportedDataItem();
        item.setItemIndex(itemIndex);
        readRawValues(jsonNode, item);
        return item;
    }

    protected ImportedObject createImportedObject(JsonNode objectNode) {
        ImportedObject importedObject = new ImportedObject();
        readRawValues(objectNode, importedObject);
        return importedObject;
    }

    protected void readRawValues(JsonNode objectNode, ImportedObject importedObject) {
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            Object rawValue;
            JsonNode childNode = field.getValue();
            if (childNode.isNull()) {
                rawValue = null;
            } else {
                if (childNode.isObject()) {
                    rawValue = createImportedObject(childNode);
                } else if (childNode.isArray()) {
                    rawValue = createImportedObjectList(childNode);
                } else {
                    rawValue = childNode.asText();
                }
            }
            importedObject.addRawValue(field.getKey(), rawValue);
        }
    }

    protected ImportedObjectList createImportedObjectList(JsonNode rootNode) {
        ImportedObjectList listObject = new ImportedObjectList();
        Iterator<JsonNode> children = rootNode.elements();
        while (children.hasNext()) {
            JsonNode childNode = children.next();
            listObject.addImportedObject(createImportedObject(childNode));
        }
        return listObject;
    }

    @Override
    public ImportedData extract(InputStream inputStream, ImportConfiguration importConfiguration) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(inputStream);
            return getImportedData(rootNode);
        } catch (JsonProcessingException e) {
            throw new ImportException(e, ImportErrorType.GENERAL, "Error while parsing JSON: " + e.getMessage());
        } catch (IOException e) {
            throw new ImportException(e, ImportErrorType.GENERAL, "I/O error: " + e.getMessage());
        }
    }

    @Override
    public ImportedData extract(byte[] inputData, ImportConfiguration importConfiguration) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(inputData);
            return getImportedData(rootNode);
        } catch (JsonProcessingException e) {
            throw new ImportException(e, ImportErrorType.GENERAL, "Error while parsing JSON: " + e.getMessage());
        } catch (IOException e) {
            throw new ImportException(e, ImportErrorType.GENERAL, "I/O error: " + e.getMessage());
        }
    }
}
