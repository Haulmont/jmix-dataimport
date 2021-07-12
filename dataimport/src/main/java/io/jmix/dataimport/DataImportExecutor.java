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

import io.jmix.core.*;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.validation.EntityValidationException;
import io.jmix.dataimport.exception.ImportUniqueAbortException;
import io.jmix.dataimport.extractor.data.ImportedData;
import io.jmix.dataimport.extractor.entity.EntityExtractionResult;
import io.jmix.dataimport.extractor.entity.EntityExtractor;
import io.jmix.dataimport.model.configuration.DuplicateEntityPolicy;
import io.jmix.dataimport.model.configuration.ImportConfiguration;
import io.jmix.dataimport.model.configuration.ImportTransactionStrategy;
import io.jmix.dataimport.model.configuration.UniqueEntityConfiguration;
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping;
import io.jmix.dataimport.model.result.EntityImportError;
import io.jmix.dataimport.model.result.ImportErrorType;
import io.jmix.dataimport.model.result.ImportResult;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceException;
import java.util.*;
import java.util.stream.Collectors;

@Component("datimp_DataImportExecutor")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DataImportExecutor {
    protected static final Logger log = LoggerFactory.getLogger(DataImportExecutor.class);

    @Autowired
    protected Metadata metadata;
    @Autowired
    protected MetadataTools metadataTools;
    @Autowired
    protected EntityImportPlans entityImportPlans;
    @Autowired
    protected EntityStates entityStates;
    @Autowired
    protected EntityImportExport entityImportExport;
    @Autowired
    protected DuplicateEntityManager duplicateEntityManager;
    @Autowired
    protected FetchPlans fetchPlans;
    @Autowired
    protected EntityPopulator entityPopulator;
    @Autowired
    protected EntityExtractor entityExtractor;

    protected ImportResult importResult = new ImportResult();

    protected ImportConfiguration importConfiguration;
    protected ImportedData importedData;

    public DataImportExecutor(ImportConfiguration importConfiguration, ImportedData importedData) {
        this.importConfiguration = importConfiguration;
        this.importedData = importedData;
    }

    public ImportResult importData() {
        if (importConfiguration == null) {
            throw new IllegalStateException("Import configuration is not set to execute data import");
        }

        importResult.setConfigurationCode(importConfiguration.getCode());
        try {
            if (importConfiguration.getTransactionStrategy() == ImportTransactionStrategy.SINGLE_TRANSACTION) {
                importInOneTransaction();
            } else {
                importInMultipleTransactions();
            }
        } catch (Exception e) {
            resetImportResult(e, String.format("Error while importing the data: %s", e.getMessage()), ImportErrorType.GENERAL);
        }

        return importResult;
    }

    public void importInOneTransaction() {
        try {
            importResult.setSuccess(true);
            List<EntityExtractionResult> extractionResults = null;
            try {
                extractionResults = entityExtractor.extract(importConfiguration, importedData);
            } catch (Exception e) {
                log.error("Entities extraction failed: ", e);
                resetImportResult(e, "Entities extraction failed: " + e.getMessage(), ImportErrorType.DATA_BINDING);
            }

            if (extractionResults != null) {
                importResult.setNumOfProcessedEntities(extractionResults.size());
                List<Object> entitiesToImport = extractionResults.stream()
                        .filter(this::checkExtractedEntity)
                        .map(EntityExtractionResult::getEntity)
                        .collect(Collectors.toList());
                importEntities(entitiesToImport);
            }
        } catch (ImportUniqueAbortException e) {
            resetImportResult(e, String.format("Unique violation occurred with Unique Policy ABORT for entity: '%s' with data item: '%s'. Found entity: '%s'",
                    e.getEntityExtractionResult().getEntity(), e.getEntityExtractionResult().getImportedDataItem(),
                    e.getExistingEntity()), ImportErrorType.UNIQUE_VIOLATION);
        }
    }

    public void importInMultipleTransactions() {
        try {
            importResult.setSuccess(true);
            importedData.getItems().forEach(dataItem -> {
                EntityExtractionResult extractionResult = null;
                try {
                    extractionResult = entityExtractor.extract(importConfiguration, dataItem);
                } catch (Exception e) {
                    log.error(String.format("Entity extraction failed for data item: %s", dataItem.toString()), e);
                    importResult.setSuccess(false);
                    importResult.addFailedEntity(new EntityImportError()
                            .setImportedDataItem(dataItem)
                            .setErrorType(ImportErrorType.DATA_BINDING)
                            .setErrorMessage("Error during entity extraction: " + e.getMessage()));
                }
                if (extractionResult != null) {
                    boolean needToImport = checkExtractedEntity(extractionResult);
                    if (needToImport) {
                        importEntity(extractionResult);
                    }
                }
            });
            importResult.setNumOfProcessedEntities(importedData.getItems().size());
        } catch (ImportUniqueAbortException e) {
            log.error("Import failed: ", e);
            importResult.setSuccess(false);
            importResult.setErrorType(ImportErrorType.UNIQUE_VIOLATION)
                    .setErrorMessage(String.format("Unique violation occurred with Unique Policy ABORT for entity: %s with data row: %s. Found entity: %s",
                            e.getEntityExtractionResult().getEntity(), e.getEntityExtractionResult().getImportedDataItem(),
                            e.getExistingEntity()));
        }
    }

    protected boolean checkExtractedEntity(EntityExtractionResult entityExtractionResult) {
        boolean needToImport = checkEntityDuplicates(entityExtractionResult);
        if (needToImport) {
            return checkPreImportPredicate(entityExtractionResult);
        }
        return false;
    }

    protected boolean checkEntityDuplicates(EntityExtractionResult extractionResult) {
        if (CollectionUtils.isNotEmpty(importConfiguration.getUniqueEntityConfigurations())) {
            FetchPlan fetchPlan = getFetchPlanBuilder(createEntityImportPlan(extractionResult.getEntity())).build();
            for (UniqueEntityConfiguration configuration : importConfiguration.getUniqueEntityConfigurations()) {
                Object existingEntity = duplicateEntityManager.load(extractionResult.getEntity(), configuration, fetchPlan);
                if (existingEntity != null) {
                    if (configuration.getPolicy() == DuplicateEntityPolicy.UPDATE) {
                        EntityInfo entityInfo = entityPopulator.populateProperties(existingEntity, importConfiguration, extractionResult.getImportedDataItem());
                        existingEntity = entityInfo.getEntity();
                        extractionResult.setEntity(existingEntity);
                        return true;
                    } else if (configuration.getPolicy() == DuplicateEntityPolicy.ABORT) {
                        throw new ImportUniqueAbortException(existingEntity, extractionResult);
                    } else {
                        importResult.addFailedEntity(createEntityImportErrorResult(extractionResult,
                                "Entity not imported since it is already existing and Unique policy is set to SKIP",
                                ImportErrorType.UNIQUE_VIOLATION));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    protected boolean checkPreImportPredicate(EntityExtractionResult entityExtractionResult) {
        boolean needToImport = executePreCommitPredicateIfNecessary(entityExtractionResult);
        if (!needToImport) {
            importResult.addFailedEntity(createEntityImportErrorResult(entityExtractionResult,
                    "Entity not imported due to pre-commit predicate", ImportErrorType.VALIDATION));
        }
        return needToImport;
    }

    protected boolean executePreCommitPredicateIfNecessary(EntityExtractionResult result) {
        if (importConfiguration.getPreImportPredicate() != null) {
            try {
                return importConfiguration.getPreImportPredicate().test(result);
            } catch (Exception e) {
                log.error("Pre-commit predicate execution failed with: ", e);
                importResult.addFailedEntity(createEntityImportErrorResult(result, String.format("Pre-commit predicate execution failed with: %s", e.getMessage()), ImportErrorType.SCRIPTING));
                return false;
            }
        }
        return true;
    }

    protected void importEntities(List<Object> entitiesToImport) {
        try {
            Collection<Object> importedEntities = tryToImportEntities(entitiesToImport);
            importResult.setImportedEntityIds(importedEntities.stream().map(EntityValues::getId).collect(Collectors.toList()));
        } catch (EntityValidationException e) {
            resetImportResult(e, e.getMessage() + "\nTransaction abort - no entity is stored in the database.", ImportErrorType.VALIDATION);
        } catch (PersistenceException e) {
            log.error("Import failed: ", e);
            resetImportResult(e, "Error while executing import: " + e.getMessage() + "\nTransaction abort - no entity is stored in the database.",
                    ImportErrorType.PERSISTENCE);
        }
    }

    protected EntityImportError createEntityImportErrorResult(EntityExtractionResult result, String errorMessage, ImportErrorType importErrorType) {
        return new EntityImportError(result.getEntity())
                .setImportedDataItem(result.getImportedDataItem())
                .setErrorMessage(errorMessage)
                .setErrorType(importErrorType);

    }

    protected void resetImportResult(Exception e, String errorMessage, ImportErrorType errorType) {
        log.error("Import failed: ", e);
        importResult.setSuccess(false)
                .setNumOfProcessedEntities(0)
                .setErrorMessage(errorMessage)
                .setErrorType(errorType);
    }

    protected FetchPlanBuilder getFetchPlanBuilder(EntityImportPlan plan) {
        FetchPlanBuilder builder = fetchPlans.builder(plan.getEntityClass());
        plan.getProperties().forEach(entityImportPlanProperty -> {
            EntityImportPlan propertyPlan = entityImportPlanProperty.getPlan();
            if (propertyPlan != null) {
                builder.add(entityImportPlanProperty.getName(), getFetchPlanBuilder(propertyPlan));
            } else {
                builder.add(entityImportPlanProperty.getName());
            }
        });
        return builder;
    }

    protected void importEntity(EntityExtractionResult entityExtractionResult) {
        try {
            Collection<Object> importedEntities = tryToImportEntities(Collections.singletonList(entityExtractionResult.getEntity()));
            importResult.addImportedEntityId(EntityValues.getId(importedEntities.iterator().next()));
        } catch (EntityValidationException e) {
            log.error("Import failed: ", e);
            importResult.setSuccess(false);
            importResult.addFailedEntity(createEntityImportErrorResult(entityExtractionResult, e.toString(), ImportErrorType.VALIDATION));
        } catch (PersistenceException e) {
            log.error("Import failed: ", e);
            importResult.setSuccess(false);
            importResult.addFailedEntity(createEntityImportErrorResult(entityExtractionResult,
                    String.format("Error while importing entity: %s", e.getMessage()),
                    ImportErrorType.PERSISTENCE));
        }
    }

    protected Collection<Object> tryToImportEntities(List<Object> entitiesToImport) {
        List<Object> resultList = new ArrayList<>();
        entitiesToImport.forEach(entityToImport -> {
            EntityImportPlan entityImportPlan = createEntityImportPlan(entityToImport);
            Collection<Object> importedEntities = entityImportExport.importEntities(Collections.singletonList(entityToImport), entityImportPlan, true);
            Collection filteredImportedEntities = importedEntities.stream()
                    .filter(importedEntity -> metadata.getClass(importedEntity).getName().equals(importConfiguration.getEntityMetaClass()))
                    .collect(Collectors.toList());
            resultList.add(filteredImportedEntities.iterator().next());
        });
        return resultList;
    }

    protected EntityImportPlan createEntityImportPlan(Object entityToImport) {
        MetaClass entityMetaClass = metadata.getClass(importConfiguration.getEntityMetaClass());
        EntityImportPlanBuilder entityImportPlanBuilder = createEntityImportPlanBuilder(entityMetaClass, importConfiguration.getPropertyMappings(), entityToImport);
        return entityImportPlanBuilder.build();
    }

    protected EntityImportPlanBuilder createEntityImportPlanBuilder(MetaClass ownerEntityMetaClass,
                                                                    List<PropertyMapping> propertyMappings,
                                                                    @Nullable Object ownerEntity) {
        EntityImportPlanBuilder builder = entityImportPlans.builder(ownerEntityMetaClass.getJavaClass());
        propertyMappings.forEach(propertyMapping -> {
            String propertyName = propertyMapping.getPropertyName();
            if (propertyMapping.isAssociation()) {
                MetaProperty property = ownerEntityMetaClass.getProperty(propertyName);
                EntityImportPlanBuilder propertyImportPlanBuilder = null;
                if (ownerEntity != null) {
                    Object propertyValue = EntityValues.getValue(ownerEntity, propertyName);
                    if (propertyValue == null || propertyValue instanceof Collection) {
                        propertyImportPlanBuilder = createEntityImportPlanBuilder(property.getRange().asClass(), propertyMapping.getPropertyMappings(), null);
                    } else if (entityStates.isNew(propertyValue)) {
                        propertyImportPlanBuilder = createEntityImportPlanBuilder(property.getRange().asClass(), propertyMapping.getPropertyMappings(), propertyValue);
                    }
                } else {
                    propertyImportPlanBuilder = createEntityImportPlanBuilder(property.getRange().asClass(), propertyMapping.getPropertyMappings(), null);
                }
                if (propertyImportPlanBuilder != null) {
                    addAssociationPropertyToImportPlan(builder, propertyName, property, propertyImportPlanBuilder.build());
                }
            } else {
                builder.addProperties(propertyName);
            }
        });
        return builder;
    }

    protected void addAssociationPropertyToImportPlan(EntityImportPlanBuilder builder, String propertyName, MetaProperty property, EntityImportPlan propertyImportPlan) {
        OneToOne oneToOneAnnotation = property.getAnnotatedElement().getAnnotation(OneToOne.class);
        ManyToOne manyToOneAnnotation = property.getAnnotatedElement().getAnnotation(ManyToOne.class);
        OneToMany oneToManyAnnotation = property.getAnnotatedElement().getAnnotation(OneToMany.class);
        if (oneToOneAnnotation != null) {
            builder.addOneToOneProperty(propertyName, propertyImportPlan);
        }
        if (manyToOneAnnotation != null) {
            builder.addManyToOneProperty(propertyName, propertyImportPlan);
        }
        if (oneToManyAnnotation != null) {
            builder.addOneToManyProperty(propertyName, propertyImportPlan, CollectionImportPolicy.KEEP_ABSENT_ITEMS);
        }
    }
}
