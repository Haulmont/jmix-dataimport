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

import io.jmix.core.FetchPlan
import io.jmix.core.Resources
import io.jmix.dataimport.DataImporter
import io.jmix.dataimport.builder.ImportConfigurationBuilders
import io.jmix.dataimport.model.configuration.ImportTransactionStrategy
import io.jmix.dataimport.model.configuration.mapping.ReferenceEntityPolicy
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.Order
import test_support.entity.Product

import java.text.SimpleDateFormat

class DataImporterTest extends DataImportSpec {
    @Autowired
    protected DataImporter dataImporter
    @Autowired
    protected ImportConfigurationBuilders configurationBuilders
    @Autowired
    protected Resources resources


    def 'test successful import in single transaction'() {
        given:
        def importConfig = configurationBuilders.byEntityClass(Product, "import-product")
                .addSimplePropertyMapping("name", "name")
                .addSimplePropertyMapping("price", "price")
                .addSimplePropertyMapping("special", "special")
                .withInputDataFormat("xml")
                .withBooleanFormats("Yes", "No")
                .withTransactionStrategy(ImportTransactionStrategy.SINGLE_TRANSACTION)
                .build()

        def xmlContent = resources.getResourceAsString("/test_support/input_data_files/xml/one_product.xml")

        when: 'data imported'
        def result = dataImporter.importData(importConfig, xmlContent)

        then:
        result.success
        result.importedEntityIds.size() == 1

        def importedProduct = dataManager.load(Product)
                .id(result.importedEntityIds.get(0))
                .fetchPlan(FetchPlan.LOCAL)
                .one() as Product
        importedProduct.name == 'Cotek Battery Charger'
        importedProduct.price == 30.1
        importedProduct.special != null
        !importedProduct.special
    }

    def 'test successful import in transaction per entity from Excel'() {
        given:
        def importConfig = configurationBuilders.byEntityClass(Order, "import-order")
                .addSimplePropertyMapping("orderNumber", "Order Num")
                .addSimplePropertyMapping("date", "Order Date")
                .addSimplePropertyMapping("amount", "Order Amount")
                .addAssociationPropertyMapping("customer.name", "Customer Name", ReferenceEntityPolicy.CREATE)
                .addAssociationPropertyMapping("customer.email", "Customer Email", ReferenceEntityPolicy.CREATE)
                .withInputDataFormat("xlsx")
                .withTransactionStrategy(ImportTransactionStrategy.TRANSACTION_PER_ENTITY)
                .withDateFormat("dd/MM/yyyy HH:mm")
                .build()

        def excelInputStream = resources.getResourceAsStream("/test_support/input_data_files/xlsx/orders.xlsx")

        when: 'data imported'
        def result = dataImporter.importData(importConfig, excelInputStream)

        then:
        result.success
        result.importedEntityIds.size() == 3
        def customer = loadCustomer('John Dow')

        def importedOrder = loadOrder(result.importedEntityIds.get(0), "order-with-customer")
        importedOrder.orderNumber == '#123'
        importedOrder.date == new SimpleDateFormat("dd/MM/yyyy HH:mm").parse('12/12/2020 12:30')
        importedOrder.amount == 85.2
        importedOrder.customer != null
        importedOrder.customer == customer
    }
}
