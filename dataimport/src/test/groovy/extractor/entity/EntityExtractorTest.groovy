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

package extractor.entity

import io.jmix.dataimport.InputDataFormat
import io.jmix.dataimport.configuration.ImportConfiguration
import io.jmix.dataimport.configuration.ImportConfigurationBuilder
import io.jmix.dataimport.extractor.data.ImportedData
import io.jmix.dataimport.extractor.data.ImportedDataItem
import io.jmix.dataimport.extractor.entity.EntityExtractor
import io.jmix.dataimport.configuration.mapping.ReferenceMultiFieldPropertyMapping
import io.jmix.dataimport.configuration.mapping.ReferenceImportPolicy
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.Customer
import test_support.entity.Product

class EntityExtractorTest extends DataImportSpec {

    @Autowired
    protected EntityExtractor entityExtractor;

    def 'test one entity extraction'() {
        given:
        def configuration = ImportConfiguration.builder(Product, InputDataFormat.XLSX)
                .addSimplePropertyMapping("name", "Product Name")
                .addSimplePropertyMapping("price", "Price")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Product Name', 'Solar-One HUP Flooded Battery 48V')
        importedDataItem.addRawValue('Price', '210.55')

        when: 'entity extracted'
        def extractedEntity = entityExtractor.extract(configuration, importedDataItem)

        then:
        def product = extractedEntity as Product
        checkProduct(product, 'Solar-One HUP Flooded Battery 48V', 210.55, null)
    }

    def 'test several unique entities extraction'() {
        def ordersPropertyMapping = ReferenceMultiFieldPropertyMapping.builder("orders", ReferenceImportPolicy.CREATE)
                .addSimplePropertyMapping("orderNumber", "orderNum")
                .addSimplePropertyMapping("amount", "orderAmount")
                .addSimplePropertyMapping("date", "orderDate")
                .lookupByAllSimpleProperties()
                .build()


        def configuration = ImportConfiguration.builder(Customer, InputDataFormat.XLSX)
                .addSimplePropertyMapping("name", "name")
                .addPropertyMapping(ordersPropertyMapping)
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem1 = new ImportedDataItem()
        importedDataItem1.addRawValue("name", "John Dow")
        importedDataItem1.addRawValue("orderNum", "#001")
        importedDataItem1.addRawValue("orderDate", "12/06/2021 12:00")
        importedDataItem1.addRawValue("orderAmount", "20")

        def importedDataItem2 = new ImportedDataItem()
        importedDataItem2.addRawValue("name", "Tom Smith")
        importedDataItem2.addRawValue("orderNum", "#002")
        importedDataItem2.addRawValue("orderDate", "25/06/2021 12:00")
        importedDataItem2.addRawValue("orderAmount", "50")

        ImportedData importedData = new ImportedData()
        importedData.addItem(importedDataItem1)
        importedData.addItem(importedDataItem2)

        when: 'entities extracted'
        def entityExtractionResults = entityExtractor.extract(configuration, importedData)

        then:
        entityExtractionResults.size() == 2
        def entityExtractionResult1 = entityExtractionResults[0]
        entityExtractionResult1.importedDataItem == importedDataItem1

        def entityExtractionResult2 = entityExtractionResults[1]
        entityExtractionResult2.importedDataItem == importedDataItem2

        def customer1 = entityExtractionResult1.entity as Customer
        checkCustomer(customer1, 'John Dow', null, null)
        customer1.orders.size() == 1
        checkOrder(customer1.orders[0], '#001', '12/06/2021 12:00', 20)

        def customer2 = entityExtractionResult2.entity as Customer
        checkCustomer(customer2, 'Tom Smith', null, null)
        customer2.orders.size() == 1
        checkOrder(customer2.orders[0], '#002', '25/06/2021 12:00', 50)
    }


    def 'test several entities extraction with duplicates'() {
        def configuration = ImportConfiguration.builder(Customer, InputDataFormat.XLSX)
                .addSimplePropertyMapping("name", "name")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('orders', ReferenceImportPolicy.CREATE)
                        .addSimplePropertyMapping("orderNumber", "orderNum")
                        .addSimplePropertyMapping("amount", "orderAmount")
                        .addSimplePropertyMapping("date", "orderDate")
                        .build())
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem1 = new ImportedDataItem()
        importedDataItem1.addRawValue("name", "John Dow")
        importedDataItem1.addRawValue("orderNum", "#001")
        importedDataItem1.addRawValue("orderDate", "12/06/2021 12:00")
        importedDataItem1.addRawValue("orderAmount", "20")

        def importedDataItem2 = new ImportedDataItem()
        importedDataItem2.addRawValue("name", "John Dow")
        importedDataItem2.addRawValue("orderNum", "#002")
        importedDataItem2.addRawValue("orderDate", "25/06/2021 12:00")
        importedDataItem2.addRawValue("orderAmount", "50")

        ImportedData importedData = new ImportedData()
        importedData.addItem(importedDataItem1)
        importedData.addItem(importedDataItem2)

        when: 'entity extracted'
        def entityExtractionResults = entityExtractor.extract(configuration, importedData)

        then:
        entityExtractionResults.size() == 1
        def entityExtractionResult = entityExtractionResults[0]
        entityExtractionResult.importedDataItem == importedDataItem2

        def customer = entityExtractionResult.entity as Customer
        checkCustomer(customer, 'John Dow', null, null)
        customer.orders.size() == 2

        checkOrder(customer.orders[0], '#001', '12/06/2021 12:00', 20)
        checkOrder(customer.orders[1], '#002', '25/06/2021 12:00', 50)
    }
}
