package step.functions.packages.migration;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.migration.MigrationManager;
import step.migration.tasks.MigrationManagerTasksPlugin;

/**
 * This plugin is responsible for the registration of the migration tasks
 */
@Plugin(dependencies= {MigrationManagerTasksPlugin.class})
public class MigrationTasksRegistrationPlugin  extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		MigrationManager migrationManager = context.get(MigrationManager.class);
		migrationManager.register(ScreenTemplateFunctionTableExtensionsMigrationTask.class);
	}
}
