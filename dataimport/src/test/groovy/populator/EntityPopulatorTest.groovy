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
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec
import test_support.entity.Order
import test_support.entity.Product

import java.text.SimpleDateFormat

class EntityPopulatorTest extends DataImportSpec {
    @Autowired
    protected ImportConfigurationBuilders configurationBuilders

    @Autowired
    protected EntityPopulator entityPopulator


    def 'test simple properties from string values'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Product, "product")
                .addSimplePropertyMapping("name", "Product Name")
                .addSimplePropertyMapping("price", "Price")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Product Name', 'Solar-One HUP Flooded Battery 48V')
        importedDataItem.addRawValue('Price', '210.55')

        when: 'entity populated'
        def product = dataManager.create(Product)
        def entityFillingInfo = entityPopulator.populateProperties(product, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == product
        entityFillingInfo.createdAssociations.size() == 0
        product.name == 'Solar-One HUP Flooded Battery 48V'
        product.price == 210.55
    }

    def 'test date property'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Order, "order")
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
        entityFillingInfo.createdAssociations.size() == 0
        order.date == orderDate
    }

    def 'test boolean property with custom format'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Product, "product")
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

    def 'test boolean property'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Product, "product")
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

    def 'test not valid number property'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Product, "product")
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

    def 'test custom value for simple property'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Product, "product")
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
        def entityFillingInfo = entityPopulator.populateProperties(specialProduct, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == specialProduct
        specialProduct.price == 0
    }

    def 'test custom value for association property'() {

    }
}
