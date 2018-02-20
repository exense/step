/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/

package step.datapool.sequence;

import org.junit.Test;

import junit.framework.Assert;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.DataPoolFactory;
import step.datapool.DataSet;
import step.datapool.sequence.IntSequenceDataPool;

public class IntSequenceDataPoolTest {

	@Test
	public void testCSVReaderDataPool() {
		
		int nbIncrementsWanted = 3;
		
		IntSequenceDataPool poolConf = new IntSequenceDataPool();
		poolConf.setStart(new DynamicValue<Integer>(1));
		poolConf.setEnd(new DynamicValue<Integer>(10));
		poolConf.setInc(new DynamicValue<Integer>(1));

		DataSet<?> pool = DataPoolFactory.getDataPool("sequence", poolConf, null);

		pool.init();
		Integer value = incrementNtimes(pool, nbIncrementsWanted);
		pool.close();
		
		Assert.assertEquals(nbIncrementsWanted, value.intValue());
	}

	private Integer incrementNtimes(DataSet<?> pool, int n) {
		int result = 0;
		for(int i = 0; i < n; i++)
			result = increment(pool);
		return result;
	}

	private Integer increment(DataSet<?> pool) {
		return (Integer)pool.next().getValue();
	}

}
