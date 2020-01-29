package step.core.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.Sequence;
import step.core.GlobalContext;
import step.core.GlobalContextBuilder;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectFilter;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.planbuilder.BaseArtefacts;

public class ExportManagerTest {

	@Test
	public void testExportArtefactWithChildren() throws IOException, InterruptedException, TimeoutException {
		GlobalContext c = GlobalContextBuilder.createGlobalContext();
		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(BaseArtefacts.sequence()).endBlock().build();
		c.getPlanAccessor().save(plan);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c.getPlanAccessor());
			exportManager.exportPlan(plan.getId().toString(), outputStream);
			
			InMemoryPlanAccessor planAccessor = new InMemoryPlanAccessor();
			ImportManager importManager = new ImportManager(planAccessor);
			importManager.importPlans(testExportFile, dummyObjectEnricher());
			
			Plan actualPlan = planAccessor.get(plan.getId());
			Assert.assertEquals(plan.getId(), actualPlan.getId());
			Assert.assertEquals(plan.getRoot(), actualPlan.getRoot());
		} finally {
			testExportFile.delete();
		}
	}

	protected ObjectEnricher dummyObjectEnricher() {
		return new ObjectEnricher() {
			
			@Override
			public void accept(Object t) {
			}
			
			@Override
			public Map<String, String> getAdditionalAttributes() {
				return null;
			}
		};
	}
	
	@Test
	public void testExportAllArtefacts() throws IOException, InterruptedException, TimeoutException {
		GlobalContext c = GlobalContextBuilder.createGlobalContext();
		Sequence rootSequence = BaseArtefacts.sequence();
		Plan plan = PlanBuilder.create().startBlock(rootSequence).add(BaseArtefacts.sequence()).endBlock().build();
		c.getPlanAccessor().save(plan);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c.getPlanAccessor());
			exportManager.exportAllPlans(outputStream, dummyObjectFilter());
			
			InMemoryPlanAccessor planAccessor = new InMemoryPlanAccessor();
			ImportManager importManager = new ImportManager(planAccessor);
			importManager.importPlans(testExportFile, dummyObjectEnricher());
			
			Plan actualPlan = planAccessor.get(plan.getId());
			Assert.assertEquals(plan.getId(), actualPlan.getId());
			Assert.assertEquals(plan.getRoot(), actualPlan.getRoot());
		} finally {
			testExportFile.delete();
		}
	}

	protected ObjectFilter dummyObjectFilter() {
		return new ObjectFilter() {
			
			@Override
			public boolean test(Object t) {
				return true;
			}
			
			@Override
			public Map<String, String> getAdditionalAttributes() {
				return null;
			}
		};
	}
}
