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

import ch.exense.commons.io.FileHelper;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.CallFunction;
import step.artefacts.ForEachBlock;
import step.artefacts.Sequence;
import step.artefacts.TestSet;
import step.artefacts.handlers.FunctionLocator;
import step.artefacts.handlers.PlanLocator;
import step.artefacts.handlers.SelectorHelper;
import step.attachments.FileResolver;
import step.core.Controller;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.artefacts.AbstractArtefact;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.encryption.EncryptionManager;
import step.core.encryption.EncryptionManagerException;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.imports.ImportConfiguration;
import step.core.imports.ImportManager;
import step.core.imports.ImportResult;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.PlanEntity;
import step.core.plans.builder.PlanBuilder;
import step.datapool.excel.ExcelDataPool;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.FunctionEntity;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.migration.MigrationManager;
import step.migration.tasks.MigrateArtefactsToPlans;
import step.migration.tasks.MigrateAssertNegation;
import step.migration.tasks.MigrateFunctionCallsById;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.planbuilder.FunctionArtefacts;
import step.plugins.functions.types.CompositeFunction;
import step.plugins.functions.types.CompositeFunctionType;
import step.plugins.parametermanager.ParameterManagerControllerPlugin;
import step.resources.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static step.planbuilder.BaseArtefacts.callPlan;
import static step.planbuilder.BaseArtefacts.sequence;

public class ExportManagerTest {

	private PlanAccessor planAccessor;
	private EntityManager entityManager;
	private MigrationManager migrationManager;
	private EncryptionManager encryptionManager;
	private LocalResourceManagerImpl resourceManager;
	private FunctionAccessor functionAccessor;
	private Accessor<Parameter> parameterAccessor;
	private ResourceAccessor resourceAccessor;
	private ResourceRevisionAccessor resourceRevisionAccessor;
	
	@Before
	public void before() {
		EncryptionManager encryptionManager = new EncryptionManager() {
			@Override
			public String encrypt(String value) {
				return "###"+value;
			}
			
			@Override
			public String decrypt(String encryptedValue) {
				return encryptedValue.replaceFirst("###", "");
			}
			
			@Override
			public boolean isKeyPairChanged() {
				return false;
			}
			
			@Override
			public boolean isFirstStart() {
				return false;
			}
		};
		
		newContext(encryptionManager);
	}

	private void newContext(EncryptionManager encryptionManager) {
		this.encryptionManager = encryptionManager;
		
		planAccessor = new InMemoryPlanAccessor();
		functionAccessor = new InMemoryFunctionAccessorImpl();
		parameterAccessor = new AbstractAccessor<>(new InMemoryCollection<>());

		resourceAccessor = new InMemoryResourceAccessor();
		resourceRevisionAccessor = new InMemoryResourceRevisionAccessor();
		resourceManager = new LocalResourceManagerImpl(new File("resources"), resourceAccessor, resourceRevisionAccessor);

		entityManager = new EntityManager();
		
		FileResolver fileResolver = new FileResolver(resourceManager);
		SelectorHelper selectorHelper = new SelectorHelper(new DynamicJsonObjectResolver(new DynamicJsonValueResolver(new ExpressionHandler())));
		FunctionLocator functionLocator = new FunctionLocator(functionAccessor, selectorHelper);
		entityManager
				.register(new Entity<>(Parameter.ENTITY_NAME, parameterAccessor, Parameter.class))
				.register(new PlanEntity(planAccessor, new PlanLocator(planAccessor, selectorHelper), entityManager))
				.register(new FunctionEntity(functionAccessor, functionLocator, entityManager))
				.register(new ResourceEntity(resourceAccessor, resourceManager, fileResolver, entityManager))
				.register(new Entity<>(EntityManager.resourceRevisions, resourceRevisionAccessor, ResourceRevision.class));
		
		entityManager.registerExportHook(new ParameterManagerControllerPlugin.ParameterExportBiConsumer());
		entityManager.registerImportHook(new ParameterManagerControllerPlugin.ParameterImportBiConsumer(encryptionManager));
		entityManager.registerImportHook(new ResourceImporter(resourceManager));
		
		migrationManager = new MigrationManager();
		migrationManager.register(MigrateArtefactsToPlans.class);
		migrationManager.register(MigrateAssertNegation.class);
		migrationManager.register(MigrateFunctionCallsById.class);
	}
	
	@Test
	public void testExportPlanById() throws Exception {
		Plan plan = PlanBuilder.create().startBlock(sequence()).add(sequence()).endBlock().build();
		planAccessor.save(plan);
		
		File testExportFile = new File("testExport.zip");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = newExportManager();
			Map<String, String> metadata = buildMetadata();
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, null);
			exportManager.exportById(exportConfig, plan.getId().toString());
			assertTrue(FileHelper.isArchive(testExportFile));
			
			ImportManager importManager = createNewContextAndGetImportManager();
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(), List.of("plans"), true));
			
			Plan actualPlan = planAccessor.get(plan.getId());
			assertEquals(plan.getId(), actualPlan.getId());
			assertEquals(plan.getRoot(), actualPlan.getRoot());
		} finally {
			testExportFile.delete();
		}
	}

	private Map<String, String> buildMetadata() {
		Map<String,String> metadata = new HashMap<>();
		metadata.put("version", Controller.VERSION.toString());
		metadata.put("export-time" , "1589542872475");
		metadata.put("user", "admin");
		return metadata;
	}

	private ExportManager newExportManager() {
		return new ExportManager(entityManager, resourceManager);
	}

	@Test
	public void testExportKeywordById() throws Exception {
		Function function = new Function();
		functionAccessor.save(function);

		File testExportFile = new File("testExport.zip");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = newExportManager();
			Map<String, String> metadata = buildMetadata();
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), EntityManager.functions, true, null);
			exportManager.exportById(exportConfig, function.getId().toString());
			assertTrue(FileHelper.isArchive(testExportFile));

			ImportManager importManager = createNewContextAndGetImportManager();
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(), List.of(EntityManager.functions), true));
			functionAccessor.save(function);
			Function actualFunction = functionAccessor.get(function.getId());

			assertEquals(function.getId(), actualFunction.getId());
		} finally {
			testExportFile.delete();
		}
	}

	protected ObjectEnricher dummyObjectEnricher() {
		return new ObjectEnricher() {

			@Override
			public void accept(EnricheableObject t) {
			}

			@Override
			public TreeMap<String, String> getAdditionalAttributes() {
				return null;
			}
		};
	}

	@Test
	public void testExportAllPlans() throws Exception {
		Sequence rootSequence = sequence();
		Plan plan = PlanBuilder.create().startBlock(rootSequence).add(sequence()).endBlock().build();
		planAccessor.save(plan);
		Plan plan2 = PlanBuilder.create().startBlock(rootSequence).add(sequence()).endBlock().build();
		planAccessor.save(plan2);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = newExportManager();
			Map<String, String> metadata = buildMetadata();
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, null);
			exportManager.exportAll(exportConfig);
			
			ImportManager importManager = createNewContextAndGetImportManager();
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(), List.of("plans"), true));
			
			Plan actualPlan = planAccessor.get(plan.getId());
			Plan actualPlan2 = planAccessor.get(plan2.getId());
			assertEquals(plan.getId(), actualPlan.getId());
			assertEquals(plan.getRoot(), actualPlan.getRoot());
			assertEquals(plan2.getId(), actualPlan2.getId());
		} finally {
			testExportFile.delete();
		}
	}
	
	@Test
	public void testExportAllPlansWithParameters() throws Exception {
		Sequence rootSequence = sequence();
		Plan plan = PlanBuilder.create().startBlock(rootSequence).add(sequence()).endBlock().build();
		planAccessor.save(plan);
		Plan plan2 = PlanBuilder.create().startBlock(rootSequence).add(sequence()).endBlock().build();
		planAccessor.save(plan2);
		

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
			ExportManager exportManager = newExportManager();
			Map<String, String> metadata = buildMetadata();
			List<String> additionalEntities = new ArrayList<>();
			additionalEntities.add(Parameter.ENTITY_NAME);

			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, additionalEntities);
			ExportResult exportResult = exportManager.exportAll(exportConfig);

			assertEquals(2,exportResult.getMessages().size());
			assertEquals(ParameterManagerControllerPlugin.EXPORT_PROTECT_PARAM_WARN,exportResult.getMessages().toArray()[1]);
			assertEquals(ParameterManagerControllerPlugin.EXPORT_ENCRYPT_PARAM_WARN,exportResult.getMessages().toArray()[0]);

			EncryptionManager encryptionManager = new EncryptionManager() {
				@Override
				public String encrypt(String value) {
					return "###"+value;
				}
				
				@Override
				public String decrypt(String encryptedValue) {
					return encryptedValue.replaceFirst("###", "");
				}
				
				@Override
				public boolean isKeyPairChanged() {
					return false;
				}
				
				@Override
				public boolean isFirstStart() {
					return false;
				}
			};
			newContext(encryptionManager);
			ImportManager importManager = newImportManager();
			ImportConfiguration importConfiguration = new ImportConfiguration(testExportFile, dummyObjectEnricher(), null, true);
			ImportResult importResult = importManager.importAll(importConfiguration);
			assertEquals(1,importResult.getMessages().size());
			assertEquals(ParameterManagerControllerPlugin.IMPORT_RESET_WARN,importResult.getMessages().toArray()[0]);

			Plan actualPlan = planAccessor.get(plan.getId());
			Plan actualPlan2 = planAccessor.get(plan2.getId());
			Parameter actualParam = parameterAccessor.get(savedParam.getId());
			Parameter actualParamProtected = parameterAccessor.get(savedParamProtected.getId());
			Parameter actualParamProtectedEncrypted = parameterAccessor.get(savedParamProtectedEncrypted.getId());
			assertEquals(plan.getId(), actualPlan.getId());
			assertEquals(plan.getRoot(), actualPlan.getRoot());
			assertEquals(plan2.getId(), actualPlan2.getId());
			assertEquals(savedParam.getId(), actualParam.getId());
			assertEquals(savedParamProtected.getId(), actualParamProtected.getId());
			assertEquals(true, actualParamProtected.getProtectedValue());
			assertEquals(ParameterManager.RESET_VALUE, actualParamProtected.getValue());
			assertNull(actualParamProtected.getEncryptedValue());
			assertEquals(savedParamProtectedEncrypted.getId(), actualParamProtectedEncrypted.getId());
			assertEquals(true, actualParamProtectedEncrypted.getProtectedValue());
			assertNull(actualParamProtectedEncrypted.getValue());
			assertEquals("###Value", actualParamProtectedEncrypted.getEncryptedValue());
		} finally {
			testExportFile.delete();
		}
	}

	@Test
	public void testImportNewEncryptionManager() throws Exception {
		Parameter paramProtectedEncrypted = new Parameter(null,"key_pwd","Value","desc");
		paramProtectedEncrypted.setProtectedValue(true);
		paramProtectedEncrypted.setEncryptedValue(encryptionManager.encrypt(paramProtectedEncrypted.getValue()));
		paramProtectedEncrypted.setValue(null);
		Parameter savedParamProtectedEncrypted = parameterAccessor.save(paramProtectedEncrypted);

		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = newExportManager();
			Map<String, String> metadata = buildMetadata();
			List<String> additionalEntities = new ArrayList<>();
			additionalEntities.add(Parameter.ENTITY_NAME);

			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, additionalEntities);
			ExportResult exportResult = exportManager.exportAll(exportConfig);

			assertEquals(1,exportResult.getMessages().size());
			assertEquals(ParameterManagerControllerPlugin.EXPORT_ENCRYPT_PARAM_WARN,exportResult.getMessages().toArray()[0]);

			//Override previous encryption manager to simulate new instance
			EncryptionManager encryptionManagerNewInstance = new EncryptionManager() {
				@Override
				public String encrypt(String value) {
					return "###"+value;
				}
				@Override
				public String decrypt(String encryptedValue) throws EncryptionManagerException {
					throw new EncryptionManagerException("Error while decrypting value");
				}
				@Override
				public boolean isKeyPairChanged() {
					return false;
				}
				@Override
				public boolean isFirstStart() {
					return false;
				}
			};
			newContext(encryptionManagerNewInstance);
			ImportManager importManager = newImportManager();
			ImportConfiguration importConfiguration = new ImportConfiguration(testExportFile, dummyObjectEnricher(), null, true);
			ImportResult importResult = importManager.importAll(importConfiguration);
			assertEquals(1,importResult.getMessages().size());
			assertEquals(ParameterManagerControllerPlugin.IMPORT_DECRYPT_FAIL_WARN,importResult.getMessages().toArray()[0]);

			Parameter actualParamProtectedEncrypted = parameterAccessor.get(savedParamProtectedEncrypted.getId());
			assertEquals(savedParamProtectedEncrypted.getId(), actualParamProtectedEncrypted.getId());
			assertEquals(true, actualParamProtectedEncrypted.getProtectedValue());
			assertEquals(ParameterManager.RESET_VALUE, actualParamProtectedEncrypted.getValue());
			assertEquals(null, actualParamProtectedEncrypted.getEncryptedValue());
		} finally {
			testExportFile.delete();
		}
	}

	@Test
	public void testImportNoEncryptionManager() throws Exception {
		Parameter paramProtectedEncrypted = new Parameter(null,"key_pwd","Value","desc");
		paramProtectedEncrypted.setProtectedValue(true);
		paramProtectedEncrypted.setEncryptedValue(encryptionManager.encrypt(paramProtectedEncrypted.getValue()));
		paramProtectedEncrypted.setValue(null);
		Parameter savedParamProtectedEncrypted = parameterAccessor.save(paramProtectedEncrypted);

		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = newExportManager();
			Map<String, String> metadata = buildMetadata();
			List<String> additionalEntities = new ArrayList<>();
			additionalEntities.add(Parameter.ENTITY_NAME);

			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, additionalEntities);
			ExportResult exportResult = exportManager.exportAll(exportConfig);

			assertEquals(1,exportResult.getMessages().size());
			assertEquals(ParameterManagerControllerPlugin.EXPORT_ENCRYPT_PARAM_WARN,exportResult.getMessages().toArray()[0]);

			// Create a new context without encryption manager
			newContext(null);
			ImportManager importManager = newImportManager();
			ImportConfiguration importConfiguration = new ImportConfiguration(testExportFile, dummyObjectEnricher(), null, true);
			ImportResult importResult = importManager.importAll(importConfiguration);
			assertEquals(1,importResult.getMessages().size());
			assertEquals(ParameterManagerControllerPlugin.IMPORT_DECRYPT_NO_EM_WARN,importResult.getMessages().toArray()[0]);

			Parameter actualParamProtectedEncrypted = parameterAccessor.get(savedParamProtectedEncrypted.getId());
			assertEquals(savedParamProtectedEncrypted.getId(), actualParamProtectedEncrypted.getId());
			assertEquals(true, actualParamProtectedEncrypted.getProtectedValue());
			assertEquals(ParameterManager.RESET_VALUE, actualParamProtectedEncrypted.getValue());
			assertNull(actualParamProtectedEncrypted.getEncryptedValue());
		} finally {
			testExportFile.delete();
		}
	}

	@Test
	public void testImportProtectedToEncrypted() throws Exception {
		Parameter paramProtectedEncrypted = new Parameter(null,"key_pwd","Value","desc");
		paramProtectedEncrypted.setProtectedValue(true);
		Parameter savedParamProtected = parameterAccessor.save(paramProtectedEncrypted);

		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = newExportManager();
			Map<String, String> metadata = buildMetadata();
			List<String> additionalEntities = new ArrayList<>();
			additionalEntities.add(Parameter.ENTITY_NAME);

			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, additionalEntities);
			ExportResult exportResult = exportManager.exportAll(exportConfig);

			assertEquals(1,exportResult.getMessages().size());
			assertEquals(ParameterManagerControllerPlugin.EXPORT_PROTECT_PARAM_WARN,exportResult.getMessages().toArray()[0]);

			ImportManager importManager = createNewContextAndGetImportManager();
			ImportConfiguration importConfiguration = new ImportConfiguration(testExportFile, dummyObjectEnricher(), null, true);
			ImportResult importResult = importManager.importAll(importConfiguration);
			assertEquals(1,importResult.getMessages().size());
			assertEquals(ParameterManagerControllerPlugin.IMPORT_RESET_WARN,importResult.getMessages().toArray()[0]);

			Parameter actualParamProtectedEncrypted = parameterAccessor.get(savedParamProtected.getId());
			assertEquals(savedParamProtected.getId(), actualParamProtectedEncrypted.getId());
			assertEquals(true, actualParamProtectedEncrypted.getProtectedValue());
			assertEquals(ParameterManager.RESET_VALUE, actualParamProtectedEncrypted.getValue());
			assertNull(actualParamProtectedEncrypted.getEncryptedValue());
		} finally {
			testExportFile.delete();
		}
	}

	@Test
	public void testImportFromOlderVersions() throws Exception {
		testImportFromOlderVersion("60a515f058146c742b50a87b", "60a515fa58146c742b50a8cb", "60a5162658146c742b50aa0f", "60a5168958146c742b50af79", "export_3_15_7.sta");
		testImportFromOlderVersion("60a8e0d3dc92e67be456f0e2", "60a8e0d3dc92e67be456f0dc", "60a8e0d3dc92e67be456f0cc", "60a8e0d3dc92e67be456f0e4", "export_3_16_1.sta");
	}

	private void testImportFromOlderVersion(final String testSet01Id, final String testCase01Id,
			final String composite01Id, final String parameter01Id, String exportFileName) throws Exception {
		URL resource = getClass().getClassLoader().getResource("./step/core/export/"+exportFileName);
		File testImportFile = new File(resource.getFile());

		ImportManager importManager = createNewContextAndGetImportManager();
		importManager.importAll(new ImportConfiguration(testImportFile, dummyObjectEnricher(), null, true));

		Plan testSet01 = planAccessor.get(testSet01Id);
		assertNotNull(testSet01);
		assertEquals("TestSet 01", testSet01.getAttribute(AbstractOrganizableObject.NAME));
		
		Plan testCase01 = planAccessor.get(testCase01Id);
		assertNotNull(testCase01);
		assertEquals("TestCase 01", testCase01.getAttribute(AbstractOrganizableObject.NAME));
		
		Function composite01 = functionAccessor.get(composite01Id);
		assertNotNull(composite01);
		
		Parameter parameter01 = parameterAccessor.get(parameter01Id);
		assertNotNull(parameter01);
		assertEquals("Param01", parameter01.getKey());
		assertEquals("Value01", parameter01.getValue());
	}
	
	@Test
	public void testOlderParametersImport() throws Exception {
		URL resource = getClass().getClassLoader().getResource("./step/core/export/allParameters_3_15.sta");
		File testImportFile = new File(resource.getFile());

		ImportManager importManager = createNewContextAndGetImportManager();
		importManager.importAll(new ImportConfiguration(testImportFile, dummyObjectEnricher(), null, true));

		Parameter parameterClear = parameterAccessor.get("6059b301e7ca79765f814864");
		Parameter parameterProtected = parameterAccessor.get("6059b2d0e7ca79765f81451a");
		assertNotNull(parameterClear);
		assertNotNull(parameterProtected);
		assertEquals("value_clear",parameterClear.getValue());
		assertEquals(false,parameterClear.getProtectedValue());
		assertEquals(ParameterManager.RESET_VALUE,parameterProtected.getValue());
		assertEquals(true,parameterProtected.getProtectedValue());
		//assertEquals(originPlanId, actualPlan.getId().toString());
	}


	@Test
	public void testExportPlanByIdWithParameters() throws Exception {
		Plan plan = PlanBuilder.create().startBlock(sequence()).add(sequence()).endBlock().build();
		planAccessor.save(plan);
		
		Parameter param = new Parameter(null,"key","Value","desc");
		Parameter savedParam = parameterAccessor.save(param);
		
		File testExportFile = new File("testExport.zip");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = newExportManager();
			Map<String, String> metadata = buildMetadata();
			List<String> additionalEntities = new ArrayList<>();
			additionalEntities.add(Parameter.ENTITY_NAME);
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, additionalEntities);
			exportManager.exportById(exportConfig, plan.getId().toString());
			assertTrue(FileHelper.isArchive(testExportFile));
			
			ImportManager importManager = createNewContextAndGetImportManager();
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(), null, true));
			
			Parameter actualParam = parameterAccessor.get(savedParam.getId());
			Plan actualPlan = planAccessor.get(plan.getId());
			assertEquals(plan.getId(), actualPlan.getId());
			assertEquals(plan.getRoot(), actualPlan.getRoot());
			assertEquals(savedParam.getId(), actualParam.getId());
		} finally {
			testExportFile.delete();
		}
	}
	
	@Test
	public void testExportPlanByWithCustomFields() throws Exception {
		Sequence seq = sequence();
		seq.addCustomField("key", "value");
		Plan plan = PlanBuilder.create().startBlock(sequence()).add(seq).endBlock().build();
		planAccessor.save(plan);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = newExportManager();
			Map<String, String> metadata = buildMetadata();
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", false, null);
			exportManager.exportById(exportConfig, plan.getId().toString());
			
			ImportManager importManager = createNewContextAndGetImportManager();
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(), List.of("plans"), true));
			
			Plan actualPlan = planAccessor.get(plan.getId());
			assertEquals(plan.getId(), actualPlan.getId());
			assertEquals(plan.getRoot(), actualPlan.getRoot());
		} finally {
			testExportFile.delete();
		}
	}

	@Test
	public void testExportPlanRecursively() throws Exception {
		testExportPlansRecursively(false, true);
	}

	@Test
	public void testExportPlanRecursivelyNewReferences() throws Exception {
		testExportPlansRecursively(false, false);
	}

	@Test
	public void testExportPlanRecursivelyWithPansOnly() throws Exception {
		testExportPlansRecursively(true, true);
	}

	@Test
	public void testExportPlanRecursivelyNewReferencesWithPansOnly() throws Exception {
		testExportPlansRecursively(true, false);
	}

	private void testExportPlansRecursively(boolean plansOnly, boolean overwrite) throws Exception {
		Sequence rootSequence = sequence();
		Plan plan = PlanBuilder.create().startBlock(rootSequence).add(sequence()).endBlock().build();
		planAccessor.save(plan);
		Function function = new Function();
		String functionName = UUID.randomUUID().toString();
		function.addAttribute(AbstractOrganizableObject.NAME, functionName);
		functionAccessor.save(function);
		Sequence sequence = sequence();
		sequence.addChild(callPlan(plan.getId().toString()));

		CallFunction callFunction = FunctionArtefacts.keyword(functionName);
		sequence.addChild(callFunction);
		Plan plan2 = PlanBuilder.create().startBlock(rootSequence).add(sequence).endBlock().build();
		planAccessor.save(plan2);

		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = newExportManager();
			Map<String, String> metadata = buildMetadata();
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", true, null);
			exportManager.exportById(exportConfig, plan2.getId().toString());

			planAccessor.getAll().forEachRemaining(p -> planAccessor.remove(p.getId()));
			functionAccessor.getAll().forEachRemaining(p -> functionAccessor.remove(p.getId()));

			ImportManager importManager = createNewContextAndGetImportManager();
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(), plansOnly ? List.of("plans") : null, overwrite));

			assertEquals(2, planAccessor.stream().count());
			assertEquals(plansOnly ? 0 : 1, functionAccessor.stream().count());

			Plan actualPlan = planAccessor.get(plan.getId());
			Plan actualPlan2 = planAccessor.get(plan2.getId());
			Function actualFunction = functionAccessor.get(function.getId());
			if (overwrite) {
				assertEquals(plan.getId(), actualPlan.getId());
				assertEquals(plan.getRoot(), actualPlan.getRoot());
				assertEquals(plan2.getId(), actualPlan2.getId());
				if (plansOnly) {
					assertNull(actualFunction);
				} else {
					assertEquals(function.getId(), actualFunction.getId());
				}
			} else {
				assertNull(actualPlan);
				assertNull(actualPlan2);
				assertNull(actualFunction);
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
		CompositeFunctionType compositeFunctionType = new CompositeFunctionType(null);
		CompositeFunction function = compositeFunctionType.newFunction();
		compositeFunctionType.setupFunction(function);
		String functionName = UUID.randomUUID().toString();
		function.addAttribute(AbstractOrganizableObject.NAME, functionName);
		functionAccessor.save(function);
		
		Sequence sequence = sequence();
		CallFunction callFunction = FunctionArtefacts.keyword(functionName);
		sequence.addChild(callFunction);
		Plan plan = PlanBuilder.create().startBlock(sequence()).add(sequence).endBlock().build();
		planAccessor.save(plan);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = newExportManager();
			Map<String, String> metadata = buildMetadata();
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", true, null);
			exportManager.exportById(exportConfig, plan.getId().toString());
						
			newContext(null);
			
			ImportManager importManager = createNewContextAndGetImportManager();
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(), null, overwrite));
			
			AtomicInteger nbPlans = new AtomicInteger(0);
			planAccessor.getAll().forEachRemaining(p-> nbPlans.incrementAndGet());
			AtomicInteger nbFunctions = new AtomicInteger(0);
			functionAccessor.getAll().forEachRemaining(f->nbFunctions.incrementAndGet());
			assertEquals(1, nbPlans.intValue());
			assertEquals(1, nbFunctions.intValue());
			
			Plan actualPlan = planAccessor.get(plan.getId());
			Function actualFunction = functionAccessor.get(function.getId());

			if (overwrite) {
				assertEquals(plan.getId(), actualPlan.getId());
				assertEquals(plan.getRoot(), actualPlan.getRoot());
				assertEquals(function.getId(), actualFunction.getId());
			} else {
				assertNull(actualPlan);
				assertNull(actualFunction);
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
		// Create a resource
		Resource resource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_DATASOURCE, this.getClass().getResourceAsStream("dummyExcel.xls"), "TestResource.txt", false, null);
		assertNotNull(resource);
			
		ForEachBlock f = new ForEachBlock();
		ExcelDataPool p = new ExcelDataPool();
		p.setFile(new DynamicValue<> (FileResolver.RESOURCE_PREFIX + resource.getId().toHexString()));
		p.getHeaders().setValue(true);
		f.setDataSource(p);
		f.setDataSourceType("excel");
		
		Plan plan = PlanBuilder.create().startBlock(sequence())
											.startBlock(sequence())
												.add(f)
											.endBlock()
										.endBlock().build();
		String uniqueName = UUID.randomUUID().toString();
		plan.addAttribute(AbstractOrganizableObject.NAME, uniqueName);
		planAccessor.save(plan);
		
		File testExportFile = new File("testExport.json");
		try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
			ExportManager exportManager = newExportManager();
			Map<String, String> metadata = buildMetadata();
			ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", true, null);
			exportManager.exportById(exportConfig, plan.getId().toString());
			//delete created resource
			resourceManager.deleteResource(resource.getId().toHexString());
						
			newContext(null);
			
			ImportManager importManager = createNewContextAndGetImportManager();
			importManager.importAll(new ImportConfiguration(testExportFile, dummyObjectEnricher(), null, overwrite));
			
			Plan actualPlan = planAccessor.get(plan.getId());

			AtomicInteger nbPlans = new AtomicInteger(0);
			planAccessor.getAll().forEachRemaining(pp-> nbPlans.incrementAndGet());
			AtomicInteger nbResources = new AtomicInteger(0);
			resourceAccessor.getAll().forEachRemaining(r->{
				nbResources.incrementAndGet();
				//resourceManagerImport.deleteResource(r.getId().toHexString());
			});
			assertEquals(1, nbPlans.intValue());
			assertEquals(1, nbResources.intValue());
			
			//TODO check resource file was imported (do we keep same revision?)
			//resourceFile.exists()

			if (overwrite) {
				assertEquals(plan.getId(), actualPlan.getId());
				assertEquals(plan.getRoot(), actualPlan.getRoot());
				
				String newResourceId = getResourceIdOfExcelDatapool(plan);
				// Assert that the referenced resource id didn't change 
				assertFalse(newResourceId.isBlank());
				assertEquals(resource.getId().toHexString(), newResourceId);
				// Assert that the resource was imported properly under the same id
				Resource actualResource = resourceManager.getResource(resource.getId().toHexString());
				assertNotNull(actualResource);
			} else {
				// Assert that the id of the plan changed and thus that it cannot be found with the old id
				assertNull(actualPlan);
				// Assert that the plan has been properly imported
				HashMap<String, String> attributes = new HashMap<>();
				attributes.put(AbstractOrganizableObject.NAME, uniqueName);
				Plan newPlan = planAccessor.findByAttributes(attributes);
				assertNotNull(newPlan);
				String newResourceId = getResourceIdOfExcelDatapool(newPlan);
				// Assert that the referenced resource id has been updated properly
				assertFalse(newResourceId.isBlank());
				assertNotEquals(newResourceId, resource.getId().toHexString());
				// Assert that the resource was imported properly under the new id
				Resource actualResource = resourceManager.getResource(newResourceId);
				assertNotNull(actualResource);
			}
			
		} finally {
			testExportFile.delete();
		}
	}

	public String getResourceIdOfExcelDatapool(Plan newPlan) {
		ForEachBlock forEach = (ForEachBlock)newPlan.getRoot().getChildren().get(0).getChildren().get(0);
		ExcelDataPool dataPool = (ExcelDataPool) forEach.getDataSource();
		String newResource = dataPool.getFile().get();
		return newResource.replace(FileResolver.RESOURCE_PREFIX, "");
	}

	protected ObjectPredicate dummyObjectPredicate() {
		return t -> true;
	}
	
	@Test
	public void testImport_3_12() throws Exception {
		URL resource = getClass().getClassLoader().getResource("./step/core/export/3_12.json");
		File testImportFile = new File(resource.getFile());
		
		testImport3_12(testImportFile, true);
		testImport3_12(testImportFile, false);
	}

	private void testImport3_12(File testImportFile, boolean overwriteIds) throws Exception {
		//create a new context to test the import
		ImportManager importManager = createNewContextAndGetImportManager();
		importManager.importAll(new ImportConfiguration(testImportFile, dummyObjectEnricher(), List.of("plans"), overwriteIds));
		
		Plan actualPlan = planAccessor.findByAttributes(Map.of(AbstractOrganizableObject.NAME, "DataSet_while"));
		assertNotEquals("5c3860fb66d4260008813172", actualPlan.getId().toString());
		assertEquals(actualPlan, actualPlan);
		AbstractArtefact root = actualPlan.getRoot();
		assertEquals("DataSet_while", root.getAttribute(AbstractOrganizableObject.NAME));
		
		AbstractArtefact firstChild = root.getChildren().get(0);
		assertEquals("DataSet", firstChild.getAttribute(AbstractOrganizableObject.NAME));
	}
	
	@Test
	public void testImportVisualPlan_3_13() throws Exception {
		String resourcePath = "./step/core/export/3_13_visualPlan.json";
		String originPlanId = "5e8d7edb4cf3ad5e290d77e9";
		testOlderPlanImport(resourcePath, originPlanId, true);
	}

	@Test
	public void testImportVisualPlanWithAssert_3_13() throws Exception {
		String resourcePath = "./step/core/export/ExportVisualPlan3_13.json";
		String originPlanId = "5f87f3c83ff2d04dd8f9a3f7";
		// Test without overwrite
		testOlderPlanImport(resourcePath, originPlanId, false, TestSet.class, false);
		// Test with overwrite
		testOlderPlanImport(resourcePath, originPlanId, true, TestSet.class, true);
	}

	@Test
	public void testImportVisualPlanWithAssert_3_12() throws Exception {
		String resourcePath = "./step/core/export/TestWithAssert_3_12.json";
		String originPlanId = "5f87f3c83ff2d04dd8f9a3f7";
		testOlderPlanImport(resourcePath, originPlanId, false);
	}
	
	// @Test //not working in OS
	public void testImportTextPlan_3_13() throws Exception {
		String resourcePath = "./step/core/export/3_13_textPlan.json";
		String originPlanId = "5eb2789c117dff15d2bc8bc0";
		testOlderPlanImport(resourcePath, originPlanId, true);
	}
	
	@Test
	public void testImportPlan_3_12_() throws Exception {
		String resourcePath = "step/core/export/3_12_and_before.json";
		String originPlanId = "5c87c128958a7c000b6fb7d9";
		testOlderPlanImport(resourcePath, originPlanId, false);
	}
	
	protected void testOlderPlanImport(String resourcePath, String originPlanId, boolean assertOverwrite) throws Exception {
		// Test without overwrite
		testOlderPlanImport(resourcePath, originPlanId, false, Sequence.class, false);
		// Test with overwrite
		testOlderPlanImport(resourcePath, originPlanId, assertOverwrite, Sequence.class, true);
	}
	
	protected void testOlderPlanImport(String resourcePath, String originPlanId, boolean assertOverwrite, Class<?> planRootElementClass, boolean overwrite) throws Exception {
		URL resource = getClass().getClassLoader().getResource(resourcePath);
		File testImportFile = new File(resource.getFile());
		
		//create a new context to test the import
		ImportManager importManager = createNewContextAndGetImportManager();
		importManager.importAll(new ImportConfiguration(testImportFile, dummyObjectEnricher(), Arrays.asList("plans"), overwrite));
		
		Plan actualPlan = planAccessor.findByAttributes(Map.of(AbstractOrganizableObject.NAME, "Test"));
		// Due to the migration from the collection artefacts to plans overwrite is not supported when importing from 3.12 and below
		if(assertOverwrite) {
			assertEquals(originPlanId, actualPlan.getId().toString());
		}
		assertEquals(planRootElementClass, actualPlan.getRoot().getClass());
	}
	
	private ImportManager createNewContextAndGetImportManager() throws IOException {
		before();
		return new ImportManager(entityManager, migrationManager, Controller.VERSION);
	}
	
	private ImportManager newImportManager() throws IOException {
		return new ImportManager(entityManager, migrationManager, Controller.VERSION);
	}
}
