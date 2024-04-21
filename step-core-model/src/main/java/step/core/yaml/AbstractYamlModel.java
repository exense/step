/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

public class AbstractYamlModel {
    private static final Logger log = LoggerFactory.getLogger(AbstractYamlModel.class);

    protected void copyFieldsToObject(Object to, boolean ignoreNulls) {
        List<Field> allFieldsYaml = getAutoCopyFields();
        List<Field> allFieldsTo = ReflectionUtils.getAllFieldsInHierarchy(to.getClass(), null);

        for (Field fieldYaml : allFieldsYaml) {
            Field fieldTo = allFieldsTo.stream().filter(f -> fieldYaml.getName().equals(f.getName())).findFirst().orElse(null);
            if (fieldTo == null) {
                log.error("No target field '{}' has been found in {}. To copy the value from yaml model " +
                        "you should either have the same field in DB model or use the YamlFieldNoCopy annotation " +
                        "in yaml model and implement the custom copying", fieldYaml.getName(), to.getClass());
            } else {
                copyValue(fieldYaml, fieldTo, this, to, ignoreNulls);
            }
        }
    }

    protected void copyFieldsFromObject(Object from, boolean ignoreNulls){
        List<Field> allFieldsFrom = ReflectionUtils.getAllFieldsInHierarchy(from.getClass(), null);
        List<Field> allFieldsYaml = getAutoCopyFields();
        for (Field fieldYaml : allFieldsYaml) {
            Field fieldFrom = allFieldsFrom.stream().filter(f -> fieldYaml.getName().equals(f.getName())).findFirst().orElse(null);
            if(fieldFrom == null){
                log.error("No source field '{}' has been found in {}. To copy the value from DB model to YAML model " +
                        "you should either have the same fields in both models or use the YamlFieldNoCopy annotation " +
                        "in yaml model and implement the custom copying", fieldYaml.getName(), from.getClass());
            } else {
                copyValue(fieldFrom, fieldYaml, from, this, ignoreNulls);
            }
        }
    }

    private List<Field> getAutoCopyFields() {
        return ReflectionUtils.getAllFieldsInHierarchy(this.getClass(), AbstractYamlModel.class)
                .stream()
                .filter(f -> !f.isAnnotationPresent(YamlFieldCustomCopy.class))
                .filter(f -> !f.isAnnotationPresent(JsonIgnore.class))
                .collect(Collectors.toList());
    }

    private void copyValue(Field fieldFrom, Field fieldTo, Object from, Object to, boolean ignoreNulls) {
        fieldFrom.setAccessible(true);
        fieldTo.setAccessible(true);
        try {
            Object fieldValue = fieldFrom.get(from);
            if (fieldValue != null || !ignoreNulls) {
                fieldTo.set(to, fieldValue);
            }
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Unable to copy " + fieldFrom.getName() + " field from " + from.getClass() + " to " + to.getClass());
        }
    }


}
