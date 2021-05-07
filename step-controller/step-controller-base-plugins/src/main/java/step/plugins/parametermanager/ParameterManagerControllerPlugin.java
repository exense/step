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

import java.util.List;
import java.util.function.BiConsumer;

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
import step.core.export.ExportConfiguration;
import step.core.imports.GenericDBImporter;
import step.core.imports.ImportConfiguration;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.tables.TableRegistry;
import step.engine.plugins.ExecutionEnginePlugin;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.plugins.encryption.EncryptionManagerDependencyPlugin;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.InputType;
import step.plugins.screentemplating.ScreenInput;
import step.plugins.screentemplating.ScreenInputAccessor;
import step.plugins.screentemplating.ScreenTemplatePlugin;

@Plugin(dependencies= {ObjectHookControllerPlugin.class, ScreenTemplatePlugin.class, EncryptionManagerDependencyPlugin.class})
public class ParameterManagerControllerPlugin extends AbstractControllerPlugin {
	
	private static final String entityName = "parameters";

	public static Logger logger = LoggerFactory.getLogger(ParameterManagerControllerPlugin.class);

	private ParameterManager parameterManager;
	private EncryptionManager encryptionManager;
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		// The encryption manager might be null
		encryptionManager = context.get(EncryptionManager.class);

		Collection<Parameter> collection = context.getCollectionFactory().getCollection("parameters", Parameter.class);

		Accessor<Parameter> parameterAccessor = new AbstractAccessor<Parameter>(collection);
		context.put("ParameterAccessor", parameterAccessor);
		
		context.get(TableRegistry.class).register("parameters", new ParameterTable(collection));
		
		ParameterManager parameterManager = new ParameterManager(parameterAccessor, encryptionManager, context.getConfiguration());
		context.put(ParameterManager.class, parameterManager);
		this.parameterManager = parameterManager;
		
		context.getEntityManager().register(new Entity<Parameter, Accessor<Parameter>> (
				ParameterManagerControllerPlugin.entityName, 
				parameterAccessor,
				Parameter.class,
				new GenericDBImporter<Parameter, Accessor<Parameter>>(context)));
		context.getEntityManager().registerExportHook(new ParameterExportBiConsumer(context));
		context.getEntityManager().registerImportHook(new ParameterImportBiConsumer(context));
		
		context.getServiceRegistrationCallback().registerService(ParameterServices.class);
	}

	@Override
	public void initializeData(GlobalContext context) throws Exception {
		createScreenInputDefinitionsIfNecessary(context);
		
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

	private static final String PARAMETER_DIALOG = "parameterDialog";
	private static final String PARAMETER_TABLE = "parameterTable";

	private void createScreenInputDefinitionsIfNecessary(GlobalContext context) {
		// Parameter table
		ScreenInputAccessor screenInputAccessor = context.get(ScreenInputAccessor.class);
		List<ScreenInput> parameterTable = screenInputAccessor.getScreenInputsByScreenId(PARAMETER_TABLE);
		Input keyInput = new Input(InputType.TEXT, "key", "Key", "Keys containing 'pwd' or 'password' will be automatically protected", null);
		keyInput.setValueHtmlTemplate("<entity-icon entity=\"stBean\" entity-name=\"'parameter'\"/> <parameter-key parameter=\"stBean\" st-options=\"stOptions\" />");
		if(parameterTable.isEmpty()) {
			screenInputAccessor.save(new ScreenInput(0, PARAMETER_TABLE, keyInput));
			screenInputAccessor.save(new ScreenInput(1, PARAMETER_TABLE, new Input(InputType.TEXT, "value", "Value", null, null)));
			screenInputAccessor.save(new ScreenInput(2, PARAMETER_TABLE, new Input(InputType.TEXT, "activationExpression.script", "Activation script", null, null)));
			screenInputAccessor.save(new ScreenInput(3, PARAMETER_TABLE, new Input(InputType.TEXT, "priority", "	Priority", null, null)));
		}
		
		// Ensure the key input is always up to date
		parameterTable.forEach(i->{
			Input input = i.getInput();
			if(input.getId().equals("key")) {
				i.setInput(keyInput);
				screenInputAccessor.save(i);
			}
		});
		
		// Edit parameter dialog
		if(screenInputAccessor.getScreenInputsByScreenId(PARAMETER_DIALOG).isEmpty()) {
			Input input = new Input(InputType.TEXT, "key", "Key", "Keys containing 'pwd' or 'password' will be automatically protected", null);
			screenInputAccessor.save(new ScreenInput(0, PARAMETER_DIALOG, input));
			screenInputAccessor.save(new ScreenInput(1, PARAMETER_DIALOG, new Input(InputType.TEXT, "value", "Value", null, null)));
			screenInputAccessor.save(new ScreenInput(2, PARAMETER_DIALOG, new Input(InputType.TEXT, "description", "Description", null, null)));
			screenInputAccessor.save(new ScreenInput(3, PARAMETER_DIALOG, new Input(InputType.TEXT, "activationExpression.script", "Activation script", null, null)));
			screenInputAccessor.save(new ScreenInput(4, PARAMETER_DIALOG, new Input(InputType.TEXT, "priority", "	Priority", null, null)));
		}
	}

	public static String EXPORT_PROTECT_PARAM_WARN = "The parameter list contains protected parameter. The values of these parameters won't be exported and will have to be reset at import.";
	public static String EXPORT_ENCRYPT_PARAM_WARN = "The parameter list contains encrypted parameters. The values of these parameters will be reset if you import them on an other installation of step.";
	public static String IMPORT_DECRYPT_FAIL_WARN = "The export file contains encrypted parameter which could not be decrypted. The values of these parameters will be reset.";
	public static String IMPORT_DECRYPT_NO_EM_WARN = "The export file contains encrypted parameters. The values of these parameters will be reset.";
	public static String IMPORT_RESET_WARN = "The export file contains protected parameters. Their values must be reset.";

	public static class ParameterExportBiConsumer implements BiConsumer<Object, ExportConfiguration> {

		GlobalContext context;

		public ParameterExportBiConsumer(GlobalContext context) {
			this.context = context;
		}

		@Override
		public void accept(Object object_, ExportConfiguration exportConfiguration) {
			if (object_ != null && object_ instanceof Parameter) {
				Parameter param = (Parameter) object_;
				//if protected and not encrypted, mask value by changing it to reset value
				if (param.getProtectedValue() != null && param.getProtectedValue()) {
					if (param.getValue() != null) {
						param.setValue(ParameterManager.RESET_VALUE);
						exportConfiguration.addMessage(EXPORT_PROTECT_PARAM_WARN);
					} else {
						exportConfiguration.addMessage(EXPORT_ENCRYPT_PARAM_WARN);
					}
				}
			}
		}
	}

	public static class ParameterImportBiConsumer implements BiConsumer<Object, ImportConfiguration> {

		GlobalContext context;

		public ParameterImportBiConsumer(GlobalContext context) {
			this.context = context;
		}

		@Override
		public void accept(Object object_, ImportConfiguration importConfiguration) {
			if (object_ != null && object_ instanceof Parameter) {
				Parameter param = (Parameter) object_;
				//if importing protected and encrypted value
				if (param.getProtectedValue() != null && param.getProtectedValue()) {
					if (param.getValue() == null) {
						EncryptionManager encryptionManager = context.get(EncryptionManager.class);
						//if we have a valid encryption manager and can still decrypt keep encrypted value, else reset
						if (encryptionManager != null && param.getEncryptedValue() != null) {
							try {
								encryptionManager.decrypt(param.getEncryptedValue());
							} catch (EncryptionManagerException e) {
								param.setValue(ParameterManager.RESET_VALUE);
								param.setEncryptedValue(null);
								importConfiguration.addMessage(IMPORT_DECRYPT_FAIL_WARN);
							}
						} else {
							param.setValue(ParameterManager.RESET_VALUE);
							param.setEncryptedValue(null);
							importConfiguration.addMessage(IMPORT_DECRYPT_NO_EM_WARN);
						}
					} else {
						param.setValue(ParameterManager.RESET_VALUE);
						importConfiguration.addMessage(IMPORT_RESET_WARN);
					}
				}
			}
		}
	}


}
