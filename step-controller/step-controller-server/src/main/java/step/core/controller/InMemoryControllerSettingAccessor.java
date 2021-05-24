package step.core.controller;

import ch.exense.commons.core.collections.inmemory.InMemoryCollection;

public class InMemoryControllerSettingAccessor extends ControllerSettingAccessorImpl implements ControllerSettingAccessor {

	public InMemoryControllerSettingAccessor() {
		super(new InMemoryCollection<ControllerSetting>());
	}
}
