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

package io.jmix.dataimport.property.binding.impl;

import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.dataimport.model.configuration.ImportConfiguration;
import io.jmix.dataimport.model.configuration.mapping.PropertyMapping;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

@Component("datimp_SimplePropertyValueProvider")
public class SimplePropertyValueProvider {
    protected final static Logger log = LoggerFactory.getLogger(SimplePropertyValueProvider.class);

    @Nullable
    public Object getValue(@Nullable Object rawValue, MetaClass entityMetaClass, PropertyMapping propertyMapping, ImportConfiguration importConfiguration) {
        MetaProperty metaProperty = entityMetaClass.getProperty(propertyMapping.getPropertyName());
        Class<?> javaType = metaProperty.getJavaType();
        Object resultValue = null;
        if (Integer.class.isAssignableFrom(javaType)) {
            resultValue = getIntegerValue(rawValue);
        } else if (Long.class.isAssignableFrom(javaType)) {
            resultValue = getLongValue(rawValue);
        } else if (Double.class.isAssignableFrom(javaType)) {
            resultValue = getDoubleValue(rawValue);
        } else if (Date.class.isAssignableFrom(javaType)) {
            resultValue = getDateValue(importConfiguration, rawValue);
        } else if (LocalDate.class.isAssignableFrom(javaType)) {
            resultValue = getLocalDateValue(importConfiguration, rawValue);
        } else if (Boolean.class.isAssignableFrom(javaType)) {
            resultValue = getBooleanValue(importConfiguration, rawValue);
        } else if (BigDecimal.class.isAssignableFrom(javaType)) {
            resultValue = getBigDecimalValue(rawValue);
        } else if (Enum.class.isAssignableFrom(javaType)) {
            resultValue = getEnumValue(rawValue, (Class<Enum>) javaType);
        } else if (isStringValue(rawValue)) {
            resultValue = rawValue;
        }
        return resultValue == null ? propertyMapping.getDefaultValue() : resultValue;
    }

    @Nullable
    protected Enum getEnumValue(Object rawValue, Class<Enum> javaType) {
        if (isStringValue(rawValue)) {
            Class<Enum> enumClass = javaType;
            String enumValue = ((String) rawValue).toUpperCase();
            if (enumClass.isEnum()) {
                try {
                    return Enum.valueOf(enumClass, enumValue);
                } catch (IllegalArgumentException e) {
                    log.info("Enum value could not be found: $value for Enum: ${enumType.simpleName}. Will be ignored");
                    log.debug("Details: ", e);
                }
            }
        }
        return null;
    }

    @Nullable
    protected BigDecimal getBigDecimalValue(Object rawValue) {
        if (isStringValue(rawValue)) {
            try {
                return new BigDecimal((String) rawValue);
            } catch (NumberFormatException e) {
                log.warn(String.format("Number could not be read: '%s' in.Will be ignored.", rawValue));
            }
        } else if (rawValue instanceof BigDecimal) {
            return (BigDecimal) rawValue;
        }
        return null;
    }

    @Nullable
    protected Double getDoubleValue(Object rawValue) {
        if (isStringValue(rawValue)) {
            try {
                return Double.parseDouble((String) rawValue);
            } catch (NumberFormatException e) {
                log.warn(String.format("Number could not be read: '%s' in .Will be ignored.", rawValue));
            }
        } else if (rawValue instanceof Double) {
            return (Double) rawValue;
        }
        return null;
    }

    @Nullable
    protected Long getLongValue(Object rawValue) {
        if (isStringValue(rawValue)) {
            try {
                return Long.parseLong((String) rawValue);
            } catch (NumberFormatException e) {
                log.warn(String.format("Number could not be read: '%s' in .Will be ignored.", rawValue));
            }
        } else if (rawValue instanceof Long) {
            return (Long) rawValue;
        }
        return null;
    }

    @Nullable
    protected Integer getIntegerValue(Object rawValue) {
        if (isStringValue(rawValue)) {
            try {
                return Integer.parseInt((String) rawValue);
            } catch (NumberFormatException e) {
                log.warn(String.format("Number could not be read: '%s' in .Will be ignored.", rawValue));
            }
        } else if (rawValue instanceof Integer) {
            return (Integer) rawValue;
        }
        return null;
    }

    protected boolean isStringValue(Object rawValue) {
        return rawValue instanceof String;
    }

    @Nullable
    protected Boolean getBooleanValue(ImportConfiguration importConfiguration, Object rawValue) {
        if (isStringValue(rawValue)) {
            String customBooleanTrueValue = importConfiguration.getBooleanTrueValue();
            String customBooleanFalseValue = importConfiguration.getBooleanFalseValue();

            if (StringUtils.isNotEmpty(customBooleanTrueValue) || StringUtils.isNotEmpty(customBooleanFalseValue)) {
                if (StringUtils.equalsIgnoreCase(customBooleanTrueValue, (String) rawValue)) {
                    return true;
                }
                return StringUtils.equalsIgnoreCase(customBooleanFalseValue, (String) rawValue) ? false : null;
            }
            try {
                return Boolean.parseBoolean((String) rawValue);
            } catch (Exception e) {
                log.warn("Boolean could not be read: '$rawValue' in [$dataRow]. Will be ignored.");
            }
        } else if (rawValue instanceof Boolean) {
            return (Boolean) rawValue;
        }
        return null;
    }

    @Nullable
    protected Date getDateValue(ImportConfiguration importConfiguration, Object rawValue) {
        if (isStringValue(rawValue)) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(importConfiguration.getDateFormat());
                return formatter.parse((String) rawValue);
            } catch (ParseException e) {
                log.warn(String.format("Date could not be read: '%' in []," +
                        " because it does not match the configured date format: '%s'. Will be ignored.", rawValue, importConfiguration.getDateFormat()));
            }
        } else if (rawValue instanceof Date) {
            return (Date) rawValue;
        }
        return null;
    }

    @Nullable
    protected LocalDate getLocalDateValue(ImportConfiguration importConfiguration, Object rawValue) {
        if (isStringValue(rawValue)) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(importConfiguration.getDateFormat());
                return LocalDate.parse((String) rawValue, formatter);
            } catch (DateTimeParseException e) {
                log.warn(String.format("Date could not be read: '%' in []," +
                        " because it does not match the configured date format: '%s'. Will be ignored.", rawValue, importConfiguration.getDateFormat()));
            }
        } else if (rawValue instanceof LocalDate) {
            return (LocalDate) rawValue;
        }
        return null;
    }
}
