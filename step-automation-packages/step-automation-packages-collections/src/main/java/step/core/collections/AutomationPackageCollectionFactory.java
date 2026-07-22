/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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
package step.core.collections;

import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapper;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.automation.packages.yaml.AutomationPackageYamlFragmentManager;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.plans.Plan;
import step.functions.Function;
import step.parameter.Parameter;
import step.parameter.automation.AutomationPackageParameter;
import step.plans.parser.yaml.YamlPlan;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class AutomationPackageCollectionFactory implements CollectionFactory {

    private final InMemoryCollectionFactory baseFactory;
    private final AutomationPackageYamlFragmentManager fragmentManager;
    private final Map<String, Collection<?>> collectionsByName = new ConcurrentHashMap<>();

    public AutomationPackageCollectionFactory(Properties properties, AutomationPackageYamlFragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
        this.baseFactory = new InMemoryCollectionFactory(properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<T> getCollection(String name, Class<T> entityClass) {
        return (Collection<T>) collectionsByName.computeIfAbsent(name, (_name) -> {

            if (!AbstractOrganizableObject.class.isAssignableFrom(entityClass)) {
                return baseFactory.getCollection(name, entityClass);
            }

            return new AutomationPackageCollection<>(fragmentManager, entityClass);
        });
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Collection<EntityVersion> getVersionedCollection(String name) {
        // TODO: I'm pretty sure the previous implementation was incorrect.
        // Fix this once we need it and know what the correct implementation is.
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        baseFactory.close();
    }
}
