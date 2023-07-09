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
package step.artefacts.handlers;

import jakarta.json.JsonObject;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import jakarta.json.JsonValue;
import step.artefacts.Assert;
import step.artefacts.Assert.AssertOperator;
import step.artefacts.handlers.asserts.*;
import step.artefacts.reports.AssertReportNode;
import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

import java.util.HashMap;
import java.util.Map;

public class AssertHandler extends ArtefactHandler<Assert, AssertReportNode> {

	private final Map<AssertOperator, AssertOperatorHandler> operatorHandlers;

	public AssertHandler() {
		this.operatorHandlers = new HashMap<>();
		this.operatorHandlers.put(AssertOperator.EQUALS, new EqualsOperatorHandler());
		this.operatorHandlers.put(AssertOperator.CONTAINS, new ContainsOperatorHandler());
		this.operatorHandlers.put(AssertOperator.BEGINS_WITH, new BeginsWithOperatorHandler());
		this.operatorHandlers.put(AssertOperator.ENDS_WITH, new EndsWithOperatorHandler());
		this.operatorHandlers.put(AssertOperator.MATCHES, new MatchesOperatorHandler());
		this.operatorHandlers.put(AssertOperator.GREATER_THAN, new GreaterThanOperatorHandler());
		this.operatorHandlers.put(AssertOperator.LESS_THAN, new LessThanOperatorHandler());
	}

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
			AssertOperator operator = artefact.getOperator();
			node.setKey(key);

			ValueResolvingResult valueResolvingResult = resolveValue(outputJson, key, operator);

			boolean passed = false;
			if(valueResolvingResult.actualResolved) {
				node.setActual(valueResolvingResult.actual == null ? null : valueResolvingResult.actual.toString());
				
				String expectedValue = artefact.getExpected().get();
				node.setExpected(expectedValue);

				//boolean negate = artefact.isNegate();
				boolean negate = artefact.getDoNegate().get();

				AssertResult assertResult = applyOperator(key, valueResolvingResult.actual, expectedValue, negate, operator);

				node.setDescription(assertResult.getDescription());
				node.setMessage(assertResult.getMessage());
				node.setStatus(assertResult.isPassed() ? ReportNodeStatus.PASSED : ReportNodeStatus.FAILED);

				passed = assertResult.isPassed();
			} else {
				node.setMessage(valueResolvingResult.message);
				node.setStatus(ReportNodeStatus.FAILED);
			}
			
			if(!passed) {
				String customErrorMessage = artefact.getCustomErrorMessage().get();
				if(customErrorMessage != null && !customErrorMessage.isEmpty()) {
					node.setMessage(customErrorMessage);
				}
			}
		} else {
			node.setStatus(ReportNodeStatus.NORUN);
		}
	}

	private ValueResolvingResult resolveValue(JsonObject outputJson, String key, AssertOperator operator) {
		if(key.startsWith("$")) {
			return resolveJsonPathValue(outputJson, key, operator);
		} else {
			return resolveSimpleValue(outputJson, key, operator);
		}
	}

	private ValueResolvingResult resolveJsonPathValue(JsonObject outputJson, String key, AssertOperator operator) {
		ValueResolvingResult valueResolvingResult = new ValueResolvingResult();
		try {
			Object result = JsonPath.parse(outputJson.toString()).read(key);
			if (getOperatorHandler(operator).isSupported(result)) {
				valueResolvingResult.actual = result;
				valueResolvingResult.actualResolved = true;
			} else {
				valueResolvingResult.actualResolved = false;
				valueResolvingResult.message = "The json path '" + key + "' return an object of type "
						+ (result == null ? "null" : result.getClass().getSimpleName()) + " which is not supported for operator " + operator.name();
			}
		} catch (PathNotFoundException e) {
			valueResolvingResult.message = e.getMessage();
		}
		return valueResolvingResult;
	}

	private ValueResolvingResult resolveSimpleValue(JsonObject outputJson, String key, AssertOperator operator) {
		ValueResolvingResult result = new ValueResolvingResult();
		if (outputJson.containsKey(key)) {
			JsonValue jsonValue = outputJson.get(key);
			if (jsonValue == null) {
				result.actual = null;
				result.actualResolved = true;
			} else if (jsonValue.getValueType() == JsonValue.ValueType.STRING) {
				result.actual = outputJson.getString(key);
				result.actualResolved = true;
			} else if (jsonValue.getValueType() == JsonValue.ValueType.NUMBER) {
				result.actual = outputJson.getJsonNumber(key).numberValue();
				result.actualResolved = true;
			} else if (jsonValue.getValueType() == JsonValue.ValueType.FALSE || jsonValue.getValueType() == JsonValue.ValueType.TRUE) {
				result.actual = outputJson.getBoolean(key);
				result.actualResolved = true;
			} else {
				result.message = "Type of " + key + " (" + jsonValue.getValueType() + ") is not supported";
				result.actualResolved = false;
			}

			if(result.actualResolved){
				if(!getOperatorHandler(operator).isSupported(result.actual)) {
					result.message = "Type of " + key + " ("
							+ (result.actual == null ? "null" : result.actual.getClass().getSimpleName())
							+ ") is not supported for operator " + operator;
					result.actualResolved = false;
				}
			}

		} else {
			result.message = "Unable to execute assertion. The keyword output doesn't contain the attribute '" + key + "'";
		}
		return result;
	}

	private AssertResult applyOperator(String key, Object actual, String expectedValueString, boolean negate, AssertOperator operator) {
		return getOperatorHandler(operator).apply(key, actual, expectedValueString, negate);
	}

	@Override
	public AssertReportNode createReportNode_(ReportNode parentNode, Assert artefact) {
		return new AssertReportNode();
	}

	private AssertOperatorHandler getOperatorHandler(AssertOperator operator) {
		AssertOperatorHandler handler = operatorHandlers.get(operator);
		if (handler == null) {
			throw new IllegalStateException("Handler is not defined for operator " + operator);
		}
		return handler;
	}

	private static class ValueResolvingResult {
		private Object actual = null;
		private boolean actualResolved = false;
		private String message = null;
	}

}
