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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class ImportedData {
    protected List<ImportedDataItem> items = new ArrayList<>();
    protected List<String> fieldNames = new ArrayList<>(); //name to map a field from file to entity property

    public List<ImportedDataItem> getItems() {
        return items;
    }

    public void setItems(List<ImportedDataItem> items) {
        this.items = items;
    }

    public List<String> getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(List<String> fieldNames) {
        this.fieldNames = fieldNames;
    }


    public void addItem(ImportedDataItem item) {
        this.items.add(item);
    }

    public void addFieldName(String fieldName) {
        this.fieldNames.add(fieldName);
    }

    @Nullable
    public ImportedDataItem getDataItem(int itemIndex) {
        return Objects.requireNonNull(items.stream()
                .filter(dataItem -> dataItem.getItemIndex() == itemIndex)
                .findFirst()
                .orElse(null));
    }
}
