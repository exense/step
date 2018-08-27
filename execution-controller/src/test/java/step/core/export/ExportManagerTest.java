package step.core.export;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.Sequence;
import step.core.GlobalContext;
import step.core.GlobalContextBuilder;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.InMemoryArtefactAccessor;
import step.core.plans.LocalPlanRepository;
import step.core.plans.Plan;
import step.planbuilder.PlanBuilder;

public class ExportManagerTest {

	@Test
	public void testExportArtefactWithChildren() throws IOException, InterruptedException, TimeoutException {
		GlobalContext c = GlobalContextBuilder.createGlobalContext();
		Plan plan = PlanBuilder.create().startBlock(PlanBuilder.sequence()).add(PlanBuilder.sequence()).endBlock().build();
		LocalPlanRepository repo = new LocalPlanRepository(c.getArtefactAccessor());
		repo.save(plan);
		
		File testExportFile = new File("testExport.json");
		try {
			ExportManager exportManager = new ExportManager(c.getArtefactAccessor());
			exportManager.exportArtefactWithChildren(plan.getRoot().getId().toString(), testExportFile);
			
			InMemoryArtefactAccessor artefactAccessor = new InMemoryArtefactAccessor();
			ImportManager importManager = new ImportManager(artefactAccessor);
			importManager.importArtefacts(testExportFile);
			
			Collection<? extends AbstractArtefact> artefacts = artefactAccessor.getCollection();
			Assert.assertEquals(2, artefacts.size());			
		} finally {
			testExportFile.delete();
		}
	}
	
	@Test
	public void testExportAllArtefacts() throws IOException, InterruptedException, TimeoutException {
		GlobalContext c = GlobalContextBuilder.createGlobalContext();
		Sequence rootSequence = PlanBuilder.sequence();
		rootSequence.setRoot(true);
		Plan plan = PlanBuilder.create().startBlock(rootSequence).add(PlanBuilder.sequence()).endBlock().build();
		LocalPlanRepository repo = new LocalPlanRepository(c.getArtefactAccessor());
		repo.save(plan);
		
		File testExportFile = new File("testExport.json");
		try {
			ExportManager exportManager = new ExportManager(c.getArtefactAccessor());
			exportManager.exportAllArtefacts(testExportFile);
			
			InMemoryArtefactAccessor artefactAccessor = new InMemoryArtefactAccessor();
			ImportManager importManager = new ImportManager(artefactAccessor);
			importManager.importArtefacts(testExportFile);
			
			Collection<? extends AbstractArtefact> artefacts = artefactAccessor.getCollection();
			Assert.assertEquals(2, artefacts.size());			
		} finally {
			testExportFile.delete();
		}
	}
}
