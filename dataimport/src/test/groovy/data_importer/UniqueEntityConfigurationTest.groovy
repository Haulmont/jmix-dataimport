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
import io.jmix.dataimport.InputDataFormat
import io.jmix.dataimport.configuration.ImportConfiguration
import io.jmix.dataimport.configuration.ImportConfigurationBuilder
import io.jmix.dataimport.configuration.DuplicateEntityPolicy
import io.jmix.dataimport.configuration.UniqueEntityConfiguration
import io.jmix.dataimport.configuration.mapping.ReferenceMultiFieldPropertyMapping
import io.jmix.dataimport.configuration.mapping.ReferenceImportPolicy
import io.jmix.dataimport.configuration.mapping.SimplePropertyMapping
import io.jmix.dataimport.result.EntityImportErrorType
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.Customer

class UniqueEntityConfigurationTest extends DataImportSpec {
    @Autowired
    protected DataImporter dataImporter
    @Autowired
    protected Resources resources

    def 'test unique entity configuration with UPDATE policy'() {
        given:
        def importConfig = new ImportConfiguration(Customer, InputDataFormat.XML)
                .addPropertyMapping(new SimplePropertyMapping("name", "name"))
                .addPropertyMapping(new SimplePropertyMapping("email", "email"))
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder("orders", ReferenceImportPolicy.CREATE)
                        .withDataFieldName("order")
                        .addSimplePropertyMapping("orderNumber", "number")
                        .addSimplePropertyMapping("date", "date")
                        .addSimplePropertyMapping("amount", "amount")
                        .lookupByAllSimpleProperties()
                        .build())
                .addUniqueEntityConfiguration(new UniqueEntityConfiguration(Arrays.asList("name", "email"), DuplicateEntityPolicy.UPDATE))
                .setDateFormat('dd/MM/yyyy HH:mm')


        def xmlContent = resources.getResourceAsStream("/test_support/input_data_files/xml/customer_with_orders.xml")
        def customer = loadCustomer('Shelby Robinson', FetchPlan.BASE)

        when: 'data imported'
        def result = dataImporter.importData(importConfig, xmlContent)

        then:
        result.success
        result.importedEntityIds.size() == 1

        def customer1 = loadEntity(Customer, result.importedEntityIds[0], "customer-with-orders") as Customer
        customer1 == customer
        customer1.orders != null
        customer1.orders.sort(order -> order.orderNumber)
        customer1.orders.size() == 2
        checkOrder(customer1.orders[0], '#001', '12/02/2021 12:00', 50.5)
        checkOrder(customer1.orders[1], '#002', '12/05/2021 17:00', 25)
    }

    def 'test unique entity configuration with ABORT policy'() {
        given:
        def importConfig = ImportConfiguration.builder(Customer, InputDataFormat.XML)
                .addSimplePropertyMapping("name", "name")
                .addSimplePropertyMapping("email", "email")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder("orders", ReferenceImportPolicy.CREATE)
                        .withDataFieldName("order")
                        .addSimplePropertyMapping("orderNumber", "number")
                        .addSimplePropertyMapping("date", "date")
                        .addSimplePropertyMapping("amount", "amount")
                        .build())
                .addUniqueEntityConfiguration(DuplicateEntityPolicy.ABORT, "name", "email")
                .withDateFormat('dd/MM/yyyy HH:mm')
                .build()

        def xmlContent = resources.getResourceAsStream("/test_support/input_data_files/xml/customer_with_orders.xml")

        when: 'data imported'
        def result = dataImporter.importData(importConfig, xmlContent)

        then:
        !result.success
        result.importedEntityIds.size() == 0
        result.errorMessage != null
    }

    def 'test unique entity configuration with SKIP policy'() {
        given:
        def importConfig = ImportConfiguration.builder(Customer, InputDataFormat.XML)
                .addSimplePropertyMapping("name", "name")
                .addSimplePropertyMapping("email", "email")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder("orders", ReferenceImportPolicy.CREATE)
                        .withDataFieldName("order")
                        .addSimplePropertyMapping("orderNumber", "number")
                        .addSimplePropertyMapping("date", "date")
                        .addSimplePropertyMapping("amount", "amount")
                        .build())
                .addUniqueEntityConfiguration(DuplicateEntityPolicy.SKIP, Arrays.asList("name", "email"))
                .withDateFormat('dd/MM/yyyy HH:mm')
                .build()

        def xmlContent = resources.getResourceAsStream("/test_support/input_data_files/xml/customer_with_orders.xml")

        when: 'data imported'
        def result = dataImporter.importData(importConfig, xmlContent)

        then:
        result.success
        result.importedEntityIds.size() == 0
        result.failedEntities.size() == 1

        result.failedEntities[0].errorType == EntityImportErrorType.UNIQUE_VIOLATION
        def customer = result.failedEntities[0].entity as Customer
        checkCustomer(customer, 'Shelby Robinson', 'robinson@mail.com', null)
    }
}