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
package step.datapool.inmemory;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.AbstractArtefactTest;
import ch.exense.commons.core.model.dynamicbeans.DynamicValue;
import step.datapool.DataPoolFactory;
import step.datapool.DataPoolRow;
import step.datapool.DataSet;

public class JsonStringDataPoolTest extends AbstractArtefactTest {

	@Test
	public void testEmpty() {;

		JsonStringDataPoolConfiguration poolConf = new JsonStringDataPoolConfiguration();
		poolConf.setJson(new DynamicValue<String>("{}"));
		
		DataSet<?> pool = DataPoolFactory.getDataPool("json", poolConf, newExecutionContext());

		pool.init();

		Assert.assertNull(pool.next());
		
		pool.close();
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void testJsonDataPool() {
		

		JsonStringDataPoolConfiguration poolConf = new JsonStringDataPoolConfiguration();
		poolConf.setJson(new DynamicValue<String>("{ \"a\" : [\"va1\", \"va2\", \"va3\"], \"b\" : [\"vb1\", \"vb2\", \"vb3\"] }"));

		DataSet<?> pool = DataPoolFactory.getDataPool("json", poolConf, newExecutionContext());

		pool.init();
		pool.next();
		DataPoolRow row = pool.next();
		pool.close();
		
		Assert.assertEquals("vb2", ((Map)row.getValue()).get("b"));
	}
}
