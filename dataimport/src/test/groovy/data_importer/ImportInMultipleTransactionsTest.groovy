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
import io.jmix.dataimport.configuration.ImportConfigurationBuilder
import io.jmix.dataimport.configuration.DuplicateEntityPolicy
import io.jmix.dataimport.configuration.ImportTransactionStrategy
import io.jmix.dataimport.configuration.mapping.ReferenceMultiFieldPropertyMapping
import io.jmix.dataimport.configuration.mapping.ReferencePropertyMapping
import io.jmix.dataimport.configuration.mapping.ReferenceImportPolicy
import io.jmix.dataimport.result.EntityImportErrorType
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.*

class ImportInMultipleTransactionsTest extends DataImportSpec {
    @Autowired
    protected DataImporter dataImporter
    @Autowired
    protected Resources resources

    def 'test successful import without references'() {
        given:
        def importConfig = new ImportConfigurationBuilder(Product, "import-product")
                .addSimplePropertyMapping("name", "name")
                .addSimplePropertyMapping("price", "price")
                .addSimplePropertyMapping("special", "special")
                .withInputDataFormat("xml")
                .withBooleanFormats("Yes", "No")
                .withTransactionStrategy(ImportTransactionStrategy.TRANSACTION_PER_ENTITY)
                .build()

        def xmlContent = resources.getResourceAsStream("/test_support/input_data_files/xml/one_product.xml")

        when: 'data imported'
        def result = dataImporter.importData(importConfig, xmlContent)

        then:
        result.success
        result.importedEntityIds.size() == 1

        def importedProduct = dataManager.load(Product)
                .id(result.importedEntityIds.get(0))
                .fetchPlan(FetchPlan.LOCAL)
                .one() as Product
        checkProduct(importedProduct, 'Cotek Battery Charger', 30.1, false)
    }

    def 'test import with existing and new one-to-one references'() {
        given:
        def importConfig = new ImportConfigurationBuilder(Customer, "import-customer-with-bonus-cards")
                .addSimplePropertyMapping("name", "name")
                .addSimplePropertyMapping("email", "email")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('bonusCard')
                        .withDataFieldName('bonusCard')
                        .withReferenceImportPolicy(ReferenceImportPolicy.CREATE_IF_MISSING)
                        .addSimplePropertyMapping("cardNumber", "number")
                        .addSimplePropertyMapping("isActive", "isActive")
                        .addSimplePropertyMapping("balance", "balance")
                        .lookupByAllSimpleProperties()
                        .build())
                .withInputDataFormat("json")
                .withTransactionStrategy(ImportTransactionStrategy.TRANSACTION_PER_ENTITY)
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

        def customer1 = loadEntity(Customer, result.importedEntityIds.get(0), "customer-with-bonus-card") as Customer
        checkBonusCard(customer1.bonusCard, '12345-67890', true, 100 as BigDecimal)

        def customer2 = loadEntity(Customer, result.importedEntityIds.get(1), "customer-with-bonus-card") as Customer
        customer2.bonusCard != null
        customer2.bonusCard == bonusCard
    }

    def 'test import with existing and new many-to-one reference'() {
        given:
        def importConfig = new ImportConfigurationBuilder(Order, "import-orders")
                .addSimplePropertyMapping("orderNumber", "Order Num")
                .addSimplePropertyMapping("date", "Order Date")
                .addSimplePropertyMapping("amount", "Order Amount")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('customer')
                        .withReferenceImportPolicy(ReferenceImportPolicy.CREATE_IF_MISSING)
                        .addSimplePropertyMapping("name", "Customer Name")
                        .addSimplePropertyMapping("email", "Customer Email")
                        .lookupByAllSimpleProperties()
                        .build())
                .withInputDataFormat("csv")
                .withDateFormat('dd/MM/yyyy HH:mm')
                .withTransactionStrategy(ImportTransactionStrategy.TRANSACTION_PER_ENTITY)
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

        def order1 = loadEntity(Order, result.importedEntityIds.get(0), "order-with-customer") as Order
        order1.customer != null
        order1.customer == customer

        def order2 = loadEntity(Order, result.importedEntityIds.get(1), "order-with-customer") as Order
        order2.customer != null
        order2.customer == customer

        def order3 = loadEntity(Order, result.importedEntityIds.get(2), "order-with-customer") as Order
        order3.customer != null
        checkCustomer(order3.customer, 'Tom Smith', 't.smith@mail.com', null)
    }

    def 'test import with one-to-many association'() {
        given:
        def importConfig = new ImportConfigurationBuilder(Customer, "import-customers")
                .addSimplePropertyMapping("name", "Customer Name")
                .addSimplePropertyMapping("email", "Customer Email")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('orders')
                        .withReferenceImportPolicy(ReferenceImportPolicy.CREATE)
                        .addSimplePropertyMapping("orderNumber", "Order Num")
                        .addSimplePropertyMapping("date", "Order Date")
                        .addSimplePropertyMapping("amount", "Order Amount")
                        .lookupByAllSimpleProperties()
                        .build())
                .withInputDataFormat("csv")
                .withDateFormat('dd/MM/yyyy HH:mm')
                .withTransactionStrategy(ImportTransactionStrategy.TRANSACTION_PER_ENTITY)
                .build()

        def csvContent = resources.getResourceAsStream("/test_support/input_data_files/csv/orders.csv")

        when: 'data imported'
        def result = dataImporter.importData(importConfig, csvContent)

        then:
        result.success
        result.importedEntityIds.size() == 3

        def customer1 = loadEntity(Customer, result.importedEntityIds.get(0), "customer-with-orders") as Customer
        customer1.orders != null
        customer1.orders.size() == 1
        checkOrder(customer1.orders.get(0), '#123', '12/12/2020 12:30', null)

        def customer2 = loadEntity(Customer, result.importedEntityIds.get(1), "customer-with-orders") as Customer
        customer2.orders != null
        customer2.orders.size() == 1
        checkOrder(customer2.orders.get(0), '#4567', '03/05/2021 14:00', null)

        def customer3 = loadEntity(Customer, result.importedEntityIds.get(2), "customer-with-orders") as Customer
        customer3.orders != null
        customer3.orders.size() == 1
        checkOrder(customer3.orders.get(0), '#237', '02/04/2021 10:00', null)
    }

    def 'test import with nested references from Excel'() {
        given:
        def importConfig = new ImportConfigurationBuilder(Order, "import-order")
                .addSimplePropertyMapping("orderNumber", "Order Number")
                .addSimplePropertyMapping("date", "Order Date")
                .addSimplePropertyMapping("amount", "Order Amount")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('customer') //mapping for customer
                        .withReferenceImportPolicy(ReferenceImportPolicy.CREATE_IF_MISSING)
                        .addSimplePropertyMapping('name', "Customer Name")
                        .addSimplePropertyMapping("email", "Customer Email")
                        .lookupByAllSimpleProperties()
                        .build())
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('paymentDetails')
                        .withReferenceImportPolicy(ReferenceImportPolicy.CREATE)
                        .addSimplePropertyMapping("date", "Payment Date")
                        .addCustomPropertyMapping("paymentType", "Payment Type", customValueContext -> {
                            String paymentType = customValueContext.getRawValue() as String
                            paymentType = paymentType.replace("\\s+", "_")
                            return PaymentType.fromId(paymentType);
                        })
                        .addSimplePropertyMapping("bonusAmount", "Bonus Amount")
                        .addPropertyMapping(ReferencePropertyMapping.byEntityPropertyName('bonusCard')
                                .withDataFieldName('Bonus Card Number')
                                .withLookupPropertyName('cardNumber')
                                .withReferenceImportPolicy(ReferenceImportPolicy.IGNORE_IF_MISSING)
                                .build())
                        .build())
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder("lines")
                        .withReferenceImportPolicy(ReferenceImportPolicy.CREATE) //mapping for order lines
                        .addReferencePropertyMapping("product", "name", "Product Name",
                                ReferenceImportPolicy.IGNORE_IF_MISSING)
                        .addSimplePropertyMapping("quantity", "Quantity")
                        .build())
                .withInputDataFormat("xlsx")
                .withTransactionStrategy(ImportTransactionStrategy.TRANSACTION_PER_ENTITY)
                .addUniqueEntityConfiguration(DuplicateEntityPolicy.UPDATE, "orderNumber", "date", "amount")
                .withDateFormat("dd/MM/yyyy HH:mm")
                .build()

        def excelInputStream = resources.getResourceAsStream("/test_support/input_data_files/xlsx/orders_with_customers_and_lines.xlsx")

        when: 'data imported'
        def result = dataImporter.importData(importConfig, excelInputStream)

        then:
        result.success
        result.numOfProcessedEntities == 4
        result.importedEntityIds.size() == 3

        def order1 = loadEntity(Order, result.importedEntityIds.get(0), "order-full") as Order
        order1.customer != null
        checkOrder(order1, '#001', '12/02/2021 12:00', 50.5)
        checkCustomer(order1.customer, 'Mike Spencer', 'm.spencer@mail.com', null)
        checkPaymentDetails(order1.paymentDetails, '12/02/2021 12:00', PaymentType.CASH, null, 10 as BigDecimal)

        order1.lines != null
        order1.lines.size() == 2
        order1.lines.sort(orderLine -> orderLine.quantity)
        checkOrderLine(order1.lines.get(0), 'Outback Power Nano-Carbon Battery 12V', 4)
        checkOrderLine(order1.lines.get(1), 'Fullriver Sealed Battery 6V', 5)

        def order2 = loadEntity(Order, result.importedEntityIds.get(1), "order-full") as Order
        checkOrder(order2, '#123', '23/03/2021 18:00', 6.25)
        checkCustomer(order2.customer, 'Tom Smith', 't.smith@mail.com', null)
        checkPaymentDetails(order2.paymentDetails, '23/03/2021 18:00', PaymentType.CREDIT_CARD, null, null)

        order2.lines != null
        order2.lines.size() == 1
        checkOrderLine(order2.lines.get(0), 'Outback Power Nano-Carbon Battery 12V', 1)
    }

    def 'test pre-import predicate'() {
        given:
        def importConfig = new ImportConfigurationBuilder(Order, "import-order")
                .addSimplePropertyMapping("orderNumber", "Order Num")
                .addSimplePropertyMapping("date", "Order Date")
                .addSimplePropertyMapping("amount", "Order Amount")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('customer')
                        .withReferenceImportPolicy(ReferenceImportPolicy.IGNORE_IF_MISSING)
                        .addSimplePropertyMapping('name', 'Customer Name')
                        .addSimplePropertyMapping('email', 'Customer Email')
                        .lookupByAllSimpleProperties()
                        .build())
                .withInputDataFormat("xlsx")
                .withTransactionStrategy(ImportTransactionStrategy.TRANSACTION_PER_ENTITY)
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
        result.numOfProcessedEntities == 3
        result.importedEntityIds.size() == 0
        result.failedEntities.size() == 3
        result.failedEntities.get(0).errorType == EntityImportErrorType.VALIDATION
        result.failedEntities.get(1).errorType == EntityImportErrorType.VALIDATION
        result.failedEntities.get(2).errorType == EntityImportErrorType.VALIDATION
    }

    def 'test entity validation exception'() {
        given:
        def importConfig = new ImportConfigurationBuilder(OrderLine, "import-lines")
                .addPropertyMapping(ReferenceMultiFieldPropertyMapping.builder('order')
                        .withReferenceImportPolicy(ReferenceImportPolicy.CREATE_IF_MISSING)
                        .addSimplePropertyMapping("orderNumber", "orderNumber")
                        .addSimplePropertyMapping('amount', 'orderAmount')
                        .addSimplePropertyMapping('date', 'orderDate')
                        .lookupByAllSimpleProperties()
                        .build())
                .addReferencePropertyMapping('product', 'name', "productName", ReferenceImportPolicy.IGNORE_IF_MISSING)
                .addSimplePropertyMapping("quantity", "quantity")
                .withDateFormat('dd/MM/yyyy HH:mm')
                .withInputDataFormat("xml")
                .withTransactionStrategy(ImportTransactionStrategy.TRANSACTION_PER_ENTITY)
                .build()
        InputStream xmlContent = resources.getResourceAsStream("/test_support/input_data_files/xml/order_lines.xml")

        when: 'data imported'
        def importResult = dataImporter.importData(importConfig, xmlContent)

        then:
        !importResult.success
        importResult.numOfProcessedEntities == 2
        importResult.importedEntityIds.size() == 1
        importResult.failedEntities.size() == 1

        def orderLine = loadEntity(OrderLine, importResult.importedEntityIds.get(0), 'orderLine-full') as OrderLine
        checkOrderLine(orderLine, 'Outback Power Nano-Carbon Battery 12V', 4)
        checkOrder(orderLine.order, '#002', '28/06/2021 12:00', 25)

        def entityImportError = importResult.failedEntities.get(0)
        entityImportError.errorType == EntityImportErrorType.VALIDATION
        def failedOrderLine = entityImportError.entity as OrderLine
        checkOrderLine(failedOrderLine, null, 1)
        checkOrder(failedOrderLine.order, '#001', '24/06/2021 10:00', 210.55)
    }
}
