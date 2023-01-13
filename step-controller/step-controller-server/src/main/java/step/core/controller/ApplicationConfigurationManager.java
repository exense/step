package step.core.controller;

import ch.exense.commons.app.Configuration;

public class ApplicationConfigurationManager {

	public ApplicationConfigurationBuilder getDefaultBuilder(Configuration configuration) {
		return new ApplicationConfigurationBuilder()
				.setDebug(configuration.getPropertyAsBoolean("debug", false))
				.setDemo(configuration.getPropertyAsBoolean("demo", false))
				.setDefaultUrl(configuration.getProperty("ui.default.url", null))
				.setAuthentication(false)
				.setNoLoginMask(true)
				.setUserManagement(false)
				.setRoleManagement(false)
				.setProjectMembershipManagement(false)
				.setPasswordManagement(false)
				.setTitle(configuration.getProperty("ui.title", "Step"));
	}
}
