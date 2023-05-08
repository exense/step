package step.core.controller;

import step.core.collections.inmemory.InMemoryCollection;

public class InMemoryControllerSettingAccessor extends ControllerSettingAccessorImpl implements ControllerSettingAccessor {

	public InMemoryControllerSettingAccessor() {
		super(new InMemoryCollection<ControllerSetting>());
	}

	protected ControllerSetting copy(ControllerSetting controllerSetting){
		if(controllerSetting != null) {
			ControllerSetting copy = new ControllerSetting(controllerSetting.getKey(), controllerSetting.getValue());
			copy.setId(controllerSetting.getId());
			return copy;
		}
		return null;
	}

}
