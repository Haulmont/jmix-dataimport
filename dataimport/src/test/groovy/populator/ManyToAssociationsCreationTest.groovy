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
import test_support.entity.Customer
import test_support.entity.CustomerGrade
import test_support.entity.Order

class ManyToAssociationsCreationTest extends DataImportSpec {
    @Autowired
    protected ImportConfigurationBuilders configurationBuilders

    @Autowired
    protected EntityPopulator entityPopulator


    def 'test association creation using data from imported object'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Order, "order")
                .addPropertyMapping(new PropertyMapping("customer", "customer", ReferenceEntityPolicy.CREATE)
                        .addSimplePropertyMapping("name", "name")
                        .addSimplePropertyMapping("email", "email")
                        .addSimplePropertyMapping("grade", "grade"))
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        def customerImportedObject = new ImportedObject()
                .addRawValue("name", "John Dow")
                .addRawValue("email", "j.dow@mail.com")
                .addRawValue("grade", "Bronze")
        importedDataItem.addRawValue('customer', customerImportedObject)

        when: 'entity populated'

        def order = dataManager.create(Order)
        def entityFillingInfo = entityPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        entityFillingInfo.createdAssociations.size() == 1

        def createdCustomer = entityFillingInfo.createdAssociations.get(0).createdObject as Customer

        order.customer == createdCustomer
        createdCustomer.name == 'John Dow'
        createdCustomer.email == 'j.dow@mail.com'
        createdCustomer.grade == CustomerGrade.BRONZE
    }

    def 'test association creation using data imported data item'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Order, "order")
                .addPropertyMapping(new PropertyMapping("customer", null, ReferenceEntityPolicy.CREATE)
                        .addSimplePropertyMapping("name", "Customer Name")
                        .addSimplePropertyMapping("email", "Customer Email")
                        .addSimplePropertyMapping("grade", "Customer Grade"))
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Customer Name', "John Dow")
        importedDataItem.addRawValue('Customer Email', "j.dow@mail.com")
        importedDataItem.addRawValue('Customer Grade', "Bronze")

        when: 'entity populated'

        def order = dataManager.create(Order)
        def entityFillingInfo = entityPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        entityFillingInfo.createdAssociations.size() == 1

        def createdCustomer = entityFillingInfo.createdAssociations.get(0).createdObject as Customer
        order.customer == createdCustomer
        createdCustomer.name == 'John Dow'
        createdCustomer.email == 'j.dow@mail.com'
        createdCustomer.grade == CustomerGrade.BRONZE
    }

    def 'test ignore not existing many-to-one association using data from imported data item'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Order, "order")
                .addAssociationPropertyMapping("customer.name", "Customer Name", ReferenceEntityPolicy.IGNORE)
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Customer Name', 'John Dow')

        when: 'entity populated'
        def order = dataManager.create(Order)
        def entityFillingInfo = entityPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        entityFillingInfo.createdAssociations.size() == 0
        order.customer == null
    }

    def 'test load existing association using data from imported item'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Order, "order")
                .addAssociationPropertyMapping("customer.name", "Customer Name", ReferenceEntityPolicy.IGNORE)
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue('Customer Name', 'Parker Leighton')

        def customer = loadCustomer('Parker Leighton')

        when: 'entity populated'
        def order = dataManager.create(Order)
        def entityFillingInfo = entityPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        entityFillingInfo.createdAssociations.size() == 0
        order.customer == customer
    }

    def 'test load existing association from separate imported object'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Order, "order")
                .addPropertyMapping(new PropertyMapping("customer", "customer", ReferenceEntityPolicy.IGNORE)
                        .addSimplePropertyMapping("name", "name"))
                .build()

        def importedDataItem = new ImportedDataItem()
        def customerImportedObject = new ImportedObject()
                .addRawValue("name", "Parker Leighton")
        importedDataItem.addRawValue('customer', customerImportedObject)

        def customer = loadCustomer('Parker Leighton')

        when: 'entity populated'
        def order = dataManager.create(Order)
        def entityFillingInfo = entityPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        entityFillingInfo.createdAssociations.size() == 0
        order.customer == customer
    }
}
