package step.core.export;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import ch.exense.commons.io.FileHelper;
import step.artefacts.CallFunction;
import step.artefacts.ForEachBlock;
import step.artefacts.Sequence;
import step.attachments.FileResolver;
import step.core.GlobalContext;
import step.core.GlobalContextBuilder;
import step.core.accessors.CRUDAccessor;
import step.core.accessors.InMemoryCRUDAccessor;
import step.core.accessors.collections.CollectionRegistry;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.Entity;
import step.core.imports.GenericDBImporter;
import step.core.imports.ImportManager;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.datapool.excel.ExcelDataPool;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.FunctionCRUDAccessor;
import step.planbuilder.BaseArtefacts;
import step.plugins.functions.types.CompositeFunction;
import step.plugins.functions.types.CompositeFunctionType;
import step.plugins.parametermanager.Parameter;
import step.plugins.parametermanager.ParameterCollection;
import step.plugins.parametermanager.ParameterManager;
import step.plugins.parametermanager.ParameterManagerPlugin;
import step.resources.Resource;
import step.resources.ResourceAccessor;
import step.resources.ResourceManager;

public class ExportManagerTest {
	
	public static GlobalContext createGlobalContext() {
		GlobalContext context = GlobalContextBuilder.createGlobalContext();
		//From ParameterManagerPlugin
		InMemoryCRUDAccessor<Parameter> parameterAccessor = new InMemoryCRUDAccessor<Parameter>();
		context.put("ParameterAccessor", parameterAccessor);
		context.getEntityManager().register(new Entity<Parameter, CRUDAccessor<Parameter>> (
				ParameterManagerPlugin.entityName, 
				parameterAccessor,
				Parameter.class,
				new GenericDBImporter<Parameter, CRUDAccessor<Parameter>>(context)));
		return context;
	}

	@Test
	public void testExportPlanById() throws Exception {
		GlobalContext c = GlobalContextBuilder.createGlobalContext();
		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(BaseArtefacts.sequence()).endBlock().build();
		c.getPlanAccessor().save(plan);
		
		File testExportFile = new File("testExport.zip");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			metadata.put("export-time" , "1589542872475");
			metadata.put("user", "admin");
			exportManager.exportById(outputStream, dummyObjectEnricher(), metadata, dummyObjectPredicate(), plan.getId().toString(), "plans", false, null);
			Assert.assertTrue(FileHelper.isArchive(testExportFile));
			
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
			importManager.importAll(testExportFile, dummyObjectEnricher(), Arrays.asList("plans"), true);
			
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
			exportManager.exportAll(outputStream, dummyObjectEnricher(), metadata, dummyObjectPredicate(), "plans", false, null);
			
			//create a new context to test the import
			c = GlobalContextBuilder.createGlobalContext();
			ImportManager importManager = new ImportManager(c);
			importManager.importAll(testExportFile, dummyObjectEnricher(), Arrays.asList("plans"), true);
			
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
	public void testExportAllPlansWithParameters() throws Exception {
		GlobalContext c = createGlobalContext();
		InMemoryCRUDAccessor<Parameter> parameterAccessor = (InMemoryCRUDAccessor<Parameter>) c.get("ParameterAccessor");
		
		Sequence rootSequence = BaseArtefacts.sequence();
		Plan plan = PlanBuilder.create().startBlock(rootSequence).add(BaseArtefacts.sequence()).endBlock().build();
		c.getPlanAccessor().save(plan);
		Plan plan2 = PlanBuilder.create().startBlock(rootSequence).add(BaseArtefacts.sequence()).endBlock().build();
		c.getPlanAccessor().save(plan2);
		
		Parameter param = new Parameter(null,"key","Value","desc");
		Parameter savedParam = parameterAccessor.save(param);
		
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			List<String> additionalEntities = new ArrayList<String>();
			additionalEntities.add(ParameterManagerPlugin.entityName);
			exportManager.exportAll(outputStream, dummyObjectEnricher(), metadata, dummyObjectPredicate(), "plans", false, additionalEntities);
			
			//create a new context to test the import
			c = createGlobalContext();
			parameterAccessor = (InMemoryCRUDAccessor<Parameter>) c.get("ParameterAccessor");

			ImportManager importManager = new ImportManager(c);
			importManager.importAll(testExportFile, dummyObjectEnricher(), null, true);
			
			Plan actualPlan = c.getPlanAccessor().get(plan.getId());
			Plan actualPlan2 = c.getPlanAccessor().get(plan2.getId());
			Parameter actualParam = parameterAccessor.get(savedParam.getId());
			Assert.assertEquals(plan.getId(), actualPlan.getId());
			Assert.assertEquals(plan.getRoot(), actualPlan.getRoot());
			Assert.assertEquals(plan2.getId(), actualPlan2.getId());
			Assert.assertEquals(savedParam.getId(), actualParam.getId());
		} finally {
			testExportFile.delete();
		}
	}
	
	@Test
	public void testExportPlanByIdWithParameters() throws Exception {
		GlobalContext c = createGlobalContext();
		InMemoryCRUDAccessor<Parameter> parameterAccessor = (InMemoryCRUDAccessor<Parameter>) c.get("ParameterAccessor");
		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(BaseArtefacts.sequence()).endBlock().build();
		c.getPlanAccessor().save(plan);
		
		Parameter param = new Parameter(null,"key","Value","desc");
		Parameter savedParam = parameterAccessor.save(param);
		
		File testExportFile = new File("testExport.zip");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			metadata.put("export-time" , "1589542872475");
			metadata.put("user", "admin");
			List<String> additionalEntities = new ArrayList<String>();
			additionalEntities.add(ParameterManagerPlugin.entityName);
			exportManager.exportById(outputStream, dummyObjectEnricher(), metadata, dummyObjectPredicate(), plan.getId().toString(), "plans", false, additionalEntities);
			Assert.assertTrue(FileHelper.isArchive(testExportFile));
			
			//create a new context to test the import
			c = createGlobalContext();
			parameterAccessor = (InMemoryCRUDAccessor<Parameter>) c.get("ParameterAccessor");

			ImportManager importManager = new ImportManager(c);
			importManager.importAll(testExportFile, dummyObjectEnricher(), null, true);
			
			Parameter actualParam = parameterAccessor.get(savedParam.getId());
			Plan actualPlan = c.getPlanAccessor().get(plan.getId());
			Assert.assertEquals(plan.getId(), actualPlan.getId());
			Assert.assertEquals(plan.getRoot(), actualPlan.getRoot());
			Assert.assertEquals(savedParam.getId(), actualParam.getId());
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
			exportManager.exportById(outputStream, dummyObjectEnricher(), metadata, dummyObjectPredicate(), plan.getId().toString(), "plans", false, null);
			
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
			importManager.importAll(testExportFile, dummyObjectEnricher(), Arrays.asList("plans"), true);
			
			Plan actualPlan = c.getPlanAccessor().get(plan.getId());
			Assert.assertEquals(plan.getId(), actualPlan.getId());
			Assert.assertEquals(plan.getRoot(), actualPlan.getRoot());
		} finally {
			testExportFile.delete();
		}
	}
	
	@Test
	public void testExportPlanRecursively() throws Exception {
		testExportPlansRecursively(true);
	}
	
	@Test
	public void testExportPlanRecursivelyNewReferences() throws Exception {
		testExportPlansRecursively(false);
	}
	
	private void testExportPlansRecursively(boolean overwrite) throws Exception {
		GlobalContext c = GlobalContextBuilder.createGlobalContext();
		Sequence rootSequence = BaseArtefacts.sequence();
		Plan plan = PlanBuilder.create().startBlock(rootSequence).add(BaseArtefacts.sequence()).endBlock().build();
		c.getPlanAccessor().save(plan);
		Function function = new Function();
		FunctionCRUDAccessor functionAccessor = (FunctionCRUDAccessor) c.get(FunctionAccessor.class);
		functionAccessor.save(function);
		Sequence sequence = BaseArtefacts.sequence();
		sequence.addChild(BaseArtefacts.callPlan(plan.getId().toString()));
		CallFunction callFunction = new CallFunction();
		callFunction.setFunctionId(function.getId().toString());
		sequence.addChild(callFunction);
		Plan plan2 = PlanBuilder.create().startBlock(rootSequence).add(sequence).endBlock().build();
		c.getPlanAccessor().save(plan2);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			exportManager.exportById(outputStream, dummyObjectEnricher(), metadata, dummyObjectPredicate(), plan2.getId().toString(), "plans", true, null);
			
			/*DEBUG
			  try (BufferedReader br = new BufferedReader(
			 
					new InputStreamReader(new FileInputStream(testExportFile), StandardCharsets.UTF_8));) {
				String line;
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}*/
						
			//create a new context to test the import
			c = GlobalContextBuilder.createGlobalContext();
			ImportManager importManager = new ImportManager(c);
			importManager.importAll(testExportFile, dummyObjectEnricher(), null, overwrite);
			
			AtomicInteger nbPlans = new AtomicInteger(0);
			c.getPlanAccessor().getAll().forEachRemaining(p->{nbPlans.incrementAndGet();});
			AtomicInteger nbFunctions = new AtomicInteger(0);
			functionAccessor = (FunctionCRUDAccessor) c.get(FunctionAccessor.class);
			functionAccessor.getAll().forEachRemaining(f->nbFunctions.incrementAndGet());
			Assert.assertEquals(2, nbPlans.intValue());
			Assert.assertEquals(1, nbFunctions.intValue());
			
			Plan actualPlan = c.getPlanAccessor().get(plan.getId());
			Plan actualPlan2 = c.getPlanAccessor().get(plan2.getId());
			Function actualFunction = functionAccessor.get(function.getId());
			if (overwrite) {
				Assert.assertEquals(plan.getId(), actualPlan.getId());
				Assert.assertEquals(plan.getRoot(), actualPlan.getRoot());
				Assert.assertEquals(plan2.getId(), actualPlan2.getId());
				Assert.assertEquals(function.getId(), actualFunction.getId());
			} else {
				Assert.assertNull(actualPlan);
				Assert.assertNull(actualPlan2);
				Assert.assertNull(actualFunction);
			}
		} finally {
			testExportFile.delete();
		}
	}
	
	@Test
	public void testExportPlansWithCompo() throws Exception {
		testExportPlansWithCompoFct(true);
	}
	
	@Test
	public void testExportPlansWithCompoNewReferences() throws Exception {
		testExportPlansWithCompoFct(false);
	}
	
	
	private void testExportPlansWithCompoFct(boolean overwrite) throws Exception {
		GlobalContext c = GlobalContextBuilder.createGlobalContext();
		CompositeFunctionType compositeFunctionType = new CompositeFunctionType(c.getPlanAccessor());
		CompositeFunction function = compositeFunctionType.newFunction();
		compositeFunctionType.setupFunction(function);
		String compositePlanId = function.getPlanId();
		FunctionCRUDAccessor functionAccessor = (FunctionCRUDAccessor) c.get(FunctionAccessor.class);
		functionAccessor.save(function);
		
		Sequence sequence = BaseArtefacts.sequence();
		CallFunction callFunction = new CallFunction();
		callFunction.setFunctionId(function.getId().toString());
		sequence.addChild(callFunction);
		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(sequence).endBlock().build();
		c.getPlanAccessor().save(plan);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			exportManager.exportById(outputStream, dummyObjectEnricher(), metadata, dummyObjectPredicate(), plan.getId().toString(), "plans", true, null);
						
			//create a new context to test the import
			c = GlobalContextBuilder.createGlobalContext();
			functionAccessor = (FunctionCRUDAccessor) c.get(FunctionAccessor.class);
			ImportManager importManager = new ImportManager(c);
			importManager.importAll(testExportFile, dummyObjectEnricher(), null, overwrite);
			
			AtomicInteger nbPlans = new AtomicInteger(0);
			c.getPlanAccessor().getAll().forEachRemaining(p->{nbPlans.incrementAndGet();});
			AtomicInteger nbFunctions = new AtomicInteger(0);
			functionAccessor.getAll().forEachRemaining(f->nbFunctions.incrementAndGet());
			Assert.assertEquals(2, nbPlans.intValue());
			Assert.assertEquals(1, nbFunctions.intValue());
			
			Plan actualPlan = c.getPlanAccessor().get(plan.getId());
			Plan actualCompositePlan = c.getPlanAccessor().get(compositePlanId);
			Function actualFunction = functionAccessor.get(function.getId());

			if (overwrite) {
				Assert.assertEquals(plan.getId(), actualPlan.getId());
				Assert.assertEquals(plan.getRoot(), actualPlan.getRoot());
				Assert.assertEquals(compositePlanId, actualCompositePlan.getId().toHexString());
				Assert.assertEquals(function.getId(), actualFunction.getId());
			} else {
				Assert.assertNull(actualPlan);
				Assert.assertNull(actualCompositePlan);
				Assert.assertNull(actualFunction);
			}
		} finally {
			testExportFile.delete();
		}
	}
	
	@Test
	public void testExportPlansWithResource() throws Exception {
		testExportPlansWithResourceFct(true);
	}
	
	@Test
	public void testExportPlansWithResourceNewReferences() throws Exception {
		testExportPlansWithResourceFct(false);
	}
	
	public void testExportPlansWithResourceFct(boolean overwrite) throws Exception {
		GlobalContext c = GlobalContextBuilder.createGlobalContext();
		
		
		// Create a resource
		ResourceManager resourceManager = c.get(ResourceManager.class);
		Resource resource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_DATASOURCE, this.getClass().getResourceAsStream("dummyExcel.xls"), "TestResource.txt", false, null);
		Assert.assertNotNull(resource);
			
		ForEachBlock f = new ForEachBlock();
		ExcelDataPool p = new ExcelDataPool();
		p.setFile(new DynamicValue<String> (FileResolver.RESOURCE_PREFIX + resource.getId().toHexString()));
		p.getHeaders().setValue(true);
		f.setDataSource(p);
		f.setDataSourceType("excel");
		
		Sequence sequence = BaseArtefacts.sequence();
		sequence.addChild(f);
		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(sequence).endBlock().build();
		c.getPlanAccessor().save(plan);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			exportManager.exportById(outputStream, dummyObjectEnricher(), metadata, dummyObjectPredicate(), plan.getId().toString(), "plans", true, null);
			//delete created resource
			File resourceFile = resourceManager.getResourceFile(resource.getId().toHexString()).getResourceFile();
			resourceManager.deleteResource(resource.getId().toHexString());
						
			//create a new context to test the import
			c = GlobalContextBuilder.createGlobalContext();
			ResourceManager resourceManagerImport = c.get(ResourceManager.class);

			ImportManager importManager = new ImportManager(c);
			importManager.importAll(testExportFile, dummyObjectEnricher(), null, overwrite);
			
			Plan actualPlan = c.getPlanAccessor().get(plan.getId());
			Resource actualResource = c.get(ResourceAccessor.class).get(resource.getId());

			AtomicInteger nbPlans = new AtomicInteger(0);
			c.getPlanAccessor().getAll().forEachRemaining(pp->{nbPlans.incrementAndGet();});
			AtomicInteger nbResources = new AtomicInteger(0);
			c.get(ResourceAccessor.class).getAll().forEachRemaining(r->{
				nbResources.incrementAndGet();
				resourceManagerImport.deleteResource(r.getId().toHexString());
			});
			Assert.assertEquals(1, nbPlans.intValue());
			Assert.assertEquals(1, nbResources.intValue());
			
			//TODO check resource file was imported (do we keep same revision?)
			//resourceFile.exists()

			if (overwrite) {
				Assert.assertEquals(plan.getId(), actualPlan.getId());
				Assert.assertEquals(plan.getRoot(), actualPlan.getRoot());
				Assert.assertEquals(resource.getId(), actualResource.getId());
				
			} else {
				Assert.assertNull(actualPlan);
				Assert.assertNull(actualResource);
			}
			
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
		importManager.importAll(testImportFile, dummyObjectEnricher(), Arrays.asList("plans"), true);
		
		Plan actualPlan = c.getPlanAccessor().get(originPlanId);
		Assert.assertEquals(originPlanId, actualPlan.getId().toString());
	}
}
