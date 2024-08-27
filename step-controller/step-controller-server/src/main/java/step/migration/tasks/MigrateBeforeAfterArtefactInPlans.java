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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static step.plans.parser.yaml.migrations.AfterBeforeYamlMigrationTask.*;

/**
 * This migration task will migrate all plans to be compatible with the new artefact properties (before and after for
 * all artefacts, beforeThread and afterThread for thread group). Previously specifc artefact (i.e. BeforeSequence) were
 * used directly as children "property" and need to be moved over to the new class property.
 * The implementation is similar to the YAML migration for the "DB" syntax in AfterBeforeYamlMigrationTask
 */
public class MigrateBeforeAfterArtefactInPlans extends MigrationTask {

	public static final String ARTEFACT_BEFORE_SEQUENCE = "BeforeSequence";
	public static final String ARTEFACT_AFTER_SEQUENCE = "AfterSequence";
	public static final String ARTEFACT_BEFORE_THREAD = "BeforeThread";
	public static final String ARTEFACT_AFTER_THREAD = "AfterThread";
	private final Collection<Document> planCollection;

	public MigrateBeforeAfterArtefactInPlans(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3,26,0), collectionFactory, migrationContext);

		planCollection = collectionFactory.getCollection("plans", Document.class);
	}
	
	@Override
	public void runUpgradeScript() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		logger.info("Migrating BeforeSequence, AfterSequence, BeforeThread and AfterThread to the respective artefact properties for all plans...");
		
		planCollection.find(Filters.empty(), null, null, null, 0).forEach(p -> {
			try {
				DocumentObject root = p.getObject("root");
				migrateBeforeAfterSequence(root);
				planCollection.save(p);
				successCount.incrementAndGet();
			} catch(Exception e) {
				errorCount.incrementAndGet();
                logger.error("Error while migrating BeforeSequence, AfterSequence, BeforeThread and AfterThread artefacts for plan: {}", p, e);
			}
		});
        logger.info("Migrated {} plans.", successCount.get());
		if(errorCount.get()>0) {
			logger.error("Got "+errorCount+" errors while migrating sleep controls. See previous error logs for details.");
		}
	}

	@Override
	public void runDowngradeScript() {

	}

	/**
	 * The implementation is very similar to the one for the DB objects, but the payload is too different to share the same code
	 * @param artifact: the deserilaization of the JSON artefact as DocumentObject to be modified
	 */
	private void migrateBeforeAfterSequence(DocumentObject artifact) {
		List<DocumentObject> children = artifact.getArray(ARTEFACT_CHILDREN);
		if (children != null && !children.isEmpty()) {
			//Attach the children of the beforeSequence to the parent before property
			moveToParentSource(artifact, children, ARTEFACT_BEFORE_SEQUENCE, ARTEFACT_BEFORE_PROPERTY);
			//Attach the children of the afterSequence to the parent after property
			moveToParentSource(artifact, children, ARTEFACT_AFTER_SEQUENCE, ARTEFACT_AFTER_PROPERTY);
			moveToParentSource(artifact, children, ARTEFACT_BEFORE_THREAD, ARTEFACT_BEFORE_THREAD_PROPERTY);
			moveToParentSource(artifact, children, ARTEFACT_AFTER_THREAD, ARTEFACT_AFTER_THREAD_PROPERTY);
			//getArray return a copy, we must explicitly set it back
			artifact.put(ARTEFACT_CHILDREN, children);
			//Process children recursively
			for (DocumentObject child : children) {
				migrateBeforeAfterSequence(child);
			}
		}
	}

	private void moveToParentSource(DocumentObject sourceArtifactProperties, List<DocumentObject> sourceChildren, String fromArtefactKey, String toPropertyKey) {
		List<DocumentObject> fromArray = sourceChildren.stream().filter(child -> fromArtefactKey.equals(child.getString("_class"))).collect(Collectors.toList());
		for (DocumentObject artifact: fromArray) {
			List<DocumentObject> innerChildren = artifact.getArray(ARTEFACT_CHILDREN);
			if (innerChildren != null && !innerChildren.isEmpty()) {
				DocumentObject toProperty = sourceArtifactProperties.getObject(toPropertyKey);
				if (toProperty == null) {
					toProperty = new DocumentObject();
					toProperty.put("continueOnError", artifact.getObject("continueOnError"));
					sourceArtifactProperties.put(toPropertyKey, toProperty);
				}
				List<DocumentObject> toSteps = toProperty.getArray("steps");
				if (toSteps == null) {
					toSteps = new ArrayList<>();
					toProperty.put("steps", toSteps);
				}
				toSteps.addAll(innerChildren);
				toProperty.put(STEPS, toSteps);
				//Also process children of legacy before,after Sequence,thread recursively
				moveToParentSource(artifact, innerChildren, fromArtefactKey, toPropertyKey);
			}
		}
		sourceChildren.removeAll(fromArray);
	}
}
