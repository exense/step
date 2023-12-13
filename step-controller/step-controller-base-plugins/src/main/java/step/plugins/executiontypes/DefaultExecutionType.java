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
package step.plugins.executiontypes;

import step.core.GlobalContext;
import step.core.execution.type.ExecutionType;
import step.core.views.ViewManager;
import step.plugins.views.functions.ReportNodeStatusDistribution;

public class DefaultExecutionType extends ExecutionType {

	private ViewManager viewManager;
	
	public DefaultExecutionType(GlobalContext context) {
		super("Default");
		this.viewManager = context.get(ViewManager.class);
	}

	@Override
	public Object getExecutionSummary(String executionId) {
		ReportNodeStatusDistribution distribution = (ReportNodeStatusDistribution) viewManager.queryView("statusDistributionForFunctionCalls", executionId);
		return distribution;
	}

}
