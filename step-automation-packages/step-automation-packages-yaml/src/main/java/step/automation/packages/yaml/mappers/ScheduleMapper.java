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
package step.automation.packages.yaml.mappers;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapper;
import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapping;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapper;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapping;
import step.automation.packages.yaml.AutomationPackageYamlFragmentManager;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.Plan;
import step.core.repositories.RepositoryObjectReference;
import step.core.scheduler.CronExclusion;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.scheduler.automation.AutomationPackageSchedule;
import step.core.yaml.YamlMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Maps the schedules of an automation package to scheduler tasks and back. A schedule refers to its plans by name,
 * a task by id: the names are resolved against the plans of the package.
 * <p>
 * The plan of a schedule may be one the package does not hold as a YAML plan, for instance a plain text plan or a
 * plan defined in code. The task then refers to no plan, and the plan name is kept as the description of its
 * execution parameters, from which it is written back.
 */
@BusinessObjectToYamlMapping(sourceClass = ExecutiontTaskParameters.class)
@YamlToBusinessObjectMapping
public class ScheduleMapper implements
    BusinessObjectToYamlMapper<ExecutiontTaskParameters, AutomationPackageSchedule>,
    YamlToBusinessObjectMapper<AutomationPackageSchedule, ExecutiontTaskParameters> {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleMapper.class);

    private final AutomationPackageYamlFragmentManager fragmentManager;

    public ScheduleMapper(AutomationPackageYamlFragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    @Override
    public ExecutiontTaskParameters toBusinessObject(AutomationPackageSchedule schedule) {
        ExecutiontTaskParameters task = new ExecutiontTaskParameters();
        task.addAttribute(AbstractOrganizableObject.NAME, schedule.getName());
        task.setActive(schedule.getActive() == null || schedule.getActive());
        task.setCronExpression(schedule.getCron());
        List<String> cronExclusions = schedule.getCronExclusions();
        if (cronExclusions != null && !cronExclusions.isEmpty()) {
            task.setCronExclusions(cronExclusions.stream().map(e -> new CronExclusion(e, "")).collect(Collectors.toList()));
        }
        YamlMetadata.applyTo(task, schedule.getMetadata());

        Map<String, String> repositoryParameters = new HashMap<>();
        findPlanByName(schedule.getPlanName())
            .ifPresent(plan -> repositoryParameters.put(RepositoryObjectReference.PLAN_ID, plan.getId().toString()));
        RepositoryObjectReference repositoryObject = new RepositoryObjectReference(RepositoryObjectReference.LOCAL_REPOSITORY_ID, repositoryParameters);
        task.setExecutionsParameters(new ExecutionParameters(repositoryObject, schedule.getPlanName(), schedule.getExecutionParameters()));

        String assertionPlanName = schedule.getAssertionPlanName();
        if (assertionPlanName != null && !assertionPlanName.isEmpty()) {
            findPlanByName(assertionPlanName).ifPresentOrElse(plan -> task.setAssertionPlan(plan.getId()),
                () -> logger.warn("The assertion plan '{}' of the schedule '{}' is not a YAML plan of the automation package, it is ignored",
                    assertionPlanName, schedule.getName()));
        }
        return task;
    }

    @Override
    public AutomationPackageSchedule toYamlObject(ExecutiontTaskParameters task) {
        AutomationPackageSchedule schedule = new AutomationPackageSchedule(null);
        schedule.setName(task.getAttribute(AbstractOrganizableObject.NAME));
        // a schedule is active unless stated otherwise
        schedule.setActive(task.isActive() ? null : false);
        schedule.setCron(task.getCronExpression());
        List<CronExclusion> cronExclusions = task.getCronExclusions();
        if (cronExclusions != null && !cronExclusions.isEmpty()) {
            schedule.setCronExclusions(cronExclusions.stream().map(CronExclusion::getCronExpression).collect(Collectors.toList()));
        }
        schedule.setMetadata(YamlMetadata.extractFrom(task));

        ExecutionParameters executionParameters = task.getExecutionsParameters();
        if (executionParameters != null) {
            schedule.setPlanName(findPlanNameById(getPlanId(executionParameters)).orElse(executionParameters.getDescription()));
            schedule.setExecutionParameters(executionParameters.getCustomParameters());
        }
        ObjectId assertionPlan = task.getAssertionPlan();
        if (assertionPlan != null) {
            schedule.setAssertionPlanName(findPlanNameById(assertionPlan.toString()).orElse(null));
        }
        return schedule;
    }

    private static String getPlanId(ExecutionParameters executionParameters) {
        RepositoryObjectReference repositoryObject = executionParameters.getRepositoryObject();
        if (repositoryObject == null || repositoryObject.getRepositoryParameters() == null) {
            return null;
        }
        return repositoryObject.getRepositoryParameters().get(RepositoryObjectReference.PLAN_ID);
    }

    private Optional<Plan> findPlanByName(String planName) {
        if (planName == null) {
            return Optional.empty();
        }
        return plans().filter(plan -> Objects.equals(plan.getAttribute(AbstractOrganizableObject.NAME), planName)).findFirst();
    }

    private Optional<String> findPlanNameById(String planId) {
        if (planId == null) {
            return Optional.empty();
        }
        return plans().filter(plan -> plan.getId().toString().equals(planId)).findFirst()
            .map(plan -> plan.getAttribute(AbstractOrganizableObject.NAME));
    }

    private Stream<Plan> plans() {
        Iterable<Plan> plans = fragmentManager.getBusinessObjects(Plan.class);
        return StreamSupport.stream(plans.spliterator(), false);
    }

    @Override
    public String getCollectionName() {
        return AutomationPackageSchedule.FIELD_NAME_IN_AP;
    }

    @Override
    public boolean dependsOnOtherEntities() {
        return true;
    }
}
