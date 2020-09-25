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
package step.localrunner;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import step.core.execution.ExecutionEngine;
import step.core.plans.runner.DefaultPlanRunner;

/**
 * A runner that runs plans and functions locally.
 * The list of classes containing functions has to be passed to the constructor
 * 
 * @deprecated Use {@link ExecutionEngine} instead
 * @author Jérôme Comte
 *
 */
public class LocalPlanRunner extends DefaultPlanRunner {
	
	/**
	 * @param functionClasses functionClasses the list of Classes containing the functions (aka Keywords)
	 */
	public LocalPlanRunner(Class<?>... functionClasses) {
		this(null, Arrays.asList(functionClasses));
	}
	
	/**
	 * @param functionClasses functionClasses the list of Classes containing the functions (aka Keywords)
	 */
	public LocalPlanRunner(List<Class<?>> functionClasses) {
		this(null, functionClasses);
	}
	
	/**
	 * @param properties a map containing the properties that are usually set under Parameters in the UI
	 * @param functionClasses the list of Classes containing the functions (aka Keywords)
	 */
	public LocalPlanRunner(Map<String, String> properties, Class<?>... functionClasses) {
		super(properties);
	}
	
	/**
	 * @param properties a map containing the properties that are usually set under Parameters in the UI
	 * @param functionClasses the list of Classes containing the functions (aka Keywords)
	 */
	public LocalPlanRunner(Map<String, String> properties, List<Class<?>> functionClasses) {
		super(properties);
	}
}
