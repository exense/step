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

import step.artefacts.FunctionGroup;
import step.core.dynamicbeans.DynamicValue;

public class YamlFunctionGroup extends YamlTokenSelector<FunctionGroup> {

    private DynamicValue<String> dockerImage = new DynamicValue<>();

    private DynamicValue<String> containerUser = new DynamicValue<>();

    private DynamicValue<String> containerCommand = new DynamicValue<>();

    public YamlFunctionGroup() {
        super(FunctionGroup.class);
    }

    @Override
    protected void fillArtefactFields(FunctionGroup res) {
        super.fillArtefactFields(res);
        if (dockerImage != null) {
            res.setDockerImage(dockerImage);
        }
        if (containerCommand != null) {
            res.setContainerCommand(containerCommand);
        }
        if (containerUser != null) {
            res.setContainerUser(containerUser);
        }
    }

    @Override
    protected void fillYamlArtefactFields(FunctionGroup artefact) {
        super.fillYamlArtefactFields(artefact);
        if (artefact.getDockerImage() != null) {
            this.dockerImage = artefact.getDockerImage();
        }
        if (artefact.getContainerUser() != null) {
            this.containerUser = artefact.getContainerUser();
        }
        if (artefact.getContainerCommand() != null) {
            this.containerCommand = artefact.getContainerCommand();
        }
    }
}
