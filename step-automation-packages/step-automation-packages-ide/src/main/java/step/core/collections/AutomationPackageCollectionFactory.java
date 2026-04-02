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

import java.io.IOException;
import java.util.Properties;

import step.automation.packages.yaml.AutomationPackageYamlFragmentManager;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.plans.Plan;

public class AutomationPackageCollectionFactory implements CollectionFactory {

    private final InMemoryCollectionFactory baseFactory;
    private final AutomationPackageYamlFragmentManager fragmentManager;

    public AutomationPackageCollectionFactory(Properties properties, AutomationPackageYamlFragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
        this.baseFactory = new InMemoryCollectionFactory(properties);
    }

    @Override
	public <T> Collection<T> getCollection(String name, Class<T> entityClass) {

        if (entityClass == Plan.class)  {
            return (Collection<T>) new AutomationPackagePlanCollection(fragmentManager);
        }

        return baseFactory.getCollection(name, entityClass);
	}

	@Override
	public Collection<EntityVersion> getVersionedCollection(String name) {
        Collection<EntityVersion> baseCollection = baseFactory.getCollection(name, EntityVersion.class);
        return baseCollection;
	}

    @Override
    public void close() throws IOException {
        baseFactory.close();
    }
}
