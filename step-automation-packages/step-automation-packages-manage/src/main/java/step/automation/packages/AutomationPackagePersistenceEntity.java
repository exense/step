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

import step.automation.packages.accessor.AutomationPackageAccessor;
import step.core.GlobalContext;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.entities.DependencyTreeVisitorHook;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.plans.Plan;
import step.core.scheduler.ExecutiontTaskParameters;
import step.functions.Function;

public class AutomationPackagePersistenceEntity extends Entity<AutomationPackagePersistence, AutomationPackageAccessor> {
    public static final String AUTOMATION_PACKAGE_ID = "automationPackageId";
    public static final String entityName = "automationPackage";

    public AutomationPackagePersistenceEntity(String name, AutomationPackageAccessor accessor, GlobalContext context) {
        super(name, accessor, AutomationPackagePersistence.class);

        //Add hooks for function entity
        EntityManager entityManager = context.getEntityManager();
        entityManager.addDependencyTreeVisitorHook(automationPackageReferencesHook(entityManager));
    }

    private static DependencyTreeVisitorHook automationPackageReferencesHook(EntityManager em) {
        return (o, context) -> {
            if (o instanceof Function || o instanceof Plan || o instanceof ExecutiontTaskParameters) {
                AbstractIdentifiableObject identifiableObject = (AbstractIdentifiableObject) o;
                String id = (String) identifiableObject.getCustomField(AUTOMATION_PACKAGE_ID);
                if (id != null) {
                    if(context.isRecursive()) {
                        context.visitEntity(entityName, id);
                    }

                    String newEntityId = context.resolvedEntityId(entityName, id);
                    if(newEntityId != null) {
                        identifiableObject.addCustomField(AUTOMATION_PACKAGE_ID, newEntityId);
                    }
                }
            }
        };
    }


}
