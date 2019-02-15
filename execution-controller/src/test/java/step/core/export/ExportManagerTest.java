package step.core.export;

import java.io.File;
import java.io.FileOutputStream;
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
import step.core.plans.builder.PlanBuilder;
import step.planbuilder.BaseArtefacts;

public class ExportManagerTest {

	@Test
	public void testExportArtefactWithChildren() throws IOException, InterruptedException, TimeoutException {
		GlobalContext c = GlobalContextBuilder.createGlobalContext();
		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(BaseArtefacts.sequence()).endBlock().build();
		LocalPlanRepository repo = new LocalPlanRepository(c.getArtefactAccessor());
		repo.save(plan);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c.getArtefactAccessor());
			exportManager.exportArtefactWithChildren(plan.getRoot().getId().toString(), outputStream);
			
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
		Sequence rootSequence = BaseArtefacts.sequence();
		rootSequence.setRoot(true);
		Plan plan = PlanBuilder.create().startBlock(rootSequence).add(BaseArtefacts.sequence()).endBlock().build();
		LocalPlanRepository repo = new LocalPlanRepository(c.getArtefactAccessor());
		repo.save(plan);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c.getArtefactAccessor());
			exportManager.exportAllArtefacts(outputStream);
			
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
