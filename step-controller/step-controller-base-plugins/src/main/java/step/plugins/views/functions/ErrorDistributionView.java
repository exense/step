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
package step.plugins.views.functions;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.reports.ReportNode;
import step.plugins.views.AbstractView;
import step.plugins.views.View;

@View
public class ErrorDistributionView extends AbstractView<ErrorDistribution> {	
	
	protected static final String DEFAULT_KEY = "Other";
	
	@Override
	public void afterReportNodeSkeletonCreation(ErrorDistribution model, ReportNode node) {
	}

	@Override
	public void beforeReportNodeExecution(ErrorDistribution model, ReportNode node) {
	}


	@Override
	public void afterReportNodeExecution(ErrorDistribution model, ReportNode node) {
		if(node instanceof CallFunctionReportNode && node.persistNode()) {
			model.setCount(model.getCount()+1);
			if(node.getError()!=null && node.getError().getMsg()!=null) {
				model.setErrorCount(model.getErrorCount()+1);
				model.incrementByMsg(node.getError().getMsg());
				model.incrementByCode(node.getError().getCode() == null?"default":node.getError().getCode().toString()); //temporary due to Integer type
			}
		}
	}

	@Override
	public ErrorDistribution init() {
		return new ErrorDistribution(500, DEFAULT_KEY);
	}

	@Override
	public String getViewId() {
		return "errorDistribution";
	}

	@Override
	public void rollbackReportNode(ErrorDistribution model, ReportNode node) {
		if(node instanceof CallFunctionReportNode && node.persistNode()) {
			model.setCount(model.getCount()-1);
			if(node.getError()!=null && node.getError().getMsg()!=null) {
				model.setErrorCount(model.getErrorCount()-1);
				model.decrementByMsg(node.getError().getMsg());
				model.decrementByCode(node.getError().getCode() == null?"default":node.getError().getCode().toString()); //temporary due to Integer type
			}
		}
	}
}
