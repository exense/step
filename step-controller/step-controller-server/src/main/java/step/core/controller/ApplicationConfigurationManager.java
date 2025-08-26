package step.core.controller;

import ch.exense.commons.app.Configuration;

import static step.framework.server.ControllerServer.UI_CONTEXT_ROOT_CFG_KEY;
import static step.framework.server.ControllerServer.UI_CONTEXT_ROOT_DEFAULT_VALUE;

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
				.setTitle(configuration.getProperty("ui.title", "Step"))
				.setContextRoot(configuration.getProperty(UI_CONTEXT_ROOT_CFG_KEY, UI_CONTEXT_ROOT_DEFAULT_VALUE))
				.setForceLegacyReporting(configuration.getPropertyAsBoolean("ui.reporting.force.legacy", false));
	}
}
