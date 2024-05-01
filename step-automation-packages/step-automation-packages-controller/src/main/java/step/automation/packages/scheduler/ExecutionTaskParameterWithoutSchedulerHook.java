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
package step.automation.packages.scheduler;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.*;
import step.automation.packages.AutomationPackageHook;
import step.automation.packages.AutomationPackageContent;
import step.core.accessors.AbstractOrganizableObject;
import step.core.entities.Entity;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.RepositoryObjectReference;
import step.core.scheduler.ExecutionTaskAccessor;
import step.core.scheduler.InMemoryExecutionTaskAccessor;
import step.core.scheduler.automation.AutomationPackageSchedule;
import step.core.scheduler.CronExclusion;
import step.core.scheduler.ExecutiontTaskParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExecutionTaskParameterWithoutSchedulerHook implements AutomationPackageHook<ExecutiontTaskParameters> {

    private static final Logger log = LoggerFactory.getLogger(ExecutionTaskParameterWithoutSchedulerHook.class);
    protected static final String EXECUTION_TASK_ACCESSOR_EXTENSION = "executionTaskAccessor";

    private final ExecutionTaskAccessor mainExecutionTaskAccessor;

    public ExecutionTaskParameterWithoutSchedulerHook(ExecutionTaskAccessor mainExecutionTaskAccessor) {
        this.mainExecutionTaskAccessor = mainExecutionTaskAccessor;
        this.mainExecutionTaskAccessor.createIndexIfNeeded(AutomationPackageEntity.getIndexField());
    }

    @Override
    public void onMainAutomationPackageManagerCreate(Map<String, Object> extensions) {
       extensions.put(EXECUTION_TASK_ACCESSOR_EXTENSION, mainExecutionTaskAccessor);
    }

    @Override
    public void onIsolatedAutomationPackageManagerCreate(Map<String, Object> extensions) {
        InMemoryExecutionTaskAccessor inMemoryAccessor = new InMemoryExecutionTaskAccessor();
        inMemoryAccessor.createIndexIfNeeded(AutomationPackageEntity.getIndexField());
        extensions.put(EXECUTION_TASK_ACCESSOR_EXTENSION, inMemoryAccessor);
    }

    @Override
    public void onPrepareStaging(String fieldName, AutomationPackageContext apContext,
                                 AutomationPackageContent apContent, List<?> objects,
                                 AutomationPackage oldPackage, AutomationPackageStaging targetStaging) {
        targetStaging.getAdditionalObjects().put(
                AutomationPackageSchedule.FIELD_NAME_IN_AP,
                prepareExecutionTasksParamsStaging((List<AutomationPackageSchedule>) objects,
                        apContent,
                        apContext,
                        oldPackage,
                        targetStaging.getPlans()
                )
        );
    }

    @Override
    public void onCreate(List<? extends ExecutiontTaskParameters> entities, AutomationPackageContext context) {
        for (ExecutiontTaskParameters entity : entities) {
            // make sure the execution parameter of the schedule are enriched too (required to execute in same project
            // as the schedule and populate event bindings
            context.getEnricher().accept(entity.getExecutionsParameters());
            mainExecutionTaskAccessor.save(entity);
        }
    }

    @Override
    public void onDelete(AutomationPackage automationPackage, AutomationPackageContext context) {
        List<ExecutiontTaskParameters> schedules = getPackageSchedules(automationPackage.getId(), context);
        for (ExecutiontTaskParameters schedule : schedules) {
            try {
                mainExecutionTaskAccessor.remove(schedule.getId());
            } catch (Exception e) {
                log.error("Error while deleting task {} for automation package {}",
                        schedule.getId().toString(), automationPackage.getAttribute(AbstractOrganizableObject.NAME), e
                );
            }
        }
    }

    protected List<ExecutiontTaskParameters> prepareExecutionTasksParamsStaging(List<AutomationPackageSchedule> objects,
                                                                                AutomationPackageContent packageContent,
                                                                                AutomationPackageContext packageContext,
                                                                                AutomationPackage oldPackage,
                                                                                List<Plan> plansStaging) {
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
                Plan assertionPlan = lookupPlanByName(plansStaging, assertionPlanName, packageContext);
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

            Plan plan = lookupPlanByName(plansStaging, planNameFromSchedule, packageContext);
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
        Entity.reuseOldIds(completeExecTasksParameters, oldPackage != null ? getPackageSchedules(oldPackage.getId(), packageContext) : new ArrayList<>());
        completeExecTasksParameters.forEach(packageContext.getEnricher());
        return completeExecTasksParameters;
    }

    protected Plan lookupPlanByName(List<Plan> plansStaging, String planName, AutomationPackageContext context) {
        Plan plan = plansStaging.stream().filter(p -> Objects.equals(p.getAttribute(AbstractOrganizableObject.NAME), planName)).findFirst().orElse(null);
        if (plan == null) {
            // schedule can reference the existing persisted plan (not defined inside the automation package)
            plan = getPlanAccessor(context).findByAttributes(Map.of(AbstractOrganizableObject.NAME, planName));
        }
        return plan;
    }

    private static PlanAccessor getPlanAccessor(AutomationPackageContext context) {
        return (PlanAccessor) context.getExtensions().get(AutomationPackageContext.PLAN_ACCESSOR);
    }

    protected List<ExecutiontTaskParameters> getPackageSchedules(ObjectId automationPackageId, AutomationPackageContext context) {
        return getExecutionTaskAccessor(context).findManyByCriteria(AutomationPackageEntity.getAutomationPackageIdCriteria(automationPackageId)).collect(Collectors.toList());
    }

    protected ExecutionTaskAccessor getExecutionTaskAccessor(AutomationPackageContext context){
        return (ExecutionTaskAccessor) context.getExtensions().get(EXECUTION_TASK_ACCESSOR_EXTENSION);
    }
}
