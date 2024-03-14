package step.functions.packages.migration;

import java.util.List;

import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.functions.packages.FunctionPackagePlugin;
import step.migration.MigrationContext;
import step.migration.MigrationTask;
import step.plugins.screentemplating.ScreenTemplatePlugin;

public class ScreenTemplateFunctionTableExtensionsMigrationTask extends MigrationTask {

	private final Collection<Document> screenInputs;

	public ScreenTemplateFunctionTableExtensionsMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3, 14, 0), collectionFactory, migrationContext);

		screenInputs = getDocumentCollection("screenInputs");
	}

	@Override
	public void runUpgradeScript() {
		screenInputs
				.find(Filters.and(List.of(Filters.equals("screenId", ScreenTemplatePlugin.FUNCTION_TABLE_EXTENSIONS),
						Filters.equals("input.id", "customFields.functionPackageId"))), null, null, null, 0)
				.forEach(t -> {
					Document input = (Document) t.get("input");
					if (input != null) {
						input.put("searchMapperService", "rest/table/functionPackage/searchIdsBy/attributes.name");

						screenInputs.save(t);
						logger.info("Migrating screen input of type " + ScreenTemplatePlugin.FUNCTION_TABLE_EXTENSIONS
								+ " to " + t);
					} else {
						logger.warn("Migrating screen input of type " + ScreenTemplatePlugin.FUNCTION_TABLE_EXTENSIONS
								+ " failed: no input found in " + t);
					}
				});
	}

	@Override
	public void runDowngradeScript() {

	}
}