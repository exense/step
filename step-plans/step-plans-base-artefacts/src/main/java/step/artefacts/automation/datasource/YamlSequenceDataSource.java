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
package step.artefacts.automation.datasource;

import step.core.dynamicbeans.DynamicValue;
import step.datapool.sequence.IntSequenceDataPool;

public class YamlSequenceDataSource extends AbstractYamlDataSource<IntSequenceDataPool> {

    protected DynamicValue<Integer> start = new DynamicValue<Integer>(1);

    protected DynamicValue<Integer> end = new DynamicValue<Integer>(2);

    protected DynamicValue<Integer> inc = new DynamicValue<Integer>(1);

    public YamlSequenceDataSource() {
        super("sequence");
    }

    @Override
    public void fillDataPoolConfiguration(IntSequenceDataPool res) {
        super.fillDataPoolConfiguration(res);
        if (start != null) {
            res.setStart(start);
        }
        if (end != null) {
            res.setEnd(end);
        }
        if (inc != null) {
            res.setInc(inc);
        }
    }

    @Override
    public void fillFromDataPoolConfiguration(IntSequenceDataPool dataPoolConfiguration, boolean isForWriteEditable) {
        super.fillFromDataPoolConfiguration(dataPoolConfiguration, isForWriteEditable);
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

    public DynamicValue<Integer> getStart() {
        return start;
    }

    public void setStart(DynamicValue<Integer> start) {
        this.start = start;
    }

    public DynamicValue<Integer> getEnd() {
        return end;
    }

    public void setEnd(DynamicValue<Integer> end) {
        this.end = end;
    }

    public DynamicValue<Integer> getInc() {
        return inc;
    }

    public void setInc(DynamicValue<Integer> inc) {
        this.inc = inc;
    }
}
