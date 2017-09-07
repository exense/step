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

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

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
			
			String actual = null;
			boolean actualResolved = false;
			if(key.startsWith("$")) {
				try {
					Object result = JsonPath.parse(outputJson.toString()).read(key);
					if(result instanceof String) {
						actual = (String) result;
						actualResolved = true;
					} else {
						passed = false;
						message = "The json path '"+key+"' return an object of type "+result.getClass()+" which is not supported.";
					}					
				} catch(PathNotFoundException e) {
					passed = false;
					message = e.getMessage();
				}
 			} else {
				if(outputJson.containsKey(key)) {	
					actual = outputJson.getString(key);				
					actualResolved = true;
				} else {
					passed = false;
					message = "Unable to execute assertion. The keyword output doesn't contain the attribute '"+key+"'";
				}
			}
			
			if(actualResolved) {				
				Object expectedValueObject = artefact.getExpected().get();
				String expectedValue = expectedValueObject!=null?expectedValueObject.toString():null;

				boolean negate = artefact.isNegate();
				String not = negate?" not ":" ";
				
				AssertOperator operator = artefact.getOperator();
				if(operator == AssertOperator.EQUALS) {
					passed = artefact.isNegate()^expectedValue.equals(actual);
					message = "'"+key + "' expected" + not + "to be equal to '"+artefact.getExpected().get()+"' "+(passed?"and":"but")+ " was '"+actual+"'";
				} else if(operator == AssertOperator.CONTAINS) {
					passed = negate^actual.contains(expectedValue);
					message = "'"+key + "' expected" + not + "to contain '"+expectedValue+ "' "+(passed?"and":"but")+ " was '"+actual+"'";
				} else if(operator == AssertOperator.BEGINS_WITH) {
					passed = negate^actual.startsWith(expectedValue);
					message = "'"+key + "' expected" + not + "to begin with '"+expectedValue+ "' "+(passed?"and":"but")+ " was '"+actual+"'";
				} else if(operator == AssertOperator.ENDS_WITH) {
					passed = negate^actual.endsWith(expectedValue);
					message = "'"+key + "' expected" + not + "to end with '"+expectedValue+ "' "+(passed?"and":"but")+ " was '"+actual+"'";
				} else if(operator == AssertOperator.MATCHES) {
					passed = negate^actual.matches(expectedValue);
					message = "'"+key + "' expected" + not + "to match regular expression '"+expectedValue+ "' "+(passed?"and":"but")+ " was '"+actual+"'";
				} else {
					throw new RuntimeException("Unsupported operator "+operator);
				}
			}
			node.setMessage(message);			
			node.setStatus(passed?ReportNodeStatus.PASSED:ReportNodeStatus.FAILED);
		}
	}

	@Override
	public AssertReportNode createReportNode_(ReportNode parentNode, Assert artefact) {
		return new AssertReportNode();
	}
}
