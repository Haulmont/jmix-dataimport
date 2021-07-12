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

package test_support

import io.jmix.core.DataManager
import io.jmix.core.FetchPlans
import io.jmix.dataimport.extractor.data.ImportedDataItem
import io.jmix.dataimport.extractor.data.ImportedObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import test_support.entity.Customer
import test_support.entity.CustomerGrade
import test_support.entity.Order
import test_support.entity.Product

import javax.annotation.Nullable

@ContextConfiguration(classes = [DataImportTestConfiguration])
class DataImportSpec extends Specification {
    @Autowired
    DataManager dataManager

    @Autowired
    JdbcTemplate jdbcTemplate

    @Autowired
    FetchPlans fetchPlans

    void setup() {
        createCustomers()
        createProducts()
    }

    private void createCustomers() {
        def customer1 = dataManager.create(Customer)
        customer1.name = 'Parker Leighton'
        customer1.email = 'leighton@mail.com'
        customer1.grade = CustomerGrade.BRONZE

        def customer2 = dataManager.create(Customer)
        customer2.name = 'Shelby Robinson'
        customer2.email = 'robinson@mail.com'
        customer2.grade = CustomerGrade.SILVER

        dataManager.save(customer1, customer2)
    }

    private void createProducts() {
        def product1 = dataManager.create(Product)
        product1.name = 'Outback Power Nano-Carbon Battery 12V'
        product1.price = 6.25
        product1.special = true

        def product2 = dataManager.create(Product)
        product2.name = 'Fullriver Sealed Battery 6V'
        product2.price = 5.10
        product2.special = false

        dataManager.save(product1, product2)
    }

    void cleanup() {
        jdbcTemplate.update('delete from SALES_ORDER_LINE')
        jdbcTemplate.update('delete from SALES_PAYMENT_DETAILS')
        jdbcTemplate.update('delete from SALES_ORDER')
        jdbcTemplate.update('delete from SALES_BONUS_CARD')
        jdbcTemplate.update('delete from SALES_CUSTOMER')
        jdbcTemplate.update('delete from SALES_PRODUCT')
    }

    protected ImportedObject createImportedObject(Map<String, Object> rawValues) {
        return new ImportedObject().setRawValues(rawValues)
    }

    protected Customer loadCustomer(String name) {
        return dataManager.load(Customer)
                .query("e.name = :name")
                .parameter("name", name)
                .optional().orElse(null)
    }

    @Nullable
    protected Order loadOrder(Object id, String fetchPlan) {
        return dataManager.load(Order)
                .id(id)
                .fetchPlan(fetchPlan)
                .optional()
                .orElse(null)
    }
}
