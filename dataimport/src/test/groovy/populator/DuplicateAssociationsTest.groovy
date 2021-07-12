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

package populator


import io.jmix.dataimport.EntityPopulator
import io.jmix.dataimport.builder.ImportConfigurationBuilders
import io.jmix.dataimport.extractor.data.ImportedData
import io.jmix.dataimport.extractor.data.ImportedDataItem
import io.jmix.dataimport.extractor.entity.EntityExtractor
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping
import io.jmix.dataimport.model.configuration.mapping.ReferenceEntityPolicy
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.Customer

import java.text.SimpleDateFormat

class DuplicateAssociationsTest extends DataImportSpec {
    @Autowired
    protected ImportConfigurationBuilders configurationBuilders

    @Autowired
    protected EntityPopulator entityPopulator

    @Autowired
    protected EntityExtractor entityExtractor

    def 'test duplicates of entity from import configuration'() {
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
        def entityExtractionResult = entityExtractionResults.get(0)
        entityExtractionResult.importedDataItem == importedDataItem2

        def customer = entityExtractionResult.entity as Customer
        customer.name == 'John Dow'
        customer.orders.size() == 2

        def firstOrder = customer.orders.get(0)
        firstOrder.orderNumber == '#001'
        firstOrder.amount == 20
        firstOrder.date == new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('12/06/2021 12:00')

        def secondOrder = customer.orders.get(1)
        secondOrder.orderNumber == '#002'
        secondOrder.amount == 50
        secondOrder.date == new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('25/06/2021 12:00')
    }
}
