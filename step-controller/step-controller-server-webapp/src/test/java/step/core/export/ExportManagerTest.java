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
package step.core.export;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import ch.exense.commons.app.Configuration;
import org.junit.Assert;
import org.junit.Test;

import ch.exense.commons.io.FileHelper;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallFunction;
import step.artefacts.ForEachBlock;
import step.artefacts.Sequence;
import step.attachments.FileResolver;
import step.core.GlobalContext;
import step.core.GlobalContextBuilder;
import step.core.accessors.CRUDAccessor;
import step.core.accessors.InMemoryCRUDAccessor;
import step.core.dynamicbeans.DynamicValue;
import step.core.encryption.EncryptionManager;
import step.core.encryption.EncryptionManagerException;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.imports.GenericDBImporter;
import step.core.imports.ImportConfiguration;
import step.core.imports.ImportManager;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;
import step.datapool.excel.ExcelDataPool;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.parameter.ParameterManager;
import step.planbuilder.BaseArtefacts;
import step.plugins.functions.types.CompositeFunction;
import step.plugins.functions.types.CompositeFunctionType;
import step.parameter.Parameter;
import step.plugins.parametermanager.ParameterManagerControllerPlugin;
import step.plugins.parametermanager.ParameterManagerPlugin;
import step.resources.Resource;
import step.resources.ResourceManager;

public class ExportManagerTest {

	public static GlobalContext createGlobalContext() throws InstantiationException, IllegalAccessException, ClassNotFoundException, CircularDependencyException {
		GlobalContext context = createGlobalContext_();
		EncryptionManager encryptionManager = new EncryptionManager() {
			@Override
			public String encrypt(String value) throws EncryptionManagerException {
				return "###"+value;
			}

			@Override
			public String decrypt(String encryptedValue) throws EncryptionManagerException {
				return encryptedValue.replaceFirst("###", "");
			}
		};
		context.put(EncryptionManager.class,encryptionManager);

		return context;
	}

	public static GlobalContext createGlobalContext_() throws InstantiationException, IllegalAccessException, ClassNotFoundException, CircularDependencyException {
		GlobalContext context = GlobalContextBuilder.createGlobalContext();
		BaseArtefactPlugin.registerArtefacts(context.getArtefactHandlerRegistry());
		//From ParameterManagerPlugin
		InMemoryCRUDAccessor<Parameter> parameterAccessor = new InMemoryCRUDAccessor<Parameter>();
		context.put("ParameterAccessor", parameterAccessor);
		context.getEntityManager().register(new Entity<Parameter, CRUDAccessor<Parameter>> (
				ParameterManagerPlugin.entityName,
				parameterAccessor,
				Parameter.class,
				new GenericDBImporter<Parameter, CRUDAccessor<Parameter>>(context)));
		context.getEntityManager().registerExportHook(new ParameterManagerControllerPlugin.ParameterExportBiConsumer(context));
		context.getEntityManager().registerImportHook(new ParameterManagerControllerPlugin.ParameterImportBiConsumer(context));
		return context;
	}

	@Test
	public void testExportPlanById() throws Exception {
		GlobalContext c = createGlobalContext();
		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(BaseArtefacts.sequence()).endBlock().build();
		c.getPlanAccessor().save(plan);
		
		File testExportFile = new File("testExport.zip");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			metadata.put("export-time" , "1589542872475");
			metadata.put("user", "admin");
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, null);
			exportManager.exportById(exportConfig, plan.getId().toString());
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
			c = createGlobalContext();
			ImportManager importManager = new ImportManager(c);
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(c), Arrays.asList("plans"), true));
			
			Plan actualPlan = c.getPlanAccessor().get(plan.getId());
			Assert.assertEquals(plan.getId(), actualPlan.getId());
			Assert.assertEquals(plan.getRoot(), actualPlan.getRoot());
		} finally {
			testExportFile.delete();
		}
	}

	@Test
	public void testExportKeywordById() throws Exception {
		GlobalContext c = createGlobalContext();
		Function function = new Function();
		FunctionAccessor functionAccessor = (FunctionAccessor) c.get(FunctionAccessor.class);
		functionAccessor.save(function);

		File testExportFile = new File("testExport.zip");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			metadata.put("export-time" , "1589542872475");
			metadata.put("user", "admin");
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), EntityManager.functions, true, null);
			exportManager.exportById(exportConfig, function.getId().toString());
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
			c = createGlobalContext();
			ImportManager importManager = new ImportManager(c);
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(c), Arrays.asList(EntityManager.functions), true));
			functionAccessor = (FunctionAccessor) c.get(FunctionAccessor.class);
			functionAccessor.save(function);
			Function actualFunction = functionAccessor.get(function.getId());

			Assert.assertEquals(function.getId(), actualFunction.getId());
		} finally {
			testExportFile.delete();
		}
	}

	protected ObjectEnricher dummyObjectEnricher(GlobalContext context) {
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
		GlobalContext c = createGlobalContext();
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
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, null);
			exportManager.exportAll(exportConfig);
			
			//create a new context to test the import
			c = createGlobalContext();
			ImportManager importManager = new ImportManager(c);
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(c), Arrays.asList("plans"), true));
			
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

		EncryptionManager encryptionManager = c.get(EncryptionManager.class);
		Parameter param = new Parameter(null,"key","Value","desc");
		Parameter savedParam = parameterAccessor.save(param);
		Parameter paramProtected = new Parameter(null,"key_pwd","Value","desc");
		paramProtected.setProtectedValue(true);
		Parameter savedParamProtected = parameterAccessor.save(paramProtected);
		Parameter paramProtectedEncrypted = new Parameter(null,"key_pwd","Value","desc");
		paramProtectedEncrypted.setProtectedValue(true);
		paramProtectedEncrypted.setEncryptedValue(encryptionManager.encrypt(paramProtectedEncrypted.getValue()));
		paramProtectedEncrypted.setValue(null);
		Parameter savedParamProtectedEncrypted = parameterAccessor.save(paramProtectedEncrypted);
		
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			List<String> additionalEntities = new ArrayList<String>();
			additionalEntities.add(ParameterManagerPlugin.entityName);

			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, additionalEntities);
			exportManager.exportAll(exportConfig);

			Assert.assertEquals(2,exportConfig.getMessages().size());
			Assert.assertEquals(ParameterManagerControllerPlugin.EXPORT_PROTECT_PARAM_WARN,exportConfig.getMessages().toArray()[1]);
			Assert.assertEquals(ParameterManagerControllerPlugin.EXPORT_ENCRYPT_PARAM_WARN,exportConfig.getMessages().toArray()[0]);

			//create a new context to test the import
			c = createGlobalContext();
			parameterAccessor = (InMemoryCRUDAccessor<Parameter>) c.get("ParameterAccessor");

			ImportManager importManager = new ImportManager(c);
			ImportConfiguration importConfiguration = new ImportConfiguration(testExportFile, dummyObjectEnricher(c), null, true);
			importManager.importAll(importConfiguration);
			Assert.assertEquals(1,importConfiguration.getMessages().size());
			Assert.assertEquals(ParameterManagerControllerPlugin.IMPORT_RESET_WARN,importConfiguration.getMessages().toArray()[0]);

			Plan actualPlan = c.getPlanAccessor().get(plan.getId());
			Plan actualPlan2 = c.getPlanAccessor().get(plan2.getId());
			Parameter actualParam = parameterAccessor.get(savedParam.getId());
			Parameter actualParamProtected = parameterAccessor.get(savedParamProtected.getId());
			Parameter actualParamProtectedEncrypted = parameterAccessor.get(savedParamProtectedEncrypted.getId());
			Assert.assertEquals(plan.getId(), actualPlan.getId());
			Assert.assertEquals(plan.getRoot(), actualPlan.getRoot());
			Assert.assertEquals(plan2.getId(), actualPlan2.getId());
			Assert.assertEquals(savedParam.getId(), actualParam.getId());
			Assert.assertEquals(savedParamProtected.getId(), actualParamProtected.getId());
			Assert.assertEquals(true, actualParamProtected.getProtectedValue());
			Assert.assertEquals(ParameterManagerControllerPlugin.RESET_VALUE, actualParamProtected.getValue());
			Assert.assertEquals(null, actualParamProtected.getEncryptedValue());
			Assert.assertEquals(savedParamProtectedEncrypted.getId(), actualParamProtectedEncrypted.getId());
			Assert.assertEquals(true, actualParamProtectedEncrypted.getProtectedValue());
			Assert.assertEquals(null, actualParamProtectedEncrypted.getValue());
			Assert.assertEquals("###Value", actualParamProtectedEncrypted.getEncryptedValue());
		} finally {
			testExportFile.delete();
		}
	}

	@Test
	public void testImportNewEncryptionManager() throws Exception {
		GlobalContext c = createGlobalContext();
		InMemoryCRUDAccessor<Parameter> parameterAccessor = (InMemoryCRUDAccessor<Parameter>) c.get("ParameterAccessor");

		EncryptionManager encryptionManager = c.get(EncryptionManager.class);
		Parameter paramProtectedEncrypted = new Parameter(null,"key_pwd","Value","desc");
		paramProtectedEncrypted.setProtectedValue(true);
		paramProtectedEncrypted.setEncryptedValue(encryptionManager.encrypt(paramProtectedEncrypted.getValue()));
		paramProtectedEncrypted.setValue(null);
		Parameter savedParamProtectedEncrypted = parameterAccessor.save(paramProtectedEncrypted);

		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			List<String> additionalEntities = new ArrayList<String>();
			additionalEntities.add(ParameterManagerPlugin.entityName);

			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, additionalEntities);
			exportManager.exportAll(exportConfig);

			Assert.assertEquals(1,exportConfig.getMessages().size());
			Assert.assertEquals(ParameterManagerControllerPlugin.EXPORT_ENCRYPT_PARAM_WARN,exportConfig.getMessages().toArray()[0]);

			//create a new context to test the import
			c = createGlobalContext();
			parameterAccessor = (InMemoryCRUDAccessor<Parameter>) c.get("ParameterAccessor");
			//Override previous encryption manager to simulate new instance
			EncryptionManager encryptionManagerNewInstance = new EncryptionManager() {
				@Override
				public String encrypt(String value) throws EncryptionManagerException {
					return "###"+value;
				}
				@Override
				public String decrypt(String encryptedValue) throws EncryptionManagerException {
					throw new EncryptionManagerException("Error while decrypting value");
				}
			};
			c.put(EncryptionManager.class,encryptionManagerNewInstance);

			ImportManager importManager = new ImportManager(c);
			ImportConfiguration importConfiguration = new ImportConfiguration(testExportFile, dummyObjectEnricher(c), null, true);
			importManager.importAll(importConfiguration);
			Assert.assertEquals(1,importConfiguration.getMessages().size());
			Assert.assertEquals(ParameterManagerControllerPlugin.IMPORT_DECRYPT_FAIL_WARN,importConfiguration.getMessages().toArray()[0]);

			Parameter actualParamProtectedEncrypted = parameterAccessor.get(savedParamProtectedEncrypted.getId());
			Assert.assertEquals(savedParamProtectedEncrypted.getId(), actualParamProtectedEncrypted.getId());
			Assert.assertEquals(true, actualParamProtectedEncrypted.getProtectedValue());
			Assert.assertEquals(ParameterManagerControllerPlugin.RESET_VALUE, actualParamProtectedEncrypted.getValue());
			Assert.assertEquals(null, actualParamProtectedEncrypted.getEncryptedValue());
		} finally {
			testExportFile.delete();
		}
	}

	@Test
	public void testImportNoEncryptionManager() throws Exception {
		GlobalContext c = createGlobalContext();
		InMemoryCRUDAccessor<Parameter> parameterAccessor = (InMemoryCRUDAccessor<Parameter>) c.get("ParameterAccessor");

		EncryptionManager encryptionManager = c.get(EncryptionManager.class);
		Parameter paramProtectedEncrypted = new Parameter(null,"key_pwd","Value","desc");
		paramProtectedEncrypted.setProtectedValue(true);
		paramProtectedEncrypted.setEncryptedValue(encryptionManager.encrypt(paramProtectedEncrypted.getValue()));
		paramProtectedEncrypted.setValue(null);
		Parameter savedParamProtectedEncrypted = parameterAccessor.save(paramProtectedEncrypted);

		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = new ExportManager(c);
			Map<String,String> metadata = new HashMap<String,String>();
			metadata.put("version", c.getCurrentVersion().toString());
			List<String> additionalEntities = new ArrayList<String>();
			additionalEntities.add(ParameterManagerPlugin.entityName);

			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, additionalEntities);
			exportManager.exportAll(exportConfig);

			Assert.assertEquals(1,exportConfig.getMessages().size());
			Assert.assertEquals(ParameterManagerControllerPlugin.EXPORT_ENCRYPT_PARAM_WARN,exportConfig.getMessages().toArray()[0]);

			//create a new context to test the import without encpryption manager
			c = createGlobalContext_();
			parameterAccessor = (InMemoryCRUDAccessor<Parameter>) c.get("ParameterAccessor");
			ImportManager importManager = new ImportManager(c);
			ImportConfiguration importConfiguration = new ImportConfiguration(testExportFile, dummyObjectEnricher(c), null, true);
			importManager.importAll(importConfiguration);
			Assert.assertEquals(1,importConfiguration.getMessages().size());
			Assert.assertEquals(ParameterManagerControllerPlugin.IMPORT_DECRYPT_NO_EM_WARN,importConfiguration.getMessages().toArray()[0]);

			Parameter actualParamProtectedEncrypted = parameterAccessor.get(savedParamProtectedEncrypted.getId());
			Assert.assertEquals(savedParamProtectedEncrypted.getId(), actualParamProtectedEncrypted.getId());
			Assert.assertEquals(true, actualParamProtectedEncrypted.getProtectedValue());
			Assert.assertEquals(ParameterManagerControllerPlugin.RESET_VALUE, actualParamProtectedEncrypted.getValue());
			Assert.assertEquals(null, actualParamProtectedEncrypted.getEncryptedValue());
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
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, additionalEntities);
			exportManager.exportById(exportConfig, plan.getId().toString());
			Assert.assertTrue(FileHelper.isArchive(testExportFile));
			
			//create a new context to test the import
			c = createGlobalContext();
			parameterAccessor = (InMemoryCRUDAccessor<Parameter>) c.get("ParameterAccessor");

			ImportManager importManager = new ImportManager(c);
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(c), null, true));
			
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
		GlobalContext c = createGlobalContext();
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
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, null);
			exportManager.exportById(exportConfig, plan.getId().toString());
			
			//DEBUG
			/*try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new FileInputStream(testExportFile), StandardCharsets.UTF_8));) {
				String line;
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}*/
			
			//create a new context to test the import
			c = createGlobalContext();
			ImportManager importManager = new ImportManager(c);
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(c), Arrays.asList("plans"), true));
			
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
		GlobalContext c = createGlobalContext();
		Sequence rootSequence = BaseArtefacts.sequence();
		Plan plan = PlanBuilder.create().startBlock(rootSequence).add(BaseArtefacts.sequence()).endBlock().build();
		c.getPlanAccessor().save(plan);
		Function function = new Function();
		FunctionAccessor functionAccessor = (FunctionAccessor) c.get(FunctionAccessor.class);
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
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", true, null);
			exportManager.exportById(exportConfig, plan2.getId().toString());
			
			/*DEBUG
			  try (BufferedReader br = new BufferedReader(
			 
					new InputStreamReader(new FileInputStream(testExportFile), StandardCharsets.UTF_8));) {
				String line;
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}*/
						
			//create a new context to test the import
			c = createGlobalContext();
			ImportManager importManager = new ImportManager(c);
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(c), null, overwrite));
			
			AtomicInteger nbPlans = new AtomicInteger(0);
			c.getPlanAccessor().getAll().forEachRemaining(p->{nbPlans.incrementAndGet();});
			AtomicInteger nbFunctions = new AtomicInteger(0);
			functionAccessor = (FunctionAccessor) c.get(FunctionAccessor.class);
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
		GlobalContext c = createGlobalContext();
		
		CompositeFunctionType compositeFunctionType = new CompositeFunctionType(c.getPlanAccessor());
		CompositeFunction function = compositeFunctionType.newFunction();
		compositeFunctionType.setupFunction(function);
		String compositePlanId = function.getPlanId();
		FunctionAccessor functionAccessor = (FunctionAccessor) c.get(FunctionAccessor.class);
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
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", true, null);
			exportManager.exportById(exportConfig, plan.getId().toString());
						
			//create a new context to test the import
			c = createGlobalContext();
			functionAccessor = (FunctionAccessor) c.get(FunctionAccessor.class);
			ImportManager importManager = new ImportManager(c);
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(c), null, overwrite));
			
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
		GlobalContext c = createGlobalContext();
		
		// Create a resource
		ResourceManager resourceManager = c.getResourceManager();
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
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", true, null);
			exportManager.exportById(exportConfig, plan.getId().toString());
			//delete created resource
			File resourceFile = resourceManager.getResourceFile(resource.getId().toHexString()).getResourceFile();
			resourceManager.deleteResource(resource.getId().toHexString());
						
			//create a new context to test the import
			c = createGlobalContext();
			ResourceManager resourceManagerImport = c.getResourceManager();

			ImportManager importManager = new ImportManager(c);
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(c), null, overwrite));
			
			Plan actualPlan = c.getPlanAccessor().get(plan.getId());
			Resource actualResource =   c.getResourceAccessor().get(resource.getId());


			AtomicInteger nbPlans = new AtomicInteger(0);
			c.getPlanAccessor().getAll().forEachRemaining(pp->{nbPlans.incrementAndGet();});
			AtomicInteger nbResources = new AtomicInteger(0);
			c.getResourceAccessor().getAll().forEachRemaining(r->{
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

	@Test
	public void testImportVisualPlanWithAssert_3_13() throws Exception {
		String resourcePath = "./step/core/export/ExportVisualPlan3_13.json";
		String originPlanId = "5f87f3c83ff2d04dd8f9a3f7";
		testOlderPlanImport(resourcePath, originPlanId);
	}


	//Not working in junit as working with tmp collection
	// @Test
	public void testImportVisualPlanWithAssert_3_12() throws Exception {
		String resourcePath = "./step/core/export/TestWithAssert_3_12.json";
		String originPlanId = "5f87f3c83ff2d04dd8f9a3f7";
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
		String resourcePath = "step/core/export/3_13_ExportVisualPlanWithAssert.json";
		String originPlanId = null;
		testOlderPlanImport(resourcePath, originPlanId);
	}
	
	protected void testOlderPlanImport(String resourcePath, String originPlanId) throws Exception {
		GlobalContext c = createGlobalContext();
		URL resource = getClass().getClassLoader().getResource(resourcePath);
		File testImportFile = new File(resource.getFile());
		
		//create a new context to test the import
		c = createGlobalContext();
		ImportManager importManager = new ImportManager(c);
		importManager.importAll(new ImportConfiguration(testImportFile, dummyObjectEnricher(c), Arrays.asList("plans"), true));
		
		Plan actualPlan = c.getPlanAccessor().get(originPlanId);
		Assert.assertEquals(originPlanId, actualPlan.getId().toString());
	}
}
