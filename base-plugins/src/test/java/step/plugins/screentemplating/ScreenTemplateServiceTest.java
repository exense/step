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
package step.plugins.screentemplating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.jetty.server.Handler;
import org.junit.Test;

import step.core.Controller.ServiceRegistrationCallback;
import step.core.GlobalContext;

public class ScreenTemplateServiceTest {

	@Test
	public void test() {
		GlobalContext dummyContext = new GlobalContext();
		dummyContext.setServiceRegistrationCallback(new ServiceRegistrationCallback() {
			@Override
			public void registerService(Class<?> serviceClass) {}

			@Override
			public void registerHandler(Handler handler) {}
		});
		ScreenTemplatePlugin s = new ScreenTemplatePlugin();
		s.executionControllerStart(dummyContext);
		
		List<Input> inputs = s.getInputsForScreen("testScreen1", new HashMap<String, Object>());
		Assert.assertEquals(3, inputs.size());
		Assert.assertEquals(inputs.get(0), new Input(InputType.TEXT, "Param1", "Param1",null));
		
		List<Option> options = getDefaultOptionList();
		Assert.assertEquals(inputs.get(1), new Input(InputType.DROPDOWN, "Param2", "Param2",options));
		
		Assert.assertEquals(inputs.get(2), new Input(InputType.TEXT, "Param3", "LabelParam3",options));
		
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		ctx.put("user", "user1");
		inputs = s.getInputsForScreen("testScreen1", ctx);
		Assert.assertEquals(5, inputs.size());
		Assert.assertEquals(inputs.get(2), new Input(InputType.TEXT, "Param3", "LabelParam3",getOptionListForUser1()));
		Assert.assertEquals(inputs.get(3), new Input(InputType.TEXT, "Param4", "LabelParam4",options));
		Assert.assertEquals(inputs.get(4), new Input(InputType.DROPDOWN, "Param5", "Param5",options));
	}

	private List<Option> getDefaultOptionList() {
		List<Option> options = new ArrayList<Option>();
		options.add(new Option("Option1"));
		options.add(new Option("Option2"));
		return options;
	}
	
	private List<Option> getOptionListForUser1() {
		List<Option> options = new ArrayList<Option>(getDefaultOptionList());
		options.add(new Option("Option3"));
		return options;
	}


}
