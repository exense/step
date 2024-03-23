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
package step.core.yaml.serializers;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.scanner.CachedAnnotationScanner;
import step.core.yaml.deserializers.StepYamlDeserializersScanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StepYamlSerializersScanner {

    private static final Logger log = LoggerFactory.getLogger(StepYamlSerializersScanner.class);

    /**
     * Scans and returns all {@link StepYamlSerializer} classes annotated with {@link StepYamlSerializerAddOn}
     */
    public static List<SerializerBind<?>> scanSerializerAddons(ObjectMapper yamlObjectMapper) {
        List<SerializerBind<?>> result = new ArrayList<>();
        List<Class<?>> annotatedClasses = new ArrayList<>(CachedAnnotationScanner.getClassesWithAnnotation(StepYamlSerializerAddOn.LOCATION, StepYamlSerializerAddOn.class, Thread.currentThread().getContextClassLoader()));
        for (Class<?> annotatedClass : annotatedClasses) {
            if (StepYamlSerializer.class.isAssignableFrom(annotatedClass)) {
                StepYamlSerializerAddOn annotation = annotatedClass.getAnnotation(StepYamlSerializerAddOn.class);
                Arrays.stream(annotation.targetClasses()).forEach(aClass -> {
                    try {
                        StepYamlSerializer<Object> newSerializer = (StepYamlSerializer<Object>) annotatedClass.getConstructor(ObjectMapper.class).newInstance(yamlObjectMapper);
                        result.add(new SerializerBind<>((Class<Object>) aClass, newSerializer));
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot prepare serializer", e);
                    }
                });
            }
        }

        return result;
    }

    public static SimpleModule addAllSerializerAddonsToModule(SimpleModule module, ObjectMapper yamlObjectMapper){
        SimpleModule res = module;
        for (SerializerBind<?> ser : scanSerializerAddons(yamlObjectMapper)) {
            log.info("Add serializer " + ser.serializer.getClass() + " for " + ser.clazz);
            res = module.addSerializer(ser.clazz, (JsonSerializer<Object>) ser.serializer);
        }
        return res;
    }

    private static class SerializerBind<T> {
        private final Class<T> clazz;
        private final StepYamlSerializer<T> serializer;

        private SerializerBind(Class<T> clazz, StepYamlSerializer<T> serializer) {
            this.clazz = clazz;
            this.serializer = serializer;
        }
    }
}
