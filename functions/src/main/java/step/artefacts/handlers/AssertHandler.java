/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.artefacts.handlers;

import javax.json.JsonObject;

import step.artefacts.Assert;
import step.artefacts.Assert.AssertOperator;
import step.artefacts.reports.AssertReportNode;
import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class AssertHandler extends ArtefactHandler<Assert, AssertReportNode> {
	
	@Override
	protected void createReportSkeleton_(AssertReportNode parentNode, Assert artefact) {

	}

	@Override
	protected void execute_(AssertReportNode node, Assert artefact) {
		CallFunctionReportNode callFunctionReport = (CallFunctionReportNode) context.getVariablesManager().getVariable("callReport");
		if(callFunctionReport==null) {
			// TODO externalize error messages and use error codes at this place.
			throw new RuntimeException("Keyword report unreachable. Asserts should be wrapped in Keyword nodes in the test plan.");
		}
		if(callFunctionReport.getStatus()==ReportNodeStatus.PASSED) {			
			JsonObject outputJson = callFunctionReport.getOutputObject();
			String key = artefact.getActual().get();
			boolean passed = false;
			String message = null;
			if(outputJson.containsKey(key)) {				
				String var = outputJson.getString(key);
				String expectedValue = artefact.getExpected().get();
				
				AssertOperator operator = artefact.getOperator();
				if(operator == AssertOperator.EQUALS) {
					if(artefact.getExpected().get().equals(var)) {
						passed = true;
					}
					message = "Expected : '"+artefact.getExpected().get()+"' "+(passed?"and":"but")+ " was '"+var+"'";
				} else if(operator == AssertOperator.CONTAINS) {
					if(var.contains(artefact.getExpected().get())) {
						passed = true;
					}
					message = "'"+key + "' expected to contain '"+expectedValue+ "' "+(passed?"and":"but")+ " was '"+var+"'";
				} else if(operator == AssertOperator.BEGINS_WITH) {
					if(var.startsWith(artefact.getExpected().get())) {
						passed = true;
					}
					message = "'"+key + "' expected to start with '"+expectedValue+ "' "+(passed?"and":"but")+ " was '"+var+"'";
				} else if(operator == AssertOperator.ENDS_WITH) {
					if(var.endsWith(artefact.getExpected().get())) {
						passed = true;
					}
					message = "'"+key + "' expected to end with '"+expectedValue+ "' "+(passed?"and":"but")+ " was '"+var+"'";
				} else if(operator == AssertOperator.MATCHES) {
					if(var.matches(artefact.getExpected().get())) {
						passed = true;
					}
					message = "'"+key + "' expected to match regular expression '"+expectedValue+ "' "+(passed?"and":"but")+ " was '"+var+"'";
				}
			} else {
				passed = false;
				message = "Unable to execute assertion. The keyword output doesn't contain the attribute '"+key+"'";
			}
			node.setMessage(message);
			if(passed) {
				node.setStatus(ReportNodeStatus.PASSED);
			} else {
				node.setStatus(ReportNodeStatus.FAILED);
			}
		}
	}

	@Override
	public AssertReportNode createReportNode_(ReportNode parentNode, Assert artefact) {
		return new AssertReportNode();
	}
}
