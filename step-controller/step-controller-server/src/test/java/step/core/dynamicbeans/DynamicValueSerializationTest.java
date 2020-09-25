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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.expressions.ExpressionHandler;

public class DynamicValueSerializationTest {

	@Test
	public void testConstants() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		
		TestBean bean = mapper.readValue("{\"testString\":{\"value\":\"test\"}, \"testBoolean\":{\"value\":true}, \"testInteger\":{\"value\":1}}", TestBean.class);
		Assert.assertEquals("test", bean.getTestString().get());
		Assert.assertEquals(true, bean.getTestBoolean().get());
		Assert.assertEquals(1, (int)bean.getTestInteger().get());
	}
	
	@Test
	public void testDynamic() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		
		TestBean bean = mapper.readValue("{\"testString\":{\"dynamic\":true,\"expression\":\"'test'\"},\"testRecursive\":{\"value\":{\"testString\":{\"dynamic\":true,\"expression\":\"'test2'\"}}}}", TestBean.class);
		
		DynamicBeanResolver r = new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler()));
		r.evaluate(bean, null);
		Assert.assertEquals("test", bean.getTestString().get());
		Assert.assertEquals("test2", bean.getTestRecursive().get().getTestString().get());
	}
}
