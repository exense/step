package step.functions.packages.migration;

import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.HashSet;
import java.util.Set;

public class PlansInCompositeFunctionsMigrationTask extends MigrationTask {

	private final Collection<Document> functions;
	private final Collection<Document> plans;

	public PlansInCompositeFunctionsMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3, 21, 2), collectionFactory, migrationContext);

		functions = getDocumentCollection("functions");
		plans = getDocumentCollection("plans");
	}

	@Override
	public void runUpgradeScript() {
		Set<String> planIds = new HashSet<>();

		// store plans linked with composite functions in functions collection instead of plans collection
		functions
				.find(Filters.equals("type", "step.plugins.functions.types.CompositeFunction"), null, null, null, 0)
				.forEach(f -> {
					String planId = f.getString("planId");
					if (planId != null) {
						logger.info("Migrating the plan {} for the composite function {}", planId, f.getId().toString());
						Document plan = this.plans.find(Filters.id(planId), null, null, null, 0).findFirst().orElse(null);
						if (plan == null) {
							logger.error("Plan not found by id: {}. Skipping...", planId);
						} else {
							planIds.add(planId);

							f.remove("planId");
							f.put("plan", plan);
							functions.save(f);
							logger.info("The plan has been migrated for composite function {}", f.getId().toString());
						}

					} else {
						logger.info("Composite function {} is already migrated. Skipping...", f.getId().toString());
					}
				});

		logger.info("Removing {} old plans linked with composite functions...", planIds.size());
		int plansCount = 0;
		for (String planId : planIds) {
			Document plan = this.plans.find(Filters.id(planId), null, null, null, 0).findFirst().orElse(null);
			if (plan == null) {
				logger.error("Plan not found by id: {}. Skipping...", planId);
			} else {
				if (!plan.getBoolean("visible")) {
					plans.remove(Filters.id(plan.getId()));
					logger.info("The plan {} has been removed", plan.getId().toString());
					plansCount++;
				}
			}
		}
		logger.info("{} plans have been removed", plansCount);
	}

	@Override
	public void runDowngradeScript() {

	}
}