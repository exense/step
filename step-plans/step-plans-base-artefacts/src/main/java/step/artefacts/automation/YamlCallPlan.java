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

import step.artefacts.CallPlan;
import step.core.yaml.YamlFieldCustomCopy;
import step.plans.parser.yaml.model.AbstractYamlArtefact;

public class YamlCallPlan extends AbstractYamlArtefact<CallPlan> {

    private String planId;

    @YamlFieldCustomCopy
    private YamlDynamicInputs selectionAttributes = new YamlDynamicInputs("{}");

    @YamlFieldCustomCopy
    protected YamlDynamicInputs input = new YamlDynamicInputs("{}");

    public YamlCallPlan() {
        super(CallPlan.class);
    }

    @Override
    protected void fillArtefactFields(CallPlan res) {
        super.fillArtefactFields(res);
        if (this.input != null) {
            res.setInput(this.input.toDynamicValue());
        }
        if (this.selectionAttributes != null) {
            res.setSelectionAttributes(this.selectionAttributes.toDynamicValue());
        }
    }

    @Override
    protected void fillYamlArtefactFields(CallPlan artefact) {
        super.fillYamlArtefactFields(artefact);
        if (artefact.getInput() != null) {
            this.input = YamlDynamicInputs.fromDynamicValue(artefact.getInput());
        }
        if (artefact.getSelectionAttributes() != null) {
            this.selectionAttributes = YamlDynamicInputs.fromDynamicValue(artefact.getSelectionAttributes());
        }

    }
}
