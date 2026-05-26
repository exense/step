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

import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.automation.packages.yaml.AutomationPackageYamlFragmentManager;
import step.core.collections.inmemory.InMemoryCollection;
import step.functions.Function;

public class AutomationPackageFunctionCollection extends InMemoryCollection<Function> implements Collection<Function>  {


    private final AutomationPackageYamlFragmentManager fragmentManager;

    public AutomationPackageFunctionCollection(AutomationPackageYamlFragmentManager fragmentManager) {
        super(true, YamlAutomationPackageKeyword.KEYWORDS_ENTITY_NAME);
        this.fragmentManager = fragmentManager;
        initialzeRecordsFromFragments(fragmentManager);
    }

    private void initialzeRecordsFromFragments(AutomationPackageYamlFragmentManager fragmentManager) {
        // initialization into the collection memory. Calls super save to avoid calling fragmentManager.savePlan
        fragmentManager.getBusinessObjects(Function.class).forEach(super::save);
    }

    @Override
    public Function save(Function p){
        return super.save(fragmentManager.saveFunction(p));
    }

    @Override
    public void save(Iterable<Function> iterable) {
        for (Function p : iterable) {
            save(p);
        }
    }

    @Override
    public void remove(Filter filter) {
        find(filter, null, null, null, 0).forEach(fragmentManager::removeFunction);
        super.remove(filter);
    }
}
