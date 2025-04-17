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
package step.plugins.parametermanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.Accessor;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.collections.filters.Equals;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.encryption.EncryptionManager;
import step.core.entities.Entity;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.parameter.AbstractEncryptedValuesManager;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.plugins.encryption.EncryptionManagerDependencyPlugin;
import step.plugins.screentemplating.*;

import java.util.Set;
import java.util.stream.Collectors;

import static step.parameter.Parameter.PARAMETER_PROTECTED_VALUE_FIELD;
import static step.parameter.Parameter.PARAMETER_VALUE_FIELD;

@Plugin(dependencies= {ObjectHookControllerPlugin.class, ScreenTemplatePlugin.class, EncryptionManagerDependencyPlugin.class})
public class ParameterManagerControllerPlugin extends AbstractControllerPlugin {

	public static final String ENTITY_PARAMETERS = "parameters";
	public static Logger logger = LoggerFactory.getLogger(ParameterManagerControllerPlugin.class);

	private ParameterManager parameterManager;
	private EncryptionManager encryptionManager;
	
	@Override
	public void serverStart(GlobalContext context) {
		// The encryption manager might be null
		encryptionManager = context.get(EncryptionManager.class);

		Collection<Parameter> collection = context.getCollectionFactory().getCollection(ENTITY_PARAMETERS, Parameter.class);

		Accessor<Parameter> parameterAccessor = new AbstractAccessor<>(collection);
		context.put("ParameterAccessor", parameterAccessor);

		context.get(TableRegistry.class).register(ENTITY_PARAMETERS, new Table<>(collection, "param-read", true)
				.withResultItemTransformer((p,s) -> AbstractEncryptedValuesManager.maskProtectedValue(p))
				.withDerivedTableFiltersFactory(lf -> {
					Set<String> allFilterAttributes = lf.stream().map(Filters::collectFilterAttributes).flatMap(Set::stream).collect(Collectors.toSet());
					return allFilterAttributes.contains(PARAMETER_VALUE_FIELD + ".value") ? new Equals(PARAMETER_PROTECTED_VALUE_FIELD, false) : Filters.empty();
				}));
		
		ParameterManager parameterManager = new ParameterManager(parameterAccessor, encryptionManager, context.getConfiguration(), context.getDynamicBeanResolver());
		context.put(ParameterManager.class, parameterManager);
		this.parameterManager = parameterManager;
		
		context.getEntityManager().register(new Entity<> (
				Parameter.ENTITY_NAME, 
				parameterAccessor,
				Parameter.class));
		context.getEntityManager().registerExportHook(new EncryptedEntityExportBiConsumer(Parameter.class));
		context.getEntityManager().registerImportHook(new EncryptedEntityImportBiConsumer(encryptionManager, Parameter.class));
		
		context.getServiceRegistrationCallback().registerService(ParameterServices.class);
	}

	@Override
	public void initializeData(GlobalContext context) throws Exception {
		
		if(encryptionManager != null) {
			if(encryptionManager.isFirstStart()) {
				logger.info("First start of the encryption manager. Encrypting all protected parameters...");
				parameterManager.encryptAllParameters();
			}
			if(encryptionManager.isKeyPairChanged()) {
				logger.info("Key pair of encryption manager changed. Resetting all protected parameters...");
				parameterManager.resetAllProtectedParameters();
			}
		}
	}

	@Override
	public ExecutionEnginePlugin getExecutionEnginePlugin() {
		return new ParameterManagerPlugin(parameterManager);
	}


}
