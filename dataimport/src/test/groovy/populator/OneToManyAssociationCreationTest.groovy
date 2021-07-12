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

import io.jmix.core.common.util.ParamsMap
import io.jmix.dataimport.EntityPopulator
import io.jmix.dataimport.builder.ImportConfigurationBuilders
import io.jmix.dataimport.extractor.data.ImportedDataItem
import io.jmix.dataimport.extractor.data.ImportedObject
import io.jmix.dataimport.extractor.data.ImportedObjectList
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping
import io.jmix.dataimport.model.configuration.mapping.ReferenceEntityPolicy
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.Customer

import java.text.SimpleDateFormat

class OneToManyAssociationCreationTest extends DataImportSpec {
    @Autowired
    protected ImportConfigurationBuilders configurationBuilders

    @Autowired
    protected EntityPopulator entityPopulator

    def 'test creation using data from imported object list'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Customer, "customer")
                .addPropertyMapping(new PropertyMapping("orders", 'orders', ReferenceEntityPolicy.CREATE)
                        .addSimplePropertyMapping("orderNumber", "orderNumber")
                        .addSimplePropertyMapping("amount", "amount")
                        .addSimplePropertyMapping("date", "date"))
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
        entityFillingInfo.createdAssociations.size() == 2
        customer.orders.size() == 2

        def firstOrder = customer.orders.get(0)
        firstOrder.orderNumber == '#001'
        firstOrder.amount == 55.5
        firstOrder.date == new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('12/06/2021 12:00')

        def secondOrder = customer.orders.get(1)
        secondOrder.orderNumber == '#002'
        secondOrder.amount == 13
        secondOrder.date == new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('25/06/2021 17:00')
    }

    def 'test creation of nested one-to-many associations if each one has own imported object list'() {
        given:
        def orderLinesPropertyMapping = new PropertyMapping("lines", "lines", ReferenceEntityPolicy.CREATE)
                .addSimplePropertyMapping("quantity", "quantity")
                .addPropertyMapping(new PropertyMapping("product", null, ReferenceEntityPolicy.IGNORE)
                        .addSimplePropertyMapping("name", "productName"))

        def ordersPropertyMapping = new PropertyMapping("orders", 'orders', ReferenceEntityPolicy.CREATE)
                .addSimplePropertyMapping("orderNumber", "orderNumber")
                .addSimplePropertyMapping("amount", "amount")
                .addSimplePropertyMapping("date", "date")
                .addPropertyMapping(orderLinesPropertyMapping)

        def configuration = configurationBuilders.byEntityClass(Customer, "customer")
                .addSimplePropertyMapping("name", "name")
                .addPropertyMapping(ordersPropertyMapping)
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue("name", "John Dow")
        def importedObjectList = new ImportedObjectList()
                .addImportedObject(createOrderObject('#001', '20', '12/06/2021 12:00'))
        importedDataItem.addRawValue("orders", importedObjectList)

        when: 'entity populated'

        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        customer.name == 'John Dow'
        customer.orders.size() == 1

        def firstOrder = customer.orders.get(0)
        firstOrder.orderNumber == '#001'
        firstOrder.amount == 20
        firstOrder.date == new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('12/06/2021 12:00')
        firstOrder.lines.size() == 1

        def orderLine = firstOrder.lines.get(0)
        orderLine.product != null
        orderLine.product.name == 'Outback Power Nano-Carbon Battery 12V'
        orderLine.quantity == 2
    }

    def 'test creation of nested one-to-many associations without separate imported object list'() {
        def orderLinesPropertyMapping = new PropertyMapping("lines", null, ReferenceEntityPolicy.CREATE)
                .addSimplePropertyMapping("quantity", "quantity")
                .addPropertyMapping(new PropertyMapping("product", null, ReferenceEntityPolicy.IGNORE)
                        .addSimplePropertyMapping("name", "productName"))

        def ordersPropertyMapping = new PropertyMapping("orders", 'orderLines', ReferenceEntityPolicy.CREATE)
                .addSimplePropertyMapping("orderNumber", "orderNum")
                .addSimplePropertyMapping("amount", "orderAmount")
                .addSimplePropertyMapping("date", "orderDate")
                .addPropertyMapping(orderLinesPropertyMapping)


        def configuration = configurationBuilders.byEntityClass(Customer, "customer")
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
        customer.name == 'John Dow'
        customer.orders.size() == 2

        def firstOrder = customer.orders.get(0)
        firstOrder.orderNumber == '#001'
        firstOrder.amount == 30
        firstOrder.date == new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('12/06/2021 12:00')
        firstOrder.lines.size() == 1

        def firstOrderLine = firstOrder.lines.get(0)
        firstOrderLine.product != null
        firstOrderLine.product.name == 'Outback Power Nano-Carbon Battery 12V'
        firstOrderLine.quantity == 2

        def secondOrder = customer.orders.get(1)
        secondOrder.orderNumber == '#002'
        secondOrder.amount == 20
        secondOrder.date == new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('25/05/2021 12:00')
        secondOrder.lines.size() == 1

        def secondOrderLine = secondOrder.lines.get(0)
        secondOrderLine.product != null
        secondOrderLine.product.name == 'Fullriver Sealed Battery 6V'
        secondOrderLine.quantity == 4
    }

    def 'test creation of nested one-to-many associations if all data stores in imported data item'() {
        def orderLinesPropertyMapping = new PropertyMapping("lines", null, ReferenceEntityPolicy.CREATE)
                .addSimplePropertyMapping("quantity", "quantity")
                .addPropertyMapping(new PropertyMapping("product", null, ReferenceEntityPolicy.IGNORE)
                        .addSimplePropertyMapping("name", "productName"))

        def ordersPropertyMapping = new PropertyMapping("orders", null, ReferenceEntityPolicy.CREATE)
                .addSimplePropertyMapping("orderNumber", "orderNum")
                .addSimplePropertyMapping("amount", "orderAmount")
                .addSimplePropertyMapping("date", "orderDate")
                .addPropertyMapping(orderLinesPropertyMapping)

        def configuration = configurationBuilders.byEntityClass(Customer, "customer")
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
        customer.name == 'John Dow'
        customer.orders.size() == 1

        def firstOrder = customer.orders.get(0)
        firstOrder.orderNumber == '#001'
        firstOrder.amount == 20
        firstOrder.date == new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('12/06/2021 12:00')
        firstOrder.lines.size() == 1

        def orderLine = firstOrder.lines.get(0)
        orderLine.product != null
        orderLine.product.name == 'Outback Power Nano-Carbon Battery 12V'
        orderLine.quantity == 2
    }

    def 'test duplicates in one-to-many association for imported entity'() {
        given:
        def orderLinesPropertyMapping = new PropertyMapping("lines", null, ReferenceEntityPolicy.CREATE)
                .addSimplePropertyMapping("quantity", "quantity")
                .addPropertyMapping(new PropertyMapping("product", null, ReferenceEntityPolicy.IGNORE)
                        .addSimplePropertyMapping("name", "productName"))

        def ordersPropertyMapping = new PropertyMapping("orders", 'orderLines', ReferenceEntityPolicy.CREATE)
                .addSimplePropertyMapping("orderNumber", "orderNum")
                .addSimplePropertyMapping("amount", "orderAmount")
                .addSimplePropertyMapping("date", "orderDate")
                .addPropertyMapping(orderLinesPropertyMapping)


        def configuration = configurationBuilders.byEntityClass(Customer, "customer")
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
        customer.name == 'John Dow'
        customer.orders.size() == 1

        def order = customer.orders.get(0)
        order.orderNumber == '#001'
        order.amount == 50
        order.date == new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('12/06/2021 12:00')
        order.lines.size() == 2

        def firstOrderLine = order.lines.get(0)
        firstOrderLine.product != null
        firstOrderLine.product.name == 'Outback Power Nano-Carbon Battery 12V'
        firstOrderLine.quantity == 2

        def secondOrderLine = order.lines.get(1)
        secondOrderLine.product != null
        secondOrderLine.product.name == 'Fullriver Sealed Battery 6V'
        secondOrderLine.quantity == 4
    }

    protected ImportedObject createOrderObject(String orderNum, String amount, String date) {
        return new ImportedObject().addRawValue("orderNumber", orderNum)
                .addRawValue("amount", amount)
                .addRawValue("date", date)
                .addRawValue("lines", createLinesList())
    }

    protected ImportedObjectList createLinesList() {
        return new ImportedObjectList()
                .addImportedObject(createImportedObject(
                        ParamsMap.of("productName", "Outback Power Nano-Carbon Battery 12V",
                                "quantity", "2")))
    }

}
