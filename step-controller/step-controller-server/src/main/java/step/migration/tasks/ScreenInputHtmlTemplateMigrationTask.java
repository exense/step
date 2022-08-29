package step.migration.tasks;

import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.DocumentObject;
import step.core.collections.Filters;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.List;

public class ScreenInputHtmlTemplateMigrationTask extends MigrationTask {

	private final Collection<Document> screenInputs;

	public ScreenInputHtmlTemplateMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3,20,0), collectionFactory, migrationContext);
		screenInputs = collectionFactory.getCollection("screenInputs", Document.class);
	}

	@Override
	public void runUpgradeScript() {
		logger.debug("Starting migration of screen input with custom html templates.");
		screenInputs.find(Filters.empty(), null, null,null,0).forEach(d -> {
			boolean unsupported = false;
			String screenId = d.getString("screenId");
			DocumentObject input = d.getObject("input");
			String inputId = input.getString("id");
			String valueHtmlTemplate = (String) input.remove("valueHtmlTemplate");
			if (screenId.equals("functionTable") && inputId.equals("attributes.name")) {
				input.put("customUIComponents", List.of("functionEntityIcon","functionLink"));
			} else if (screenId.equals("functionTableExtensions") && inputId.equals("customFields.functionPackageId")) {
				input.put("customUIComponents", List.of("functionPackageLink"));
			} else if (screenId.equals("planTable") && inputId.equals("attributes.name")) {
				input.put("customUIComponents", List.of("planEntityIcon","planLink"));
			} else if (screenId.equals("schedulerTable") && inputId.equals("attributes.name")) {
				input.put("customUIComponents", List.of("taskEntityIcon","schedulerTaskLink"));
			} else if (screenId.equals("parameterTable") && inputId.equals("key")) {
				input.put("customUIComponents", List.of("parameterEntityIcon","parameterKey"));
			} else {
				if (valueHtmlTemplate != null && ! valueHtmlTemplate.isBlank()) {
					input.put("customUIComponents", List.of("Migration of custom html template to new UI failed. Source html template: " + valueHtmlTemplate));
					unsupported = true;
				} else {
					//this is required since postgresql will have a field valueHtmlTemplate : null
					input.put("customUIComponents", null);
				}
			}
			screenInputs.save(d);
			if (unsupported) {
				logger.error("Failed to migrate html template for screen id: " + screenId + ", input id " + inputId + " and html template " + valueHtmlTemplate);
			} else {
				logger.debug("Migrated screen input for screen " + screenId + " with input id " + inputId);
			}
		});
	}

	@Override
	public void runDowngradeScript() {

	}
}
