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
package step.automation.packages;

import step.attachments.FileResolver;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ChildrenBlock;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityConstants;
import step.core.entities.EntityReference;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Rewrites the resource references of an artefact tree - a data source above all, which the keyword
 * plugins do not map themselves. Extracted from {@code AutomationPackagePlansAttributesApplier}, whose
 * traversal this is, so that the same walk serves the deploy direction and the editor's write-back.
 * <p>
 * A property holds a reference when its getter says so, with
 * {@link EntityReference}{@code (type = }{@link EntityConstants#resources}{@code )}. That declaration is
 * the only thing distinguishing a relative path from any other string, and it is what a deployment
 * follows, so the editor follows exactly the same one - no more, no less.
 * <p>
 * Keywords are deliberately <b>not</b> handled here: each plugin maps its own fields in
 * {@code Yaml*Function.fillDeclaredFields} and back in {@code setDeclaredFieldsFromObject}, because
 * some of them are conditional. {@code YamlK6Function} builds three different shapes out of two fields,
 * and only that class can tell which one it produced.
 */
public class ResourceReferences {

    private static final String STEP_PACKAGE = "step";

    private static final Map<Class<?>, List<PropertyDescriptor>> REFERENCE_PROPERTIES = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Field>> NESTED_FIELDS = new ConcurrentHashMap<>();

    private ResourceReferences() {
    }

    /**
     * The undo of an {@link #apply(AbstractArtefact, UnaryOperator)} call. Closing it puts the previous
     * values back, which is what lets an entity be written in one form while staying in another.
     */
    public interface Restoration extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * Applies {@code mapping} to every declared resource reference of the artefact tree rooted at
     * {@code root}, children and before/after blocks included.
     * <p>
     * A {@code resource:<id>} reference is left alone - it is already absolute - as is a dynamic value,
     * which has no path to rewrite.
     *
     * @return the undo; ignore it unless the change is meant to be temporary
     */
    public static Restoration apply(AbstractArtefact root, UnaryOperator<String> mapping) {
        Objects.requireNonNull(mapping, "mapping must not be null");
        List<Runnable> undo = new ArrayList<>();
        applyToArtefact(root, mapping, undo);
        return () -> undo.forEach(Runnable::run);
    }

    private static void applyToArtefact(AbstractArtefact artefact, UnaryOperator<String> mapping, List<Runnable> undo) {
        if (artefact == null) {
            return;
        }
        applyRecursively(artefact, mapping, undo);
        applyToSteps(artefact.getChildren(), mapping, undo);
        Optional.ofNullable(artefact.getBefore()).map(ChildrenBlock::getSteps)
            .ifPresent(steps -> applyToSteps(steps, mapping, undo));
        Optional.ofNullable(artefact.getAfter()).map(ChildrenBlock::getSteps)
            .ifPresent(steps -> applyToSteps(steps, mapping, undo));
    }

    private static void applyToSteps(List<AbstractArtefact> steps, UnaryOperator<String> mapping, List<Runnable> undo) {
        if (steps != null) {
            steps.forEach(step -> applyToArtefact(step, mapping, undo));
        }
    }

    /**
     * Walks the object itself and its nested Step objects - the data source of a for-each block is a
     * field of the artefact, not an artefact of its own.
     */
    private static void applyRecursively(Object object, UnaryOperator<String> mapping, List<Runnable> undo) {
        if (object == null) {
            return;
        }
        for (PropertyDescriptor property : referencePropertiesOf(object.getClass())) {
            applyToProperty(object, property, mapping, undo);
        }
        for (Field nested : nestedFieldsOf(object.getClass())) {
            try {
                applyRecursively(nested.get(object), mapping, undo);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to read the field " + nested.getName() + " of "
                    + object.getClass().getName(), e);
            }
        }
    }

    private static void applyToProperty(Object object, PropertyDescriptor property, UnaryOperator<String> mapping,
                                        List<Runnable> undo) {
        Object value = read(object, property);
        if (value instanceof String) {
            String reference = (String) value;
            if (!FileResolver.isResource(reference)) {
                write(object, property, mapping.apply(reference));
                undo.add(() -> write(object, property, value));
            }
        } else if (value instanceof DynamicValue) {
            String reference = asString(((DynamicValue<?>) value).getValue());
            if (reference != null && !FileResolver.isResource(reference)) {
                write(object, property, new DynamicValue<>(mapping.apply(reference)));
                undo.add(() -> write(object, property, value));
            }
        } else if (value != null) {
            throw new RuntimeException("Unsupported type " + value.getClass() + " for the resource reference "
                + property.getName() + " of " + object.getClass().getName());
        }
    }

    private static String asString(Object value) {
        return value instanceof String ? (String) value : value == null ? null : value.toString();
    }

    private static Object read(Object object, PropertyDescriptor property) {
        try {
            return property.getReadMethod().invoke(object);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to read the resource reference " + property.getName() + " of "
                + object.getClass().getName(), e);
        }
    }

    private static void write(Object object, PropertyDescriptor property, Object value) {
        if (property.getWriteMethod() == null) {
            throw new RuntimeException("The resource reference " + property.getName() + " of "
                + object.getClass().getName() + " cannot be rewritten: the setter doesn't exist");
        }
        try {
            property.getWriteMethod().invoke(object, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to write the resource reference " + property.getName() + " of "
                + object.getClass().getName(), e);
        }
    }

    private static List<PropertyDescriptor> referencePropertiesOf(Class<?> aClass) {
        return REFERENCE_PROPERTIES.computeIfAbsent(aClass, key -> {
            try {
                List<PropertyDescriptor> properties = new ArrayList<>();
                for (PropertyDescriptor property : Introspector.getBeanInfo(key).getPropertyDescriptors()) {
                    Method getter = property.getReadMethod();
                    EntityReference declaration = getter == null ? null : getter.getAnnotation(EntityReference.class);
                    if (declaration != null && EntityConstants.resources.equals(declaration.type())) {
                        properties.add(property);
                    }
                }
                return properties;
            } catch (IntrospectionException e) {
                throw new RuntimeException("Unable to introspect " + key.getName(), e);
            }
        });
    }

    private static List<Field> nestedFieldsOf(Class<?> aClass) {
        return NESTED_FIELDS.computeIfAbsent(aClass, key -> {
            List<Field> fields = new ArrayList<>();
            for (Class<?> current = key; current != null && isStepClass(current); current = current.getSuperclass()) {
                fields.addAll(Stream.of(current.getDeclaredFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .filter(field -> isStepClass(field.getType()))
                    .peek(field -> field.setAccessible(true))
                    .collect(Collectors.toList()));
            }
            return fields;
        });
    }

    private static boolean isStepClass(Class<?> aClass) {
        return aClass.getPackageName().startsWith(STEP_PACKAGE + ".");
    }
}
