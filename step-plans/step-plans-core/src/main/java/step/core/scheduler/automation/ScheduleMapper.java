package step.core.scheduler.automation;

import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapper;
import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapping;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapper;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapping;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.model.ExecutionParameters;
import step.core.scheduler.CronExclusion;
import step.core.scheduler.ExecutiontTaskParameters;

import java.util.HashMap;
import java.util.Map;

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

@BusinessObjectToYamlMapping(sourceClass = ExecutiontTaskParameters.class)
@YamlToBusinessObjectMapping
public class ScheduleMapper implements BusinessObjectToYamlMapper<ExecutiontTaskParameters, AutomationPackageSchedule>,
    YamlToBusinessObjectMapper<AutomationPackageSchedule, ExecutiontTaskParameters> {

    @Override
    public AutomationPackageSchedule toYamlObject(ExecutiontTaskParameters parameters) {
        AutomationPackageSchedule yamlSchedule = new AutomationPackageSchedule(null);
        yamlSchedule.setExecutionParameters(parameters.getExecutionsParameters().getCustomParameters());
        yamlSchedule.setName(parameters.getAttribute(AbstractOrganizableObject.NAME));
        yamlSchedule.setActive(parameters.isActive());
        yamlSchedule.setPlanName(parameters.getExecutionsParameters().getPlan().getAttribute(AbstractOrganizableObject.NAME));
        yamlSchedule.setCron(parameters.getCronExpression());
        yamlSchedule.setCronExclusions(parameters.getCronExclusions().stream().map(CronExclusion::getCronExpression).toList());
        return yamlSchedule;
    }

    @Override
    public ExecutiontTaskParameters toBusinessObject(AutomationPackageSchedule yamlSchedule) {
        ExecutiontTaskParameters parameters = new ExecutiontTaskParameters();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(AbstractOrganizableObject.NAME, yamlSchedule.getName());
        parameters.setAttributes(attributes);

        parameters.setActive(yamlSchedule.getActive());
        parameters.setCronExpression(yamlSchedule.getCron());
        parameters.setCronExclusions(yamlSchedule.getCronExclusions().stream().map(e -> new CronExclusion(e, "")).toList());
        parameters.setExecutionsParameters(new ExecutionParameters());
        return parameters;
    }

    @Override
    public String getCollectionName() {
        return AutomationPackageSchedule.FIELD_NAME_IN_AP;
    }

}
