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

import step.automation.packages.StagingAutomationPackageContext;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapper;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapping;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.functions.Function;

@YamlToBusinessObjectMapping(sourceClass = YamlAutomationPackageKeyword.class)
public class YamlToFunctionMapper implements YamlToBusinessObjectMapper<Function, YamlAutomationPackageKeyword> {

    private final StagingAutomationPackageContext stagingContext;

    public YamlToFunctionMapper(StagingAutomationPackageContext stagingContext) {
        this.stagingContext = stagingContext;
    }

    @Override
    public Function toBusinessObject(YamlAutomationPackageKeyword yamlKeyword) {
        return yamlKeyword.prepareKeyword(stagingContext);
    }

    @Override
    public String getCollectionName() {
        return YamlAutomationPackageKeyword.KEYWORDS_ENTITY_NAME;
    }
}
