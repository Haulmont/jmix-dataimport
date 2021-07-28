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

import com.google.common.collect.ImmutableMap
import io.jmix.core.FetchPlan
import io.jmix.core.Resources
import io.jmix.dataimport.DataImporter
import io.jmix.dataimport.InputDataFormat
import io.jmix.dataimport.configuration.ImportConfiguration
import io.jmix.dataimport.configuration.ImportConfigurationBuilder
import io.jmix.dataimport.configuration.ImportTransactionStrategy
import io.jmix.dataimport.configuration.mapping.ReferenceImportPolicy
import io.jmix.dataimport.configuration.mapping.ReferenceMultiFieldPropertyMapping
import io.jmix.dataimport.configuration.mapping.ReferencePropertyMapping
import io.jmix.dataimport.result.EntityImportErrorType
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.*

class ImportInOneTransactionTest extends DataImportSpec {
    @Autowired
    protected DataImporter dataImporter
    @Autowired
    protected Resources resources

    def 'test successful import without references'() {
        given:
        def importConfig = ImportConfiguration.builder(Product, InputDataFormat.XML)
                .addSimplePropertyMapping("name", "name")
                .addSimplePropertyMapping("price", "price")
                .addSimplePropertyMapping("special", "special")
                .withBooleanFormats("Yes", "No")
                .withTransactionStrategy(ImportTransactionStrategy.SINGLE_TRANSACTION)
                .build()

        def xmlContent = resources.getResourceAsStream("/test_support/input_data_files/xml/one_product.xml")

        when: 'data imported'
        def result = dataImporter.importData(importConfig, xmlContent)

        then:
        result.success
        result.importedEntityIds.size() == 1

        def importedProduct = dataManager.load(Product)
                .id(result.importedEntityIds[0])
                .fetchPlan(FetchPlan.LOCAL)
                .one() as Product
        checkProduct(importedProduct, 'Cotek Battery Charger', 30.1, false)
    }

    def 'test import with existing and new one-to-one references'() {
        given:
        def importConfig = ImportConfiguration.builder(Customer, InputDataFormat.JSON)
                .addSimplePropertyMapping("name", "name")
                .addSimplePropertyMapping("email", "email")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder("bonusCard", ReferenceImportPolicy.CREATE_IF_MISSING)
                        .withDataFieldName('bonusCard')
                        .addSimplePropertyMapping("cardNumber", "number")
                        .addSimplePropertyMapping("isActive", "isActive")
                        .addSimplePropertyMapping("balance", "balance")
                        .lookupByAllSimpleProperties()
                        .build())
                .withTransactionStrategy(ImportTransactionStrategy.SINGLE_TRANSACTION)
                .build()

        def jsonContent = resources.getResourceAsStream("/test_support/input_data_files/json/customers_and_bonus_cards.json")

        def bonusCard = dataManager.create(BonusCard)
        bonusCard.cardNumber = '67890-12345'
        bonusCard.isActive = false
        bonusCard.balance = 0
        bonusCard = dataManager.save(bonusCard)

        when: 'data imported'
        def result = dataImporter.importData(importConfig, jsonContent)

        then:
        result.success
        result.importedEntityIds.size() == 2

        def customer1 = loadEntity(Customer, result.importedEntityIds[0], "customer-with-bonus-card") as Customer
        checkBonusCard(customer1.bonusCard, '12345-67890', true, 100 as BigDecimal)

        def customer2 = loadEntity(Customer, result.importedEntityIds[1], "customer-with-bonus-card") as Customer
        customer2.bonusCard != null
        customer2.bonusCard == bonusCard
    }

    def 'test import with existing and new many-to-one references'() {
        given:
        def importConfig = ImportConfiguration.builder(Order, InputDataFormat.CSV)
                .addSimplePropertyMapping("orderNumber", "Order Num")
                .addSimplePropertyMapping("date", "Order Date")
                .addSimplePropertyMapping("amount", "Order Amount")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder("customer", ReferenceImportPolicy.CREATE_IF_MISSING)
                        .addSimplePropertyMapping("name", "Customer Name")
                        .addSimplePropertyMapping("email", "Customer Email")
                        .lookupByAllSimpleProperties()
                        .build())
                .withDateFormat('dd/MM/yyyy HH:mm')
                .withTransactionStrategy(ImportTransactionStrategy.SINGLE_TRANSACTION)
                .build()

        def csvContent = resources.getResourceAsStream("/test_support/input_data_files/csv/orders.csv")

        def customer = dataManager.create(Customer)
        customer.name = 'John Dow'
        customer = dataManager.save(customer)

        when: 'data imported'
        def result = dataImporter.importData(importConfig, csvContent)

        then:
        result.success
        result.importedEntityIds.size() == 3

        def order1 = loadEntity(Order, result.importedEntityIds[0], "order-with-customer") as Order
        order1.customer != null
        order1.customer == customer

        def order2 = loadEntity(Order, result.importedEntityIds[1], "order-with-customer") as Order
        order2.customer != null
        order2.customer == customer

        def order3 = loadEntity(Order, result.importedEntityIds[2], "order-with-customer") as Order
        order3.customer != null
        checkCustomer(order3.customer, 'Tom Smith', 't.smith@mail.com', null)
    }

    def 'test import with one-to-many composition for new entity to import'() {
        given:
        def importConfig = ImportConfiguration.builder(Customer, InputDataFormat.CSV)
                .addSimplePropertyMapping("name", "Customer Name")
                .addSimplePropertyMapping("email", "Customer Email")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder("orders", ReferenceImportPolicy.CREATE)
                        .addSimplePropertyMapping("orderNumber", "Order Num")
                        .addSimplePropertyMapping("date", "Order Date")
                        .addSimplePropertyMapping("amount", "Order Amount")
                        .build())
                .withDateFormat('dd/MM/yyyy HH:mm')
                .withTransactionStrategy(ImportTransactionStrategy.SINGLE_TRANSACTION)
                .build()

        def csvContent = resources.getResourceAsStream("/test_support/input_data_files/csv/orders.csv")

        when: 'data imported'
        def result = dataImporter.importData(importConfig, csvContent)

        then:
        result.success
        result.importedEntityIds.size() == 2

        def customer1 = loadEntity(Customer, result.importedEntityIds[0], "customer-with-orders") as Customer
        customer1.orders != null
        customer1.orders.size() == 2
        customer1.orders.sort(order -> order.orderNumber)
        checkOrder(customer1.orders[0], '#123', '12/12/2020 12:30', null)
        checkOrder(customer1.orders[1], '#4567', '03/05/2021 14:00', null)

        def customer2 = loadEntity(Customer, result.importedEntityIds[1], "customer-with-orders") as Customer
        customer2.orders != null
        customer2.orders.size() == 1
        checkOrder(customer2.orders[0], '#237', '02/04/2021 10:00', null)
    }

    def 'test import with pre-import predicate'() {
        given:
        def importConfig = ImportConfiguration.builder(Order, InputDataFormat.XLSX)
                .addSimplePropertyMapping("orderNumber", "Order Num")
                .addSimplePropertyMapping("date", "Order Date")
                .addSimplePropertyMapping("amount", "Order Amount")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('customer', ReferenceImportPolicy.IGNORE_IF_MISSING)
                        .addSimplePropertyMapping('name', 'Customer Name')
                        .addSimplePropertyMapping('email', 'Customer Email')
                        .lookupByAllSimpleProperties()
                        .build())
                .withTransactionStrategy(ImportTransactionStrategy.SINGLE_TRANSACTION)
                .withDateFormat("dd/MM/yyyy HH:mm")
                .withPreImportPredicate(entityExtractionResult -> {
                    def order = entityExtractionResult.getEntity() as Order
                    return order.customer != null
                })
                .build()

        def excelInputStream = resources.getResourceAsStream("/test_support/input_data_files/xlsx/orders.xlsx")

        when: 'data imported'
        def result = dataImporter.importData(importConfig, excelInputStream)

        then:
        result.success
        result.importedEntityIds.size() == 0
        result.failedEntities.size() == 3
        result.failedEntities[0].errorType == EntityImportErrorType.VALIDATION
        result.failedEntities[1].errorType == EntityImportErrorType.VALIDATION
        result.failedEntities[2].errorType == EntityImportErrorType.VALIDATION
    }

    def 'test import with nested references from Excel'() {
        def importConfig = ImportConfiguration.builder(Order, InputDataFormat.XLSX)
                .addSimplePropertyMapping("orderNumber", "Order Number")
                .addSimplePropertyMapping("date", "Order Date")
                .addSimplePropertyMapping("amount", "Order Amount")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('customer', ReferenceImportPolicy.CREATE_IF_MISSING)
                        .addSimplePropertyMapping('name', 'Customer Name')
                        .addSimplePropertyMapping('email', 'Customer Email')
                        .lookupByAllSimpleProperties()
                        .build())
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('paymentDetails', ReferenceImportPolicy.CREATE)
                        .addSimplePropertyMapping("date", "Payment Date")
                        .addCustomPropertyMapping("paymentType", customValueContext -> {
                            String paymentType = customValueContext.getRawValues().get("Payment Type") as String
                            paymentType = paymentType.replace("\\s+", "_")
                            return PaymentType.fromId(paymentType);
                        })
                        .addSimplePropertyMapping("bonusAmount", "Bonus Amount")
                        .addReferencePropertyMapping('bonusCard', 'Bonus Card Number', 'cardNumber', ReferenceImportPolicy.IGNORE_IF_MISSING)
                        .build())
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder("lines", ReferenceImportPolicy.CREATE)//mapping for order lines
                        .addReferencePropertyMapping("product", "Product Name", "name",
                                ReferenceImportPolicy.IGNORE_IF_MISSING)
                        .addSimplePropertyMapping("quantity", "Quantity")
                        .lookupByAllSimpleProperties()
                        .build())
                .withTransactionStrategy(ImportTransactionStrategy.SINGLE_TRANSACTION)
                .withDateFormat("dd/MM/yyyy HH:mm")
                .build()

        def excelInputStream = resources.getResourceAsStream("/test_support/input_data_files/xlsx/orders_with_customers_and_lines.xlsx")

        when: 'data imported'
        def result = dataImporter.importData(importConfig, excelInputStream)

        then:
        result.success
        result.importedEntityIds.size() == 3

        def order1 = loadEntity(Order, result.importedEntityIds[0], "order-full") as Order
        order1.customer != null
        checkOrder(order1, '#001', '12/02/2021 12:00', 50.5)
        checkCustomer(order1.customer, 'Mike Spencer', 'm.spencer@mail.com', null)
        checkPaymentDetails(order1.paymentDetails, '12/02/2021 12:00', PaymentType.CASH, null, 10 as BigDecimal)

        order1.lines != null
        order1.lines.size() == 2
        order1.lines.sort(orderLine -> orderLine.quantity)
        checkOrderLine(order1.lines[0], 'Outback Power Nano-Carbon Battery 12V', 4)
        checkOrderLine(order1.lines[1], 'Fullriver Sealed Battery 6V', 5)

        def order2 = loadEntity(Order, result.importedEntityIds[1], "order-full") as Order
        checkOrder(order2, '#123', '23/03/2021 18:00', 6.25)
        checkCustomer(order2.customer, 'Tom Smith', 't.smith@mail.com', null)
        checkPaymentDetails(order2.paymentDetails, '23/03/2021 18:00', PaymentType.CREDIT_CARD, null, null)

        order2.lines != null
        order2.lines.size() == 1
        checkOrderLine(order2.lines[0], 'Outback Power Nano-Carbon Battery 12V', 1)
    }

    def 'test entity validation exception'() {
        given:
        def importConfig = ImportConfiguration.builder(OrderLine, InputDataFormat.XML)
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('order', ReferenceImportPolicy.CREATE_IF_MISSING)
                        .addSimplePropertyMapping('orderNumber', 'orderNumber')
                        .addSimplePropertyMapping('date', 'orderDate')
                        .addSimplePropertyMapping('amount', 'orderAmount')
                        .lookupByAllSimpleProperties()
                        .build()
                )
                .addReferencePropertyMapping('product', 'name', 'productName', ReferenceImportPolicy.IGNORE_IF_MISSING)
                .addSimplePropertyMapping("quantity", "quantity")
                .withDateFormat('dd/MM/yyyy HH:mm')
                .withTransactionStrategy(ImportTransactionStrategy.SINGLE_TRANSACTION)
                .build()
        InputStream xmlContent = resources.getResourceAsStream("/test_support/input_data_files/xml/order_lines.xml")

        when: 'data imported'
        def importResult = dataImporter.importData(importConfig, xmlContent)

        then:
        !importResult.success
        importResult.importedEntityIds.size() == 0
        importResult.failedEntities.size() == 0
        importResult.errorMessage != null
    }

    def 'test failed import with FAIL_IF_MISSING import policy'() {
        given:
        def importConfig = ImportConfiguration.builder(OrderLine, InputDataFormat.XML)
                .addSimplePropertyMapping("quantity", "quantity")
                .addReferencePropertyMapping("product", "name", "productName", ReferenceImportPolicy.FAIL_IF_MISSING)
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('order', ReferenceImportPolicy.CREATE)
                        .addSimplePropertyMapping('date', 'orderDate')
                        .addSimplePropertyMapping('orderNumber', 'orderNumber')
                        .addSimplePropertyMapping('amount', 'orderAmount')
                        .build())
                .withDateFormat('dd/MM/yyyy HH:mm')
                .withTransactionStrategy(ImportTransactionStrategy.SINGLE_TRANSACTION)
                .build()

        def xmlContent = resources.getResourceAsStream("/test_support/input_data_files/xml/order_lines.xml")

        when: 'data imported'
        def result = dataImporter.importData(importConfig, xmlContent)

        then:
        !result.success
        result.failedEntities.size() == 0
        result.importedEntityIds.size() == 0
        result.errorMessage != null
    }
}
