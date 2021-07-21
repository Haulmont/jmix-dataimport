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

package io.jmix.dataimport.result;

import java.util.ArrayList;
import java.util.List;

public class ImportResult {
    protected boolean success = true;
    protected String configurationCode;

    protected int numOfProcessedEntities;

    protected List<Object> importedEntityIds = new ArrayList<>();
    protected List<EntityImportError> failedEntities = new ArrayList<>();

    protected String errorMessage;

    public boolean isSuccess() {
        return success;
    }

    public ImportResult setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public List<Object> getImportedEntityIds() {
        return importedEntityIds;
    }

    public ImportResult setImportedEntityIds(List<Object> importedEntityIds) {
        this.importedEntityIds = importedEntityIds;
        return this;
    }

    public List<EntityImportError> getFailedEntities() {
        return failedEntities;
    }

    public ImportResult setFailedEntities(List<EntityImportError> failedEntities) {
        this.failedEntities = failedEntities;
        return this;
    }

    public int getNumOfProcessedEntities() {
        return numOfProcessedEntities;
    }

    public ImportResult setNumOfProcessedEntities(int numOfProcessedEntities) {
        this.numOfProcessedEntities = numOfProcessedEntities;
        return this;
    }

    public ImportResult addImportedEntityId(Object id) {
        this.importedEntityIds.add(id);
        return this;
    }

    public ImportResult addFailedEntity(EntityImportError result) {
        this.failedEntities.add(result);
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public ImportResult setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public String getConfigurationCode() {
        return configurationCode;
    }

    public void setConfigurationCode(String configurationCode) {
        this.configurationCode = configurationCode;
    }
}
