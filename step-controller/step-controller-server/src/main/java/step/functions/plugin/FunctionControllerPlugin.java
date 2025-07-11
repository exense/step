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
package step.functions.plugin;

import ch.exense.commons.app.Configuration;
import step.artefacts.handlers.FunctionLocator;
import step.artefacts.handlers.SelectorHelper;
import step.attachments.FileResolver;
import step.controller.grid.GridPlugin;
import step.controller.grid.services.FunctionServices;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.entities.EntityManager;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.FunctionAccessorImpl;
import step.functions.accessor.FunctionEntity;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.manager.FunctionManager;
import step.functions.manager.FunctionManagerImpl;
import step.functions.type.FunctionTypeConfiguration;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.client.GridClient;
import step.plugins.screentemplating.*;
import step.plugins.table.settings.TableSettings;
import step.plugins.table.settings.TableSettingsAccessor;
import step.plugins.table.settings.TableSettingsBuilder;
import step.plugins.table.settings.TableSettingsPlugin;
import step.resources.ResourceManagerControllerPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Plugin(dependencies= {ScreenTemplatePlugin.class, TableSettingsPlugin.class, GridPlugin.class, ResourceManagerControllerPlugin.class, ObjectHookControllerPlugin.class})
public class FunctionControllerPlugin extends AbstractControllerPlugin {

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		Configuration configuration = context.getConfiguration();
		
		GridClient gridClient = context.require(GridClient.class);
		FileResolver fileResolver = context.getFileResolver();

		ObjectHookRegistry objectHookRegistry = context.require(ObjectHookRegistry.class);
		
		FunctionEditorRegistry editorRegistry = new FunctionEditorRegistry();
		
		FunctionTypeConfiguration functionTypeConfiguration = new FunctionTypeConfiguration();
		functionTypeConfiguration.setFileResolverCacheConcurrencyLevel(configuration.getPropertyAsInteger("functions.fileresolver.cache.concurrencylevel", 4));
		functionTypeConfiguration.setFileResolverCacheMaximumsize(configuration.getPropertyAsInteger("functions.fileresolver.cache.maximumsize", 1000));
		functionTypeConfiguration.setFileResolverCacheExpireAfter(configuration.getPropertyAsInteger("functions.fileresolver.cache.expireafter.ms", 500));
		FunctionTypeRegistry functionTypeRegistry = new FunctionTypeRegistryImpl(fileResolver, gridClient, objectHookRegistry);

		Collection<Function> collection = context.getCollectionFactory().getCollection("functions", Function.class);
		FunctionAccessor functionAccessor = new FunctionAccessorImpl(collection);
		FunctionManager functionManager = new FunctionManagerImpl(functionAccessor, functionTypeRegistry);
		FunctionExecutionService functionExecutionService = new FunctionExecutionServiceImpl(gridClient, functionTypeRegistry, context.getDynamicBeanResolver());
		
		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));

		context.put(FunctionAccessor.class, functionAccessor);
		SelectorHelper selectorHelper = new SelectorHelper(dynamicJsonObjectResolver);
		final FunctionLocator functionLocator = new FunctionLocator(functionAccessor, selectorHelper);
		EntityManager entityManager = context.getEntityManager();
		entityManager.register(new FunctionEntity(functionAccessor, functionLocator, entityManager));
		context.put(FunctionManager.class, functionManager);
		context.put(FunctionTypeRegistry.class, functionTypeRegistry);
		context.put(FunctionEditorRegistry.class, editorRegistry);
		context.put(FunctionExecutionService.class, functionExecutionService);

		context.getServiceRegistrationCallback().registerService(FunctionServices.class);
		
		TableRegistry tableRegistry = context.get(TableRegistry.class);
		
		Collection<Function> functionCollection = context.getCollectionFactory()
				.getCollection(EntityManager.functions, Function.class);
		tableRegistry.register(EntityManager.functions, new Table<>(functionCollection, "kw-read", true)
				.withResultListFactory(()->new ArrayList<>(){}));
	}

	@Override
	public void initializeData(GlobalContext context) throws Exception {
		createScreenInputsAndTableSettingsIfNecessary(context);
	}

	protected void createScreenInputsAndTableSettingsIfNecessary(GlobalContext context) {
		ScreenInputAccessor screenInputAccessor = context.get(ScreenInputAccessor.class);
		List<ScreenInput> functionTableInputs = screenInputAccessor.getScreenInputsByScreenId(ScreenTemplatePlugin.FUNCTION_SCREEN_ID);
		// Search name input
		Optional<ScreenInput> nameInputOrig = functionTableInputs.stream().filter(i -> i.getInput().getId().equals("attributes.name")).findFirst();
		// Create it if not existing
		if(nameInputOrig.isEmpty()) {
			Input nameInput = new Input(InputType.TEXT, "attributes.name", "Name", null, null);
			nameInput.setCustomUIComponents(List.of("functionLink"));
			ScreenInput savedInput = screenInputAccessor.save(new ScreenInput(0, ScreenTemplatePlugin.FUNCTION_SCREEN_ID, nameInput, true));
			//Create table settings
			createTableSettingsIfNecessary(context, savedInput);
		} else {
			//Create table settings
			createTableSettingsIfNecessary(context, nameInputOrig.get());
		}
	}

	private void createTableSettingsIfNecessary(GlobalContext context, ScreenInput nameInput) {
		TableSettingsAccessor tableSettingsAccessor = context.get(TableSettingsAccessor.class);
		if (tableSettingsAccessor.findSystemTableSettings(EntityManager.functions).isEmpty()) {
			TableSettings setting = TableSettingsBuilder.builder().withSettingId(EntityManager.functions)
					.addColumn("bulkSelection", true)
					.addColumn("attributes.project", true)
					.addColumn("attributes.name", true, nameInput)
					.addColumn("type", true)
					.addColumn("customFields.functionPackageId", true)
					.addColumn("automationPackage", true)
					.addColumn("actions", true)
					.build();
			tableSettingsAccessor.save(setting);
		}
	}
}
