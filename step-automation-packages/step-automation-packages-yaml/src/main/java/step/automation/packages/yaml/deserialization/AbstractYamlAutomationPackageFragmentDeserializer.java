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
package step.automation.packages.yaml.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.model.AbstractAutomationPackageFragmentYaml;
import step.core.yaml.deserialization.PatchableYamlList;

import java.io.IOException;

/**
 * Wraps the bean deserializer Jackson built for a fragment class. The wrapper shares the property map of that
 * delegate, and Jackson caches the wrapper in the object mapper, so concurrent reads share both.
 * <p>
 * The delegate is therefore resolved in {@link #resolve}, which Jackson calls exactly once, under its cache lock,
 * when it creates the wrapper. {@link #createContextual} only contextualizes it: resolving there would modify the
 * shared property map on each read, and concurrent reads would fail on it.
 */
public abstract class AbstractYamlAutomationPackageFragmentDeserializer extends BeanDeserializer implements ContextualDeserializer, ResolvableDeserializer {

    protected final BeanDeserializer delegate;

    public AbstractYamlAutomationPackageFragmentDeserializer(BeanDeserializer deserializer) {
        super(deserializer);
        delegate = deserializer;
    }

    /**
     * @return a deserializer of the same type, wrapping the given contextualized delegate
     */
    protected abstract AbstractYamlAutomationPackageFragmentDeserializer withDelegate(BeanDeserializer contextualDelegate);

    @Override
    public abstract Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException;

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        return withDelegate((BeanDeserializer) delegate.createContextual(ctxt, property));
    }

    @Override
    protected void handleUnknownVanilla(JsonParser p, DeserializationContext ctxt, Object intoValue, String propName) throws IOException {
        try {
            AutomationPackageSerializationRegistry registry = (AutomationPackageSerializationRegistry) ctxt.getAttribute(AutomationPackageSerializationRegistry.class);
            Class<?> targetClass = registry.resolveClassForYamlField(propName);
            JavaType listType = ctxt.getTypeFactory()
                .constructCollectionType(PatchableYamlList.class, targetClass);
            PatchableYamlList<?> list = ctxt.readValue(p, listType);

            AbstractAutomationPackageFragmentYaml fragment = (AbstractAutomationPackageFragmentYaml) intoValue;
            fragment.setAdditionalFields(propName, list);
        } catch (ClassCastException | NullPointerException e) {
            super.handleUnknownVanilla(p, ctxt, intoValue, propName);
        }
    }

    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {
        delegate.resolve(ctxt);
    }

}
