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
package step.core.yaml.deserializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.scanner.CachedAnnotationScanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StepYamlDeserializersScanner {

    private static final Logger log = LoggerFactory.getLogger(StepYamlDeserializersScanner.class);

    /**
     * Scans and returns all {@link StepYamlDeserializer} classes annotated with {@link StepYamlDeserializerAddOn}
     */
    public static List<DeserializerBind<?>> scanDeserializerAddons(ObjectMapper yamlObjectMapper) {
        List<DeserializerBind<?>> result = new ArrayList<>();
        List<Class<?>> annotatedClasses = new ArrayList<>(CachedAnnotationScanner.getClassesWithAnnotation(StepYamlDeserializerAddOn.LOCATION, StepYamlDeserializerAddOn.class, Thread.currentThread().getContextClassLoader()));
        for (Class<?> annotatedClass : annotatedClasses) {
            if (StepYamlDeserializer.class.isAssignableFrom(annotatedClass)) {
                StepYamlDeserializerAddOn annotation = annotatedClass.getAnnotation(StepYamlDeserializerAddOn.class);
                Arrays.stream(annotation.targetClasses()).forEach(aClass -> {
                    try {
                        StepYamlDeserializer<Object> newDeserializer = (StepYamlDeserializer<Object>) annotatedClass.getConstructor(ObjectMapper.class).newInstance(yamlObjectMapper);
                        result.add(new DeserializerBind<>((Class<Object>) aClass, newDeserializer));
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot prepare deserializer", e);
                    }
                });
            }
        }

        return result;
    }

    public static SimpleModule addAllDeserializerAddonsToModule(SimpleModule module, ObjectMapper yamlObjectMapper){
        SimpleModule res = module;
        for (StepYamlDeserializersScanner.DeserializerBind<?> deser : StepYamlDeserializersScanner.scanDeserializerAddons(yamlObjectMapper)) {
            log.info("Add deserializer " + deser.deserializer.getClass() + " for " + deser.clazz);
            res = module.addDeserializer((Class<Object>) deser.clazz, deser.deserializer);
        }
        return res;
    }

    public static class DeserializerBind<T> {
        public Class<T> clazz;
        public StepYamlDeserializer<T> deserializer;

        public DeserializerBind(Class<T> clazz, StepYamlDeserializer<T> deserializer) {
            this.clazz = clazz;
            this.deserializer = deserializer;
        }
    }
}
