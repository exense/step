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

    protected DynamicValue<Integer> start = null;

    protected DynamicValue<Integer> end = null;

    protected DynamicValue<Integer> inc = null;

    public YamlForBlock() {
        super(ForBlock.class);
    }

    @Override
    protected void fillArtefactFields(ForBlock res) {
        super.fillArtefactFields(res);
        IntSequenceDataPool dataPoolConfiguration = (IntSequenceDataPool) res.getDataSource();
        if (start != null) {
            dataPoolConfiguration.setStart(start);
        }
        if (end != null) {
            dataPoolConfiguration.setEnd(end);
        }
        if (inc != null) {
            dataPoolConfiguration.setInc(inc);
        }
    }

    @Override
    protected void fillYamlArtefactFields(ForBlock artefact) {
        super.fillYamlArtefactFields(artefact);
        IntSequenceDataPool dataPoolConfiguration = (IntSequenceDataPool) artefact.getDataSource();
        if (dataPoolConfiguration.getStart() != null) {
            start = dataPoolConfiguration.getStart();
        }
        if (dataPoolConfiguration.getEnd() != null) {
            end = dataPoolConfiguration.getEnd();
        }
        if (dataPoolConfiguration.getInc() != null) {
            inc = dataPoolConfiguration.getInc();
        }
    }
}
