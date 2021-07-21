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

package entity_populator


import io.jmix.core.common.util.ParamsMap
import io.jmix.dataimport.configuration.mapping.ReferenceMultiFieldPropertyMapping
import io.jmix.dataimport.property.populator.EntityPropertiesPopulator
import io.jmix.dataimport.configuration.ImportConfigurationBuilder
import io.jmix.dataimport.extractor.data.ImportedDataItem
import io.jmix.dataimport.extractor.data.ImportedObject
import io.jmix.dataimport.extractor.data.ImportedObjectList
import io.jmix.dataimport.configuration.mapping.ReferenceImportPolicy
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.Customer

class OneToManyAssociationCreationTest extends DataImportSpec {

    @Autowired
    protected EntityPropertiesPopulator entityPopulator

    def 'test creation using data from imported object list'() {
        given:
        def configuration = new ImportConfigurationBuilder(Customer, "customer")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder("orders")
                        .withDataFieldName('orders')
                        .withReferenceImportPolicy(ReferenceImportPolicy.CREATE)
                        .addSimplePropertyMapping("orderNumber", "orderNumber")
                        .addSimplePropertyMapping("amount", "amount")
                        .addSimplePropertyMapping("date", "date")
                        .build())
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        def importedObjectList = new ImportedObjectList()
                .addImportedObject(createOrderObject('#001', '55.5', '12/06/2021 12:00'))
                .addImportedObject(createOrderObject('#002', '13', '25/06/2021 17:00'))
        importedDataItem.addRawValue("orders", importedObjectList)

        when: 'entity populated'

        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        entityFillingInfo.createdReferences.size() == 2
        customer.orders.size() == 2
        checkOrder(customer.orders.get(0), '#001', '12/06/2021 12:00', 55.5)
        checkOrder(customer.orders.get(1), '#002', '25/06/2021 17:00', 13)
    }

    def 'test creation of nested one-to-many associations if each one has own imported object list'() {
        given:
        def orderLinesPropertyMapping = ReferenceMultiFieldPropertyMapping.builder("lines")
                .withDataFieldName("lines")
                .withReferenceImportPolicy(ReferenceImportPolicy.CREATE)
                .addSimplePropertyMapping("quantity", "quantity")
                .addReferencePropertyMapping("product", 'name', 'productName', ReferenceImportPolicy.IGNORE_IF_MISSING)
                .build()

        def ordersPropertyMapping = ReferenceMultiFieldPropertyMapping.builder("orders")
                .withDataFieldName('orders')
                .withReferenceImportPolicy(ReferenceImportPolicy.CREATE)
                .addSimplePropertyMapping("orderNumber", "orderNumber")
                .addSimplePropertyMapping("amount", "amount")
                .addSimplePropertyMapping("date", "date")
                .addPropertyMapping(orderLinesPropertyMapping)
                .build()

        def configuration = new ImportConfigurationBuilder(Customer, "customer")
                .addSimplePropertyMapping("name", "name")
                .addPropertyMapping(ordersPropertyMapping)
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        def importedObjectList = new ImportedObjectList()
                .addImportedObject(createOrderObject('#001', '20', '12/06/2021 12:00'))
        importedDataItem.setRawValues(ParamsMap.of("name", "John Dow",
                "orders", importedObjectList))

        when: 'entity populated'

        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        checkCustomer(customer, 'John Dow', null, null)
        customer.orders.size() == 1


        def firstOrder = customer.orders.get(0)
        checkOrder(firstOrder, '#001', '12/06/2021 12:00', 20)
        firstOrder.lines.size() == 1
        checkOrderLine(firstOrder.lines.get(0), 'Outback Power Nano-Carbon Battery 12V', 2)
    }

    def 'test creation of nested one-to-many associations without separate imported object list'() {
        def orderLinesPropertyMapping = ReferenceMultiFieldPropertyMapping.builder("lines")
                .withReferenceImportPolicy(ReferenceImportPolicy.CREATE)
                .addSimplePropertyMapping("quantity", "quantity")
                .addReferencePropertyMapping("product", "name", "productName", ReferenceImportPolicy.IGNORE_IF_MISSING)
                .build()

        def ordersPropertyMapping = ReferenceMultiFieldPropertyMapping.builder("orders")
                .withDataFieldName('orderLines')
                .withReferenceImportPolicy(ReferenceImportPolicy.CREATE)
                .addSimplePropertyMapping("orderNumber", "orderNum")
                .addSimplePropertyMapping("amount", "orderAmount")
                .addSimplePropertyMapping("date", "orderDate")
                .addPropertyMapping(orderLinesPropertyMapping)
                .build()


        def configuration = new ImportConfigurationBuilder(Customer, "customer")
                .addSimplePropertyMapping("name", "name")
                .addPropertyMapping(ordersPropertyMapping)
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue("name", "John Dow")

        def importedObjectList = new ImportedObjectList()
                .addImportedObject(createImportedObject(ParamsMap.of("orderNum", "#001",
                        "orderAmount", "30",
                        "orderDate", "12/06/2021 12:00",
                        "productName", "Outback Power Nano-Carbon Battery 12V",
                        "quantity", "2")))
                .addImportedObject(createImportedObject(ParamsMap.of("orderNum", "#002",
                        "orderAmount", "20",
                        "orderDate", "25/05/2021 12:00",
                        "productName", "Fullriver Sealed Battery 6V",
                        "quantity", "4")))
        importedDataItem.addRawValue("orderLines", importedObjectList)

        when: 'entity populated'
        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        checkCustomer(customer, 'John Dow', null, null)
        customer.orders.size() == 2

        def firstOrder = customer.orders.get(0)
        checkOrder(firstOrder, '#001', '12/06/2021 12:00', 30)
        firstOrder.lines.size() == 1
        checkOrderLine(firstOrder.lines.get(0), 'Outback Power Nano-Carbon Battery 12V', 2)


        def secondOrder = customer.orders.get(1)
        checkOrder(secondOrder, '#002', '25/05/2021 12:00', 20)
        secondOrder.lines.size() == 1
        checkOrderLine(secondOrder.lines.get(0), 'Fullriver Sealed Battery 6V', 4)
    }

    def 'test creation of nested one-to-many associations if all data stores in imported data item'() {
        def orderLinesPropertyMapping = ReferenceMultiFieldPropertyMapping.builder("lines")
                .withReferenceImportPolicy(ReferenceImportPolicy.CREATE)
                .addSimplePropertyMapping("quantity", "quantity")
                .addReferencePropertyMapping("product", "name", "productName", ReferenceImportPolicy.IGNORE_IF_MISSING)
                .build()

        def ordersPropertyMapping = ReferenceMultiFieldPropertyMapping.builder("orders")
                .withReferenceImportPolicy(ReferenceImportPolicy.CREATE)
                .addSimplePropertyMapping("orderNumber", "orderNum")
                .addSimplePropertyMapping("amount", "orderAmount")
                .addSimplePropertyMapping("date", "orderDate")
                .addPropertyMapping(orderLinesPropertyMapping)
                .build()

        def configuration = new ImportConfigurationBuilder(Customer, "customer")
                .addSimplePropertyMapping("name", "name")
                .addPropertyMapping(ordersPropertyMapping)
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue("name", "John Dow")
        importedDataItem.addRawValue("orderNum", "#001")
        importedDataItem.addRawValue("orderDate", "12/06/2021 12:00")
        importedDataItem.addRawValue("orderAmount", "20")
        importedDataItem.addRawValue("productName", "Outback Power Nano-Carbon Battery 12V")
        importedDataItem.addRawValue("quantity", "2")

        when: 'entity populated'

        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        checkCustomer(customer, 'John Dow', null, null)
        customer.orders.size() == 1

        def firstOrder = customer.orders.get(0)
        checkOrder(firstOrder, '#001', '12/06/2021 12:00', 20)
        firstOrder.lines.size() == 1
        checkOrderLine(firstOrder.lines.get(0), 'Outback Power Nano-Carbon Battery 12V', 2)
    }

    def 'test duplicates in one-to-many association for imported entity'() {
        given:
        def orderLinesPropertyMapping = ReferenceMultiFieldPropertyMapping.builder("lines")
                .withReferenceImportPolicy(ReferenceImportPolicy.CREATE)
                .addSimplePropertyMapping("quantity", "quantity")
                .addReferencePropertyMapping("product", "name", "productName", ReferenceImportPolicy.IGNORE_IF_MISSING)
                .build()

        def ordersPropertyMapping = ReferenceMultiFieldPropertyMapping.builder("orders")
                .withDataFieldName('orderLines')
                .withReferenceImportPolicy(ReferenceImportPolicy.CREATE)
                .addSimplePropertyMapping("orderNumber", "orderNum")
                .addSimplePropertyMapping("amount", "orderAmount")
                .addSimplePropertyMapping("date", "orderDate")
                .addPropertyMapping(orderLinesPropertyMapping)
                .lookupByAllSimpleProperties()
                .build()


        def configuration = new ImportConfigurationBuilder(Customer, "customer")
                .addSimplePropertyMapping("name", "name")
                .addPropertyMapping(ordersPropertyMapping)
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue("name", "John Dow")

        def importedObjectList = new ImportedObjectList()
                .addImportedObject(createImportedObject(ParamsMap.of("orderNum", "#001",
                        "orderAmount", "50",
                        "orderDate", "12/06/2021 12:00",
                        "productName", "Outback Power Nano-Carbon Battery 12V",
                        "quantity", "2")))
                .addImportedObject(createImportedObject(ParamsMap.of("orderNum", "#001",
                        "orderAmount", "50",
                        "orderDate", "12/06/2021 12:00",
                        "productName", "Fullriver Sealed Battery 6V",
                        "quantity", "4")))
        importedDataItem.addRawValue("orderLines", importedObjectList)

        when: 'entity populated'
        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        checkCustomer(customer, 'John Dow', null, null)
        customer.orders.size() == 1

        def order = customer.orders.get(0)
        checkOrder(order, '#001', '12/06/2021 12:00', 50)
        order.lines.size() == 2
        checkOrderLine(order.lines.get(0), 'Outback Power Nano-Carbon Battery 12V', 2)
        checkOrderLine(order.lines.get(1), 'Fullriver Sealed Battery 6V', 4)
    }

    protected ImportedObject createOrderObject(String orderNum, String amount, String date) {
        def orderObject = new ImportedObject()
        orderObject.setRawValues(ParamsMap.of("orderNumber", orderNum,
                "amount", amount,
                "date", date,
                "lines", createLinesList()))
        return orderObject
    }

    protected ImportedObjectList createLinesList() {
        return new ImportedObjectList()
                .addImportedObject(createImportedObject(
                        ParamsMap.of("productName", "Outback Power Nano-Carbon Battery 12V",
                                "quantity", "2")))
    }

}
