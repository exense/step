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
package step.parameter;

import ch.exense.commons.app.Configuration;
import step.commons.activation.Activator;
import step.core.accessors.Accessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.encryption.EncryptionManager;
import step.encryption.AbstractEncryptedValuesManager;
import step.encryption.EncryptedValueManagerException;

public class ParameterManager extends AbstractEncryptedValuesManager<Parameter> {

	private final Accessor<Parameter> parameterAccessor;

	public ParameterManager(Accessor<Parameter> parameterAccessor, EncryptionManager encryptionManager, Configuration configuration, DynamicBeanResolver dynamicBeanResolver) {
		this(parameterAccessor, encryptionManager, configuration.getProperty("tec.activator.scriptEngine", Activator.DEFAULT_SCRIPT_ENGINE), dynamicBeanResolver);
	}

	public ParameterManager(Accessor<Parameter> parameterAccessor, EncryptionManager encryptionManager, String defaultScriptEngine, DynamicBeanResolver dynamicBeanResolver) {
		super(encryptionManager, defaultScriptEngine, dynamicBeanResolver);
		this.parameterAccessor = parameterAccessor;
	}

	public static ParameterManager copy(ParameterManager from, Accessor<Parameter> parameterAccessor){
		return new ParameterManager(parameterAccessor, from.encryptionManager, from.defaultScriptEngine, from.dynamicBeanResolver);
	}

	@Override
	protected Accessor<Parameter> getAccessor() {
		return parameterAccessor;
	}

	@Override
	public String getEntityNameForLogging() {
		return "parameter";
	}

	public Accessor<Parameter> getParameterAccessor() {
		return getAccessor();
	}

	@Override
	protected void validateBeforeSave(Parameter newObj) {
		super.validateBeforeSave(newObj);

		ParameterScope scope = newObj.getScope();
		if(scope != null && scope.equals(ParameterScope.GLOBAL) && newObj.getScopeEntity() != null) {
			throw new EncryptedValueManagerException("Scope entity cannot be set for " + getEntityNameForLogging() + "s with GLOBAL scope.");
		}
	}
}
