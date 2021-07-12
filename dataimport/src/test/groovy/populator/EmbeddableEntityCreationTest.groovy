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
import test_support.entity.Order

import java.text.SimpleDateFormat

class EmbeddableEntityCreationTest extends DataImportSpec {
    @Autowired
    protected ImportConfigurationBuilders configurationBuilders

    @Autowired
    protected EntityPopulator entityPopulator

    def 'test embeddable entity creation using data from imported object'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Order, "order")
                .addPropertyMapping(new PropertyMapping("deliveryDetails", "deliveryDetails", ReferenceEntityPolicy.CREATE)
                        .addSimplePropertyMapping("deliveryDate", "deliveryDate")
                        .addSimplePropertyMapping("fullAddress", "fullAddress"))
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        def deliveryDetailsImportedObject = new ImportedObject()
                .addRawValue("deliveryDate", '25/06/2021 17:00')
                .addRawValue("fullAddress", null)
        importedDataItem.addRawValue('deliveryDetails', deliveryDetailsImportedObject)

        when: 'entity populated'
        def order = dataManager.create(Order)
        def entityFillingInfo = entityPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        entityFillingInfo.createdAssociations.size() == 0
        order.deliveryDetails != null
        order.deliveryDetails.deliveryDate == new SimpleDateFormat('dd/MM/yyyy hh:mm').parse('25/06/2021 17:00')
        order.deliveryDetails.fullAddress == null
    }

    def 'test embeddable entity creation using data from imported data item'() {
        given:
        def configuration = configurationBuilders.byEntityClass(Order, "order")
                .addPropertyMapping(new PropertyMapping("deliveryDetails", null, ReferenceEntityPolicy.CREATE)
                        .addSimplePropertyMapping("deliveryDate", "deliveryDate")
                        .addSimplePropertyMapping("fullAddress", "fullAddress"))
                .withDateFormat("dd/MM/yyyy hh:mm")
                .build()

        def importedDataItem = new ImportedDataItem()
        importedDataItem.addRawValue("deliveryDate", '25/06/2021 17:00')
        importedDataItem.addRawValue("fullAddress", null)

        when: 'entity populated'
        def order = dataManager.create(Order)
        def entityFillingInfo = entityPopulator.populateProperties(order, configuration, importedDataItem)

        then:
        entityFillingInfo.entity == order
        entityFillingInfo.createdAssociations.size() == 0
        order.deliveryDetails != null
        order.deliveryDetails.deliveryDate == new SimpleDateFormat('dd/MM/yyyy hh:mm').parse('25/06/2021 17:00')
        order.deliveryDetails.fullAddress == null
    }
}
