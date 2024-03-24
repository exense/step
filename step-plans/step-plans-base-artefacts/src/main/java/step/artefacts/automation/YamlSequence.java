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

import step.artefacts.Sequence;
import step.automation.packages.AutomationPackageNamedEntity;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.plans.parser.yaml.model.AbstractYamlArtefact;
import step.plans.parser.yaml.model.YamlArtefact;

@YamlArtefact(forClass = Sequence.class)
@AutomationPackageNamedEntity(name = "sequence")
public class YamlSequence extends AbstractYamlArtefact<Sequence> {

    private DynamicValue<Boolean> continueOnError = new DynamicValue<Boolean>(false);

    private DynamicValue<Long> pacing = new DynamicValue<Long>();

    public YamlSequence() {
        this.artefactClass = Sequence.class;
    }

    @Override
    protected void fillArtefactFields(Sequence res) {
        super.fillArtefactFields(res);
        res.setContinueOnError(this.getContinueOnError());
        res.setPacing(this.getPacing());
    }

    @Override
    protected void fillYamlArtefactFields(Sequence artefact) {
        super.fillYamlArtefactFields(artefact);
        this.setContinueOnError(artefact.getContinueOnError());
        this.setPacing(artefact.getPacing());
    }

    public DynamicValue<Boolean> getContinueOnError() {
        return continueOnError;
    }

    public void setContinueOnError(DynamicValue<Boolean> continueOnError) {
        this.continueOnError = continueOnError;
    }

    public DynamicValue<Long> getPacing() {
        return pacing;
    }

    public void setPacing(DynamicValue<Long> pacing) {
        this.pacing = pacing;
    }
}
