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
import io.jmix.dataimport.extractor.data.ImportedDataItem
import io.jmix.dataimport.extractor.data.ImportedObject
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping
import io.jmix.dataimport.model.configuration.mapping.ReferenceEntityPolicy
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.BonusCard
import test_support.entity.Customer
import test_support.entity.Order
import test_support.entity.PaymentType

import java.text.SimpleDateFormat

class OneToOneAssociationCreationTest extends DataImportSpec {
    @Autowired
    protected ImportConfigurationBuilders configurationBuilders

    @Autowired
    protected EntityPopulator entityPopulator

    def 'test creation of one-to-one association from imported data item'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Customer, "customer")
                .addSimplePropertyMapping("name", "customerName")
                .addPropertyMapping(new PropertyMapping("bonusCard", null, ReferenceEntityPolicy.CREATE)
                        .addSimplePropertyMapping("cardNumber", "bonusCardNumber"))
                .build()
        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue("customerName", "John Dow")
        importedDataItem.addRawValue("bonusCardNumber", "12345-6789")

        when: 'entity populated'
        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        customer.name == 'John Dow'
        customer.bonusCard != null
        customer.bonusCard.cardNumber == '12345-6789'
    }

    def 'test creation of one-to-one association from separate imported object'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Customer, "customer")
                .addSimplePropertyMapping("name", "customerName")
                .addPropertyMapping(new PropertyMapping("bonusCard", "bonusCard", ReferenceEntityPolicy.CREATE)
                        .addSimplePropertyMapping("cardNumber", "cardNumber")
                        .addSimplePropertyMapping("isActive", "isActive")
                        .addSimplePropertyMapping("balance", "balance"))
                .build()
        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue("customerName", "John Dow")
        importedDataItem.addRawValue("bonusCard", new ImportedObject()
                .addRawValue("cardNumber", "12345-67890")
                .addRawValue("isActive", "True")
                .addRawValue("balance", "50"));

        when: 'entity populated'
        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        customer.name == 'John Dow'
        customer.bonusCard != null
        customer.bonusCard.cardNumber == '12345-67890'
        customer.bonusCard.isActive != null
        customer.bonusCard.isActive
        customer.bonusCard.balance == 50
    }

    def 'test ignore not existing one-to-one association from separate imported object'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Customer, "customer")
                .addSimplePropertyMapping("name", "customerName")
                .addPropertyMapping(new PropertyMapping("bonusCard", "bonusCard", ReferenceEntityPolicy.IGNORE)
                        .addSimplePropertyMapping("cardNumber", "cardNumber")
                        .addSimplePropertyMapping("isActive", "isActive")
                        .addSimplePropertyMapping("balance", "balance"))
                .build()
        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue("customerName", "John Dow")
        importedDataItem.addRawValue("bonusCard", new ImportedObject()
                .addRawValue("cardNumber", "12345-67890")
                .addRawValue("isActive", "True")
                .addRawValue("balance", "50"));

        when: 'entity populated'
        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        customer.name == 'John Dow'
        customer.bonusCard == null
    }

    def 'test ignore not existing one-to-one association from imported data item'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Customer, "customer")
                .addSimplePropertyMapping("name", "customerName")
                .addPropertyMapping(new PropertyMapping("bonusCard", null, ReferenceEntityPolicy.IGNORE)
                        .addSimplePropertyMapping("cardNumber", "bonusCardNumber"))
                .build()
        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue("customerName", "John Dow")
        importedDataItem.addRawValue("bonusCardNumber", "12345-6789")

        when: 'entity populated'
        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        customer.name == 'John Dow'
        customer.bonusCard == null
    }

    def 'test set existing one-to-one association from imported data item'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Customer, "customer")
                .addSimplePropertyMapping("name", "customerName")
                .addPropertyMapping(new PropertyMapping("bonusCard", null, ReferenceEntityPolicy.IGNORE)
                        .addSimplePropertyMapping("cardNumber", "bonusCardNumber"))
                .build()
        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue("customerName", "John Dow")
        importedDataItem.addRawValue("bonusCardNumber", "12345-6789")

        def bonusCard = dataManager.create(BonusCard)
        bonusCard.cardNumber = '12345-6789'
        bonusCard = dataManager.save(bonusCard)

        when: 'entity populated'
        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        customer.name == 'John Dow'
        customer.bonusCard != null
        customer.bonusCard == bonusCard
    }

    def 'test set existing existing one-to-one association from separate imported object'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Customer, "customer")
                .addSimplePropertyMapping("name", "customerName")
                .addPropertyMapping(new PropertyMapping("bonusCard", "bonusCard", ReferenceEntityPolicy.IGNORE)
                        .addSimplePropertyMapping("cardNumber", "cardNumber"))
                .build()
        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue("customerName", "John Dow")
        importedDataItem.addRawValue("bonusCard", new ImportedObject()
                .addRawValue("cardNumber", "12345-67890"));

        def bonusCard = dataManager.create(BonusCard)
        bonusCard.cardNumber = '12345-67890'
        bonusCard = dataManager.save(bonusCard)

        when: 'entity populated'
        def customer = dataManager.create(Customer)
        def entityFillingInfo = entityPopulator.populateProperties(customer, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == customer
        customer.name == 'John Dow'
        customer.bonusCard != null
        customer.bonusCard == bonusCard
    }

    def 'test creation of nested one-to-one association from imported data item'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Order, "order")
                .addSimplePropertyMapping("orderNumber", "orderNum")
                .addPropertyMapping(new PropertyMapping("paymentDetails", null, ReferenceEntityPolicy.CREATE)
                        .addSimplePropertyMapping("paymentType", "paymentType")
                        .addSimplePropertyMapping("date", "paymentDate")
                        .addSimplePropertyMapping("bonusAmount", "bonusAmount")
                        .addPropertyMapping(new PropertyMapping("bonusCard", null, ReferenceEntityPolicy.CREATE)
                                .addSimplePropertyMapping("cardNumber", "bonusCardNumber")))
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()
        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue("orderNum", "#001")
        importedDataItem.addRawValue("paymentType", "Cash")
        importedDataItem.addRawValue("paymentDate", "12/06/2021 12:00")
        importedDataItem.addRawValue("bonusCardNumber", "12345-67890")
        importedDataItem.addRawValue("bonusAmount", "0")

        when: 'entity populated'
        def order = dataManager.create(Order)
        def entityFillingInfo = entityPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        order.orderNumber == '#001'
        order.paymentDetails != null
        order.paymentDetails.paymentType == PaymentType.CASH
        order.paymentDetails.bonusAmount == 0
        order.paymentDetails.date == new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('12/06/2021 12:00')
        order.paymentDetails.bonusCard != null
        order.paymentDetails.bonusCard.cardNumber == '12345-67890'
    }

    def 'test creation of nested one-to-one association from separate imported object'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Order, "order")
                .addSimplePropertyMapping("orderNumber", "orderNum")
                .addPropertyMapping(new PropertyMapping("paymentDetails", "paymentDetails", ReferenceEntityPolicy.CREATE)
                        .addSimplePropertyMapping("paymentType", "paymentType")
                        .addSimplePropertyMapping("date", "paymentDate")
                        .addSimplePropertyMapping("bonusAmount", "bonusAmount")
                        .addPropertyMapping(new PropertyMapping("bonusCard", null, ReferenceEntityPolicy.CREATE)
                                .addSimplePropertyMapping("cardNumber", "bonusCardNumber")))
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()
        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue("orderNum", "#001")

        importedDataItem.addRawValue("paymentType", "Cash")
        importedDataItem.addRawValue("paymentDate", "12/06/2021 12:00")
        importedDataItem.addRawValue("bonusCardNumber", "12345-67890")
        importedDataItem.addRawValue("bonusAmount", "0")

        when: 'entity populated'
        def order = dataManager.create(Order)
        def entityFillingInfo = entityPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        order.orderNumber == '#001'
        order.paymentDetails != null
        order.paymentDetails.paymentType == PaymentType.CASH
        order.paymentDetails.bonusAmount == 0
        order.paymentDetails.date == new SimpleDateFormat("dd/MM/yyyy hh:mm").parse('12/06/2021 12:00')
        order.paymentDetails.bonusCard != null
        order.paymentDetails.bonusCard.cardNumber == '12345-67890'
    }
}
