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

import step.artefacts.Return;
import step.core.yaml.YamlFieldCustomCopy;
import step.core.yaml.model.AbstractYamlArtefact;

public class YamlReturn extends AbstractYamlArtefact<Return> {

    @YamlFieldCustomCopy
    protected YamlDynamicInputs output = new YamlDynamicInputs("{}");

    public YamlReturn() {
        super(Return.class);
    }

    @Override
    protected void fillArtefactFields(Return res) {
        super.fillArtefactFields(res);
        if (this.output != null) {
            res.setOutput(this.output.toDynamicValue());
        }
    }

    @Override
    protected void fillYamlArtefactFields(Return artefact) {
        super.fillYamlArtefactFields(artefact);
        if (artefact.getOutput() != null) {
            this.output = YamlDynamicInputs.fromDynamicValue(artefact.getOutput());
        }
    }
}
