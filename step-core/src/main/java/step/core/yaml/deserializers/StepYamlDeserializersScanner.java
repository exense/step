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
import step.core.scanner.CachedAnnotationScanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class StepYamlDeserializersScanner {

    /**
     * Scans and returns all {@link StepYamlDeserializer} classes annotated with {@link StepYamlDeserializerAddOn}
     */
    public static List<DeserializerBind<?>> scanDeserializerAddons(ObjectMapper yamlObjectMapper, List<Consumer<StepYamlDeserializer<?>>> configurators) {
        List<DeserializerBind<?>> result = new ArrayList<>();
        List<Class<?>> annotatedClasses = new ArrayList<>(CachedAnnotationScanner.getClassesWithAnnotation(StepYamlDeserializerAddOn.LOCATION, StepYamlDeserializerAddOn.class, Thread.currentThread().getContextClassLoader()));
        for (Class<?> annotatedClass : annotatedClasses) {
            if (StepYamlDeserializer.class.isAssignableFrom(annotatedClass)) {
                StepYamlDeserializerAddOn annotation = annotatedClass.getAnnotation(StepYamlDeserializerAddOn.class);
                Arrays.stream(annotation.targetClasses()).forEach(aClass -> {
                    try {
                        StepYamlDeserializer<Object> newDeserializer = (StepYamlDeserializer<Object>) annotatedClass.getConstructor(ObjectMapper.class).newInstance(yamlObjectMapper);
                        if(configurators != null) {
                            for (Consumer<StepYamlDeserializer<?>> configurator : configurators) {
                                configurator.accept(newDeserializer);
                            }
                        }
                        result.add(new DeserializerBind<>((Class<Object>) aClass, newDeserializer));
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot prepare deserializer", e);
                    }
                });
            }
        }

        return result;
    }

    /**
     * Scans and returns all {@link StepYamlDeserializer} classes annotated with {@link StepYamlDeserializerAddOn}
     */
    public static List<DeserializerBind<?>> scanDeserializerAddons(ObjectMapper yamlObjectMapper) {
        return scanDeserializerAddons(yamlObjectMapper, null);
    }

    public static void addAllDeserializerAddonsToModule(SimpleModule module, ObjectMapper yamlObjectMapper){
        addAllDeserializerAddonsToModule(module, yamlObjectMapper, null);
    }

    public static void addAllDeserializerAddonsToModule(SimpleModule module, ObjectMapper yamlObjectMapper, List<Consumer<StepYamlDeserializer<?>>> configurators){
        for (StepYamlDeserializersScanner.DeserializerBind<?> deser : StepYamlDeserializersScanner.scanDeserializerAddons(yamlObjectMapper, configurators)) {
            module.addDeserializer((Class<Object>) deser.clazz, deser.deserializer);
        }
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
