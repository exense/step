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
package step.script;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.io.OutputMessage;
import step.script.ScriptRunner.ScriptContext;

public class AnnotatedMethodHandlerTest {

	@Test
	public void testProperties() throws Exception {
		Map<String, String> properties = new HashMap<>();
		properties.put("test", "test");
		ScriptContext ctx = ScriptRunner.getExecutionContext(properties);
		OutputMessage out = ctx.run("testProperties", "{}");
		Assert.assertEquals("test", out.getPayload().getString("test"));
	}
	
	@Test
	public void testProperties2() throws Exception {
		Map<String, String> properties = new HashMap<>();
		properties.put("test", "test");
		ScriptContext ctx = ScriptRunner.getExecutionContext();
		OutputMessage out = ctx.run("testProperties", "{}", properties);
		Assert.assertEquals("test", out.getPayload().getString("test"));
	}
	
	@Test
	public void test() throws Exception {
		Map<String, String> properties = new HashMap<>();
		ScriptContext ctx = ScriptRunner.getExecutionContext(properties);
		OutputMessage out = ctx.run("testFunction", "{\"test\":\"test\"}");
		Assert.assertEquals("test", out.getPayload().getString("test"));
	}
	
	@Test
	public void testByMethodName() throws Exception {
		Map<String, String> properties = new HashMap<>();
		ScriptContext ctx = ScriptRunner.getExecutionContext(properties);
		OutputMessage out = ctx.run("testFunctionByMethodName", "{\"test\":\"test\"}");
		Assert.assertEquals("test", out.getPayload().getString("test"));
	}

	@Function(name = "testFunction")
	public static OutputMessage test(@Arg("test")String test) throws Exception {
		OutputMessageBuilder output = new OutputMessageBuilder();
		output.add("test", test);
		return output.build();
	}
	
	@Function
	public static OutputMessage testFunctionByMethodName(@Arg("test")String test) throws Exception {
		OutputMessageBuilder output = new OutputMessageBuilder();
		output.add("test", test);
		return output.build();
	}
	
	@Function
	public static OutputMessage testProperties(@Prop("test")String propTest) throws Exception {
		OutputMessageBuilder output = new OutputMessageBuilder();
		output.add("test", propTest);
		return output.build();
	}
}
