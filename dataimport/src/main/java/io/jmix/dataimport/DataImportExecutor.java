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
import io.jmix.core.metamodel.model.Range;
import io.jmix.core.validation.EntityValidationException;
import io.jmix.dataimport.exception.ImportUniqueAbortException;
import io.jmix.dataimport.extractor.data.ImportedData;
import io.jmix.dataimport.extractor.entity.EntityExtractionResult;
import io.jmix.dataimport.extractor.entity.EntityExtractor;
import io.jmix.dataimport.configuration.DuplicateEntityPolicy;
import io.jmix.dataimport.configuration.ImportConfiguration;
import io.jmix.dataimport.configuration.ImportTransactionStrategy;
import io.jmix.dataimport.configuration.UniqueEntityConfiguration;
import io.jmix.dataimport.configuration.mapping.ReferenceImportPolicy;
import io.jmix.dataimport.configuration.mapping.ReferenceMultiFieldPropertyMapping;
import io.jmix.dataimport.configuration.mapping.ReferencePropertyMapping;
import io.jmix.dataimport.configuration.mapping.PropertyMapping;
import io.jmix.dataimport.result.EntityImportError;
import io.jmix.dataimport.result.EntityImportErrorType;
import io.jmix.dataimport.result.ImportResult;
import io.jmix.dataimport.property.populator.EntityInfo;
import io.jmix.dataimport.property.populator.EntityPropertiesPopulator;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
    protected EntityPropertiesPopulator entityPropertiesPopulator;
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
        validateImportConfiguration();

        importResult.setConfigurationCode(importConfiguration.getCode());
        if (importConfiguration.getTransactionStrategy() == ImportTransactionStrategy.SINGLE_TRANSACTION) {
            importInOneTransaction();
        } else {
            importInMultipleTransactions();
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
                resetImportResult(e, "Entities extraction failed: " + e.getMessage());
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
                    e.getCreatedEntity(), e.getImportedDataItem(),
                    e.getExistingEntity()));
        } catch (Exception e) {
            resetImportResult(e, String.format("Error while importing the data: %s", e.getMessage()));
        }
    }

    public void importInMultipleTransactions() {
        try {
            importResult.setSuccess(true);
            importedData.getItems().forEach(dataItem -> {
                Object extractedEntity = null;
                try {
                    extractedEntity = entityExtractor.extract(importConfiguration, dataItem);
                } catch (Exception e) {
                    log.error(String.format("Entity extraction failed for data item: %s", dataItem.toString()), e);
                    importResult.setSuccess(false);
                    importResult.addFailedEntity(new EntityImportError()
                            .setImportedDataItem(dataItem)
                            .setErrorType(EntityImportErrorType.DATA_BINDING)
                            .setErrorMessage("Error during entity extraction: " + e.getMessage()));
                }
                if (extractedEntity != null) {
                    EntityExtractionResult extractionResult = new EntityExtractionResult(extractedEntity, dataItem);
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
            importResult.setErrorMessage(String.format("Unique violation occurred with Unique Policy ABORT for entity: %s with data row: %s. Found entity: %s",
                    e.getCreatedEntity(), e.getImportedDataItem(),
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
                        EntityInfo entityInfo = entityPropertiesPopulator.populateProperties(existingEntity, importConfiguration, extractionResult.getImportedDataItem());
                        existingEntity = entityInfo.getEntity();
                        extractionResult.setEntity(existingEntity);
                        return true;
                    } else if (configuration.getPolicy() == DuplicateEntityPolicy.ABORT) {
                        throw new ImportUniqueAbortException(existingEntity, extractionResult);
                    } else {
                        importResult.addFailedEntity(createEntityImportErrorResult(extractionResult,
                                "Entity not imported since it is already existing and Unique policy is set to SKIP",
                                EntityImportErrorType.UNIQUE_VIOLATION));
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
                    "Entity not imported due to pre-commit predicate", EntityImportErrorType.VALIDATION));
        }
        return needToImport;
    }

    protected boolean executePreCommitPredicateIfNecessary(EntityExtractionResult result) {
        if (importConfiguration.getPreImportPredicate() != null) {
            try {
                return importConfiguration.getPreImportPredicate().test(result);
            } catch (Exception e) {
                log.error("Pre-commit predicate execution failed with: ", e);
                importResult.addFailedEntity(createEntityImportErrorResult(result, String.format("Pre-commit predicate execution failed with: %s", e.getMessage()), EntityImportErrorType.PRE_COMMIT_PREDICATE));
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
            resetImportResult(e, e.getMessage() + "\nTransaction abort - no entity is stored in the database.");
        } catch (PersistenceException e) {
            resetImportResult(e, "Error while executing import: " + e.getMessage() + "\nTransaction abort - no entity is stored in the database.");
        }
    }

    protected EntityImportError createEntityImportErrorResult(EntityExtractionResult result, String errorMessage, EntityImportErrorType entityImportErrorType) {
        return new EntityImportError(result.getEntity())
                .setImportedDataItem(result.getImportedDataItem())
                .setErrorMessage(errorMessage)
                .setErrorType(entityImportErrorType);
    }

    protected void resetImportResult(Exception e, String errorMessage) {
        log.error(errorMessage, e);
        importResult.setSuccess(false)
                .setNumOfProcessedEntities(0)
                .setErrorMessage(errorMessage);
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
            Object importedEntityId = EntityValues.getId(importedEntities.iterator().next());
            if (!importResult.getImportedEntityIds().contains(importedEntityId)) {
                importResult.addImportedEntityId(importedEntityId);
            }
        } catch (EntityValidationException e) {
            log.error(String.format("Import failed for entity: %s, data item: %s",
                    entityImportExport.exportEntitiesToJSON(Collections.singletonList(entityExtractionResult.getEntity())),
                    entityExtractionResult.getImportedDataItem()), e);
            importResult.setSuccess(false);
            importResult.addFailedEntity(createEntityImportErrorResult(entityExtractionResult, e.toString(), EntityImportErrorType.VALIDATION));
        } catch (PersistenceException e) {
            log.error(String.format("Import failed for entity: %s, data item: %s",
                    entityImportExport.exportEntitiesToJSON(Collections.singletonList(entityExtractionResult.getEntity())),
                    entityExtractionResult.getImportedDataItem()), e);
            importResult.setSuccess(false);
            importResult.addFailedEntity(createEntityImportErrorResult(entityExtractionResult,
                    String.format("Error while importing entity: %s", e.getMessage()),
                    EntityImportErrorType.PERSISTENCE));
        }
    }

    protected Collection<Object> tryToImportEntities(List<Object> entitiesToImport) {
        List<Object> resultList = new ArrayList<>();
        entitiesToImport.forEach(entityToImport -> {
            EntityImportPlan entityImportPlan = createEntityImportPlan(entityToImport);
            Collection<Object> importedEntities = entityImportExport.importEntities(Collections.singletonList(entityToImport), entityImportPlan, true);
            Collection<Object> filteredImportedEntities = importedEntities.stream()
                    .filter(importedEntity -> (importedEntity.getClass().isAssignableFrom(importConfiguration.getEntityClass())))
                    .collect(Collectors.toList());
            resultList.add(filteredImportedEntities.iterator().next());
        });
        return resultList;
    }

    protected EntityImportPlan createEntityImportPlan(Object entityToImport) {
        MetaClass entityMetaClass = metadata.getClass(importConfiguration.getEntityClass());
        EntityImportPlanBuilder entityImportPlanBuilder = createEntityImportPlanBuilder(entityMetaClass, importConfiguration.getPropertyMappings(), entityToImport);
        return entityImportPlanBuilder.build();
    }

    protected EntityImportPlanBuilder createEntityImportPlanBuilder(MetaClass ownerEntityMetaClass,
                                                                    List<PropertyMapping> propertyMappings,
                                                                    Object ownerEntity) {
        EntityImportPlanBuilder builder = entityImportPlans.builder(ownerEntityMetaClass.getJavaClass());
        propertyMappings.forEach(propertyMapping -> {
            String propertyName = propertyMapping.getEntityPropertyName();
            MetaProperty property = ownerEntityMetaClass.getProperty(propertyName);
            Object propertyValue = EntityValues.getValue(ownerEntity, propertyName);
            if (!property.getRange().isClass() || propertyValue == null) {
                builder.addProperties(propertyName);
            } else {
                EntityImportPlanBuilder propertyImportPlanBuilder = null;
                if (propertyValue instanceof Collection) {
                    propertyImportPlanBuilder = createEntityImportPlanForCollection(property, (ReferenceMultiFieldPropertyMapping) propertyMapping);
                } else if (entityStates.isNew(propertyValue)) {
                    if (propertyMapping instanceof ReferenceMultiFieldPropertyMapping) {
                        propertyImportPlanBuilder = createEntityImportPlanBuilder(property.getRange().asClass(),
                                ((ReferenceMultiFieldPropertyMapping) propertyMapping).getReferencePropertyMappings(), propertyValue);
                    } else if (propertyMapping instanceof ReferencePropertyMapping) {
                        propertyImportPlanBuilder = entityImportPlans.builder(property.getRange().asClass().getJavaClass())
                                .addProperties(((ReferencePropertyMapping) propertyMapping).getLookupPropertyName());
                    }
                } else {
                    builder.addProperties(propertyName);
                }
                if (propertyImportPlanBuilder != null) {
                    addReferencePropertyToImportPlan(builder, propertyName, property, propertyImportPlanBuilder.build());
                }
            }
        });
        return builder;
    }

    protected EntityImportPlanBuilder createEntityImportPlanForCollection(MetaProperty property, ReferenceMultiFieldPropertyMapping referenceMapping) {
        EntityImportPlanBuilder collectionImportPlan = entityImportPlans.builder(property.getRange().asClass().getJavaClass());
        referenceMapping.getReferencePropertyMappings().forEach(propertyMapping -> collectionImportPlan.addProperties(propertyMapping.getEntityPropertyName()));
        return collectionImportPlan;
    }

    protected void addReferencePropertyToImportPlan(EntityImportPlanBuilder ownerBuilder, String propertyName, MetaProperty property, EntityImportPlan propertyImportPlan) {
        Range.Cardinality cardinality = property.getRange().getCardinality();
        switch (cardinality) {
            case ONE_TO_ONE:
                ownerBuilder.addOneToOneProperty(propertyName, propertyImportPlan);
                break;
            case MANY_TO_ONE:
                ownerBuilder.addManyToOneProperty(propertyName, propertyImportPlan);
                break;
            case ONE_TO_MANY:
                ownerBuilder.addOneToManyProperty(propertyName, propertyImportPlan, CollectionImportPolicy.KEEP_ABSENT_ITEMS);
                break;
            default:
                break;
        }
    }

    protected void validateImportConfiguration() {
        if (importConfiguration == null) {
            throw new IllegalStateException("Import configuration is not set to execute data import");
        }

        validatePropertyMappings(importConfiguration.getPropertyMappings());
    }

    protected void validatePropertyMappings(List<PropertyMapping> propertyMappings) {
        propertyMappings.forEach(propertyMapping -> {
            if (propertyMapping instanceof ReferenceMultiFieldPropertyMapping) {
                ReferenceMultiFieldPropertyMapping multiFieldPropertyMapping = (ReferenceMultiFieldPropertyMapping) propertyMapping;
                ReferenceImportPolicy referenceImportPolicy = multiFieldPropertyMapping.getReferenceImportPolicy();
                if (referenceImportPolicy == null) {
                    throw new IllegalStateException(String.format("Reference import policy is not set for property [%s]", multiFieldPropertyMapping.getEntityPropertyName()));
                }
                if (referenceImportPolicy != ReferenceImportPolicy.CREATE && CollectionUtils.isEmpty(multiFieldPropertyMapping.getLookupPropertyNames())) {
                    throw new IllegalStateException(String.format("Lookup properties are not set for property [%s]", multiFieldPropertyMapping.getEntityPropertyName()));
                }

                validatePropertyMappings(multiFieldPropertyMapping.getReferencePropertyMappings());
            }
        });
    }
}
