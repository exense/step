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
package step.datapool.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Test;

import step.artefacts.AbstractArtefactTest;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.DataPoolFactory;
import step.datapool.DataPoolRow;
import step.datapool.DataSet;

public class JsonArrayDataPoolTest extends AbstractArtefactTest {

	@Test
	public void testEmpty() {;
		JsonArrayDataPoolConfiguration configuration = new JsonArrayDataPoolConfiguration();
		configuration.setJson(new DynamicValue<String>("[]"));
		
		DataSet<?> pool = DataPoolFactory.getDataPool("json-array", configuration, newExecutionContext());

		pool.init();

		assertNull(pool.next());
		
		pool.close();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testJsonDataPool() {
		JsonArrayDataPoolConfiguration configuration = new JsonArrayDataPoolConfiguration();
		configuration.setJson(new DynamicValue<String>("[ {\"a\" : \"va1\", \"b\" : \"vb1\"}, {\"a\" : \"va2\", \"b\" : \"vb2\"}, {\"a\" : 1}, {\"a\" : []}]"));

		DataSet<?> pool = DataPoolFactory.getDataPool("json-array", configuration, newExecutionContext());

		pool.init();
		pool.next();
		DataPoolRow row = pool.next();
		assertEquals("vb2", ((Map<String, String>)row.getValue()).get("b"));
		
		row = pool.next();
		assertEquals("1", ((Map<String, String>)row.getValue()).get("a"));
		
		row = pool.next();
		assertEquals("[]", ((Map<String, String>)row.getValue()).get("a"));
		
		pool.reset();
		
		row = pool.next();
		assertEquals("va1", ((Map<String, String>)row.getValue()).get("a"));
		
		pool.close();
	}
	
	@Test
	public void testInvalidFormat() {
		JsonArrayDataPoolConfiguration configuration = new JsonArrayDataPoolConfiguration();
		configuration.setJson(new DynamicValue<String>("{}"));

		Exception actualException = null;
		try {
			DataPoolFactory.getDataPool("json-array", configuration, newExecutionContext());
		} catch(Exception e) {
			actualException = e;
		}
		assertNotNull(actualException);
	}
}
