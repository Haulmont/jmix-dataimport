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

package extractor.data

import io.jmix.core.Resources
import io.jmix.dataimport.extractor.data.impl.CsvDataExtractor
import io.jmix.dataimport.model.configuration.ImportConfiguration
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataImportSpec

class CsvDataExtractorTest extends DataImportSpec {
    @Autowired
    protected CsvDataExtractor csvDataExtractor;

    @Autowired
    protected Resources resources;

    def "test imported data from input stream"() {
        given:
        def inputStream = resources.getResourceAsStream("test_support/input_data_files/csv/products.csv")

        ImportConfiguration importConfiguration = new ImportConfiguration("sales_Product", "products-from-csv");

        when: 'imported data extracted'
        def importedData = csvDataExtractor.extract(inputStream, importConfiguration)

        then:
        importedData.fieldNames.size() == 3
        importedData.fieldNames == ['Product Name', 'Special', 'Price']

        importedData.items.size() == 2
        def firstProduct = importedData.items.get(0)
        firstProduct.itemIndex == 1
        firstProduct.rawValues.size() == 3
        firstProduct.getRawValue( 'Product Name') == 'Outback Power Nano-Carbon Battery 12V'
        firstProduct.getRawValue( 'Special') == 'Yes'
        firstProduct.getRawValue( 'Price') == '6.25'

        def secondProduct = importedData.items.get(1)
        secondProduct.itemIndex == 2
        secondProduct.rawValues.size() == 3
        secondProduct.getRawValue( 'Product Name') == 'Fullriver Sealed Battery 6V'
        secondProduct.getRawValue( 'Special') == 'No'
        secondProduct.getRawValue( 'Price') == '5.10'
    }

    def "test imported data from byte array"() {
        given:
        def inputBytes = IOUtils.toByteArray(resources.getResourceAsStream("test_support/input_data_files/csv/products.csv"))

        ImportConfiguration importConfiguration = new ImportConfiguration("sales_Product", "products-from-csv")

        when: 'imported data extracted'
        def importedData = csvDataExtractor.extract(inputBytes, importConfiguration)

        then:
        importedData.fieldNames.size() == 3
        importedData.fieldNames == ['Product Name', 'Special', 'Price']

        importedData.items.size() == 2
        def firstProduct = importedData.items.get(0)
        firstProduct.itemIndex == 1
        firstProduct.rawValues.size() == 3
        firstProduct.getRawValue( 'Product Name') == 'Outback Power Nano-Carbon Battery 12V'
        firstProduct.getRawValue( 'Special') == 'Yes'
        firstProduct.getRawValue( 'Price') == '6.25'

        def secondProduct = importedData.items.get(1)
        secondProduct.itemIndex == 2
        secondProduct.rawValues.size() == 3
        secondProduct.getRawValue( 'Product Name') == 'Fullriver Sealed Battery 6V'
        secondProduct.getRawValue( 'Special') == 'No'
        secondProduct.getRawValue( 'Price') == '5.10'
    }

    def "test import from string"() {
        given:
        def csvString = resources.getResourceAsString("test_support/input_data_files/csv/products.csv")

        when: 'imported data extracted'
        def importedData = csvDataExtractor.extract(csvString)

        then:
        importedData.fieldNames.size() == 3
        importedData.fieldNames == ['Product Name', 'Special', 'Price']

        importedData.items.size() == 2
        def firstProduct = importedData.items.get(0)
        firstProduct.itemIndex == 1
        firstProduct.rawValues.size() == 3
        firstProduct.getRawValue( 'Product Name') == 'Outback Power Nano-Carbon Battery 12V'
        firstProduct.getRawValue( 'Special') == 'Yes'
        firstProduct.getRawValue( 'Price') == '6.25'

        def secondProduct = importedData.items.get(1)
        secondProduct.itemIndex == 2
        secondProduct.rawValues.size() == 3
        secondProduct.getRawValue( 'Product Name') == 'Fullriver Sealed Battery 6V'
        secondProduct.getRawValue( 'Special') == 'No'
        secondProduct.getRawValue( 'Price') == '5.10'
    }
}
