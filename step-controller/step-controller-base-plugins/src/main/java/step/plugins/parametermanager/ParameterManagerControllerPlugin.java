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
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.encryption.EncryptionManager;
import step.core.encryption.EncryptionManagerException;
import step.core.entities.Entity;
import step.core.export.ExportContext;
import step.core.imports.ImportContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.plugins.encryption.EncryptionManagerDependencyPlugin;
import step.plugins.screentemplating.*;

import java.util.function.BiConsumer;

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
				.withResultItemEnricher(p -> ParameterServices.maskProtectedValue(p)));
		
		ParameterManager parameterManager = new ParameterManager(parameterAccessor, encryptionManager, context.getConfiguration());
		context.put(ParameterManager.class, parameterManager);
		this.parameterManager = parameterManager;
		
		context.getEntityManager().register(new Entity<> (
				Parameter.ENTITY_NAME, 
				parameterAccessor,
				Parameter.class));
		context.getEntityManager().registerExportHook(new ParameterExportBiConsumer());
		context.getEntityManager().registerImportHook(new ParameterImportBiConsumer(encryptionManager));
		
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
		return new ParameterManagerPlugin(parameterManager, encryptionManager);
	}

	public static String EXPORT_PROTECT_PARAM_WARN = "The parameter list contains protected parameter. The values of these parameters won't be exported and will have to be reset at import.";
	public static String EXPORT_ENCRYPT_PARAM_WARN = "The parameter list contains encrypted parameters. The values of these parameters will be reset if you import them on an other installation of step.";
	public static String IMPORT_DECRYPT_FAIL_WARN = "The export file contains encrypted parameter which could not be decrypted. The values of these parameters will be reset.";
	public static String IMPORT_DECRYPT_NO_EM_WARN = "The export file contains encrypted parameters. The values of these parameters will be reset.";
	public static String IMPORT_RESET_WARN = "The export file contains protected parameters. Their values must be reset.";

	public static class ParameterExportBiConsumer implements BiConsumer<Object, ExportContext> {

		@Override
		public void accept(Object object_, ExportContext exportContext) {
			if (object_ instanceof Parameter) {
				Parameter param = (Parameter) object_;
				//if protected and not encrypted, mask value by changing it to reset value
				if (param.getProtectedValue() != null && param.getProtectedValue()) {
					if (param.getValue() != null) {
						param.setValue(ParameterManager.RESET_VALUE);
						exportContext.addMessage(EXPORT_PROTECT_PARAM_WARN);
					} else {
						exportContext.addMessage(EXPORT_ENCRYPT_PARAM_WARN);
					}
				}
			}
		}
	}

	public static class ParameterImportBiConsumer implements BiConsumer<Object, ImportContext> {

		private final EncryptionManager encryptionManager;

		public ParameterImportBiConsumer(EncryptionManager encryptionManager) {
			this.encryptionManager = encryptionManager;
		}

		@Override
		public void accept(Object object_, ImportContext importContext) {
			if (object_ instanceof Parameter) {
				Parameter param = (Parameter) object_;
				//if importing protected and encrypted value
				if (param.getProtectedValue() != null && param.getProtectedValue()) {
					if (param.getValue() == null) {
						//if we have a valid encryption manager and can still decrypt keep encrypted value, else reset
						if (encryptionManager != null && param.getEncryptedValue() != null) {
							try {
								encryptionManager.decrypt(param.getEncryptedValue());
							} catch (EncryptionManagerException e) {
								param.setValue(ParameterManager.RESET_VALUE);
								param.setEncryptedValue(null);
								importContext.addMessage(IMPORT_DECRYPT_FAIL_WARN);
							}
						} else {
							param.setValue(ParameterManager.RESET_VALUE);
							param.setEncryptedValue(null);
							importContext.addMessage(IMPORT_DECRYPT_NO_EM_WARN);
						}
					} else {
						param.setValue(ParameterManager.RESET_VALUE);
						importContext.addMessage(IMPORT_RESET_WARN);
					}
				}
			}
		}
	}


}
