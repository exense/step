package step.plugins.screentemplating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

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
