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

import io.jmix.dataimport.builder.ImportConfigurationBuilders
import io.jmix.dataimport.extractor.data.ImportedData
import io.jmix.dataimport.extractor.data.ImportedDataItem
import io.jmix.dataimport.extractor.entity.EntityExtractor
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping
import io.jmix.dataimport.model.configuration.mapping.ReferenceEntityPolicy
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.Customer
import test_support.entity.Product

import java.text.SimpleDateFormat

class EntityExtractorTest extends DataImportSpec {
    @Autowired
    protected ImportConfigurationBuilders configurationBuilders

    @Autowired
    protected EntityExtractor entityExtractor;

    def 'test one entity extraction'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Product, "product")
                .addSimplePropertyMapping("name", "Product Name")
                .addSimplePropertyMapping("price", "Price")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Product Name', 'Solar-One HUP Flooded Battery 48V')
        importedDataItem.addRawValue('Price', '210.55')

        when: 'entity extracted'
        def entityExtractionResult = entityExtractor.extract(configuration, importedDataItem)

        then:
        entityExtractionResult.importedDataItem == importedDataItem
        def product = entityExtractionResult.entity as Product
        product != null
        product.name == 'Solar-One HUP Flooded Battery 48V'
        product.price == 210.55
    }

    def 'test several unique entities extraction'() {
        def ordersPropertyMapping = new PropertyMapping("orders", null, ReferenceEntityPolicy.CREATE)
                .addSimplePropertyMapping("orderNumber", "orderNum")
                .addSimplePropertyMapping("amount", "orderAmount")
                .addSimplePropertyMapping("date", "orderDate")


        def configuration = configurationBuilders.byEntityClass(Customer, "customer")
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
        def entityExtractionResult1 = entityExtractionResults.get(0)
        entityExtractionResult1.importedDataItem == importedDataItem1

        def entityExtractionResult2 = entityExtractionResults.get(1)
        entityExtractionResult2.importedDataItem == importedDataItem2

        def customer1 = entityExtractionResult1.entity as Customer
        customer1.name == 'John Dow'
        customer1.orders.size() == 1

        def firstOrder = customer1.orders.get(0)
        firstOrder.orderNumber == '#001'
        firstOrder.amount == 20
        firstOrder.date == new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('12/06/2021 12:00')

        def customer2 = entityExtractionResult2.entity as Customer
        customer2.name == 'Tom Smith'
        customer2.orders.size() == 1

        def secondOrder = customer2.orders.get(0)
        secondOrder.orderNumber == '#002'
        secondOrder.amount == 50
        secondOrder.date == new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('25/06/2021 12:00')
    }


}
