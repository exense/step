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

import step.core.objectenricher.ObjectEnricher;
import step.core.plans.Plan;
import step.resources.ResourceManager;

import java.util.List;
import java.util.Map;

/**
 * Applies the resource references of the plans of an automation package: what the descriptor holds as a
 * path relative to the package root becomes the reference the entities carry - an
 * {@code apResource:<apId>:} one at deploy time, an {@code apResource:local:} one in the editor.
 * <p>
 * Which of the two is produced is decided by the {@code AutomationPackageResourceMapper} of the
 * context, exactly as for the keyword plugins.
 */
public class AutomationPackagePlansAttributesApplier {

    private final ResourceManager resourceManager;

    public AutomationPackagePlansAttributesApplier(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void applySpecialAttributesToPlans(AutomationPackage newPackage,
                                              List<Plan> plans,
                                              AutomationPackageArchive automationPackageArchive,
                                              AutomationPackageContent packageContent,
                                              String actorUser,
                                              ObjectEnricher objectEnricher, Map<String, Object> extensions,
                                              AutomationPackageOperationMode operationMode) {
        applySpecialAttributesToPlans(prepareContext(newPackage, operationMode, automationPackageArchive, packageContent, actorUser, objectEnricher, extensions), plans);
    }

    /**
     * The variant for a caller that already has a staging context - the automation package editor, which
     * holds one whose mapper produces the local reference form. The plans are the live ones, so this
     * rewrites them in place.
     */
    public static void applySpecialAttributesToPlans(StagingAutomationPackageContext apContext, Iterable<Plan> plans) {
        AutomationPackageResourceMapper resourceMapper = apContext.getResourceMapper();
        for (Plan plan : plans) {
            ResourceReferences.apply(plan.getRoot(),
                reference -> resourceMapper.applyResourceReference(reference, apContext));
        }
    }

    protected StagingAutomationPackageContext prepareContext(AutomationPackage automationPackage, AutomationPackageOperationMode operationMode, AutomationPackageArchive automationPackageArchive, AutomationPackageContent packageContent,
                                                             String actorUser, ObjectEnricher enricher, Map<String, Object> extensions) {
        return new StagingAutomationPackageContext(new AutomationPackageResourceMapper(), automationPackage, operationMode, resourceManager, automationPackageArchive, packageContent, actorUser, enricher, extensions);
    }

}
