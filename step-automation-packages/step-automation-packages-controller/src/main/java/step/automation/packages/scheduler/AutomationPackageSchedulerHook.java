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
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.entities.Entity;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.RepositoryObjectReference;
import step.core.scheduler.*;
import step.core.scheduler.automation.AutomationPackageSchedule;

import java.util.*;
import java.util.stream.Collectors;

public class AutomationPackageSchedulerHook implements AutomationPackageHook<ExecutiontTaskParameters> {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageSchedulerHook.class);
    protected static final String EXECUTION_SCHEDULER_EXTENSION = "executionScheduler";

    private final ExecutionScheduler scheduler;

    public AutomationPackageSchedulerHook(ExecutionScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void onCreate(List<? extends ExecutiontTaskParameters> entities, AutomationPackageContext context) {
        if (isSchedulerInContext(context)) {
            ExecutionScheduler executionScheduler = getExecutionScheduler(context);
            for (ExecutiontTaskParameters entity : entities) {
                // make sure the execution parameter of the schedule are enriched too (required to execute in same project
                // as the schedule and populate event bindings
                context.getEnricher().accept(entity.getExecutionsParameters());
                executionScheduler.addOrUpdateExecutionTask(entity, false);
            }
        }
    }

    @Override
    public void onDelete(AutomationPackage automationPackage, AutomationPackageContext context) {
        if (isSchedulerInContext(context)) {
            ExecutionScheduler executionScheduler = getExecutionScheduler(context);
            List<ExecutiontTaskParameters> entities = getPackageSchedules(automationPackage.getId(), context);
            for (ExecutiontTaskParameters entity : entities) {
                executionScheduler.removeExecutionTask(entity.getId().toString());
            }
        }
    }

    @Override
    public void onMainAutomationPackageManagerCreate(Map<String, Object> extensions) {
       extensions.put(EXECUTION_SCHEDULER_EXTENSION, scheduler);
    }

    @Override
    public void onIsolatedAutomationPackageManagerCreate(Map<String, Object> extensions) {
        extensions.put(EXECUTION_SCHEDULER_EXTENSION, null);
    }

    @Override
    public void onPrepareStaging(String fieldName, AutomationPackageContext apContext,
                                 AutomationPackageContent apContent, List<?> objects,
                                 AutomationPackage oldPackage, AutomationPackageStaging targetStaging) {
        if (isSchedulerInContext(apContext)) {
            targetStaging.addAdditionalObjects(
                    AutomationPackageSchedule.FIELD_NAME_IN_AP,
                    prepareExecutionTasksParamsStaging((List<AutomationPackageSchedule>) objects,
                            apContent,
                            apContext,
                            oldPackage,
                            targetStaging.getPlans()
                    )
            );
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
            if (cronExclusionsAsStrings != null && !cronExclusionsAsStrings.isEmpty()) {
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
            //Get current package ID and use it to filter out deployd entities from previous version
            Plan enrichablePlan = new Plan();
            context.getEnricher().accept(enrichablePlan);
            // schedule can reference the existing persisted plan (not defined inside the automation package)
            Filter persistedPlanFilter = Filters.and(List.of(Filters.equals("attributes." + AbstractOrganizableObject.NAME, planName),
                    Filters.not(Filters.equals("customFields." + AutomationPackageEntity.AUTOMATION_PACKAGE_ID, enrichablePlan.getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID).toString()))));
            plan = getPlanAccessor(context).getCollectionDriver().find(persistedPlanFilter,  null, null, 1, 0).findFirst().orElse(null);
        }
        return plan;
    }

    private static PlanAccessor getPlanAccessor(AutomationPackageContext context) {
        return (PlanAccessor) context.getExtensions().get(AutomationPackageContext.PLAN_ACCESSOR);
    }

    protected List<ExecutiontTaskParameters> getPackageSchedules(ObjectId automationPackageId, AutomationPackageContext context) {
        return getExecutionScheduler(context).getExecutionTaskAccessor().findManyByCriteria(AutomationPackageEntity.getAutomationPackageIdCriteria(automationPackageId)).collect(Collectors.toList());
    }

    protected boolean isSchedulerInContext(AutomationPackageContext context)  {
        return context.getExtensions().get(EXECUTION_SCHEDULER_EXTENSION) != null;
    }

    protected ExecutionScheduler getExecutionScheduler(AutomationPackageContext context){
        return (ExecutionScheduler) context.getExtensions().get(EXECUTION_SCHEDULER_EXTENSION);
    }
}
