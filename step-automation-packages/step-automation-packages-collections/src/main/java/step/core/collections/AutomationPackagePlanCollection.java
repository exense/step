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
import step.core.collections.inmemory.InMemoryCollection;
import step.core.plans.Plan;
import step.plans.parser.yaml.YamlPlan;

public class AutomationPackagePlanCollection extends InMemoryCollection<Plan> implements Collection<Plan>  {


    private final AutomationPackageYamlFragmentManager fragmentManager;

    public AutomationPackagePlanCollection(AutomationPackageYamlFragmentManager fragmentManager) {
        super(true, YamlPlan.PLANS_ENTITY_NAME);
        this.fragmentManager = fragmentManager;
        initialzeRecordsFromFragments(fragmentManager);
    }

    private void initialzeRecordsFromFragments(AutomationPackageYamlFragmentManager fragmentManager) {
        // initialization into the collection memory. Calls super save to avoid calling fragmentManager.savePlan
        fragmentManager.getBusinessObjects(Plan.class).forEach(super::save);
    }

    @Override
    public Plan save(Plan p){
        return super.save(fragmentManager.savePlan(p));
    }

    @Override
    public void save(Iterable<Plan> iterable) {
        for (Plan p : iterable) {
            save(p);
        }
    }

    @Override
    public void remove(Filter filter) {
        find(filter, null, null, null, 0).forEach(fragmentManager::removePlan);
        super.remove(filter);
    }
}
