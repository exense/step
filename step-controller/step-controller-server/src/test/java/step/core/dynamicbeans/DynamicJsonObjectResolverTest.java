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
package step.core.dynamicbeans;

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.expressions.ExpressionHandler;

public class DynamicJsonObjectResolverTest {

	DynamicJsonObjectResolver resolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(new ExpressionHandler()));
	
	@Test
	public void test1() throws JsonProcessingException {
		TestBean bean = new TestBean();
		
		ObjectMapper m = new ObjectMapper();
		String jsonStr = m.writeValueAsString(bean);
		JsonObject o = Json.createReader(new StringReader(jsonStr)).readObject();
		
		JsonObject output = resolver.evaluate(o, null);
		Assert.assertEquals("test", output.getString("testString"));
		Assert.assertEquals(true, output.getBoolean("testBoolean"));
		Assert.assertEquals(10, output.getInt("testInteger"));
		Assert.assertEquals("test", ((JsonObject)output.getJsonArray("testArray").get(0)).getString("testString"));
		Assert.assertEquals("test", output.getJsonObject("testRecursive2").getString("testString"));
	}
	
	
}
