/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.migration.tasks;

import step.core.Version;
import step.core.collections.*;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static step.core.collections.CollectionFactory.VERSION_COLLECTION_SUFFIX;
import static step.plans.parser.yaml.migrations.AfterBeforeYamlMigrationTask.*;

/**
 * This migration task will migrate all plans to be compatible with the new artefact properties (before and after for
 * all artefacts, beforeThread and afterThread for thread group). Previously specifc artefact (i.e. BeforeSequence) were
 * used directly as children "property" and need to be moved over to the new class property.
 * The implementation is similar to the YAML migration for the "DB" syntax in AfterBeforeYamlMigrationTask
 */
public class MigrateBeforeAfterAndPropertiesArtefactInPlans extends MigrationTask {

	public static final String ARTEFACT_BEFORE_SEQUENCE = "BeforeSequence";
	public static final String ARTEFACT_AFTER_SEQUENCE = "AfterSequence";
	public static final String ARTEFACT_BEFORE_THREAD = "BeforeThread";
	public static final String ARTEFACT_AFTER_THREAD = "AfterThread";
	public static final String ARTEFACT_PERFORMANCE_ASSERT = "PerformanceAssert";
	private final Collection<Document> planCollection;
	private final Collection<Document> functionCollection;
	private final Collection<Document> planVersionCollection;
	private final Collection<Document> functionsVersionCollection;

	public MigrateBeforeAfterAndPropertiesArtefactInPlans(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3,27,0), collectionFactory, migrationContext);

		planCollection = collectionFactory.getCollection("plans", Document.class);
		planVersionCollection = collectionFactory.getCollection("plans" + VERSION_COLLECTION_SUFFIX, Document.class);
		functionCollection = getDocumentCollection("functions");
		functionsVersionCollection = collectionFactory.getCollection("functions" + VERSION_COLLECTION_SUFFIX, Document.class);
	}
	
	@Override
	public void runUpgradeScript() {
		migratePlans();
		migratePlanVersions();
		migrateCompositeFunction();
		migrateCompositeFunctionVersion();
	}

	private void migratePlans() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		logger.info("Migrating BeforeSequence, AfterSequence, BeforeThread and AfterThread to the respective artefact properties for all plans...");

		planCollection.find(Filters.empty(), null, null, null, 0).forEach(p -> {
			try {
				DocumentObject root = p.getObject("root");
				migrateAllRecursively(root);
				planCollection.save(p);
				successCount.incrementAndGet();
			} catch(Exception e) {
				errorCount.incrementAndGet();
				logger.error("Error while migrating BeforeSequence, AfterSequence, BeforeThread and AfterThread artefacts for plan: {}", p, e);
			}
		});
		logger.info("Migrated {} plans.", successCount.get());
		if(errorCount.get()>0) {
            logger.error("Got {} errors while migrating before and after controls for plans. See previous error logs for details.", errorCount);
		}
	}

	private void migratePlanVersions() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		logger.info("Migrating BeforeSequence, AfterSequence, BeforeThread and AfterThread to the respective artefact properties for all plans versions...");

		planVersionCollection.find(Filters.empty(), null, null, null, 0).forEach(p -> {
			try {
				DocumentObject root = p.getObject("entity").getObject("root");
				migrateAllRecursively(root);
				planVersionCollection.save(p);
				successCount.incrementAndGet();
			} catch(Exception e) {
				errorCount.incrementAndGet();
				logger.error("Error while migrating BeforeSequence, AfterSequence, BeforeThread and AfterThread artefacts for plan version: {}", p, e);
			}
		});
		logger.info("Migrated {} plans versions.", successCount.get());
		if(errorCount.get()>0) {
			logger.error("Got {} errors while migrating before and after controls for plans versions. See previous error logs for details.", errorCount);
		}
	}

	private void migrateCompositeFunction() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		logger.info("Migrating BeforeSequence, AfterSequence, BeforeThread and AfterThread to the respective artefact properties for all composite keywords...");

		functionCollection.find(Filters.equals("type", "step.plugins.functions.types.CompositeFunction"), null, null, null, 0)
				.forEach(c  -> {
			try {
				DocumentObject root = c.getObject("plan").getObject("root");
				migrateAllRecursively(root);
				functionCollection.save(c);
				successCount.incrementAndGet();
			} catch(Exception e) {
				errorCount.incrementAndGet();
				logger.error("Error while migrating BeforeSequence, AfterSequence, BeforeThread and AfterThread artefacts for composite: {}", c, e);
			}
		});
		logger.info("Migrated {} composite keywords.", successCount.get());
		if(errorCount.get()>0) {
			logger.error("Got {} errors while migrating before and after controls for composite keywords. See previous error logs for details.", errorCount);
		}
	}

	private void migrateCompositeFunctionVersion() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		logger.info("Migrating BeforeSequence, AfterSequence, BeforeThread and AfterThread to the respective artefact properties for all composite keyword versions...");

		functionsVersionCollection.find(Filters.equals("entity._entityClass", "step.plugins.functions.types.CompositeFunction"), null, null, null, 0)
				.forEach(c  -> {
					try {
						DocumentObject root = c.getObject("entity").getObject("plan").getObject("root");
						migrateAllRecursively(root);
						functionsVersionCollection.save(c);
						successCount.incrementAndGet();
					} catch(Exception e) {
						errorCount.incrementAndGet();
						logger.error("Error while migrating BeforeSequence, AfterSequence, BeforeThread and AfterThread artefacts for composite version: {}", c, e);
					}
				});
		logger.info("Migrated {} composite keyword versions.", successCount.get());
		if(errorCount.get()>0) {
			logger.error("Got {} errors while migrating before and after controls for composite keywords versions. See previous error logs for details.", errorCount);
		}
	}

	@Override
	public void runDowngradeScript() {

	}

	/**
	 * The implementation is very similar to the one for the DB objects, but the payload is too different to share the same code
	 * @param artifact: the deserilaization of the JSON artefact as DocumentObject to be modified
	 */
	private void migrateAllRecursively(DocumentObject artifact) {
		List<DocumentObject> children = artifact.getArray(ARTEFACT_CHILDREN);
		if (children != null && !children.isEmpty()) {
			//Attach the children of the beforeSequence to the parent before property
			moveToParentSource(artifact, children, ARTEFACT_BEFORE_SEQUENCE, ARTEFACT_BEFORE_PROPERTY);
			//Attach the children of the afterSequence to the parent after property
			moveToParentSource(artifact, children, ARTEFACT_AFTER_SEQUENCE, ARTEFACT_AFTER_PROPERTY);
			moveToParentSource(artifact, children, ARTEFACT_BEFORE_THREAD, ARTEFACT_BEFORE_THREAD_PROPERTY);
			moveToParentSource(artifact, children, ARTEFACT_AFTER_THREAD, ARTEFACT_AFTER_THREAD_PROPERTY);
			moveToParentSource(artifact, children, ARTEFACT_PERFORMANCE_ASSERT, ARTEFACT_AFTER_PROPERTY, false);
			//getArray return a copy, we must explicitly set it back
			artifact.put(ARTEFACT_CHILDREN, children);
			//Process children recursively
			for (DocumentObject child : children) {
				migrateAllRecursively(child);
			}
		}
	}

	private void moveToParentSource(DocumentObject sourceArtifactProperties, List<DocumentObject> sourceChildren, String fromArtefactKey, String toPropertyKey) {
		moveToParentSource(sourceArtifactProperties, sourceChildren, fromArtefactKey, toPropertyKey, true);
	}

	private void moveToParentSource(DocumentObject sourceArtifactProperties, List<DocumentObject> sourceChildren, String fromArtefactKey, String toPropertyKey, boolean moveChildrenOnly) {
		List<DocumentObject> fromArray = sourceChildren.stream().filter(child -> fromArtefactKey.equals(child.getString("_class"))).collect(Collectors.toList());
		for (DocumentObject artifact: fromArray) {
			if (moveChildrenOnly) {
				//This case is used for legacy beforeSequence... artefacts, we move their children to the corresponding source property (i.e. new before property)
				List<DocumentObject> innerChildren = artifact.getArray(ARTEFACT_CHILDREN);
				if (innerChildren != null && !innerChildren.isEmpty()) {
					List<DocumentObject> toSteps = getOrInitPropertySteps(sourceArtifactProperties, artifact.getObject("continueOnError"), toPropertyKey);
					toSteps.addAll(innerChildren);
					//Also process children of legacy before,after Sequence,thread recursively
					moveToParentSource(artifact, innerChildren, fromArtefactKey, toPropertyKey);
				}
			} else {
				//This case is used for instance for PerformanceAssert artefact moved from children to the after children block property
				//Add this children artefact directly to the source property 'toPropertyKey', if the target property is not yet defined use false as default for continueOnError
				List<DocumentObject> toSteps = getOrInitPropertySteps(sourceArtifactProperties, getDefaultContinueOnError(), toPropertyKey);
				toSteps.add(artifact);
			}
		}
		sourceChildren.removeAll(fromArray);
	}

	private Object getDefaultContinueOnError() {
		DocumentObject continueOnError = new DocumentObject();
		continueOnError.put("dynamic", false);
		continueOnError	.put("value", false);
		continueOnError.put("expression", null);
		continueOnError	.put("expressionType", null);
		return continueOnError;
	}
}
