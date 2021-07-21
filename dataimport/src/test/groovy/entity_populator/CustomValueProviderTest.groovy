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

import io.jmix.core.FetchPlan
import io.jmix.core.common.util.ParamsMap
import io.jmix.dataimport.InputDataFormat
import io.jmix.dataimport.property.populator.EntityPropertiesPopulator
import io.jmix.dataimport.configuration.ImportConfigurationBuilder
import io.jmix.dataimport.extractor.data.ImportedDataItem
import io.jmix.dataimport.extractor.data.ImportedObject
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.Customer
import test_support.entity.CustomerGrade
import test_support.entity.Order
import test_support.entity.Product

class CustomValueProviderTest extends DataImportSpec {

    @Autowired
    protected EntityPropertiesPopulator propertiesPopulator

    def 'test custom value for simple property'() {
        given:
        def configuration = new ImportConfigurationBuilder(Product, "product")
                .addCustomPropertyMapping("price", "Price", customValueContext -> {
                    def rawValue = customValueContext.getRawValue()
                    try {
                        def value = new BigDecimal(rawValue)
                        return value
                    } catch (Exception ex) {
                        return BigDecimal.ZERO
                    }
                })
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Price', 'string')


        when: 'entity populated'

        def specialProduct = dataManager.create(Product)
        def entityFillingInfo = propertiesPopulator.populateProperties(specialProduct, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == specialProduct
        specialProduct.price == 0
    }


    def 'test custom value of reference property if raw value is string'() {
        def configuration = new ImportConfigurationBuilder(Order, "order")
                .addCustomPropertyMapping("customer", "customerName", customValueContext -> {
                    String customerName = customValueContext.getRawValue()
                    def customer = loadCustomer(customerName, FetchPlan.BASE) as Customer
                    if (customer == null) {
                        def newCustomer = dataManager.create(Customer)
                        newCustomer.name = customerName
                        return newCustomer
                    }
                })
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('customerName', 'John Dow')


        when: 'entity populated'
        def order = dataManager.create(Order)
        def entityFillingInfo = propertiesPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        checkCustomer(order.customer, 'John Dow', null, null)
    }

    def 'test custom value of reference property if other raw values are taken from imported item'() {
        def configuration = new ImportConfigurationBuilder(Order, "order")
                .addCustomPropertyMapping("customer", "customerName", customValueContext -> {
                    String customerName = customValueContext.getRawValue()
                    def customer = loadCustomer(customerName, FetchPlan.BASE) as Customer
                    String email = customValueContext.getRawValuesSource().getRawValue('customerEmail')
                    String grade = customValueContext.getRawValuesSource().getRawValue('customerGrade')
                    if (customer == null) {
                        def newCustomer = dataManager.create(Customer)
                        newCustomer.name = customerName
                        newCustomer.email = email
                        newCustomer.grade = CustomerGrade.fromId(grade)
                        return newCustomer
                    }
                })
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('customerName', 'John Dow')
        importedDataItem.addRawValue('customerEmail', 'j.dow@mail.com')
        importedDataItem.addRawValue('customerGrade', 'Bronze')


        when: 'entity populated'
        def order = dataManager.create(Order)
        def entityFillingInfo = propertiesPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        checkCustomer(order.customer, 'John Dow', 'j.dow@mail.com', CustomerGrade.BRONZE)
    }

    def 'test custom value of reference property if raw value is object'() {
        def configuration = new ImportConfigurationBuilder(Order, "order")
                .addCustomPropertyMapping("customer", "customer", customValueContext -> {
                    ImportedObject customerObject = customValueContext.getRawValue() as ImportedObject
                    String customerName = customerObject.getRawValue('name')

                    def customer = loadCustomer(customerName, FetchPlan.BASE) as Customer
                    if (customer == null) {
                        String email = customerObject.getRawValue('email')
                        String grade = customerObject.getRawValue('grade')

                        def newCustomer = dataManager.create(Customer)
                        newCustomer.name = customerName
                        newCustomer.email = email
                        newCustomer.grade = CustomerGrade.fromId(grade)
                        return newCustomer
                    }
                })
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('customer', createImportedObject(ParamsMap.of('name', 'John Dow',
                'email', 'j.dow@mail.com',
                'grade', CustomerGrade.BRONZE.id)))


        when: 'entity populated'
        def order = dataManager.create(Order)
        def entityFillingInfo = propertiesPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        checkCustomer(order.customer, 'John Dow', 'j.dow@mail.com', CustomerGrade.BRONZE)
    }
}
