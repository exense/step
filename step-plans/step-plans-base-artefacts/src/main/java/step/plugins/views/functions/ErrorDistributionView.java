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
import step.core.reports.Error;
import step.core.views.AbstractView;
import step.core.views.View;

@View
public class ErrorDistributionView extends AbstractView<ErrorDistribution> {

	protected static final String DEFAULT_KEY = "Other";
	public static final String ERROR_DISTRIBUTION_VIEW = "errorDistribution";

	@Override
	public void afterReportNodeSkeletonCreation(ErrorDistribution model, ReportNode node) {
	}

	@Override
	public void beforeReportNodeExecution(ErrorDistribution model, ReportNode node) {
	}


	@Override
	public void afterReportNodeExecution(ErrorDistribution model, ReportNode node) {
		Error error = node.getError();
		if (error != null && error.isRoot()) {
			model.setErrorCount(model.getErrorCount() + 1);
			model.incrementByMsg(nonNullErrorMessage(error));
			model.incrementByCode(errorCodeOrDefault(error));
		}
		// TODO create dedicated view to count the number of keyword calls
		if(node instanceof CallFunctionReportNode) {
			model.setCount(model.getCount()+1);
		}
	}

	private static String errorCodeOrDefault(Error error) {
		return error.getCode() == null ? "default" : error.getCode().toString();
	}

	private static String nonNullErrorMessage(Error error) {
		return error.getMsg() != null ? error.getMsg() : "";
	}

	@Override
	public ErrorDistribution init() {
		return new ErrorDistribution(500, DEFAULT_KEY);
	}

	@Override
	public String getViewId() {
		return ERROR_DISTRIBUTION_VIEW;
	}

	@Override
	public void onReportNodeRemoval(ErrorDistribution model, ReportNode node) {
		removeReportNodeErrorIfAny(model, node);
		if(node instanceof CallFunctionReportNode) {
			model.setCount(model.getCount()-1);
		}
	}

	@Override
	public void onErrorContributionRemoval(ErrorDistribution model, ReportNode node) {
		removeReportNodeErrorIfAny(model, node);
	}

	private static void removeReportNodeErrorIfAny(ErrorDistribution model, ReportNode node) {
		Error error = node.getError();
		if (error != null && error.isRoot()) {
			model.setErrorCount(model.getErrorCount() - 1);
			model.decrementByMsg(nonNullErrorMessage(error));
			model.decrementByCode(errorCodeOrDefault(error)); //temporary due to Integer type
		}
	}
}
