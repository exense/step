package step.core.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.Sequence;
import step.core.GlobalContext;
import step.core.GlobalContextBuilder;
import step.core.imports.ImportManager;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.planbuilder.BaseArtefacts;

public class ExportManagerTest {

	@Test
	public void testExportPlanById() throws Exception {
		GlobalContext c = GlobalContextBuilder.createGlobalContext();
		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(BaseArtefacts.sequence()).endBlock().build();
		c.getPlanAccessor().save(plan);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			metadata.put("export-time" , "1589542872475");
			metadata.put("user", "admin");
			exportManager.exportById(outputStream, metadata,plan.getId().toString(), "plans");
			
			//DEBUG
			/*try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new FileInputStream(testExportFile), StandardCharsets.UTF_8));) {
				String line;
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}*/
			
			//create a new context to test the import
			c = GlobalContextBuilder.createGlobalContext();
			ImportManager importManager = new ImportManager(c);
			importManager.importAll(testExportFile, dummyObjectEnricher(), Arrays.asList("plans"));
			
			Plan actualPlan = c.getPlanAccessor().get(plan.getId());
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
	public void testExportAllPlans() throws Exception {
		GlobalContext c = GlobalContextBuilder.createGlobalContext();
		Sequence rootSequence = BaseArtefacts.sequence();
		Plan plan = PlanBuilder.create().startBlock(rootSequence).add(BaseArtefacts.sequence()).endBlock().build();
		c.getPlanAccessor().save(plan);
		Plan plan2 = PlanBuilder.create().startBlock(rootSequence).add(BaseArtefacts.sequence()).endBlock().build();
		c.getPlanAccessor().save(plan2);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			exportManager.exportAll(outputStream, metadata, dummyObjectPredicate(), "plans");
			
			//create a new context to test the import
			c = GlobalContextBuilder.createGlobalContext();
			ImportManager importManager = new ImportManager(c);
			importManager.importAll(testExportFile, dummyObjectEnricher(), Arrays.asList("plans"));
			
			Plan actualPlan = c.getPlanAccessor().get(plan.getId());
			Plan actualPlan2 = c.getPlanAccessor().get(plan2.getId());
			Assert.assertEquals(plan.getId(), actualPlan.getId());
			Assert.assertEquals(plan.getRoot(), actualPlan.getRoot());
			Assert.assertEquals(plan2.getId(), actualPlan2.getId());
		} finally {
			testExportFile.delete();
		}
	}
	
	@Test
	public void testExportPlanByWithCustomFields() throws Exception {
		GlobalContext c = GlobalContextBuilder.createGlobalContext();
		Sequence seq = BaseArtefacts.sequence();
		seq.addCustomField("key", "value");
		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(seq).endBlock().build();
		c.getPlanAccessor().save(plan);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			metadata.put("export-time" , "1589542872475");
			metadata.put("user", "admin");
			exportManager.exportById(outputStream, metadata,plan.getId().toString(), "plans");
			
			//DEBUG
			/*try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new FileInputStream(testExportFile), StandardCharsets.UTF_8));) {
				String line;
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}*/
			
			//create a new context to test the import
			c = GlobalContextBuilder.createGlobalContext();
			ImportManager importManager = new ImportManager(c);
			importManager.importAll(testExportFile, dummyObjectEnricher(), Arrays.asList("plans"));
			
			Plan actualPlan = c.getPlanAccessor().get(plan.getId());
			Assert.assertEquals(plan.getId(), actualPlan.getId());
			Assert.assertEquals(plan.getRoot(), actualPlan.getRoot());
		} finally {
			testExportFile.delete();
		}
	}

	protected ObjectPredicate dummyObjectPredicate() {
		return new ObjectPredicate() {
			@Override
			public boolean test(Object t) {
				return true;
			}
		};
	}
	
	@Test
	public void testImportVisualPlan_3_13() throws Exception {
		String resourcePath = "./step/core/export/3_13_visualPlan.json";
		String originPlanId = "5e8d7edb4cf3ad5e290d77e9";
		testOlderPlanImport(resourcePath, originPlanId);
	}
	
	//@Test //not working in OS
	public void testImportTextPlan_3_13() throws Exception {
		String resourcePath = "./step/core/export/3_13_textPlan.json";
		String originPlanId = "5eb2789c117dff15d2bc8bc0";
		testOlderPlanImport(resourcePath, originPlanId);
	}
	
	//@Test // not working in unit test as it stores data to a tmp mongo collection
	public void testImportPlan_3_12() throws Exception {
		String resourcePath = "./step/core/export/3_12_and_before.json";
		String originPlanId = null;
		testOlderPlanImport(resourcePath, originPlanId);
	}
	
	protected void testOlderPlanImport(String resourcePath, String originPlanId) throws Exception {
		GlobalContext c = GlobalContextBuilder.createGlobalContext();
		URL resource = getClass().getClassLoader().getResource(resourcePath);
		File testImportFile = new File(resource.getFile());
		
		//create a new context to test the import
		c = GlobalContextBuilder.createGlobalContext();
		ImportManager importManager = new ImportManager(c);
		importManager.importAll(testImportFile, dummyObjectEnricher(), Arrays.asList("plans"));
		
		Plan actualPlan = c.getPlanAccessor().get(originPlanId);
		Assert.assertEquals(originPlanId, actualPlan.getId().toString());
	}
}
