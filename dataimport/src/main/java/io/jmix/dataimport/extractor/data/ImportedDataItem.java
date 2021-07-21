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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class ImportedDataItem implements RawValuesSource {
    protected Map<String, Object> rawValues = new HashMap<>();
    protected int itemIndex;

    public int getItemIndex() {
        return itemIndex;
    }

    public Map<String, Object> getRawValues() {
        return rawValues;
    }

    public ImportedDataItem setItemIndex(int itemIndex) {
        this.itemIndex = itemIndex;
        return this;
    }

    public void setRawValues(Map<String, Object> rawValues) {
        this.rawValues = rawValues;
    }

    public ImportedDataItem addRawValue(String dataFieldName, @Nullable Object value) {
        this.rawValues.put(dataFieldName, value);
        return this;
    }

    @Nullable
    public Object getRawValue(String dataFieldName) {
        return rawValues.get(dataFieldName);
    }

    @Override
    public String toString() {
        return String.format("Item index: %s, Data: %s", itemIndex,
                rawValues.entrySet().stream()
                        .map(rawValueEntry -> String.format("%s: %s", rawValueEntry.getKey(), rawValueEntry.getValue()))
                        .collect(Collectors.joining(", ")));
    }
}
