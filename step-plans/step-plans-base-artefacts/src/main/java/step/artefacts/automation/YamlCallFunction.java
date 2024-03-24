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
package step.artefacts.automation;

import step.artefacts.CallFunction;
import step.automation.packages.AutomationPackageNamedEntity;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.plans.parser.yaml.model.AbstractYamlArtefact;
import step.plans.parser.yaml.model.YamlArtefact;

@YamlArtefact(forClass = CallFunction.class)
@AutomationPackageNamedEntity(name = "callKeyword")
public class YamlCallFunction extends AbstractYamlArtefact<CallFunction> {

    private YamlDynamicInputs routing = new YamlDynamicInputs("{}");

    private DynamicValue<Boolean> remote = new DynamicValue<>(true);

    private DynamicValue<String> resultMap = new DynamicValue<>();

    private YamlDynamicInputs inputs = new YamlDynamicInputs("{}");

    private YamlKeywordDefinition keyword;

    public YamlCallFunction() {
        this.artefactClass = CallFunction.class;
    }

    @Override
    protected void fillArtefactFields(CallFunction res) {
        super.fillArtefactFields(res);
        res.setRemote(this.remote);
        res.setResultMap(this.resultMap);
        res.setToken(this.routing.toDynamicValue());
        res.setArgument(this.inputs.toDynamicValue());
    }

    @Override
    protected String getDefaultArtefactName() {
        if (keyword != null && keyword.getKeywordName() != null && !keyword.getKeywordName().isEmpty()) {
            return keyword.getKeywordName();
        } else {
            return AbstractArtefact.getArtefactName(getArtefactClass());
        }
    }

}
