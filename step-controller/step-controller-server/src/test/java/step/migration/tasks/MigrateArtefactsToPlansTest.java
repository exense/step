package step.migration.tasks;

import java.util.List;

import org.junit.Test;

import step.artefacts.CallPlan;
import step.artefacts.Sequence;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.migration.MigrationContext;

public class MigrateArtefactsToPlansTest {

	private CollectionFactory collectionFactory;
	private Collection<Document> artefactCollection;
	private Collection<Document> functionCollection;

	public MigrateArtefactsToPlansTest() {
		super();
		
		collectionFactory = new InMemoryCollectionFactory(null);
		artefactCollection = collectionFactory.getCollection("artefacts", Document.class);
		functionCollection = collectionFactory.getCollection("functions", Document.class);
	}
	
	@Test
	public void test() {
		Document artefact1 = artefact(Sequence.class, true);
		Document artefact2 = artefact(Sequence.class, false);
		Document artefact3 = artefact(Sequence.class, false);
		artefact1.put("childrenIDs", List.of(artefact2.get(AbstractIdentifiableObject.ID)));
		artefact2.put("childrenIDs", List.of(artefact3.get(AbstractIdentifiableObject.ID)));
		
		Document callPlan1 = artefact(CallPlan.class, true);
		callPlan1.put("artefactId", artefact1.get(AbstractIdentifiableObject.ID).toString());
		
		Document compositeFunction1 = new Document();
		compositeFunction1.put("type", "step.plugins.functions.types.CompositeFunction");
		compositeFunction1.put("artefactId", artefact1.get(AbstractIdentifiableObject.ID).toString());
		functionCollection.save(compositeFunction1);
		
		MigrateArtefactsToPlans migrationTask = new MigrateArtefactsToPlans(collectionFactory, new MigrationContext());
		migrationTask.runUpgradeScript();
	}

	private Document artefact(Class<?> artefactClass, boolean root) {
		Document artefact1 = new Document();
		Document attributes = new Document();
		attributes.put(AbstractOrganizableObject.NAME, "Name1");
		artefact1.put("attributes", attributes);
		artefact1.put("root", root);
		artefact1.put("_class", artefactClass.getSimpleName());
		artefact1 = artefactCollection.save(artefact1);
		return artefact1;
	}

}
