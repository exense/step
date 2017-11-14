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
package step.plugins.jmeter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import step.core.dynamicbeans.DynamicValue;
import step.functions.Function;
import step.functions.Output;
import step.functions.runner.FunctionRunner;
import step.functions.runner.FunctionRunner.Context;
import step.grid.bootstrap.ResourceExtractor;

public class JMeterHandlerTest {

	@Test 
	public void test1() {
		JMeterFunction f = buildTestFunction();
		Output output = run(f, "{}");
		Assert.assertNull(output.getError());
		Assert.assertNotNull(output.getResult().get("samples"));
	}
	private Output run(JMeterFunction f, String inputJson) {
		Context ctx = FunctionRunner.getContext(new JMeterFunctionType());
		ctx.getGlobalContext().getConfiguration().put("plugins.jmeter.home", "../../distribution/template-controller/ext/jmeter");
		return FunctionRunner.getContext(new JMeterFunctionType()).run(f, inputJson, new HashMap<>());
	}
	
	private JMeterFunction buildTestFunction() {
		File file = ResourceExtractor.extractResource(this.getClass().getClassLoader(), "scripts/Demo_JMeter.jmx");
		JMeterFunction f = new JMeterFunction();
		
		f.setJmeterTestplan(new DynamicValue<String>(file.getAbsolutePath()));

		//f.setLibrariesFile(new DynamicValue<>());
		f.setId(new ObjectId());
		Map<String, String> attributes = new HashMap<>();
		attributes.put(Function.NAME, "medor");
		f.setAttributes(attributes);

		//f.setScriptFile(new DynamicValue<String>(getScriptDir() + "/" + scriptFile));
		return f;
	}
}
