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
package step.automation.packages.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageContext;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageManagerException;
import step.automation.packages.model.AutomationPackageContent;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.model.ExecutionParameters;
import step.core.objectenricher.ObjectEnricher;
import step.core.plans.Plan;
import step.core.repositories.RepositoryObjectReference;
import step.automation.packages.model.AutomationPackageSchedule;
import step.core.scheduler.CronExclusion;
import step.core.scheduler.ExecutiontTaskParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExecutionTaskParameterWithoutSchedulerHook implements AutomationPackageHook<ExecutiontTaskParameters> {

    private static final Logger log = LoggerFactory.getLogger(ExecutionTaskParameterWithoutSchedulerHook.class);

    public ExecutionTaskParameterWithoutSchedulerHook() {
    }

    @Override
    public void onPrepareStaging(String fieldName, AutomationPackageContext apContext,
                                 AutomationPackageContent apContent, List<?> objects,
                                 AutomationPackage oldPackage, AutomationPackageManager.Staging targetStaging,
                                 AutomationPackageManager manager) {
        targetStaging.getAdditionalObjects().put(
                AutomationPackageSchedule.FIELD_NAME_IN_AP,
                prepareExecutionTasksParamsStaging((List<AutomationPackageSchedule>) objects, apContext.getEnricher(), apContent, oldPackage, targetStaging.getPlans(), manager)
        );
    }

    @Override
    public void onCreate(List<? extends ExecutiontTaskParameters> entities, ObjectEnricher enricher, AutomationPackageManager manager) {
        for (ExecutiontTaskParameters entity : entities) {
            //make sure the execution parameter of the schedule are enriched too (required to execute in same project
            // as the schedule and populate event bindings
            enricher.accept(entity.getExecutionsParameters());
            manager.getExecutionTaskAccessor().save(entity);
        }
    }

    @Override
    public void onDelete(AutomationPackage automationPackage, AutomationPackageManager manager) {
        List<ExecutiontTaskParameters> schedules = manager.getPackageSchedules(automationPackage.getId());
        for (ExecutiontTaskParameters schedule : schedules) {
            try {
                manager.getExecutionTaskAccessor().remove(schedule.getId());
            } catch (Exception e) {
                log.error("Error while deleting task {} for automation package {}",
                        schedule.getId().toString(), automationPackage.getAttribute(AbstractOrganizableObject.NAME), e
                );
            }
        }
    }

    protected List<ExecutiontTaskParameters> prepareExecutionTasksParamsStaging(List<AutomationPackageSchedule> objects,
                                                                                ObjectEnricher enricher,
                                                                                AutomationPackageContent packageContent,
                                                                                AutomationPackage oldPackage,
                                                                                List<Plan> plansStaging,
                                                                                AutomationPackageManager manager) {
        List<ExecutiontTaskParameters> completeExecTasksParameters = new ArrayList<>();
        for (AutomationPackageSchedule schedule : objects) {
            ExecutiontTaskParameters execTaskParameters = new ExecutiontTaskParameters();

            execTaskParameters.setActive(schedule.getActive() == null || schedule.getActive());
            execTaskParameters.addAttribute(AbstractOrganizableObject.NAME, schedule.getName());
            execTaskParameters.setCronExpression(schedule.getCron());
            List<String> cronExclusionsAsStrings = schedule.getCronExclusions();
            if (cronExclusionsAsStrings != null && cronExclusionsAsStrings.size() > 1) {
                List<CronExclusion> cronExclusions = cronExclusionsAsStrings.stream().map(s -> new CronExclusion(s, "")).collect(Collectors.toList());
                execTaskParameters.setCronExclusions(cronExclusions);
            }
            String assertionPlanName = schedule.getAssertionPlanName();
            if (assertionPlanName != null && !assertionPlanName.isEmpty()) {
                Plan assertionPlan = manager.lookupPlanByName(plansStaging, assertionPlanName);
                if (assertionPlan == null) {
                    throw new AutomationPackageManagerException("Invalid automation package: " + packageContent.getName() +
                            ". No assertion plan with '" + assertionPlanName + "' name found for schedule " + schedule.getName());
                }
                execTaskParameters.setAssertionPlan(assertionPlan.getId());
            }

            String planNameFromSchedule = schedule.getPlanName();
            if (planNameFromSchedule == null || planNameFromSchedule.isEmpty()) {
                throw new AutomationPackageManagerException("Invalid automation package: " + packageContent.getName() +
                        ". Plan name is not defined for schedule " + schedule.getName());
            }

            Plan plan = manager.lookupPlanByName(plansStaging, planNameFromSchedule);
            if (plan == null) {
                throw new AutomationPackageManagerException("Invalid automation package: " + packageContent.getName() +
                        ". No plan with '" + planNameFromSchedule + "' name found for schedule " + schedule.getName());
            }

            RepositoryObjectReference repositoryObjectReference = new RepositoryObjectReference(
                    RepositoryObjectReference.LOCAL_REPOSITORY_ID, Map.of(RepositoryObjectReference.PLAN_ID, plan.getId().toString())
            );
            ExecutionParameters executionParameters = new ExecutionParameters(repositoryObjectReference, plan.getAttribute(AbstractOrganizableObject.NAME),
                    schedule.getExecutionParameters());
            execTaskParameters.setExecutionsParameters(executionParameters);
            completeExecTasksParameters.add(execTaskParameters);
        }
        manager.fillEntities(completeExecTasksParameters, oldPackage != null ? manager.getPackageSchedules(oldPackage.getId()) : new ArrayList<>(), enricher);
        return completeExecTasksParameters;
    }
}
