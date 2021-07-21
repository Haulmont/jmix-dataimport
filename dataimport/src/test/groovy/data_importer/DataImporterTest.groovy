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

package data_importer


import io.jmix.core.Resources
import io.jmix.dataimport.DataImporter
import io.jmix.dataimport.configuration.ImportConfigurationBuilder
import io.jmix.dataimport.configuration.ImportTransactionStrategy
import io.jmix.dataimport.configuration.mapping.ReferenceMultiFieldPropertyMapping
import io.jmix.dataimport.configuration.mapping.ReferenceImportPolicy
import io.jmix.dataimport.result.EntityImportErrorType
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.OrderLine
import test_support.entity.Product

class DataImporterTest extends DataImportSpec {
    @Autowired
    protected DataImporter dataImporter
    @Autowired
    protected Resources resources

    def 'test successful import result'() {
        given:
        def importConfig = ImportConfigurationBuilder.of(Product, "import-product")
                .addSimplePropertyMapping("name", "name")
                .addSimplePropertyMapping("price", "price")
                .addSimplePropertyMapping("special", "special")
                .withInputDataFormat("xml")
                .withBooleanFormats("Yes", "No")
                .withTransactionStrategy(ImportTransactionStrategy.TRANSACTION_PER_ENTITY)
                .build()

        def xmlContent = resources.getResourceAsStream("/test_support/input_data_files/xml/one_product.xml")

        when: 'data imported'
        def result = dataImporter.importData(importConfig, xmlContent)

        then:
        result.success
        result.importedEntityIds.size() == 1
        result.failedEntities.empty
        result.configurationCode == importConfig.code
    }

    def 'test failed import result'() {
        given:
        def importConfig = ImportConfigurationBuilder.of(OrderLine, "import-lines")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('order')
                        .withReferenceImportPolicy(ReferenceImportPolicy.CREATE_IF_MISSING)
                        .addSimplePropertyMapping('orderNumber', 'orderNumber')
                        .addSimplePropertyMapping('date', 'orderDate')
                        .addSimplePropertyMapping('amount', 'orderAmount')
                        .lookupByAllSimpleProperties()
                        .build())
                .addReferencePropertyMapping('product', 'name', 'productName', ReferenceImportPolicy.IGNORE_IF_MISSING)
                .addSimplePropertyMapping("quantity", "quantity")
                .withDateFormat('dd/MM/yyyy HH:mm')
                .withInputDataFormat("xml")
                .withTransactionStrategy(ImportTransactionStrategy.SINGLE_TRANSACTION)
                .build()
        InputStream xmlContent = resources.getResourceAsStream("/test_support/input_data_files/xml/order_lines.xml")

        when: 'data imported'
        def importResult = dataImporter.importData(importConfig, xmlContent)

        then:
        !importResult.success
        importResult.configurationCode == importConfig.code
        importResult.numOfProcessedEntities == 0
        importResult.importedEntityIds.size() == 0
        importResult.errorMessage != null
    }
}
