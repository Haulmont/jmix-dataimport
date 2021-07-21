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

import io.jmix.dataimport.property.populator.EntityPropertiesPopulator
import io.jmix.dataimport.configuration.ImportConfigurationBuilder
import io.jmix.dataimport.extractor.data.ImportedDataItem
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.Customer
import test_support.entity.CustomerGrade
import test_support.entity.Order
import test_support.entity.OrderLine
import test_support.entity.Product
import test_support.entity.TestEntity

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SimplePropertyPopulatorTest extends DataImportSpec {
    @Autowired
    protected EntityPropertiesPopulator entityPopulator


    def 'test string property'() {
        given:
        def configuration = new ImportConfigurationBuilder(Product, "product")
                .addSimplePropertyMapping("name", "Product Name")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Product Name', 'Solar-One HUP Flooded Battery 48V')

        when: 'entity populated'
        def product = dataManager.create(Product)
        def entityFillingInfo = entityPopulator.populateProperties(product, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == product
        product.name == 'Solar-One HUP Flooded Battery 48V'
    }

    def 'test valid integer property'() {
        given:
        def configuration = new ImportConfigurationBuilder(OrderLine, "orderLine")
                .addSimplePropertyMapping("quantity", "Quantity")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Quantity', '5')

        when: 'entity populated'
        def orderLine = dataManager.create(OrderLine)
        def entityFillingInfo = entityPopulator.populateProperties(orderLine, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == orderLine
        orderLine.quantity == 5
    }

    def 'test invalid integer property'() {
        given:
        def configuration = new ImportConfigurationBuilder(OrderLine, "orderLine")
                .addSimplePropertyMapping("quantity", "Quantity")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Quantity', 'invalid')

        when: 'entity populated'
        def orderLine = dataManager.create(OrderLine)
        def entityFillingInfo = entityPopulator.populateProperties(orderLine, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == orderLine
        orderLine.quantity == null
    }

    def 'test valid date value'() {
        given:
        def configuration = new ImportConfigurationBuilder(Order, "order")
                .addSimplePropertyMapping("date", "Order Date")
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Order Date', '12/06/2021 12:00')

        def orderDate = new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('12/06/2021 12:00')

        when: 'entity populated'

        def order = dataManager.create(Order)
        def entityFillingInfo = entityPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        entityFillingInfo.createdReferences.size() == 0
        order.date == orderDate
    }

    def 'test invalid date value'() {
        given:
        def configuration = new ImportConfigurationBuilder(Order, "order")
                .addSimplePropertyMapping("date", "Order Date")
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Order Date', '12.06.2021')

        when: 'entity populated'

        def order = dataManager.create(Order)
        def entityFillingInfo = entityPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        entityFillingInfo.createdReferences.size() == 0
        order.date == null
    }

    def 'test boolean true value with custom format'() {
        given:
        def configuration = new ImportConfigurationBuilder(Product, "product")
                .addSimplePropertyMapping("special", "Special")
                .withBooleanFormats("Yes", "No")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Special', 'Yes')

        when: 'entity populated'

        def specialProduct = dataManager.create(Product)
        def entityFillingInfo = entityPopulator.populateProperties(specialProduct, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == specialProduct
        specialProduct.special
    }

    def 'test boolean false value with custom format'() {
        given:
        def configuration = new ImportConfigurationBuilder(Product, "product")
                .addSimplePropertyMapping("special", "Special")
                .withBooleanFormats("Yes", "No")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Special', 'No')

        when: 'entity populated'

        def specialProduct = dataManager.create(Product)
        def entityFillingInfo = entityPopulator.populateProperties(specialProduct, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == specialProduct
        specialProduct.special != null
        !specialProduct.special
    }

    def 'test valid boolean value'() {
        given:
        def configuration = new ImportConfigurationBuilder(Product, "product")
                .addSimplePropertyMapping("special", "Special")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Special', 'true')

        when: 'entity populated'
        def specialProduct = dataManager.create(Product)
        def entityFillingInfo = entityPopulator.populateProperties(specialProduct, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == specialProduct
        specialProduct.special
    }

    def 'test invalid BigDecimal value'() {
        given:
        def configuration = new ImportConfigurationBuilder(Product, "product")
                .addSimplePropertyMapping("price", "Price")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Price', 'string')


        when: 'entity populated'

        def specialProduct = dataManager.create(Product)
        def entityFillingInfo = entityPopulator.populateProperties(specialProduct, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == specialProduct
        specialProduct.price == null
    }

    def 'test valid local date value'() {
        given:
        def configuration = new ImportConfigurationBuilder(TestEntity, "testEntity")
                .addSimplePropertyMapping('localDateProperty', 'Date')
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Date', '12/06/2021 12:00')

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(configuration.getDateFormat());

        when: 'entity populated'

        def testEntity = dataManager.create(TestEntity)
        def entityFillingInfo = entityPopulator.populateProperties(testEntity, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == testEntity
        testEntity.localDateProperty == LocalDate.parse('12/06/2021 12:00', formatter)
    }

    def 'test invalid local date value'() {
        given:
        def configuration = new ImportConfigurationBuilder(TestEntity, "testEntity")
                .addSimplePropertyMapping('localDateProperty', 'Date')
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Date', '12.06.2021')

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(configuration.getDateFormat());

        when: 'entity populated'

        def testEntity = dataManager.create(TestEntity)
        def entityFillingInfo = entityPopulator.populateProperties(testEntity, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == testEntity
        testEntity.localDateProperty == null
    }

    def 'test valid long value'() {
        given:
        def configuration = new ImportConfigurationBuilder(TestEntity, "testEntity")
                .addSimplePropertyMapping('longProperty', 'Long')
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Long', Long.MAX_VALUE.toString())

        when: 'entity populated'
        def testEntity = dataManager.create(TestEntity)
        def entityFillingInfo = entityPopulator.populateProperties(testEntity, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == testEntity
        testEntity.longProperty == Long.MAX_VALUE
    }

    def 'test invalid long value'() {
        given:
        def configuration = new ImportConfigurationBuilder(TestEntity, "testEntity")
                .addSimplePropertyMapping('longProperty', 'Long')
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Long', 'invalid')

        when: 'entity populated'
        def testEntity = dataManager.create(TestEntity)
        def entityFillingInfo = entityPopulator.populateProperties(testEntity, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == testEntity
        testEntity.longProperty == null
    }

    def 'test valid double value'() {
        given:
        def configuration = new ImportConfigurationBuilder(TestEntity, "testEntity")
                .addSimplePropertyMapping('doubleProperty', 'Double')
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Double', '1.5')

        when: 'entity populated'
        def testEntity = dataManager.create(TestEntity)
        def entityFillingInfo = entityPopulator.populateProperties(testEntity, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == testEntity
        testEntity.doubleProperty == 1.5
    }

    def 'test invalid double value'() {
        given:
        def configuration = new ImportConfigurationBuilder(TestEntity, "testEntity")
                .addSimplePropertyMapping('doubleProperty', 'Double')
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Double', 'invalid')

        when: 'entity populated'
        def testEntity = dataManager.create(TestEntity)
        def entityFillingInfo = entityPopulator.populateProperties(testEntity, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == testEntity
        testEntity.doubleProperty == null
    }

    def 'test valid enum property'() {
        given:
        def configuration = new ImportConfigurationBuilder(Customer, "customer")
                .addSimplePropertyMapping("grade", "Grade")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Grade', 'Bronze')

        when: 'entity populated'
        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        customer.grade == CustomerGrade.BRONZE
    }

    def 'test invalid enum property'() {
        given:
        def configuration = new ImportConfigurationBuilder(Customer, "customer")
                .addSimplePropertyMapping("grade", "Grade")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Grade', 'NewGrade')

        when: 'entity populated'
        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        customer.grade == null
    }
}
