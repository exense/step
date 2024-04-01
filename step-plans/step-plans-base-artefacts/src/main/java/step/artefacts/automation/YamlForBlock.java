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

import step.artefacts.ForBlock;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.sequence.IntSequenceDataPool;

public class YamlForBlock extends AbstractYamlForBlock<ForBlock> {

    protected DynamicValue<Integer> start = new DynamicValue<Integer>(1);

    protected DynamicValue<Integer> end = new DynamicValue<Integer>(2);

    protected DynamicValue<Integer> inc = new DynamicValue<Integer>(1);

    public YamlForBlock() {
        super(ForBlock.class);
    }

    @Override
    protected void fillArtefactFields(ForBlock res) {
        super.fillArtefactFields(res);
        IntSequenceDataPool dataPoolConfiguration = (IntSequenceDataPool) res.getDataSource();
        if (this.start != null) {
            dataPoolConfiguration.setStart(this.start);
        }
        if (this.end != null) {
            dataPoolConfiguration.setEnd(this.end);
        }
        if (this.inc != null) {
            dataPoolConfiguration.setInc(this.inc);
        }
    }

    @Override
    protected void fillYamlArtefactFields(ForBlock artefact) {
        super.fillYamlArtefactFields(artefact);
        IntSequenceDataPool dataPoolConfiguration = (IntSequenceDataPool) artefact.getDataSource();
        this.start = dataPoolConfiguration.getStart();
        this.end = dataPoolConfiguration.getEnd();
        this.inc = dataPoolConfiguration.getInc();
    }
}
