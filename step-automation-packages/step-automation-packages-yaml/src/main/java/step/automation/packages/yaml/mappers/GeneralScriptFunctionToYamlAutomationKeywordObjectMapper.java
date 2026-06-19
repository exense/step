package step.automation.packages.yaml.mappers;

import step.automation.packages.StagingAutomationPackageContext;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.functions.Function;
import step.plugins.java.GeneralFunctionScriptLanguage;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.java.automation.YamlGeneralScriptFunction;

import java.util.Optional;

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
@ObjectToYamlMapping(organizableObject = GeneralScriptFunction.class)
public class GeneralScriptFunctionToYamlAutomationKeywordObjectMapper extends FunctionToYamlAutomationKeywordObjectMapper<GeneralScriptFunction> {

    private final StagingAutomationPackageContext stagingContext;

    public GeneralScriptFunctionToYamlAutomationKeywordObjectMapper(StagingAutomationPackageContext stagingContext) {
        this.stagingContext = stagingContext;
    }

    @Override
    public YamlAutomationPackageKeyword getNewYamlObject(GeneralScriptFunction generalScriptFunction) {

        YamlGeneralScriptFunction yamlFunction = new YamlGeneralScriptFunction();
        setCommonAtributes(generalScriptFunction, yamlFunction);

        yamlFunction.setScriptFile(generalScriptFunction.getScriptFile());
        yamlFunction.setLibrariesFile(generalScriptFunction.getLibrariesFile());
        yamlFunction.setScriptLanguage(GeneralFunctionScriptLanguage.valueOf(generalScriptFunction.getScriptLanguage().getValue()));

        return new YamlAutomationPackageKeyword(yamlFunction, null);
    }

    @Override
    public Optional<GeneralScriptFunction> getBusinessObject(YamlAutomationPackageKeyword yamlKeyword) {
        Function function = yamlKeyword.prepareKeyword(stagingContext);
        if (function instanceof GeneralScriptFunction generalScriptFunction) {
            return Optional.of(generalScriptFunction);
        }
        return Optional.empty();
    }
}
