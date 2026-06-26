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

import step.automation.packages.yaml.AutomationPackageYamlFragmentManager;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.inmemory.InMemoryCollection;

public class AutomationPackageCollection<BO extends AbstractOrganizableObject, T> extends InMemoryCollection<BO> implements Collection<BO> {


    private final AutomationPackageYamlFragmentManager fragmentManager;

    public AutomationPackageCollection(AutomationPackageYamlFragmentManager fragmentManager, Class<T> boClass) {
        super(false);
        this.fragmentManager = fragmentManager;
        initializeRecordsFromFragments(boClass, fragmentManager);
    }

    private void initializeRecordsFromFragments(Class<T> boClass, AutomationPackageYamlFragmentManager fragmentManager) {
        // initialization into the collection memory. Calls super save to avoid calling fragmentManager.savePlan
        Iterable<BO> list = fragmentManager.getBusinessObjects(boClass);
        list.forEach(super::save);
    }

    @Override
    public BO save(BO p) {
        return fragmentManager.save(super.save(p));
    }

    @Override
    public void save(Iterable<BO> iterable) {
        for (BO object : iterable) {
            save(object);
        }
    }

    @Override
    public void remove(Filter filter) {
        find(filter, null, null, null, 0).forEach(fragmentManager::remove);
        super.remove(filter);
    }
}
